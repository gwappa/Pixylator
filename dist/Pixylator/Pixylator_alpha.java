
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.measure.ResultsTable;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.io.OpenDialog;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;

import java.awt.Rectangle;
import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

public class Pixylator_alpha extends JFrame
    implements  ActionDelegate,
                java.awt.event.ActionListener,
                ij.plugin.PlugIn
{
    private static final boolean   DEBUG = false;
    private static final boolean   DISABLE_UNPREPARED = true;
    private static Pixylator_alpha singleton = null;

    static final String DEFAULT_RESULTS_WINDOW  = "Results";
    static final String DEFAULT_MASK_WINDOW     = "Mask";

    static final String RUN_PIXYLATOR   = "run_pixylator";
    static final String SAVE_CONFIGS    = "save_configs";
    static final String LOAD_CONFIGS    = "load_configs";

    static final String OUTPUT_TYPE_CENTROID    = "centroid";
    static final String OUTPUT_TYPE_MINMAX      = "minmax";
    static final String OUTPUT_TYPE_ANGLE       = "angle";
    static final String OUTPUT_TYPE_LINE        = "line";
    static final String OUTPUT_TYPE_MASK        = "mask";

    static final int MASK_CAPACITY  = 2;
    static final int INITIAL_WIDTH  = 800;
    static final int INITIAL_HEIGHT = 550;
    static final int SLIDER_WEIGHT  = 3;
    static final int BITMASK8       = 0xFF;


    // GUI parts
    FrameControl        _frame      = new FrameControl();
    ROIControl          _roi        = new ROIControl();
    HistogramControl    _sampling   = new HistogramControl();
    HueHistogram        _histo      = new HueHistogram();
    HueMaskControl[]    _masks      = new HueMaskControl[MASK_CAPACITY];
    OutputSelector      _output     = new OutputSelector();
    JButton             _run        = new JButton("Run");
    JButton             _save       = new JButton("Save...");
    JButton             _load       = new JButton("Load...");


    // reference image
    ImagePlus    _ip;

    // the buffer for calulation of centroid etc.
    CoordinateBuffer [] _buf         = new CoordinateBuffer[MASK_CAPACITY];

    // image and ROI setting
    int _imagewidth, _imageheight, _slicenumber, _slicestart, _slicestop, _nprocess, _offsetx, _offsety, _runwidth, _runheight;

    // output settings
    boolean _calc_mask, _calc_cent, _calc_minmax, _calc_angle, _calc_line, _calc_results;

    // results pointers
    ResultsTable _results;
    ImagePlus    _maskout;

    public Pixylator_alpha()
    {
        super("Pixylator Control");
        initOutputOptions();
        setupMasks();
        setupUI();
    }

    private void initOutputOptions()
    {
        _output.addOption(OUTPUT_TYPE_CENTROID, "Centroid", true);
        _output.addOption(OUTPUT_TYPE_MINMAX, "Min/Max", true);
        _output.addOption(OUTPUT_TYPE_LINE, "Line coefficients", true);
        _output.addOption(OUTPUT_TYPE_ANGLE, "Angle", true);
        _output.addOption(OUTPUT_TYPE_MASK, "Mask image(s)", true);
    }

    /**
    *   the worker for "Run" button. Activate the tracker thread.
    */
    public void runTracking()
    {
        Thread t = new Thread(new Tracker());
        t.start();
    }

    public void saveConfigs()
    {
        Properties properties = new Properties();
        if(!_frame.saveConfigs(properties))     IJ.log("***frame settings were not stored properly.");
        if(!_roi.saveConfigs(properties))       IJ.log("***ROI settings were not stored properly.");
        if(!_sampling.saveConfigs(properties))  IJ.log("***Histogram settings were not stored properly.");
        for(int i=0; i<MASK_CAPACITY; i++)
            if(!_masks[i].saveConfigs(properties, i)) IJ.log("***Mask settings #"+String.valueOf(i)+" were not stored properly.");
        if(!_output.saveConfigs(properties))    IJ.log("***Output settings were not stored properly.");

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
        if(!_output.loadConfigs(properties))    IJ.log("***Output settings were not loaded properly.");

        IJ.showStatus("loaded from: "+name);
    }

    private void logException(Exception e)
    {
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        IJ.log(w.toString());
    }

    /**
    *   called with each slice of images during tracking
    */
    // TODO: do i actually need "show"?
    public void trackOnSlice(int slice, boolean show) 
        throws ClassCastException
    {
        if( DEBUG )
            IJ.log("running on slice: "+String.valueOf(slice));

        ColorProcessor imp = null;
        ColorProcessor mmp = null;

        if( _slicenumber > 1 ){
            // TODO: maybe it is better to see the progress 'visually'...
            imp = (ColorProcessor)(_ip.getStack().getProcessor(slice));
            if( _calc_mask ){
                mmp = (ColorProcessor)(_maskout.getStack().getProcessor(slice));
            }
        } else {
            imp = (ColorProcessor)(_ip.getProcessor());
            if( _calc_mask ){
                mmp = (ColorProcessor)(_maskout.getProcessor());
            }
        }

        for(int i=0; i<MASK_CAPACITY; i++)
            _buf[i].reset();

        // iterate: read pixel, add to buffer
        int xx, yy;
        int[] rgb = {-1, -1, -1};
        int hue;
        for(int x=0; x<_runwidth; x++)
        {
            xx = x + _offsetx;
            for(int y=0; y<_runheight; y++)
            {
                yy  = y + _offsety;
                rgb = imp.getPixel(xx, yy, rgb);
                hue = Hue.fromRGB(rgb[0], rgb[1], rgb[2]);

                for(int i=0; i<MASK_CAPACITY; i++)
                {
                    if( _masks[i].contains(hue) )
                    {
                        _buf[i].add(xx, yy);
                        if( _calc_mask )
                        {
                            mmp.set(xx, yy, _masks[i].getRGB());
                        }
                    }
                } 
            }
        }

        // output: depending on output settings
        if( DEBUG ){
            IJ.log("output for slice: "+String.valueOf(slice));
            IJ.log("counter: "+String.valueOf(_results.getCounter()));
        }

        String roiname;

        for(int i=0; i<MASK_CAPACITY; i++)
        {
            roiname = _masks[i].getName();
            if( _calc_cent )
            {
                _results.addValue(roiname+"_X", _buf[i].getMeanX());
                _results.addValue(roiname+"_Y", _buf[i].getMeanY());
                if(DEBUG)
                    IJ.log("meanX: "+String.valueOf(_buf[i].getMeanX()));
            }
            if( _calc_minmax )
            {
                _results.addValue(roiname+"_minX", _buf[i].getMinX());
                _results.addValue(roiname+"_maxX", _buf[i].getMaxX());
                _results.addValue(roiname+"_minY", _buf[i].getMinY());
                _results.addValue(roiname+"_maxY", _buf[i].getMaxY());
            }
            if( _calc_angle ){
                _results.addValue(roiname+"_rad", _buf[i].getRadian());
                _results.addValue(roiname+"_deg", _buf[i].getDegrees());
            }
            if( _calc_line ){
                _results.addValue(roiname+"_linA", _buf[i].getA());
                _results.addValue(roiname+"_linB", _buf[i].getB());
            }
        }

        if( DEBUG )
            IJ.log("done with slice: "+String.valueOf(slice));
    }

    /**
    *   called at the beginning of tracking.
    */
    protected void initResults()
    {
        if( DEBUG ){
            IJ.log("initializing");
            IJ.log("image width: "+String.valueOf(_imagewidth));
            IJ.log("image height: "+String.valueOf(_imageheight));
            IJ.log("slice number: "+String.valueOf(_slicenumber));
        }

        _calc_mask      = _output.getOption(OUTPUT_TYPE_MASK);
        _calc_cent      = _output.getOption(OUTPUT_TYPE_CENTROID);
        _calc_minmax    = _output.getOption(OUTPUT_TYPE_MINMAX);
        _calc_angle     = _output.getOption(OUTPUT_TYPE_ANGLE);
        _calc_line      = _output.getOption(OUTPUT_TYPE_LINE);
        _calc_results   = _calc_cent || _calc_minmax || _calc_angle || _calc_line;

        if( _calc_results ){
            _results = new ResultsTable();
        } else {
            _results = null;
        }

        if( _calc_mask ){
            _maskout = IJ.createImage(DEFAULT_MASK_WINDOW, "RGB-black",
                                        _imagewidth, _imageheight, _nprocess);
        } else {
            _maskout = null;
        }

        if( DEBUG )
            IJ.log("done initializing");

        IJ.showStatus("Tracking...");
        IJ.showProgress(0, _nprocess);
    }

    /**
    *   called during tracking, at slice transitions.
    */
    protected void nextResult()
    {
        if( DEBUG )
            IJ.log("next frame...");

        if( _results != null )
            _results.incrementCounter();

    }

    /**
    *   called at the end of tracking.
    */
    protected void finalizeResults()
    {
        if( DEBUG )
            IJ.log("finalizing...");

        if( _results != null ){
            _results.updateResults();
            _results.showRowNumbers(true);
            _results.show(DEFAULT_RESULTS_WINDOW);
        }

        if( _maskout != null ){
            // IJ.log("opening mask image");
            _maskout.show();
        }
    }

    /**
    *   updates Pixylator's image information with the current image info on ImageJ.
    *   called from different callbacks. 
    */
    public void updateWithImage()
    {
        // validate: is there an image?
        _ip = IJ.getImage();

        // setup: get width, height of the image, create buffer + mask image? depending on output settings
        _imagewidth   = _ip.getWidth();
        _imageheight  = _ip.getHeight();
        _slicenumber  = _ip.getStackSize();
    }

    /**
    *   updates Pixylator's ROI settings with those of current ROI in ImageJ,
    *   by eventually calling Pixylator.setROI() method.
    */
    public void updateWithROI()
    {
        updateWithImage();
        Roi roi = _ip.getRoi();
        if( roi == null ){
            setRoi(0, 0, _imagewidth, _imageheight);
        } else {
            Rectangle r = roi.getBounds();
            setRoi(r.x, r.y, r.width, r.height);
        }
    }

    /**
    *   sets the ROI of ImageJ to be Pixylator's current ROI settings.
    */
    public void exportCurrentROI()
    {
        updateWithImage();
        _ip.setRoi(new Rectangle(_roi.getRoiX(), _roi.getRoiY(), _roi.getRoiWidth(), _roi.getRoiHeight()));
    }

    /**
    *   updates Pixylator's bounds info with those of child GUI components.
    *   a shorthand for initProcessBounds(use_all_frames=false, process_full_field=false)
    */
    protected boolean initProcessBounds()
    {
        return initProcessBounds(false, false);
    }

    /**
    *   updates Pixylator's bounds info with those of child GUI components.
    *
    *   @param use_all_frames       whether or not all frames are used to define the bounds
    *   @param process_full_field   whether or not the full field is used as the bounds
    */
    protected boolean initProcessBounds(boolean use_all_frames, boolean process_full_field)
    {
        if( process_full_field ){
            _offsetx    = 0;
            _offsety    = 0;
            _runwidth   = _imagewidth;
            _runheight  = _imageheight;
        } else {
            _offsetx    = _roi.getRoiX();
            _offsety    = _roi.getRoiY();
            _runwidth   = _roi.getRoiWidth();
            _runheight  = _roi.getRoiHeight();
        }

        if( use_all_frames ){
            _slicestart = 1;
            _slicestop  = _slicenumber;
            _nprocess   = _slicenumber;
        } else {
            _slicestart = _frame.getStart();
            _slicestop  = _frame.getStop();

            if( _slicestart < 1 ){
                _slicestart = 1;
            } else if( _slicestart > _slicenumber ){
                IJ.showMessage("Error on frame settings", "Starting slice "+String.valueOf(_slicestart)+" is too large");
                return false;
            }

            if ( _slicestop > _slicenumber ){
                _slicestop = _slicenumber;
            }
            _nprocess   = _slicestop - _slicestart + 1;
        }
        return true;
    }

    /**
    *   sets Pixylator's ROI settings (actually those of its child ROIControl object)
    */
    public void setRoi(int x, int y, int width, int height)
    {
        _roi.setRoiX(x);
        _roi.setRoiY(y);
        _roi.setRoiWidth(width);
        _roi.setRoiHeight(height);
    }

    /**
    *   resets Pixylator's ROI settings to full field of view of the current image in ImageJ,
    *   by eventually calling Pixylator.setROI() method.
    */
    public void resetROI()
    {
        updateWithImage();
        setRoi(0, 0, _imagewidth, _imageheight);
    }

    public void updateHistogram(boolean use_all_frames, boolean process_full_field)
    {
        Thread t = new Thread(new HistogramUpdater(use_all_frames, process_full_field));
        t.start();
    }

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
            updateHistogram(false, false);
        } else if ( command.equals(ROIControl.CURRENT_ROI) ) {
            updateWithROI();
        } else if ( command.equals(ROIControl.EXPORT_ROI) ) {
            exportCurrentROI();
        } else if ( command.equals(ROIControl.RESET_ROI) ) { 
            resetROI();
        } else if ( command.equals(FrameControl.SET_FRAME_START) ) {
            updateWithImage();
            _frame.setStart(_ip.getCurrentSlice());
        } else if ( command.equals(FrameControl.SET_FRAME_STOP) ) {
            updateWithImage();
            _frame.setStop(_ip.getCurrentSlice());
        } else if ( command.equals(FrameControl.SET_FRAME_ALL) ) {
            updateWithImage();
            _frame.setStart(1);
            _frame.setStop(_slicenumber);
        } else if ( command.equals(SAVE_CONFIGS) ) {
            saveConfigs();  
        } else if ( command.equals(LOAD_CONFIGS) ) {
            loadConfigs();
        } else
            IJ.showMessage("Pixylator error","command not found: "+command);
    }

    @Override
    public void run(String arg)
    {
        if( singleton == null ){
            singleton = new Pixylator_alpha();
            singleton.performAction(FrameControl.SET_FRAME_ALL);
            singleton.resetROI();
        }

        try{
            singleton.setMenuBar(IJ.getInstance().getMenuBar());
        } catch (NullPointerException e) {
            // do nothing
        }
        singleton.updateHistogram(true, true);
        singleton.setVisible(true);
    }

    private void setupUI()
    {
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(5, 10, 20, 10));
        setContentPane(content);

        GridBagLayout layout = new GridBagLayout();
        content.setLayout(layout);
        int lastline = 2*HueMaskControl.UI_GRID_HEIGHT + 2;


        layoutAboveSlider(_histo, content, layout, 0, 1, 3, GridBagConstraints.CENTER, GridBagConstraints.BOTH);
        HueMaskControl.layoutUI(_masks[0], content, layout, 0, 2);
        HueMaskControl.layoutUI(_masks[1], content, layout, 0, 2 + HueMaskControl.UI_GRID_HEIGHT);
        layoutOutputUI(_output, content, layout, HueMaskControl.UI_GRID_WIDTH, 0, HueMaskControl.UI_GRID_HEIGHT*2+2);

        _run.setActionCommand(RUN_PIXYLATOR);
        _run.addActionListener(this);
        _save.setActionCommand(SAVE_CONFIGS);
        _save.addActionListener(this);
        _load.setActionCommand(LOAD_CONFIGS);
        _load.addActionListener(this);

        GridBagConstraints con = new GridBagConstraints();
        con.gridx       = HueMaskControl.UI_GRID_WIDTH;
        con.gridy       = lastline;
        con.gridwidth   = 2;
        con.gridheight  = 1;
        con.weightx     = 1;
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
        con.anchor      = GridBagConstraints.LINE_START;
        layout.setConstraints(configs, con);
        content.add(configs);

        _sampling.setDelegate(this);
        _sampling.setBorder(BorderFactory.createTitledBorder("Histogram settings:"));
        layoutAboveSlider(_sampling, content, layout, 0, 0, 2, GridBagConstraints.LAST_LINE_START, GridBagConstraints.HORIZONTAL);

        _frame.setDelegate(this);
        _frame.setBorder(BorderFactory.createTitledBorder("Frame settings:"));
        con.gridx       = 0;
        con.gridy       = 0;
        con.gridwidth   = 2;
        con.gridheight  = 1;
        con.anchor      = GridBagConstraints.CENTER;
        con.fill        = GridBagConstraints.BOTH;
        layout.setConstraints(_frame, con);
        content.add(_frame);

        _roi.setDelegate(this);
        _roi.setBorder(BorderFactory.createTitledBorder("ROI:"));
        con.gridx       = 0;
        con.gridy       = 1;
        con.gridwidth   = 2;
        con.gridheight  = 1;
        con.anchor      = GridBagConstraints.CENTER;
        con.fill        = GridBagConstraints.BOTH;
        layout.setConstraints(_roi, con);
        content.add(_roi);

        if( DEBUG ){
            for(int i=0; i<361; i++)
            {
                _histo.add(i, i);
            }
        }
    }

    private void layoutOutputUI(OutputSelector out, Container content, GridBagLayout layout, int offsetx, int offsety, int height)
    {
        GridBagConstraints con = new GridBagConstraints();

        con.gridx       = offsetx;
        con.gridy       = offsety;
        con.gridwidth   = 1;
        con.gridheight  = height;
        con.anchor      = GridBagConstraints.FIRST_LINE_START;
        con.weightx     = 2;
        con.weighty     = 1;
        con.fill        = GridBagConstraints.BOTH;
        layout.setConstraints(out, con);
        content.add(out);
    }

    private void layoutAboveSlider(JComponent comp, Container content, GridBagLayout layout, 
        int offsetx, int offsety, int height, int anchor, int fill)
    {
        GridBagConstraints con = new GridBagConstraints();

        con.gridx       = offsetx + HueMaskControl.UI_SLIDER_OFFSETX;
        con.gridy       = offsety + 0;
        con.gridwidth   = 1;
        con.gridheight  = 1;
        con.anchor      = anchor;
        con.fill        = fill;
        con.weightx     = HueMaskControl.UI_SLIDER_WEIGHT;
        con.weighty     = height;
        layout.setConstraints(comp, con);
        content.add(comp);
    }

    private void setupMasks()
    {
        _masks[0] = new HueMaskControl("Magenta");
        _masks[0].setOnset(275);
        _masks[0].setOffset(360);
        _masks[1] = new HueMaskControl("Green");
        _masks[1].setOnset(120);
        _masks[1].setOffset(180);

        for(int i=0; i<MASK_CAPACITY; i++)
            _buf[i] = new CoordinateBuffer();
    }

    protected class Tracker implements Runnable
    {
        @Override
        public void run(){
            updateWithImage();
            if( !initProcessBounds() ) return;
            initResults();

            // per each slice:
            for( int slice=_slicestart; slice<=_slicestop; slice++ ){
                nextResult();
                try{
                    trackOnSlice(slice, true); // throws ClassCastException
                } catch (ClassCastException e) {
                    IJ.showMessage("Process aborted", "Pixylator only accepts color images");
                    return;
                }
                IJ.showProgress(slice, _nprocess);
            }

            finalizeResults();
        }
    }

    protected class HistogramUpdater implements Runnable
    {
        boolean _use_all_frames         = false;
        boolean _process_full_field     = false;

        public HistogramUpdater(boolean use_all_frames, boolean process_full_field)
        {
            _use_all_frames     = use_all_frames;
            _process_full_field = process_full_field;
        }

        @Override
        public void run(){
            updateWithImage();
            if( !initProcessBounds(_use_all_frames, _process_full_field) ) return;

            _histo.reset();

            // initialize with sample number
            int  maxsample   = _sampling.getSampleNumber();
            int  perslice    = _runwidth*_runheight;
            long totalpixels = ((long)_nprocess)*_runwidth*_runheight;
            int  inc         = 1;
            if( totalpixels < maxsample ){
                maxsample = (int)totalpixels;
                // inc remains 1
            } else {
                // totalpixels is larger than maxsample
                inc = (int)(totalpixels/maxsample);
            }


            IJ.showStatus("Preparing histogram...");
            IJ.showProgress(0, maxsample);

            int slice=1, xpos=0, ypos=0, hue;
            ImageProcessor imp = (_slicenumber > 1)? _ip.getStack().getProcessor(1) : _ip.getProcessor();
            int[] rgb;

            for(int i=0; i<maxsample; i++)
            {
                try {
                    rgb     = _ip.getPixel(xpos, ypos);
                    hue     = Hue.fromRGB(rgb[0],rgb[1],rgb[2]);
                    _histo.add(hue);
                } catch (Exception e) {
                    logException(e);
                }

                IJ.showProgress(i, maxsample);

                // updating the position
                xpos += inc;
                if( xpos >= _imagewidth ){
                    xpos -= _imagewidth;
                    if( (++ypos) >= _imageheight ){
                        ypos -= _imageheight;
                        if( (++slice) > _nprocess ){
                            break;
                        } else {
                            // assuming that it is a stack
                            imp = _ip.getStack().getProcessor(slice);
                        }
                    }
                }
            }
            IJ.showProgress(1.0);
            _histo.paintImmediately(0, 0, _histo.getWidth(), _histo.getHeight());
        }
    }

    public static void main(String [] args)
    {
        Pixylator_alpha frame = new Pixylator_alpha();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
