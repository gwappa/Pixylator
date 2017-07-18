
package lab.proj.chaos.colortrack;

import java.io.IOException;

import ij.IJ;
import ij.io.SaveDialog;

import lab.proj.chaos.vio.H264Writer;
import lab.proj.chaos.vio.ColorFormatter;

public class H264MaskOutput
    implements MaskOutput
{
    static final boolean    DEBUG   = false;

    static final String OUTPUT_NAME = "H.264 file output";
    static final String OUTPUT_KEY  = "h264_output";
    static final String EXT         = ".avi";

    H264Writer out = null;
    byte[]     buf = null;
    int        slice = 0;

    int        width, height;

    @Override
    public String getElementName(){
        return OUTPUT_NAME;
    }

    @Override
    public String getElementKey(){
        return OUTPUT_KEY;
    }

    @Override
    public boolean startOutput(String title, int width, int height, int nslice) throws IOException
    {
        SaveDialog  dialog      = new SaveDialog("Save as H.264...", PREFIX+FileNameFunctions.getBaseName(title), EXT);
        String      filename    = dialog.getFileName();
        if( filename == null ){
            return false; // cancel pixylation
        }
        String      path        = new java.io.File(dialog.getDirectory(), filename).getAbsolutePath();

        try {
            out = new H264Writer(path, width, height);
        } catch (Exception e) {
            throw new IOException("could not open H.264 file", e);
        }

        this.width  = width;
        this.height = height;

        buf     = new byte[width*height*3];
        if( DEBUG ){
            IJ.log("H264MaskOutput: starting logging.");
        }
        return true;
    }

    @Override
    public void nextSlice(int slice) throws IOException
    {
        flush();
        for(int i=0; i<buf.length; i++){
            buf[i] = (byte)0x00;
        }
        this.slice = slice;
        if( DEBUG ){
            IJ.log("H264MaskOutput: starting slice #"+slice);
        }
    }

    @Override
    public void putPixel(int x, int y, int rgb, int gray)
    {
        ColorFormatter.RGBBytes RGB = ColorFormatter.getBytes(rgb);
        int offset = (width*(y-1) + (x-1))*3;
        buf[offset++] = RGB.R;
        buf[offset++] = RGB.G;
        buf[offset++] = RGB.B;
    }

    @Override
    public void endOutput(int slices) throws IOException
    {
        try {
            flush();
        } catch (IOException e) {
            IJLogger.logError(e);
        }
        if( out != null ){
            if( DEBUG ){
                IJ.log("H264MaskOutput: closing file...");
            }
            out.close();
        }
        out = null;
        buf = null;
        IJ.freeMemory();
    }

    /**
    *   flushes the current buffer into the output
    */
    protected void flush() throws IOException
    {
        if( (out == null) || (slice == 0) ){
            if( DEBUG ){
                IJ.log("H264MaskOutput: no flushing for now...");
            }
            return;
        }
        try {
            if( DEBUG ){
                IJ.log("H264MaskOutput: writing slice #"+slice);
            }
            out.write(buf);
        } catch (Exception e) {
            throw new IOException("error occurred while writing slice #"+slice, e);
        }
    }
}
