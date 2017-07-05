
package lab.proj.chaos.colortrack;

public class Luma
{
    static final int PHOTOSHOP  = 0;
    static final int BT709      = 1;
    static final int BT601      = 2;
    static final int DESATURATE = 3;
    static final int CUSTOM     = 4;

    static final int METHOD     = PHOTOSHOP;

    /**
    *   get 'luma' from 0-255 r/g/b values.
    *   @return Luma value in 0-255 range.
    */
    static int fromRGB(int r, int g, int b)
    {
        assert r>=0;
        assert g>=0;
        assert b>=0;

        switch(METHOD)
        {
            case PHOTOSHOP:
                return (int)Math.round(0.3*r + 0.59*g + 0.11*b);
            case BT709:
                return (int)Math.round(0.2126*r + 0.7152*g + 0.0722*b);
            case BT601:
                return (int)Math.round(0.299*r + 0.587*g + 0.114*b);
            case DESATURATE:
                return (Math.max(Math.max(r,g),b) + Math.min(Math.min(r,g),b))/2;
            case CUSTOM:
            default:
                // 0.2875*r + 0.5928125*g + 0.1086875*b;
                return ((r<<5 + r<<2) + (g<<6 + g<<4 - g) + (b<<3 + b<<2 + b))>>7;
        }
    }
}
