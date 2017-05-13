
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.measure.ResultsTable;
import ij.gui.Roi;
import ij.gui.NewImage;

import java.util.Random;
import java.io.StringWriter;
import java.io.PrintWriter;

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
    private static final boolean   DISABLE_UNPREPARED = false;
    private static Pixylator_alpha singleton = null;
    private static Random          randomize = new Random();

    static final String DEFAULT_RESULTS_WINDOW  = "Results";
    static final String DEFAULT_MASK_WINDOW     = "Mask";

    static final String RUN_PIXYLATOR   = "run_pixylator";
    static final String SAVE_CONFIGS    = "save_configs";
    static final String LOAD_CONFIGS    = "load_configs";

    static final int MASK_CAPACITY  = 2;
    static final int INITIAL_WIDTH  = 800;
    static final int INITIAL_HEIGHT = 550;
    static final int SLIDER_WEIGHT  = 3;
    static final int BITMASK8       = 0xFF;

    FrameControl        _frame      = new FrameControl();
    ROIControl          _roi        = new ROIControl();
    HistogramControl    _sampling   = new HistogramControl();
    HueHistogram        _histo      = new HueHistogram();
    HueMaskControl[]    _masks      = new HueMaskControl[MASK_CAPACITY];
    OutputSelector      _output     = new OutputSelector();
    JButton             _run        = new JButton("Run");
    JButton             _save       = new JButton("Save...");
    JButton             _load       = new JButton("Load...");


    ImagePlus    _ip;
    CoordinateBuffer [] _buf         = new CoordinateBuffer[MASK_CAPACITY];
    int _imagewidth, _imageheight, _slicenumber, _slicestart, _slicestop, _nprocess, _offsetx, _offsety, _runwidth, _runheight;
    ResultsTable _results;
    ImagePlus    _maskout;

    public Pixylator_alpha()
    {
        super("Pixylator Control");
        setupMasks();
        setupUI();
    }

    public void runTracking()
    {
        Thread t = new Thread(new Tracker());
        t.start();
    }

    public void trackOnSlice(int slice, boolean show)
        throws ClassCastException
    {
        if( DEBUG )
            IJ.log("running on slice: "+String.valueOf(slice));

        ColorProcessor imp = null;
        ColorProcessor mmp = null;

        if( _slicenumber > 1 ){
            // TODO: maybe it is better to see the progress 'visually'...
            // _ip.setSlice(slice);
            // imp = (ColorProcessor)(_ip.getProcessor());
            imp = (ColorProcessor)(_ip.getStack().getProcessor(slice));
            if( _output.calculateMask ){
                // _maskout.setSlice(slice);
                // mmp = (ColorProcessor)(_maskout.getProcessor());
                mmp = (ColorProcessor)(_maskout.getStack().getProcessor(slice));
            }
        } else {
            imp = (ColorProcessor)(_ip.getProcessor());
            if( _output.calculateMask ){
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
                        if( _output.calculateMask )
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
            if( _output.calculateCentroid )
            {
                _results.addValue(roiname+"_X", _buf[i].getMeanX());
                _results.addValue(roiname+"_Y", _buf[i].getMeanY());
                if(DEBUG)
                    IJ.log("meanX: "+String.valueOf(_buf[i].getMeanX()));
            }
            if( _output.calculateMinMax )
            {
                _results.addValue(roiname+"_minX", _buf[i].getMinX());
                _results.addValue(roiname+"_maxX", _buf[i].getMaxX());
                _results.addValue(roiname+"_minY", _buf[i].getMinY());
                _results.addValue(roiname+"_maxY", _buf[i].getMaxY());
            }
        }

        if( DEBUG )
            IJ.log("done with slice: "+String.valueOf(slice));
    }

    protected void initResults()
    {
        if( DEBUG ){
            IJ.log("initializing");
            IJ.log("image width: "+String.valueOf(_imagewidth));
            IJ.log("image height: "+String.valueOf(_imageheight));
            IJ.log("slice number: "+String.valueOf(_slicenumber));
        }

        if( _output.calculateCentroid || _output.calculateMinMax ){
            _results = new ResultsTable();
        } else {
            _results = null;
        }

        if( _output.calculateMask ){
            _maskout = IJ.createImage(DEFAULT_MASK_WINDOW, "RGB-black",
                                        _imagewidth, _imageheight, _nprocess);
        } else {
            _maskout = null;
        }

        if( DEBUG )
            IJ.log("done initializing");

        IJ.showStatus("Traking...");
        IJ.showProgress(0, _nprocess);
    }

    protected void nextResult()
    {
        if( DEBUG )
            IJ.log("next frame...");

        // TODO: check if the results window is open
        _results.incrementCounter();

        // TODO: check if the mask image output is open
        // TODO: update properly
    }

    protected void finalizeResults()
    {
        if( DEBUG )
            IJ.log("finalizing...");

        // TODO: check if the results window is open
        if( _results != null ){
            _results.updateResults();
            _results.showRowNumbers(true);
            _results.show(DEFAULT_RESULTS_WINDOW);
        }

        // TODO: check if the mask image output is open
        if( _maskout != null ){
            _maskout.show();
        }
    }

    public void updateWithImage()
    {
        // validate: is there an image?
        _ip = IJ.getImage();

        // setup: get width, height of the image, create buffer + mask image? depending on output settings
        _imagewidth   = _ip.getWidth();
        _imageheight  = _ip.getHeight();
        _slicenumber  = _ip.getStackSize();
    }

    protected boolean initProcessBounds()
    {
        // TODO: deal with ROI settings here...
        _offsetx    = _roi.getRoiX();
        _offsety    = _roi.getRoiY();
        _runwidth   = _roi.getRoiWidth();
        _runheight  = _roi.getRoiHeight();
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
        return true;
    }

    public void setRoi(int x, int y, int width, int height)
    {
        _roi.setRoiX(x);
        _roi.setRoiY(y);
        _roi.setRoiWidth(width);
        _roi.setRoiHeight(height);
    }

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

    public void resetROI()
    {
        updateWithImage();
        setRoi(0, 0, _imagewidth, _imageheight);
    }

    public void updateHistogram()
    {
        Thread t = new Thread(new HistogramUpdater());
        t.start();
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        performAction(e.getActionCommand());
    }

    @Override
    public void performAction(String command)
    {
        if( command.equals(RUN_PIXYLATOR) )
        {
            runTracking();
            return;
        } else if ( command.equals(HistogramControl.REPLOT_HISTOGRAM) ) {
            updateHistogram();
        } else if ( command.equals(ROIControl.CURRENT_ROI) ) {
            updateWithROI();
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
        } else
            IJ.showMessage("TODO","should perform: "+command);
    }

    @Override
    public void run(String arg)
    {
        if( singleton == null ){
            singleton = new Pixylator_alpha();
        }

        try{
            singleton.setMenuBar(IJ.getInstance().getMenuBar());
        } catch (NullPointerException e) {
            // do nothing
        }
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

        // TODO: implement properly
        _sampling.setDelegate(this);
        _sampling.setBorder(BorderFactory.createTitledBorder("Histogram settings:"));
        layoutAboveSlider(_sampling, content, layout, 0, 0, 2, GridBagConstraints.LAST_LINE_START, GridBagConstraints.HORIZONTAL);

        // TODO: probably separate as FrameControl
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

        // TODO: probably separate as ROIControl
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

        if( DISABLE_UNPREPARED ){
            _histo.setEnabled(false);
            _sampling.setEnabled(false);
            _load.setEnabled(false);
            _save.setEnabled(false);
        }

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
        _masks[0].setOffset(330);
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
        @Override
        public void run(){
            updateWithImage();
            if( !initProcessBounds() ) return;

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
                    StringWriter w = new StringWriter();
                    e.printStackTrace(new PrintWriter(w));
                    IJ.log(w.toString());
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
            _histo.revalidate();
        }
    }

    public static void main(String [] args)
    {
        Pixylator_alpha frame = new Pixylator_alpha();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
