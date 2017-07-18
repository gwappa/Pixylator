/**
*   ImageStackOutput.java
*   @author Keisuke Sehara
*/

package lab.proj.chaos.colortrack;

import java.io.IOException;

import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;

public class ImageStackOutput
    implements MaskOutput
{
    static final boolean DEBUG  = false;

    static final String NAME        = "ImageJ image stack";
    static final String KEY         = "image_stack";

    boolean     isStack     = false;
    int         width       = 0;
    int         height      = 0;
    String      imageName   = "mask";
    ImageStack  stack       = null;
    ColorProcessor current  = null;

    protected ColorProcessor createSlice()
    {
        if( DEBUG ){
            IJ.log("ImageStackOutput: creating a new slice...");
        }
        ColorProcessor proc = new ColorProcessor(width, height);
        proc.setColor(0); // black
        proc.fill();
        return proc;
    }

    @Override
    public String getElementName(){
        return NAME;
    }

    @Override
    public String getElementKey(){
        return KEY;
    }

    @Override
    public boolean startOutput(String title, int width, int height, int slices) throws IOException
    {
        isStack = (slices > 1);
        String base = FileNameFunctions.getBaseName(title);
        imageName = PREFIX + base;

        this.width  = width;
        this.height = height;

        if( DEBUG ){
            IJ.log("ImageStackOutput:  isStack="+String.valueOf(isStack));
            IJ.log("ImageStackOutput:  title="+title);
            IJ.log("ImageStackOutput:   base="+base);
            IJ.log("ImageStackOutput:   name="+imageName);
            IJ.log("ImageStackOutput:  width="+width);
            IJ.log("ImageStackOutput: height="+height);
            IJ.log("ImageStackOutput: creating an image...");
        }

        if( isStack ){
            stack = new ImageStack(width, height);
        } else {
            current = createSlice();
        }
        return true;
    }

    @Override
    public void endOutput(int slices) throws IOException
    {
        if( DEBUG ){
            IJ.log("ImageStackOutput: displaying the image...");
        }
        ImagePlus image = null;
        if( isStack ){
            image = new ImagePlus(imageName, stack);
        } else {
            image = new ImagePlus(imageName, current);
        }
        if (image != null)
            image.show();

        image = null;
        stack = null;
        current = null;
    }

    @Override
    public void nextSlice(int slice) throws IOException
    {
        if( isStack ){
            current = createSlice();
            stack.addSlice(current);
        } else {
            // do nothing
        }
    }

    @Override
    public void putPixel(int x, int y, int rgb, int gray)
    {
        current.putPixel(x, y, rgb);
    }
}
