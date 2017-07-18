
package lab.proj.chaos.colortrack;

/**
*   an abstraction of generation of mask images.
*/
public interface MaskOutput
    extends TrackerElement
{
    static final String PREFIX      = "MASK_";

    /**
    *   color the specified pixel on the current slice.
    */
    void putPixel(int x, int y, int rgb, int gray);
}
