
package lab.proj.chaos.colortrack;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JCheckBox;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;

import java.awt.Component;
import java.awt.event.ActionEvent;

import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Iterator;

/**
*   a UI/Config for selection of outputs: centroid, min-max
*   @deprecated
*   use OutputControl instead.
*
*   TODO: add mouse listener to take care of the cursor/hue value on the display
*/
public class OutputSelector extends JPanel
    implements java.awt.event.ActionListener
{
    private static final long serialVersionUID = 10L;

    static final boolean DEBUG = false;

    List<String>            _keys       = new LinkedList<String>();
    Map<String, String>     _labels     = new Hashtable<String, String>();
    Map<String, JCheckBox>  _buttons    = new Hashtable<String, JCheckBox>();
    Map<String, Boolean>    _values     = new Hashtable<String, Boolean>();

    Component               _glue       = Box.createVerticalGlue();

    // String[]    _options = {"Centroid", "Min/Max", "Mask Image(s)"};
    // JCheckBox[] _buttons = new JCheckBox[3];

    // public boolean calculateCentroid = true;
    // public boolean calculateMinMax   = false;
    // public boolean calculateMask     = true;

    public OutputSelector()
    {
        initUI();
    }

    public OutputSelector addOption(String key, String label, boolean initial)
    {
        JCheckBox button = new JCheckBox(label, initial);
        button.setActionCommand(key);
        button.addActionListener(this);
        remove(_glue);
        add(button);
        add(_glue);

        _keys.add(key);
        _labels.put(key, label);
        _buttons.put(key, button);
        _values.put(key, initial);

        return this;
    }

    public boolean getOption(String key)
    {
        if( _keys.contains(key) ){
            return _values.get(key);
        } else {
            return false;
        }
    }

    public void setOption(String key, boolean value)
    {
        if( _keys.contains(key) ){
            _values.put(key, value);
            if( DEBUG ){
                System.err.println("set "+key+" to: "+String.valueOf(value));
            }
        }
    }

    public boolean saveConfigs(Properties properties)
    {
        Iterator<String> i = _keys.iterator();
        String key;
        while(i.hasNext()){
            key = i.next();
            properties.setProperty("output."+key, String.valueOf(_values.get(key)));
        }
        // properties.setProperty("output.centroid", String.valueOf(calculateCentroid));
        // properties.setProperty("output.minmax", String.valueOf(calculateMinMax));
        // properties.setProperty("output.mask", String.valueOf(calculateMask));
        return true;
    }

    public boolean loadConfigs(Properties properties)
    {
        Iterator<String> i = _keys.iterator();
        String key;
        while(i.hasNext()){
            key = i.next();
            _buttons.get(key).setSelected(Boolean.valueOf(properties.getProperty("output."+key)));
        }
        // _buttons[0].setSelected(Boolean.valueOf(properties.getProperty("output.centroid")));
        // _buttons[1].setSelected(Boolean.valueOf(properties.getProperty("output.minmax")));
        // _buttons[2].setSelected(Boolean.valueOf(properties.getProperty("output.mask")));
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String command = e.getActionCommand();
        setOption(command, _buttons.get(command).isSelected());

        // if( command.equals(_options[0]) )
        // {
        //     calculateCentroid = _buttons[0].isSelected();
        // }
        // else if ( command.equals(_options[1]) )
        // {
        //     calculateMinMax   = _buttons[1].isSelected();
        // }
        // else if ( command.equals(_options[2]) )
        // {
        //     calculateMask     = _buttons[2].isSelected();
        // }
    }

    private void initUI()
    {
        setBorder(BorderFactory.createTitledBorder("Output:"));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        // JCheckBox button;
        // boolean[] initial = {calculateCentroid, calculateMinMax, calculateMask};

        // for( int i=0; i<_options.length; i++ )
        // {
        //     button = new JCheckBox(_options[i], initial[i]);
        //     button.setActionCommand(_options[i]);
        //     button.addActionListener(this);
        //     add(button);
        //     _buttons[i] = button;
        // }
        // add(Box.createVerticalGlue());

        add(_glue);
    }

    public static void main(String[] args)
    {
        OutputSelector out = new OutputSelector();
        out.addOption("centroid", "Centroid", true);
        out.addOption("minmax", "Min/Max", true);
        out.addOption("mask", "Mask image(s)", true);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(out);
        frame.setSize(400, 200);
        frame.setVisible(true);
    }
}
