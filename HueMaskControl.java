

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;


/**
*   The object that represents the control over the hue masks for object detection.
* 
*   Object name, hue onset, hue offset and the corresponding (mask) color can be obtained through
*   their appropriate getter methods (e.g. getName() and so on). For some of the above properties,
*   there are setter methods (setName(), setOnset(), setOffset()) as well, for the purpose of
*   calling from another object.
*
*   Although a HueMaskControl object can be a stand-alone JComponent, a flexible layout can be done
*   by getting individual layout components (such as nameLabel, nameField, huesample etc.) as 
*   public fields.
*/
public class HueMaskControl extends JComponent
    implements  javax.swing.event.ChangeListener,
                java.awt.event.ActionListener
{
    public static final int UI_GRID_WIDTH       = 5;
    public static final int UI_GRID_HEIGHT      = 2;
    public static final int UI_SLIDER_OFFSETX   = 3;
    public static final int UI_SLIDER_WEIGHT    = 3;

    public JLabel       nameLabel    = null;
    public JTextField   nameField    = null;
    public JLabel       onsetLabel   = null;
    public JSlider      onsetSlider  = null;
    public JTextField   onsetField   = null;

    public JLabel       offsetLabel  = null;
    public JSlider      offsetSlider = null;
    public JTextField   offsetField  = null;

    public JLabel       hueSample    = null;


    String      _name       = null;
    int         _onset      = Hue.MINIMUM;
    int         _offset     = Hue.MAXIMUM;
    Color       _color      = Color.WHITE;
    boolean _sliderChanging = false;
    boolean _fieldChanging  = false;

    public HueMaskControl(String name)
    {
        super();
        _name = name;
        setup();
    }

    /**
    *   implementation for ActionListener (receives events from text fields)
    */
    public void actionPerformed(ActionEvent ae)
    {

        Object src = ae.getSource();
        if ( src == nameField )
        {
            _fieldChanging = true;
            setName(nameField.getText());
            _fieldChanging = false;
        }

        if(_fieldChanging == true)
            return;
        int value = 0;
        if ( src == onsetField )
        {
            try{
                value = Integer.parseInt(onsetField.getText());
                _fieldChanging = true;
                setOnset(value);
                _fieldChanging = false;
            } catch (NumberFormatException e1) {
                onsetField.setText(String.valueOf(_onset));
            } catch (RuntimeException e2) {
                onsetField.setText(String.valueOf(_onset));
            }
        } else if ( src == offsetField )
        {
            try{
                value = Integer.parseInt(offsetField.getText());
                _fieldChanging = true;
                setOffset(value);
                _fieldChanging = false;
            } catch (NumberFormatException e3) {
                offsetField.setText(String.valueOf(_offset));
            } catch (RuntimeException e4) {
                offsetField.setText(String.valueOf(_offset));
            }
        }
    }

    /**
    *   tests whether this HueMaskControl contains the specified [0,360] 'hue' value
    *   i.e. in the range [onset, offset).
    */
    public boolean contains(int hue)
    {
        return (hue >= _onset) && (hue < _offset);
    }

    public Color getColor()
    {
        return _color;
    }

    public String getName()
    {
        return _name;
    }

    public int getOnset()
    {
        return _onset;
    }

    public int getOffset()
    {
        return _offset;
    }

    public static void layoutUI(HueMaskControl mask, java.awt.Container content,
        GridBagLayout layout, int offsetx, int offsety)
    {
        GridBagConstraints con = new GridBagConstraints();

        // add name label
        con.gridx       = offsetx + 0;
        con.gridy       = offsety + 0;
        con.gridwidth   = 1; con.gridheight  = 1;
        con.weightx     = 1;
        layout.setConstraints(mask.nameLabel, con);
        content.add(mask.nameLabel);

        // add name field
        con.gridx       = offsetx + 0;
        con.gridy       = offsety + 1;
        con.gridwidth   = 1; con.gridheight  = 1;
        con.weightx     = 1;
        layout.setConstraints(mask.nameField, con);
        content.add(mask.nameField);

        // add hue sample
        con.gridx       = offsetx + 1;
        con.gridy       = offsety + 0;
        con.gridwidth   = 1; con.gridheight  = 2;
        con.fill        = GridBagConstraints.BOTH;
        con.weightx     = 1;
        layout.setConstraints(mask.hueSample, con);
        content.add(mask.hueSample);

        // add onset label
        con.gridx       = offsetx + 2;
        con.gridy       = offsety + 0;
        con.gridwidth   = 1; con.gridheight  = 1;
        con.fill        = GridBagConstraints.EAST;
        con.weightx     = 1;
        layout.setConstraints(mask.onsetLabel, con);
        content.add(mask.onsetLabel);

        // add offset label
        con.gridx       = offsetx + 2;
        con.gridy       = offsety + 1;
        con.weightx     = 1;
        layout.setConstraints(mask.offsetLabel, con);
        content.add(mask.offsetLabel);

        // add onset slider
        con.gridx       = offsetx + 3;
        con.gridy       = offsety + 0;
        con.fill        = GridBagConstraints.HORIZONTAL;
        con.weightx     = UI_SLIDER_WEIGHT;
        layout.setConstraints(mask.onsetSlider, con);
        content.add(mask.onsetSlider);

        // add offset slider
        con.gridx       = offsetx + 3;
        con.gridy       = offsety + 1;
        con.weightx     = 3;
        layout.setConstraints(mask.offsetSlider, con);
        content.add(mask.offsetSlider);

        // add onset field
        con.gridx       = offsetx + 4;
        con.gridy       = offsety + 0;
        con.fill        = GridBagConstraints.WEST;
        con.weightx     = 1;
        layout.setConstraints(mask.onsetField, con);
        content.add(mask.onsetField);

        // add offset field
        con.gridx       = offsetx + 4;
        con.gridy       = offsety + 1;
        con.weightx     = 1;
        layout.setConstraints(mask.offsetField, con);
        content.add(mask.offsetField);
    }

    public void setName(String name)
    {
        _name = name;
        if(!_fieldChanging)
            nameField.setText(_name);
    }

    /**
    *   actual setter for the onset value.
    *   to prevent infinite loop, flag _sliderChanging or _fieldChanging when updating from UI.
    */
    public void setOnset(int value)
        throws RuntimeException
    {
        if( value < Hue.MINIMUM || value > _offset ){
            throw new RuntimeException();
        }
        _onset = value;
        if( !_sliderChanging ) onsetSlider.setValue(_onset);
        if( !_fieldChanging )  onsetField.setText(String.valueOf(_onset));
        updateSample();
    }

    /**
    *   actual setter for the offset value.
    *   to prevent infinite loop, flag _sliderChanging or _fieldChanging when updating from UI.
    */
    public void setOffset(int value)
        throws RuntimeException
    {
        if( value > Hue.MAXIMUM || value < _onset ){
            throw new RuntimeException();
        }
        _offset = value;
        if( !_sliderChanging ) offsetSlider.setValue(_offset);
        if( !_fieldChanging )  offsetField.setText(String.valueOf(_offset));
        updateSample();
    }

    /**
    *   sets up the GUI components
    */
    private void setup()
    {

        GridBagConstraints con = new GridBagConstraints();

        nameLabel       = new JLabel("Object name:");
        // TODO: set alignment

        nameField       = new JTextField(_name, 10);
        nameField.addActionListener(this);

        hueSample       = new JLabel("    \n    \n");
        hueSample.setOpaque(true);
        updateSample();

        onsetLabel      = new JLabel("Onset: ");
        // TODO: set alignment
        offsetLabel     = new JLabel("Offset: ");
        // TODO: set alignment

        onsetSlider     = new JSlider(JSlider.HORIZONTAL, Hue.MINIMUM, Hue.MAXIMUM, _onset);
        onsetSlider.addChangeListener(this);

        onsetField      = new JTextField(String.valueOf(_onset), 4);
        onsetField.addActionListener(this);

        offsetSlider    = new JSlider(JSlider.HORIZONTAL, Hue.MINIMUM, Hue.MAXIMUM, _offset);
        offsetSlider.addChangeListener(this);

        offsetField      = new JTextField(String.valueOf(_offset), 4);
        offsetField.addActionListener(this);

        GridBagLayout gridbag = new GridBagLayout();
        setLayout(gridbag);
        layoutUI(this, this, gridbag, 0, 0);
    }

    /**
    *   implementation for ChangeListener (receives from sliders)
    */
    public void stateChanged(ChangeEvent e)
    {
        if( _sliderChanging == true )
            return;

        Object src = e.getSource();

        if( src == onsetSlider )
        {
            _sliderChanging = true;
            try{
                setOnset(onsetSlider.getValue());
            } catch (RuntimeException e1) {
                onsetSlider.setValue(_onset);
            }
            _sliderChanging = false;
        } else if ( src == offsetSlider )
        {
            _sliderChanging = true;
            try {   
                setOffset(offsetSlider.getValue());
            } catch (RuntimeException e2) {
                offsetSlider.setValue(_offset);
            }
            _sliderChanging = false;
        }
    }

    /**
    *   change the color sample, following the change in onset or offset
    */
    private void updateSample()
    {
        _color = Hue.getColor((_onset + _offset)/2);
        hueSample.setBackground(_color);
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new HueMaskControl("test"));
        frame.setSize(600, 180);
        frame.setVisible(true);
    }
}
