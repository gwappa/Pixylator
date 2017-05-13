
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.Container;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

public class Alpha_Pixylator extends JFrame
{
    static final int MASK_CAPACITY  = 2;
    static final int INITIAL_WIDTH  = 500;
    static final int INITIAL_HEIGHT = 400;
    static final int SLIDER_WEIGHT  = 3;

    HueHistogram        _histo = new HueHistogram();
    HueMaskControl[]    _masks = new HueMaskControl[MASK_CAPACITY];

    public Alpha_Pixylator()
    {
        super("Alpha Pixylator");
        setupMasks();
        setupUI();
    }

    private void setupUI()
    {
        setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        Container content = getContentPane();

        GridBagLayout layout = new GridBagLayout();
        content.setLayout(layout);


        layoutHistogramUI(_histo, content, layout, HueMaskControl.UI_SLIDER_OFFSETX, 0);
        HueMaskControl.layoutUI(_masks[0], content, layout, 0, 1);
        HueMaskControl.layoutUI(_masks[1], content, layout, 0, 1 + HueMaskControl.UI_GRID_HEIGHT);

        GridBagConstraints con = new GridBagConstraints();
        JPanel commands = new JPanel();
        con.gridy       = 1 + 2*(HueMaskControl.UI_GRID_HEIGHT);
        con.gridx       = 0;
        con.gridwidth   = HueMaskControl.UI_GRID_WIDTH;
        con.gridheight  = 1;
        con.fill        = GridBagConstraints.BOTH;
        layout.setConstraints(commands, con);
        content.add(commands);

        for(int i=0; i<361; i++)
        {
            _histo.add(i, i);
        }
    }

    private void layoutHistogramUI(HueHistogram histo, Container content, GridBagLayout layout, int offsetx, int offsety)
    {
        GridBagConstraints con = new GridBagConstraints();

        // add histogram
        con.gridx       = offsetx + 0;
        con.gridy       = offsety + 0;
        con.gridwidth   = 1; con.gridheight = 1;
        con.fill        = GridBagConstraints.BOTH;
        con.weightx     = HueMaskControl.UI_SLIDER_WEIGHT;
        con.weighty     = 2;
        layout.setConstraints(_histo, con);
        content.add(_histo);
    }

    private void setupMasks()
    {
        _masks[0] = new HueMaskControl("Red");
        _masks[1] = new HueMaskControl("Green");
    }

    public static void main(String [] args)
    {
        Alpha_Pixylator frame = new Alpha_Pixylator();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
