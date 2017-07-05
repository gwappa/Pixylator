
package lab.proj.chaos.colortrack;

public interface PixylatorDirectives
{
    // general configuration
    static final int    MASK_CAPACITY   = 2;

    // below are commands that you can give as the argument of run()
    static final String RUN_PIXYLATOR   = "run_pixylator";
    static final String ABORT_PIXYLATOR = "abort_pixylator";
    static final String SAVE_CONFIGS    = "save_configs";
    static final String LOAD_CONFIGS    = "load_configs";

    static final String ROLE_FRAME_CONTROL      = "frame_control";
    static final String ROLE_ROI_CONTROL        = "roi_control";
    static final String ROLE_SAMPLING_CONTROL   = "histo_control";

    static final String PREVIEW_WINDOW_TITLE    = "Pixylator Preview";
}
