

import java.awt.Color;

public class Hue
{
    public static int   MINIMUM = 0;
    public static int   MAXIMUM = 360;
    public static double PART   = 60.0;

    private static Color[] _colors              = new Color[361];
    private static boolean _colors_initialized   = false;

    /**
    *   returns a java.awt.Color instance that corresponds to 'hue'
    *
    *   @param hue  hue value in the [0, 360] range (i.e. 'normalized').
    */
    public static Color getColor(int hue)
    {
        if(!_colors_initialized) initializeColors();
        return _colors[hue];
    }

    /**
    *   initializes _colors entries
    */
    private static void initializeColors()
    {
        int[] rgb;

        for(int hue=0; hue<_colors.length; hue++)
        {
            rgb = toRGB(hue);
            _colors[hue] = new Color(rgb[0], rgb[1], rgb[2]);
        }
        _colors_initialized = true;
    }

    /**
    *   normalizes the hue value to the [0, 360] range.
    */
    public static int normalize(int hue)
    {
        while(hue > MAXIMUM) hue -= MAXIMUM;
        while(hue < MINIMUM) hue += MAXIMUM;
        return hue;
    }

    /**
    *   computes the hue value from RGB indices.
    *
    *   @param r,g,b    [0,255] integer values.
    *   @returns        a hue value in the [0,360] range.
    */
    public static int fromRGB(int r, int g, int b)
    {
        double rr = r*1.0/255;
        double gg = g*1.0/255;
        double bb = b*1.0/255;
        double hh = Double.NaN;
        double mm;

        if( (rr >= gg) && (gg >= bb) ){
            hh = (rr == 0)? 0.0: (gg - bb)/(rr - bb);
        } else if( (gg > rr) && (rr >= bb) ) {
            hh = 2.0 - (rr - bb)/(gg - bb);
        } else if ( (gg >= bb) && (bb > rr) ) {
            hh = 2.0 + (bb - rr)/(gg - rr);
        } else if ( (bb > gg) && (gg > rr) ) {
            hh = 4.0 - (gg - rr)/(bb - rr);
        } else if ( (bb > rr) && (rr >= gg) ) {
            hh = 4.0 + (rr - gg)/(bb - gg);
        } else {
            hh = 6.0 - (bb - gg)/(rr - gg);
        }

        return (int)Math.round(hh*60);
    }

    /**
    *   calculates RGB values, as a pure color, that the given 'hue' represents.
    *
    *   @param hue  hue value in the [0, 360] range. (i.e. 'normalized')
    *   @returns    {r, g, b} in an integer array.
    */
    public static int[] toRGB(int hue)
    {
        double part = hue/PART;
        double x = 1 - Math.abs(part%2 - 1);

        double r=0.0, g=0.0, b=0.0;

        if( part>=0 && part<1 ){        r = 1.0;    g = x;   }
        else if( part>=1 && part<2 ){   r = x;      g = 1.0; }
        else if( part>=2 && part<3 ){   g = 1.0;    b = x;   }
        else if( part>=3 && part<4 ){   g = x;      b = 1.0; }
        else if( part>=4 && part<5 ){   r = x;      b = 1.0; }
        else {                          r = 1.0;    b = x;   }

        int[] rgb = new int[3];
        rgb[0] = (int)Math.round(r*255);
        rgb[1] = (int)Math.round(g*255);
        rgb[2] = (int)Math.round(b*255);
        return rgb;
    }
}