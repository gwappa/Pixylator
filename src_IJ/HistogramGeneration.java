
package lab.proj.chaos.colortrack;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class HistogramGeneration
    implements Runnable, ImageSelectionListener,
                ParameterListener
{
    static final boolean DEBUG = false;

    ActionDelegate  delegate    = null;
    HueHistogram    histo       = null;
    ImagePlus       target      = null;

    int maxSample   = 1;

    int startFrame  = 1;
    int stopFrame   = 1;

    int imageWidth  = 0;
    int imageHeight = 0;
    int imageDepth  = 0;

    int originX     = 0;
    int originY     = 0;
    int roiWidth    = 0;
    int roiHeight   = 0;


    public HistogramGeneration(ActionDelegate delegate, HueHistogram histo)
    {
        this.delegate   = delegate;
        this.histo      = histo;
    }

    @Override
    public void run(){
        try {
            if( DEBUG ) {
                IJ.log("starting histogram generation...");
                IJ.log(String.format("%s: width=%d, height=%d, depth=%d", target.getTitle(), imageWidth, imageHeight, imageDepth));
                IJ.log(String.format("origin=(%d,%d), extent=(%d,%d), frames=(%d,%d), max=%d",
                                        originX, originY, roiWidth, roiHeight, startFrame, stopFrame, maxSample));
            }

            histo.reset();
            if( target == null ){
                if( DEBUG )
                    IJ.log("aborted histogram generation: no target image.");
                return;
            }
            if( originX > imageWidth || originY > imageHeight ){
                if( DEBUG )
                    IJ.log("aborted histogram generation: no valid ROI.");
                IJ.showStatus("Pixylator: reset ROI to generate histogram.");
                return;
            }
            if( startFrame > imageDepth ){
                if( DEBUG )
                    IJ.log("aborted histogram generation: no valid slice.");
                IJ.showStatus("Pixylator: reset frame settings to generate histogram.");
                return;
            }

            long totalPixels = ((long)(stopFrame - startFrame + 1))*roiWidth*roiHeight;
            int  inc         = 1;
            int  sample      = maxSample;
            if( totalPixels < sample ){
                sample = (int)totalPixels;
                // inc remains 1
            } else {
                // totalpixels is larger than maxsample
                // then, scale 'inc' to fill all pixels
                inc = (int)(totalPixels/sample);
            }
            if( DEBUG )
                IJ.log(String.format("sample=%d, inc=%d", sample, inc));



            IJ.showStatus("Preparing histogram...");
            IJ.showProgress(0.0);

            int current = startFrame;
            int hue;
            int xpos = originX;
            int ypos = originY;
            int xlim = originX + roiWidth;
            int ylim = originY + roiHeight;
            ImageProcessor imp = (imageDepth > 1)? target.getStack().getProcessor(current) : target.getProcessor();
            int[] rgb = new int[] {0, 0, 0};

            for(int i=0; i<sample; i++)
            {
                if( Thread.interrupted() ){
                    if( DEBUG )
                        IJ.log("histogram generation was aborted from Pixylator.");
                    break;
                }

                try {
                    rgb     = imp.getPixel(xpos, ypos, rgb);
                    hue     = Hue.fromRGB(rgb[0],rgb[1],rgb[2]);
                    histo.add(hue);
                } catch (Exception e) {
                    IJLogger.logError(e);
                }

                IJ.showProgress(i*1.0/maxSample);

                // updating the position
                xpos += inc;
                if( xpos >= xlim ){
                    xpos -= roiWidth;
                    if( (++ypos) >= ylim ){
                        ypos -= roiHeight;
                        if( (++current) > stopFrame ){
                            break;
                        } else {
                            // assuming that it is a stack
                            imp = target.getStack().getProcessor(current);
                        }
                    }
                }
            }
            IJ.showProgress(1.0);
            histo.paintImmediately(0, 0, histo.getWidth(), histo.getHeight());
            if( DEBUG )
                IJ.log("done.");
        } catch (Throwable t) {
            IJLogger.logError(t);
        }
    }

    @Override
    public void parameterUpdate(Object src, String role, String key, int value)
    {
        // TODO: update fields accordingly
        // src should be either FrameControl, ROIControl or HistogramControl
        boolean update = true;

        if ( key.equals(HistogramControl.SAMPLE_NUMBER) ){
            maxSample = value;
        } else if ( key.equals(FrameControl.FRAME_START) ){
            startFrame = value;
        } else if ( key.equals(FrameControl.FRAME_STOP) ){
            stopFrame = value;
        } else if ( key.equals(ROIControl.ROI_X) ){
            originX = value;
        } else if ( key.equals(ROIControl.ROI_Y) ){
            originY = value;
        } else if ( key.equals(ROIControl.ROI_WIDTH) ){
            roiWidth = value;
        } else if ( key.equals(ROIControl.ROI_HEIGHT) ){
            roiHeight = value;
        } else {
            IJ.log("irrelevant key for HistogramGeneration: "+key);
            update = false;
        }
        if( update ){
            delegate.performAction(HistogramControl.REPLOT_HISTOGRAM);
        }

        if( DEBUG )
            IJ.log(String.format("%s: %s[%s] -> %d", "HistogramGeneration",role,key,value));
    }

    @Override
    public void selectedImageChanged(ImagePlus image)
    {
        target = image;
        if( target != null ){
            imageWidth = target.getWidth();
            imageHeight = target.getHeight();
            imageDepth = target.getStackSize();

            delegate.performAction(HistogramControl.REPLOT_HISTOGRAM);
        }

        if( DEBUG )
            IJ.log(String.format("%s: image changed: %s", "HistogramGeneration", (image == null)? "<none>":target.getTitle()));
    }
}
