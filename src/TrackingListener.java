
package lab.proj.chaos.colortrack;

import java.io.IOException;

/**
*   an abstraction of any type of results generation during color tracking.
*/
public interface TrackingListener
{
    /**
    *   returns the name of the parameter to show
    */
    String getParamName();

    /**
    *   returns the 'key' to the parameter (saved in the configs)
    */
    String getParamKey();

    /**
    *   a callback to let the object know that the tracking is starting.
    */
    void startOutput(String title, int width, int height, int nslice) throws IOException;

    /**
    *   a callback to let the object know that the tracking is over.
    */
    void endOutput(int slices) throws IOException;

    /**
    *   single slice is over. increment the counter.
    *   this method is called before proceeding to every slice, including the first slice.
    */
    void nextSlice(int slice) throws IOException;
}
