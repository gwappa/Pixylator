
package lab.proj.chaos.colortrack;

public interface Measurement
    extends TrackerElement
{
    static final int INT_TYPE       = 0;
    static final int DOUBLE_TYPE    = 1;

    /**
    *   returns the value type that this TrackingListener returns.
    */
    int getValueType();

    /**
    *   for duplicating itself for other categories.
    */
    Measurement cloneMeasurement();

    /**
    *   add a coordinate into a calculation buffer.
    */
    void addPixel(int x, int y, int gray);

    /**
    *   writes the statistics for current slice into the output.
    */
    void write(MeasurementOutput out) throws java.io.IOException;
}
