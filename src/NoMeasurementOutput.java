/**
*   NoMeasurementOutput.java
*   @author Keisuke Sehara
*/

package lab.proj.chaos.colortrack;

import java.io.IOException;
import java.util.List;

public class NoMeasurementOutput
    implements MeasurementOutput
{
    static final String NAME    = "No output";
    static final String KEY     = "no_output";

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
        // do nothing
        return true;
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
    public void print(String category, String attrname, double value) throws IOException
    {
        // do nothing
    }

    @Override
    public void print(String category, String attrname, int value) throws IOException
    {
        // do nothing
    }
}
