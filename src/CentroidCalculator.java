
package lab.proj.chaos.colortrack;

public class CentroidCalculator
    extends AbstractMeasurement
{
    static final String STATISTIC_NAME  = "Centroid";
    static final String STATISTIC_KEY   = "centroid";
    static final String MEAN_X          = "Mean_X";
    static final String MEAN_Y          = "Mean_Y";

    protected int   sumx = 0;
    protected int   sumy = 0;
    protected int   count = 0;

    public CentroidCalculator()
    {
        super(STATISTIC_NAME, STATISTIC_KEY, DOUBLE_TYPE);
    }

    @Override
    public Measurement cloneMeasurement() {
        return new CentroidCalculator();
    }

    @Override
    public void addPixel(int x, int y, int gray) {
        sumx += x;
        sumy += y;
        count++;
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
        out.print(getCategory(), MEAN_X, sumx*1.0/count);
        out.print(getCategory(), MEAN_Y, sumy*1.0/count);
    }
}
