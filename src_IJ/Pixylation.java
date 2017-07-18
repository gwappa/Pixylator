/**
*   Pixylation.java
*   @author Keisuke Sehara
*/

package lab.proj.chaos.colortrack;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;

/**
*   a class that actually does the job of color tracking.
*   normally this job is done in a separate thread.
*/
public class Pixylation
    implements  Runnable,
                ParameterListener,
                ImageSelectionListener
{
    private static final boolean DEBUG = false;

    HueMaskControl[] masks = null;
    int[] maskColors       = null;
    int nMask       = 0;

    int startFrame  = 1;
    int stopFrame   = 1;
    int nFrame      = 1;

    int originX     = 0;
    int originY     = 0;
    int roiWidth       = 1;
    int roiHeight      = 1;

    ImagePlus image = null;
    String  title       = null;
    int     imageWidth  = 0;
    int     imageHeight = 0;
    int     imageDepth  = 0;

    MeasurementOutput out = null;
    List<List<Measurement>>  stats = null;
    List<MaskOutput> views = null;
    boolean measure = true;

    public Pixylation(HueMaskControl[] masks)
    {
        this.masks  = masks;
        nMask       = masks.length;
        maskColors  = new int[nMask];
        stats = new ArrayList<List<Measurement>>(nMask);
        for(int i=0; i<nMask; i++){
            stats.add(new LinkedList<Measurement>());
        }
        views = new LinkedList<MaskOutput>();
    }

    /**
    *   clears the list of listeners.
    */
    public void clearListeners()
    {
        if( DEBUG ){
            IJ.log("Pixylation: resetting the previous settings...");
        }
        for(int i=0; i<nMask; i++)
            stats.get(i).clear();
        views.clear();
        out = null;
        if( DEBUG ){
            IJ.log("Pixylation: cleared the listener lists.");
        }
    }

    /**
    *   adds a list of Measurement's.
    */
    public void setMeasurements(List<Measurement> calcs)
    {
        Iterator<Measurement> it;
        for(int i=0; i<nMask; i++){
            it = calcs.iterator();
            while(it.hasNext())
                stats.get(i).add(it.next().cloneMeasurement());
        }
        if( DEBUG ){
            IJ.log("Pixylation: added "+calcs.size()+" measurements.");
        }
    }

    /**
    *   adds a MascOutput.
    */
    public void addMaskOutput(MaskOutput out)
    {
        views.add(out);
        if( DEBUG ){
            IJ.log("Pixylation: added mask output: "+out.toString());
        }
    }

    /**
    *   sets the AttributeOutput
    */
    public void setMeasurementOutput(MeasurementOutput out)
    {
        this.out = out;
        if( DEBUG ){
            IJ.log("Pixylation: set measurement output: "+out.toString());
        }
    }

    /**
    *   initializes all the tracking listeners properly.
    */
    protected boolean initTracking() throws IOException
    {
        measure = (stats.get(0).size() > 0) && (out != null);
        List<TrackerElement> initialized = new LinkedList<TrackerElement>();
        TrackerElement element;
        boolean canceled = false;
        Iterator<? extends TrackerElement> it;
        String maskname;

        // process stats
        if( DEBUG ){
            IJ.log("Pixylation: initializing measurements...");
        }
        if( measure ){
            for(int i=0; i<nMask; i++){
                it = stats.get(i).iterator();
                maskname = masks[i].getName();
                while(it.hasNext()){
                    element = it.next();
                    if( !element.startOutput(maskname, imageWidth, imageHeight, nFrame) ){
                        canceled = true;
                        break;
                    }
                    initialized.add(element);
                }
            }
        }

        if( !canceled ){
            // process views
            if( DEBUG ){
                IJ.log("Pixylation: initializing mask outputs...");
            }
            it = views.iterator();
            while(it.hasNext()){
                element = it.next();
                if( !element.startOutput(title, imageWidth, imageHeight, nFrame) ){
                    canceled = true;
                    break;
                }
                initialized.add(element);
            }
        }

        if( !canceled ){
            // process out
            if( DEBUG ){
                IJ.log("Pixylation: initializing the measurement output...");
            }
            if( measure ){
                if( !out.startOutput(title, imageWidth, imageHeight, nFrame) ){
                    canceled = true;
                } else {
                    initialized.add(out);
                }
            }
        }

        if( canceled ){
            it = initialized.iterator();
            while(it.hasNext()){
                it.next().endOutput(0);
            }
            return false;
        } else {
            return true;
        }
    }

    /**
    *   calls nextSlice() on all the tracking listeners.
    */
    protected void nextSlice(int slice) throws IOException
    {
        if( DEBUG ){
            IJ.log("Pixylation: preparing for the next slice...");
        }
        Iterator<? extends TrackerElement> it;

        if( measure ){
            // process stats
            for(int i=0; i<nMask; i++){
                it = stats.get(i).iterator();
                while(it.hasNext())
                    it.next().nextSlice(slice);
            }

            // process out
            out.nextSlice(slice);
        }

        // process views
        it = views.iterator();
        while(it.hasNext())
            it.next().nextSlice(slice);
    }

    /**
    *   calls add() to corresponding TrackerElements.
    */
    protected void addPixel(int category, int x, int y, int gray)
    {
        if( measure ){
            // process stats
            Iterator<Measurement> ic = stats.get(category).iterator();
            while(ic.hasNext())
                ic.next().addPixel(x, y, gray);
        }

        // process views
        Iterator<MaskOutput> iv = views.iterator();
        while(iv.hasNext())
            iv.next().putPixel(x, y, maskColors[category], gray);
    }

    /**
    *   let StatisticCalculator's summarize the current slice.
    */
    protected void writeStatistics() throws IOException
    {
        if( !measure )
            return;
        Iterator<Measurement> it;
        for(int i=0; i<nMask; i++){
            it = stats.get(i).iterator();
            while(it.hasNext())
                it.next().write(out);
        }
    }

    /**
    *   finalizes all the tracking listeners.
    */
    protected void finalizeTracking(int slices)
    {
        if( DEBUG ){
            IJ.log("Pixylation: finalizing the tracking operation...");
        }

        Iterator<? extends TrackerElement> it;

        if( measure ){
            // process stats
            for(int i=0; i<nMask; i++){
                it = stats.get(i).iterator();
                while(it.hasNext()){
                    try {
                        it.next().endOutput(slices);
                    } catch (IOException ioe) {
                        IJLogger.logError(ioe);
                    }
                }
            }

            // process out
            try {
                out.endOutput(slices);
            } catch (IOException ioe) {
                IJLogger.logError(ioe);
            }
        }

        // process views
        it = views.iterator();
        while(it.hasNext()){
            try {
                it.next().endOutput(slices);
            } catch (IOException ioe) {
                IJLogger.logError(ioe);
            }
        }
    }

    /**
    *   performs pixylation.
    */
    @Override
    public void run(){
        try {
            assert image != null;

            // TODO: check that there is at least 1 measurement+measure-output or mask output

            nFrame = stopFrame - startFrame + 1;
            for(int i=0; i<nMask; i++)
                maskColors[i] = masks[i].getRGB();
            IJ.resetEscape();

            try {
                if( initTracking() == false ){
                    return;
                }
            } catch (IOException ioe) {
                IJLogger.logError(ioe);
                return;
            }

            int current = startFrame;
            // per each slice:
            try {
                while( current<=stopFrame ){
                    if( Thread.interrupted() || IJ.escapePressed() ){
                        throw new InterruptedException("Abort command received.");
                    }
                    nextSlice(current);
                    IJ.showStatus("Pixylation: processing slice #"+current+"... (press Esc to abort)");
                    try{
                        trackSingleSlice(current); // throws ClassCastException
                    } catch (ClassCastException e) {
                        IJ.showMessage("Process aborted", "Pixylator only accepts color images");
                        return;
                    }
                    IJ.showProgress(++current, nFrame);
                    IJ.freeMemory();
                }
                IJ.showStatus("done Pixylation.");
            } catch (InterruptedException ie) {
                IJ.showStatus("aborted Pixylation.");
                return;
            } catch (Exception e) {
                IJLogger.logError(e);
                IJ.showStatus("error during Pixylation.");
                return;
            } finally {
                finalizeTracking(current);
                IJ.showProgress(1.0);
            }
        } catch (Throwable e) {
            IJLogger.logError(e);
        }
    }


    /**
    *   called with each slice of images during tracking
    */
    public void trackSingleSlice(int slice)
        throws ClassCastException, IOException
    {
        assert image != null;

        if( DEBUG )
            IJ.log("Pixylation: running on slice: "+String.valueOf(slice));

        ColorProcessor imp = null;

        if( (slice == 1) && (imageDepth ==1) ){
            imp = (ColorProcessor)(image.getProcessor());
        } else {
            imp = (ColorProcessor)(image.getStack().getProcessor(slice));
        }

        // iterate: read pixel, add to buffer
        int xx, yy;
        int[] rgb = {-1, -1, -1};
        int hue, gray;
        for(int x=0; x<roiWidth; x++)
        {
            xx = x + originX;
            for(int y=0; y<roiHeight; y++)
            {
                yy   = y + originY;
                rgb  = imp.getPixel(xx, yy, rgb);
                hue  = Hue.fromRGB(rgb[0], rgb[1], rgb[2]);
                gray = Luma.fromRGB(rgb[0], rgb[1], rgb[2]);

                for(int i=0; i<nMask; i++)
                {
                    if( masks[i].contains(hue) )
                    {
                        addPixel(i, xx, yy, gray);
                    }
                }
            }
        }

        writeStatistics();

        if( DEBUG )
            IJ.log("Pixylation: done with slice: "+String.valueOf(slice));
    }

    @Override
    public void parameterUpdate(Object src, String role, String key, int value)
    {
        // TODO: update fields accordingly
        // src should be either FrameControl or ROIControl
        // hue settings should be read directly from the 'mask' objects.
        if ( key.equals(FrameControl.FRAME_START) ){
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
            IJ.log("irrelevant key for Pixylation: "+role);
        }
        if( DEBUG )
            IJ.log(String.format("%s: %s[%s] -> %d", "Pixylation",role,key,value));
    }

    @Override
    public void selectedImageChanged(ImagePlus image)
    {
        this.image = image;

        if( image != null ){
            imageWidth  = image.getWidth();
            imageHeight = image.getHeight();
            imageDepth  = image.getStackSize();
            title       = image.getTitle();
        }

        if( DEBUG )
            IJ.log(String.format("%s: image changed: %s", "Pixylation", (image==null)? "<none>":image.getTitle()));
    }
}
