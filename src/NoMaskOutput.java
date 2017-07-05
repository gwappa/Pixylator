/**
*   NoMaskOutput.java
*   @author Keisuke Sehara
*/

package lab.proj.chaos.colortrack;

import java.io.IOException;

public class NoMaskOutput
    implements MaskOutput
{
    static final String NAME    = "No output";
    static final String KEY     = "no_output";

    @Override
    public String getParamName(){
        return NAME;
    }

    @Override
    public String getParamKey(){
        return KEY;
    }

    @Override
    public void startOutput(String title, int width, int height, int slices) throws IOException
    {
        // do nothing
    }

    @Override
    public void endOutput(int slices) throws IOException
    {
        // do nothing
    }

    @Override
    public void nextSlice(int slice) throws IOException
    {
        // do nothing
    }

    @Override
    public void putPixel(int x, int y, int rgb, int gray)
    {
        // do nothing
    }
}
