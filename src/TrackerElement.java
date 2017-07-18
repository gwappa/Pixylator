
package lab.proj.chaos.colortrack;

import java.io.IOException;

/**
*   an abstraction of any type of results generation during color tracking.
*/
public interface TrackerElement
{
    /**
    *   returns the name of the parameter to show
    */
    String getElementName();

    /**
    *   returns the 'key' to the parameter (saved in the configs)
    */
    String getElementKey();

    /**
    *   a callback to let the object know that the tracking is starting.
    *   @return whether to accept start (return false to cancel)
    */
    boolean startOutput(String title, int width, int height, int nslice) throws IOException;

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
