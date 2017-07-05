
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
*   a UI/Config for selection of statistics
*/
public class MeasurementControl extends JPanel
    implements java.awt.event.ActionListener
{
    private static final long serialVersionUID  = 10L;
    private static final String PANEL_TITLE     = "Measurements";
    private static final String CONFIG_KEY      = "measure";

    static final boolean DEBUG = false;

    List<String>            _keys       = new LinkedList<String>();
    Map<String, Measurement> _calcs    = new Hashtable<String, Measurement>();
    Map<String, String>     _labels     = new Hashtable<String, String>();
    Map<String, JCheckBox>  _buttons    = new Hashtable<String, JCheckBox>();
    Map<String, Boolean>    _values     = new Hashtable<String, Boolean>();

    Component               _glue       = Box.createVerticalGlue();

    public MeasurementControl()
    {
        initUI();
    }

    public MeasurementControl addMeasurement(Measurement calc)
    {
        String key = calc.getParamKey();
        boolean initial = false;

        JCheckBox button = new JCheckBox(calc.getParamName(), initial);
        button.setActionCommand(key);
        button.addActionListener(this);
        remove(_glue);
        add(button);
        add(_glue);

        _keys.add(key);
        _calcs.put(key, calc);
        _buttons.put(key, button);
        _values.put(key, initial);

        return this;
    }

    public List<Measurement> getEnabledMeasurements()
    {
        List<Measurement> available = new LinkedList<Measurement>();
        Iterator<String> it = _keys.iterator();
        String key;
        while(it.hasNext()){
            key = it.next();
            if( _values.get(key).equals(true) ){
                available.add(_calcs.get(key));
            }
        }
        return available;
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
            properties.setProperty(CONFIG_KEY+"."+key, String.valueOf(_values.get(key)));
        }
        return true;
    }

    public boolean loadConfigs(Properties properties)
    {
        Iterator<String> i = _keys.iterator();
        String key;
        while(i.hasNext()){
            key = i.next();
            _buttons.get(key).setSelected(Boolean.valueOf(properties.getProperty(CONFIG_KEY+"."+key)));
        }
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        String key = e.getActionCommand();
        setOption(key, _buttons.get(key).isSelected());

    }

    private void initUI()
    {
        setBorder(BorderFactory.createTitledBorder(PANEL_TITLE));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        add(_glue);
    }
}
