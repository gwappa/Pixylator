
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
*   HueHistogram class draws a log-histogram of the hue for the given image.
*
*   ## Usage
*   1. create a HueHistogram.
*   2. (optional, when renewing) call reset().
*   3. call add() until you cover your image.
*/
public class HueHistogram extends JComponent
{
    int baseline  = 10;
    int linewidth = 1;
    int binwidth  = 4;
    int bincount;
    int[] counts   = null;

    public HueHistogram()
    {
        counts = new int[Hue.MAXIMUM + 1];
        reset();
    }

    /**
    *   adds 'number' samples to 'rank'.
    */
    public void add(int rank, int number)
    {
        counts[rank/binwidth] += number;
    }

    /**
    *   adds one sample to 'rank'.
    */
    public void add(int rank)
    {
        counts[rank/binwidth]++;
    }

    /**
    *   calculates the height for a given bin (just because it is a bit confusing for me).
    *
    *   maximum = logmax
    *   minimum = log(1) = 0
    *   value   = log(count+1)
    *   scale: (value - minimum)*height/(maximum - minimum) = value*height/maximum
    */
    private int binHeight(int histoheight, double logmax, int count)
    {
        return (int)Math.round(Math.log(count+1)*histoheight/logmax);
    }

    /**
    *   overrides JComponent's paintComponent()
    */
    public void paintComponent(Graphics g)
    {
        int width   = getWidth();
        int height  = getHeight();

        // draw background
        int liney   = height - baseline;
        if( liney < 1 )  liney  = 1;
        g.setColor(Color.WHITE);
        g.fillRect(0,0,width,height);
        g.setColor(Color.BLACK);
        g.fillRect(0,liney,width,linewidth);

        // calculate maximum
        int     maxcount = 0;
        double  logmax   = 0.0;
        for(int bin=0; bin<bincount; bin++)
        {
            if( counts[bin] > maxcount )
                maxcount = counts[bin];
        }
        if( maxcount == 0 )
            return; // no need to "draw histogram"
        else
            logmax = Math.log(maxcount+1);

        // draw histogram
        int histoheight = height - baseline;
        int onset       = 0;
        int offset;
        int onsetx, binx;
        int biny;
        for(int bin=0; bin<bincount; bin++)
        {
            offset = (bin+1)*binwidth;
            onsetx = onset*width/360;
            binx   = (offset*width/360) - onsetx;
            biny   = binHeight(histoheight, logmax, counts[bin]);
            g.setColor(Hue.getColor(Hue.normalize(onset + (binwidth/2))));
            g.fillRect(onsetx, histoheight-biny, binx, biny);
            onset  = offset;
        }
    }

    /**
    *   resets the histogram (other than the baseline setting)
    */
    public void reset()
    {
        for(int i=0; i<counts.length; i++)
        {
            counts[i] = 0;
        }
        bincount = (Hue.MAXIMUM/binwidth) + 1;
    }

    public static void main(String[] args)
    {
        HueHistogram histo = new HueHistogram();
        for(int i=0; i<361; i++)
        {
            histo.add(i, i);
        }

        JFrame frame = new JFrame("HueHistogram");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setContentPane(histo);
        frame.setVisible(true);
    }
}