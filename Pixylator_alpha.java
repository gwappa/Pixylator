
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.measure.ResultsTable;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.io.OpenDialog;

import java.util.Properties;
import java.util.List;
import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;

import java.awt.Rectangle;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.WindowEvent;

import lab.proj.chaos.colortrack.*;

/**
*   TODO: save configs for OutputControl
*   TODO: reorganize what to import
*/
public class Pixylator_alpha extends JFrame
    implements  ij.plugin.PlugIn,
                java.awt.event.WindowListener,
                java.awt.event.ActionListener,
                ActionDelegate,
                ImageSelectionListener,
                PixylatorDirectives
{
    private static final long serialVersionUID = 10L;

    // general configuration
    private static final boolean   DEBUG                = false;
    private static final boolean   DISABLE_UNPREPARED   = true;

    // GUI configuration
    static final int    INITIAL_WIDTH           = 900;
    static final int    INITIAL_HEIGHT          = 550;
    static final int    SEPARATOR_WEIGHT        = 1;
    static final int    SLIDER_WEIGHT           = 3;
    static final int    BITMASK8                = 0xFF;
    static final String DEFAULT_RESULTS_WINDOW  = "Results";
    static final String DEFAULT_MASK_WINDOW     = "Mask";

    // singleton management
    private static boolean          initialized     = false;
    private static Pixylator_alpha  singleton       = null;


    // GUI parts
    FrameControl        _frame      = new FrameControl();
    ROIControl          _roi        = new ROIControl();
    HistogramControl    _sampling   = new HistogramControl();
    HueHistogram        _histo      = new HueHistogram();
    HueMaskControl[]    _masks      = new HueMaskControl[MASK_CAPACITY];
    MeasurementControl  _measure    = new MeasurementControl();
    OutputControl       _output     = new OutputControl();
    JButton             _run        = new JButton("Run");
    JButton             _save       = new JButton("Save...");
    JButton             _load       = new JButton("Load...");

    ImageSelector       _images     = new ImageSelector();
    HistogramGeneration  _histogen   = null;
    Pixylation          _tracker    = null;

    List<Measurement>   _calculators = null;
    Thread              _tracking   = null;
    Thread              _generation = null;


    // reference image
    ImagePlus    _ip;

    // the buffer for calulation of centroid etc.
    // CoordinateBuffer [] _buf         = new CoordinateBuffer[MASK_CAPACITY];

    // image and ROI setting
    int _imagewidth, _imageheight, _slicenumber, _slicestart, _slicestop, _nprocess, _offsetx, _offsety, _runwidth, _runheight;

    // output settings
    // boolean _calc_mask, _calc_cent, _calc_minmax, _calc_angle, _calc_line, _calc_results;

    // results pointers
    // ResultsTable _results;
    // ImagePlus    _maskout;


    public Pixylator_alpha()
    {
        super("Pixylator Control");
        addWindowListener(this);

        setupMasks();
        setupUI();
        _tracker    = new Pixylation(_masks);
        _histogen   = new HistogramGeneration(this, _histo);

        _frame.addParameterListener(_tracker, ROLE_FRAME_CONTROL);
        _frame.addParameterListener(_histogen, ROLE_FRAME_CONTROL);
        _roi.addParameterListener(_tracker, ROLE_ROI_CONTROL);
        _roi.addParameterListener(_histogen, ROLE_ROI_CONTROL);
        _sampling.addParameterListener(_histogen, ROLE_SAMPLING_CONTROL);
        _images.addImageSelectionListener(this);
    }

    protected void forceUpdateCurrentImage(){
        _images.update();
        this.selectedImageChanged(_images.getSelectedImage());
    }

    /**
    *   a general exception handler. writes the stack trace to ImageJ log window.
    */
    private void logException(Exception e)
    {
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        IJ.log(w.toString());
    }

    /**
    *   PlugIn implementation.
    *   initialize the singleton (if not yet) and shows it.
    *   if a color image is already there, shows a Pixylation preview window.
    */
    @Override
    public void run(String arg)
    {
        if( singleton == null ){
            singleton = new Pixylator_alpha();
        }
        singleton.forceUpdateCurrentImage();
        singleton.setVisible(true);
    }

    /**
    *   initializes HueMaskControl objects.
    */
    private void setupMasks()
    {
        _masks[0] = new HueMaskControl("Magenta");
        _masks[0].setOnset(275);
        _masks[0].setOffset(360);
        _masks[1] = new HueMaskControl("Green");
        _masks[1].setOnset(120);
        _masks[1].setOffset(180);
    }

    /**
    *   initializes UI elements.
    */
    private void setupUI()
    {
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(5, 10, 20, 10));
        setContentPane(content);

        GridBagLayout layout = new GridBagLayout();
        content.setLayout(layout);
        int lastline = 2*HueMaskControl.UI_GRID_HEIGHT + 7; // target, separator, frame, roi(2)

        GridBagConstraints con = new GridBagConstraints();

        // configure target image (0, 0, all, 1)
        JComboBox<String> imagebox = new JComboBox<String>(_images);
        JPanel image_selection = new JPanel();
        image_selection.setLayout(new BoxLayout(image_selection, BoxLayout.LINE_AXIS));
        image_selection.add(new JLabel("Target image: "));
        image_selection.add(imagebox);
        image_selection.add(Box.createHorizontalGlue());
        con.gridx       = 0;
        con.gridy       = 0;
        con.gridwidth   = GridBagConstraints.REMAINDER;
        con.gridheight  = 1;
        con.anchor      = GridBagConstraints.LINE_START;
        con.fill        = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(image_selection, con);
        content.add(image_selection);

        // separator below the image selector (0, 1, all, 1)
        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        con.gridx       = 0;
        con.gridy       = 1;
        con.gridwidth   = GridBagConstraints.REMAINDER;
        con.gridheight  = 1;
        con.weighty     = SEPARATOR_WEIGHT;
        con.anchor      = GridBagConstraints.LINE_START;
        con.fill        = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(sep, con);
        content.add(sep);

        // configure FrameControl (0, 2, 2, 1)
        _frame.setDelegate(this);
        con.gridx       = 0;
        con.gridy       = 2;
        con.gridwidth   = 2;
        con.gridheight  = 1;
        con.weighty     = 3;
        con.anchor      = GridBagConstraints.CENTER;
        con.fill        = GridBagConstraints.BOTH;
        layout.setConstraints(_frame, con);
        content.add(_frame);

        // configure ROIControl (0, 3, 2, 2)
        _roi.setDelegate(this);
        con.gridx       = 0;
        con.gridy       = 3;
        con.gridwidth   = 2;
        con.gridheight  = 2;
        con.weighty     = 4;
        con.anchor      = GridBagConstraints.CENTER;
        con.fill        = GridBagConstraints.BOTH;
        layout.setConstraints(_roi, con);
        content.add(_roi);

        // configure histogram setting (0+slider, 2, 1, 1)
        _sampling.setDelegate(this);
        layoutAboveSlider(_sampling, content, layout, 0, 2, 1, 1, GridBagConstraints.LAST_LINE_START, GridBagConstraints.HORIZONTAL);

        // configure histogram (0+slider, 3, 1, 2)
        layoutAboveSlider(_histo, content, layout, 0, 3, 2, 4, GridBagConstraints.CENTER, GridBagConstraints.BOTH);

        // configure measurements (4, 2, 1, 3)
        _measure.addMeasurement(new CentroidCalculator());
        _measure.addMeasurement(new CMCalculator());
        layoutOutputUI(_measure, content, layout, HueMaskControl.UI_GRID_WIDTH, 2, 2, 3);

        // configure outputs (4, 5, 1, 5)
        layoutOutputUI(_output, content, layout, HueMaskControl.UI_GRID_WIDTH, 3, 2, 3);

        // configure MaskControl UI
        HueMaskControl.layoutUI(_masks[0], content, layout, 0, 6);
        HueMaskControl.layoutUI(_masks[1], content, layout, 0, 6 + HueMaskControl.UI_GRID_HEIGHT);

        // separator above the command box (0, last-1, all, 1)
        sep = new JSeparator(JSeparator.HORIZONTAL);
        con.gridx       = 0;
        con.gridy       = lastline - 1;
        con.gridwidth   = GridBagConstraints.REMAINDER;
        con.gridheight  = 1;
        con.weighty     = SEPARATOR_WEIGHT;
        con.anchor      = GridBagConstraints.LINE_START;
        con.fill        = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(sep, con);
        content.add(sep);

        // configure command box (0, last, all, 1)
        _run.setActionCommand(RUN_PIXYLATOR);
        _run.addActionListener(this);
        _save.setActionCommand(SAVE_CONFIGS);
        _save.addActionListener(this);
        _load.setActionCommand(LOAD_CONFIGS);
        _load.addActionListener(this);

        con.gridx       = HueMaskControl.UI_GRID_WIDTH+1;
        con.gridy       = lastline;
        con.gridwidth   = 1;
        con.gridheight  = 1;
        con.weightx     = 1;
        con.weighty     = 1;
        con.fill        = GridBagConstraints.HORIZONTAL;
        con.anchor      = GridBagConstraints.LINE_END;
        layout.setConstraints(_run, con);
        content.add(_run);

        JPanel configs = new JPanel();
        configs.setLayout(new BoxLayout(configs, BoxLayout.LINE_AXIS));
        configs.add(_load);
        configs.add(_save);
        con.gridx       = 0;
        con.gridy       = lastline;
        con.gridwidth   = 3;
        con.gridheight  = 1;
        con.weightx     = 1;
        con.weighty     = 1;
        con.anchor      = GridBagConstraints.LINE_START;
        layout.setConstraints(configs, con);
        content.add(configs);

        if( DEBUG ){
            for(int i=0; i<361; i++)
            {
                _histo.add(i, i);
            }
        }
    }

    /**
    *   a subroutine for laying out the OutputSelector.
    */
    private void layoutOutputUI(JPanel out, Container content, GridBagLayout layout,
        int offsetx, int offsety, int colspan, int height)
    {
        GridBagConstraints con = new GridBagConstraints();

        con.gridx       = offsetx;
        con.gridy       = offsety;
        con.gridwidth   = 2;
        con.gridheight  = colspan;
        con.anchor      = GridBagConstraints.FIRST_LINE_START;
        con.weightx     = 3;
        con.weighty     = height;
        con.fill        = GridBagConstraints.BOTH;
        layout.setConstraints(out, con);
        content.add(out);
    }

    /**
    *   a subroutine to lay out components above the slider of HueMaskControl's.
    */
    private void layoutAboveSlider(JComponent comp, Container content, GridBagLayout layout,
        int offsetx, int offsety, int rowspan, int height, int anchor, int fill)
    {
        GridBagConstraints con = new GridBagConstraints();

        con.gridx       = offsetx + HueMaskControl.UI_SLIDER_OFFSETX;
        con.gridy       = offsety + 0;
        con.gridwidth   = 1;
        con.gridheight  = rowspan;
        con.anchor      = anchor;
        con.fill        = fill;
        con.weightx     = HueMaskControl.UI_SLIDER_WEIGHT;
        con.weighty     = height;
        layout.setConstraints(comp, con);
        content.add(comp);
    }

    /**
    *   ActionListener implementation. calls performAction() with the action command.
    */
    @Override
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        performAction(e.getActionCommand());
    }

    /**
    *   lets Pixylator perform a command.
    *
    *   run_pixylator       runs a tracking process (see Pixylator.runTracking()).
    *   replot_histogram    lets the histogram to replot itself (see Pixylator.updateHistogram()).
    *   current_roi         sets the ROI settings according to the current ROI in ImageJ
    *                       (see Pixylator.updateWithROI()).
    *   reset_roi           resets the ROI settings, back to the full-field of the current image
    *                       (see Pixylator.resetROI()).
    *   set_frame_start     sets the current frame to be the start frame (see FrameControl.setStart()).
    *   set_frame_stop      sets the current frame to be the stop frame (see FrameControl.setStop()).
    *   set_frame_all       sets to process all the frames according to the current image.
    *                       (see FrameControl.setStart()/setStop()).
    *   load_configs
    *   save_configs
    */
    @Override
    public void performAction(String command)
    {
        if( command.equals(RUN_PIXYLATOR) )
        {
            runTracking();
            return;
        } else if ( command.equals(HistogramControl.REPLOT_HISTOGRAM) ) {
            runHistogramGeneration();
            return;
        } else if ( command.equals(ROIControl.CURRENT_ROI) ) {
            updateWithROI();
        } else if ( command.equals(ROIControl.EXPORT_ROI) ) {
            exportCurrentROI();
        } else if ( command.equals(ROIControl.RESET_ROI) ) {
            resetROI();
        } else if ( command.equals(FrameControl.SET_FRAME_START) ) {
            _frame.setStart(_ip.getCurrentSlice());
        } else if ( command.equals(FrameControl.SET_FRAME_STOP) ) {
            _frame.setStop(_ip.getCurrentSlice());
        } else if ( command.equals(FrameControl.SET_FRAME_ALL) ) {
            _frame.setStart(1);
            _frame.setStop(_ip.getStackSize());
        } else if ( command.equals(SAVE_CONFIGS) ) {
            saveConfigs();
        } else if ( command.equals(LOAD_CONFIGS) ) {
            loadConfigs();
        } else
            IJ.showMessage("Pixylator error","command not found: "+command);
    }

    /**
    *   the worker for "Run" button.
    *   Activates the tracker thread.
    */
    public void runTracking()
    {
        if( DEBUG ){
            IJ.log("Pixylator alpha: preparing for tracking...");
        }
        resetTrackingThread();
        _tracker.clearListeners();
        _tracker.setMeasurements(_measure.getEnabledMeasurements());
        _tracker.addMaskOutput(_output.getMaskOutput());
        _tracker.setMeasurementOutput(_output.getMeasurementOutput());
        if( DEBUG ){
            IJ.log("Pixylator alpha: starting Pixylation...");
        }
        _tracking.start();
    }

    /**
    *   called from runTracking().
    *   waits for previous thread to terminate and renews it.
    */
    private void resetTrackingThread(){
        if( (_tracking != null) && _tracking.isAlive() ){
            try{
                _tracking.interrupt();
                _tracking.join();
            } catch (InterruptedException e) {
                IJ.error("Pixylator error", "Pixylator could not abort tracking. May be a bug?");
            }
        }
        _tracking = new Thread(_tracker, "Pixylation");
        try {
            _tracking.setPriority(Thread.MAX_PRIORITY-1);
        } catch (Exception e) {
            // just give up
        }
        if( DEBUG ){
            IJ.log("Pixylator alpha: new tracker thread is created.");
        }
    }

    public void runHistogramGeneration()
    {
        resetHistogramGenerationThread();
        _generation.start();
    }

    public void resetHistogramGenerationThread(){
        if( (_generation != null) && _generation.isAlive() ){
            try{
                _generation.interrupt();
                _generation.join();
            } catch (InterruptedException e) {
                IJ.error("Pixylator error", "Pixylator could not abort histogram generation. May be a bug?");
            }
        }
        _generation = new Thread(_histogen, "HistogramGeneration");
        try {
            _generation.setPriority(Thread.MAX_PRIORITY-1);
        } catch (Exception e) {
            // just give up
        }
    }

    /**
    *   the worker for "Current ROI" button.
    *   updates Pixylator's ROI settings with those of current ROI in ImageJ,
    *   by eventually calling Pixylator.setROI() method.
    */
    public void updateWithROI()
    {
        Roi roi = _ip.getRoi();
        if( roi == null ){
            setRoi(0, 0, _imagewidth, _imageheight);
        } else {
            Rectangle r = roi.getBounds();
            setRoi(r.x, r.y, r.width, r.height);
        }
    }

    /**
    *   a subroutine for updateWithROI() that sets Pixylator's ROI settings
    *   (actually those of its child ROIControl object).
    */
    public void setRoi(int x, int y, int width, int height)
    {
        _roi.setRoiX(x);
        _roi.setRoiY(y);
        _roi.setRoiWidth(width);
        _roi.setRoiHeight(height);
    }

    /**
    *   the worker for "Reset ROI" button.
    *   resets Pixylator's ROI settings to full field of view of the current image in ImageJ,
    *   by eventually calling Pixylator.setROI() method.
    */
    public void resetROI()
    {
        setRoi(0, 0, _ip.getWidth(), _ip.getHeight());
    }

    /**
    *   the worker for "Show ROI" button.
    *   sets the ROI of ImageJ to be Pixylator's current ROI settings.
    */
    public void exportCurrentROI()
    {
        _ip.setRoi(new Rectangle(_roi.getRoiX(), _roi.getRoiY(), _roi.getRoiWidth(), _roi.getRoiHeight()));
    }

    /**
    *   the worker for "Save" button. saves the frame/ROI/histogram/mask/output configurations into a file.
    */
    public void saveConfigs()
    {
        Properties properties = new Properties();
        if(!_frame.saveConfigs(properties))     IJ.log("***frame settings were not stored properly.");
        if(!_roi.saveConfigs(properties))       IJ.log("***ROI settings were not stored properly.");
        if(!_sampling.saveConfigs(properties))  IJ.log("***Histogram settings were not stored properly.");
        for(int i=0; i<MASK_CAPACITY; i++)
            if(!_masks[i].saveConfigs(properties, i)) IJ.log("***Mask settings #"+String.valueOf(i)+" were not stored properly.");
        if(!_measure.saveConfigs(properties))    IJ.log("***Measurement settings were not stored properly.");

        SaveDialog dialog = new SaveDialog("Save configs...", "configs", ".pxyl");
        String dir = dialog.getDirectory();
        String name = dialog.getFileName();
        if( name == null ){
            return;
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(dir, name));
            properties.store(out, "Pixylator (alpha) configuration file");

        } catch (IOException e) {
            logException(e);
        } finally {
            if( out != null ){
                try {
                    out.close();
                } catch (IOException e2) {
                    // do nothing
                }
            }
        }
        IJ.showStatus("saved to: "+name);
    }

    /**
    *   the worker for "Load" button. loads the frame/ROI/histogram/mask/output configurations from a file.
    */
    public void loadConfigs()
    {
        Properties properties = new Properties();

        OpenDialog dialog = new OpenDialog("Open configs...");
        String dir = dialog.getDirectory();
        String name = dialog.getFileName();
        if( name == null ){
            return;
        } else if ( !name.endsWith(".pxyl") &&
                    !IJ.showMessageWithCancel("May not be a Pixylator file", "Do you really wish to open '"+name+"'?") ){
            return;
        }

        FileInputStream in = null;
        try {
            in = new FileInputStream(new File(dir, name));
            properties.load(in);
        } catch (IOException e) {
            logException(e);
            return;
        } finally {
            if( in != null ){
                try {
                    in.close();
                } catch (IOException e2) {
                    // do nothing
                }
            }
        }

        if(!_frame.loadConfigs(properties))     IJ.log("***frame settings were not loaded properly.");
        if(!_roi.loadConfigs(properties))       IJ.log("***ROI settings were not loaded properly.");
        if(!_sampling.loadConfigs(properties))  IJ.log("***Histogram settings were not loaded properly.");
        for(int i=0; i<MASK_CAPACITY; i++)
            if(!_masks[i].loadConfigs(properties, i)) IJ.log("***Mask settings #"+String.valueOf(i)+" were not loaded properly.");
        if(!_measure.loadConfigs(properties))    IJ.log("***Measurement settings were not loaded properly.");

        IJ.showStatus("loaded from: "+name);
    }


    @Override
    public void selectedImageChanged(ImagePlus image){
        _ip = image;
        _tracker.selectedImageChanged(image);
        _histogen.selectedImageChanged(image);
    }

    /**
    *   called whenever the user opens/activates Pixylator window,
    *   including when opening for the first time and resuming from its minimized state.
    */
    @Override
    public void windowActivated(WindowEvent e){
        if( DEBUG )
            IJ.log("activated Pixylator.");
        _images.update();
    }

    /**
    *   probably not called until the end of ImageJ.
    */
    @Override
    public void windowClosed(WindowEvent e){
        if( DEBUG )
            IJ.log("closed Pixylator.");
    }

    /**
    *   probably not called until the end of ImageJ.
    */
    @Override
    public void windowClosing(WindowEvent e){
        // do nothing for now
    }

    /**
    *   called when the user closes/deactivates the Pixylator control,
    *   including when the window is minimized.
    */
    @Override
    public void windowDeactivated(WindowEvent e){
        if( DEBUG )
            IJ.log("deactivated Pixylator.");
    }

    /**
    *   called when the user resumes the Pixylator control from its minimized state.
    */
    @Override
    public void windowDeiconified(WindowEvent e){
        if( DEBUG )
            IJ.log("deiconified Pixylator.");
    }

    /**
    *   called when the user minimizes the Pixylator control.
    */
    @Override
    public void windowIconified(WindowEvent e){
        if( DEBUG )
            IJ.log("iconified Pixylator.");
    }

    /**
    *   called only on the very first call of "run".
    */
    @Override
    public void windowOpened(WindowEvent e){
        if( DEBUG )
            IJ.log("opened Pixylator.");
    }


    public static void main(String [] args)
    {
        Pixylator_alpha frame = new Pixylator_alpha();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
