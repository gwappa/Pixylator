
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JCheckBox;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;

import java.awt.event.ActionEvent;

/**
*   a UI/Config for selection of outputs: centroid, min-max
*
*   TODO: add mouse listener to take care of the cursor/hue value on the display
*/
public class OutputSelector extends JPanel
    implements java.awt.event.ActionListener
{
    String[]    _options = {"Centroid", "Min/Max", "Mask Image(s)"};
    JCheckBox[] _buttons = new JCheckBox[3];

    public boolean calculateCentroid = true;
    public boolean calculateMinMax   = false;
    public boolean calculateMask     = true;

    public OutputSelector()
    {
        initUI();
    }

    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();
        if( command.equals(_options[0]) )
        {
            calculateCentroid = _buttons[0].isSelected();
        }
        else if ( command.equals(_options[1]) )
        {
            calculateMinMax   = _buttons[1].isSelected();
        }
        else if ( command.equals(_options[2]) )
        {
            calculateMask     = _buttons[2].isSelected();
        }
    }

    private void initUI()
    {
        setBorder(BorderFactory.createTitledBorder("Output:"));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        JCheckBox button;
        boolean[] initial = {calculateCentroid, calculateMinMax, calculateMask};

        for( int i=0; i<_options.length; i++ )
        {
            button = new JCheckBox(_options[i], initial[i]);
            button.setActionCommand(_options[i]);
            button.addActionListener(this);
            add(button);
            _buttons[i] = button;
        }
        add(Box.createVerticalGlue());
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new OutputSelector());
        frame.setSize(400, 200);
        frame.setVisible(true);
    }
}
