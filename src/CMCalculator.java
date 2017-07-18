
package lab.proj.chaos.colortrack;

public class CMCalculator
    extends AbstractMeasurement
{
    static final String STATISTIC_NAME  = "Center of mass";
    static final String STATISTIC_KEY   = "cm";
    static final String MEAN_X          = "CM_X";
    static final String MEAN_Y          = "CM_Y";

    protected int   sumx = 0;
    protected int   sumy = 0;
    protected int   count = 0;

    public CMCalculator()
    {
        super(STATISTIC_NAME, STATISTIC_KEY, DOUBLE_TYPE);
    }

    @Override
    public Measurement cloneMeasurement() {
        return new CMCalculator();
    }

    @Override
    public void addPixel(int x, int y, int gray) {
        sumx  += x*gray;
        sumy  += y*gray;
        count += gray;
    }

    @Override
    public void reset() {
        sumx = 0;
        sumy = 0;
        count = 0;
    }

    @Override
    public void write(MeasurementOutput out)
        throws java.io.IOException
    {
        out.print(getCategory(), MEAN_X, (count>0)? (sumx*1.0/count):Double.NaN);
        out.print(getCategory(), MEAN_Y, (count>0)? (sumy*1.0/count):Double.NaN);
    }
}
