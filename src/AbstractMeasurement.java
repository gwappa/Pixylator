
package lab.proj.chaos.colortrack;

import java.io.IOException;

/**
*   an adapter class for Measurement subclasses
*   so that they need to implement only Measurement-specific methods.
*/
public abstract class AbstractMeasurement
    implements Measurement
{
    protected String category = "";

    String  name;
    String  key;
    int     type;

    public AbstractMeasurement(String name, String key, int type) {
        this.name = name;
        this.key  = key;
        this.type = type;
    }

    public String getCategory()
    {
        return category;
    }

    @Override
    public final String getParamName() {
        return name;
    }

    @Override
    public final String getParamKey() {
        return key;
    }

    @Override
    public final int getParamType() {
        return type;
    }

    @Override
    public void startOutput(String category, int width, int height, int nslice)
        throws IOException
    {
        this.category = category;
    }

    @Override
    public void endOutput(int slices)
        throws IOException
    {

    }

    /**
    *   resets the calculation.
    */
    @Override
    public void nextSlice(int slice)
        throws IOException
    {
        reset();
    }

    /**
    *   resets the calculation; called internally from nextSlice().
    */
    public abstract void reset();
}
