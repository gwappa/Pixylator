
import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.awt.Window;
import ij.io.OpenDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.gui.GenericDialog;

import lab.proj.chaos.vio.FFmpegReader;
import lab.proj.chaos.vio.RGBCopyTarget;
import lab.proj.chaos.vio.DefaultRGBCopyTarget;

public class Virtual_H264
    extends ij.VirtualStack
    implements ij.plugin.PlugIn
{
    private static final boolean DEBUG      = false;
    private static final double DEFAULT_FPS = 25.0;

    FFmpegReader proxy = null;
    String fileDir = null;
    String fileName = null;
    int width, height, nframe;
    double framerate;
    boolean direct = true;
    ImagePlus img = null;

    List<RGBCopyTarget> frames = null;
    RGBCopyTarget       single = null;

    @Override
    public void run(String arg)
    {
        OpenDialog  od = new OpenDialog("Select file to open", arg);
        fileName = od.getFileName();
        if (fileName == null) return;
        fileDir = od.getDirectory();
        String path = fileDir + fileName;

        proxy = new FFmpegReader(path);
        try {
            proxy.open();
            IJ.showStatus("Counting frames...");
            proxy.sweep(null); // no callback for now
            width  = proxy.getWidth();
            height = proxy.getHeight();
            nframe = proxy.getFrameCount();

            if( !configure() ){
                proxy.close();
                return;
            }

            if( !direct ){
                IJ.showStatus(String.format("Reading frames: %d/%d...",0,nframe));
                frames = new ArrayList<RGBCopyTarget>(nframe);
                synchronized (org.bytedeco.javacpp.avcodec.class){
                    for(int i=0; i<nframe; i++){
                        IJ.showStatus(String.format("Reading frames: %d/%d...",i,nframe));
                        IJ.showProgress(i, nframe);
                        single = new DefaultRGBCopyTarget(width, height);
                        proxy.copyRGBFrameUnsafe(i, single);
                        frames.add(single);
                    }
                }
                IJ.showProgress(1.0);
            } else {
                single = new DefaultRGBCopyTarget(width, height);
            }
            IJ.showStatus("done.");
            if( DEBUG ){
                IJ.log("done loading H.264 file.");
            }
            img = new ImagePlus(fileName, this);
            img.show();

            // connect window close event to closeProxy()
            Window win = WindowManager.getWindow(img.getTitle());
            win.addWindowListener(new java.awt.event.WindowAdapter(){
                public void windowClosed(java.awt.event.WindowEvent e){
                    if( DEBUG ){
                        IJ.log("window closed: "+img.getTitle());
                    }
                    closeProxy();
                }
            });
        } catch (Exception e) {
            error(e);
        }
    }

    public void closeProxy()
    {
        if( proxy == null ) return;

        try {
            proxy.close();
        } catch (Exception e) {
            error(e);
        }
    }

    public void error(Throwable e)
    {
        StringWriter stream = new StringWriter();
        e.printStackTrace(new PrintWriter(stream));
        IJ.log(stream.toString());
    }

    protected boolean configure()
    {
        GenericDialog d = new GenericDialog("Virtual H264");
        d.addNumericField("Frame rate (FPS)", DEFAULT_FPS, 2); // TODO: read from the file
        d.addCheckbox("On-memory stack (faster playback)", false);
        d.showDialog();
        if( d.wasCanceled() ){
            return false;
        }
        framerate   = d.getNextNumber();
        direct      = !(d.getNextBoolean());
        return true;
    }

    @Override
    public String getDirectory()
    {
        return fileDir;
    }

    @Override
    public String getFileName(int n)
    {
        return fileName;
    }

    @Override
    public int getSize()
    {
        return nframe;
    }

    @Override
    public String getSliceLabel(int n){
        return IJ.d2s(1.0*(n-1)/framerate) + " s";
    }

    @Override
    public int getBitDepth()
    {
        return 24; // RGB
    }

    protected int[] getRGBArray(int n)
    {
        if( DEBUG ){
            IJ.log("Virtual H264: query: slice #"+n);
        }
        try {
            if( direct ){
                proxy.copyRGBFrame(n-1, single);
                return single.toArray();
            } else {
                return frames.get(n-1).toArray();
            }
        } catch (Exception e) {
            error(e);
            return null;
        }
    }

    @Override
    public Object getPixels(int n)
    {
        return getRGBArray(n);
    }

    @Override
    public ij.process.ImageProcessor getProcessor(int n)
    {
        return new ij.process.ColorProcessor(width, height, getRGBArray(n));
    }
}
