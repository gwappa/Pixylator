
package lab.proj.chaos.colortrack;

import java.io.IOException;

/**
*   the abstraction layer for results table output.
*/
public interface MeasurementOutput
    extends TrackingListener
{
    static final String PREFIX         = "Results_";
    
    /**
    *   using the internal slice counter, output single attribute value.
    */
    void print(String category, String attrname, double value) throws IOException;
    void print(String category, String attrname, int value) throws IOException;
}
