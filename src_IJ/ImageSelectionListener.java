
package lab.proj.chaos.colortrack;

import ij.ImagePlus;

public interface ImageSelectionListener
{
    void selectedImageChanged(ImagePlus image); // can be null
}
