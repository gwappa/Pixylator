

import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import java.util.Properties;

public class ROIControl extends JPanel
    implements  java.awt.event.ActionListener,
                javax.swing.event.DocumentListener
{
    public static final String CURRENT_ROI  = "current_roi";
    public static final String EXPORT_ROI   = "export_roi";
    public static final String RESET_ROI    = "reset_roi";

    ActionDelegate  _delegate       = null;
    boolean         _valueChanging  = false;
    boolean         _fieldChanging  = false;

    int _x      = 0;
    int _y      = 0;
    int _width  = 1;
    int _height = 1;

    JTextField _xField      = new JTextField(String.valueOf(_x), 5);
    JTextField _yField      = new JTextField(String.valueOf(_y), 5);
    JTextField _widthField  = new JTextField(String.valueOf(_width), 5);
    JTextField _heightField = new JTextField(String.valueOf(_height), 5);

    JButton _readButton = new JButton("Current");
    JButton _writeButton = new JButton("Show");
    JButton _resetButton = new JButton("Full-field");

    public ROIControl()
    {
        setupUI();
    }

    public boolean saveConfigs(Properties properties)
    {
        properties.setProperty("roi.x", String.valueOf(_x));
        properties.setProperty("roi.y", String.valueOf(_y));
        properties.setProperty("roi.width", String.valueOf(_width));
        properties.setProperty("roi.height", String.valueOf(_height));
        return true;
    }

    public boolean loadConfigs(Properties properties)
    {
        try {
            setRoiX(Integer.parseInt(properties.getProperty("roi.x")));
            setRoiY(Integer.parseInt(properties.getProperty("roi.y")));
            setRoiWidth(Integer.parseInt(properties.getProperty("roi.width")));
            setRoiHeight(Integer.parseInt(properties.getProperty("roi.height")));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public int getRoiX()
    {
        return _x;
    }

    public int getRoiY()
    {
        return _y;
    }

    public int getRoiWidth()
    {
        return _width;
    }

    public int getRoiHeight()
    {
        return _height;
    }

    public void setRoiX(int x)
    {
        if( _valueChanging )
            return;

        _valueChanging = true;
        _x = x;
        if( !_fieldChanging )
            _xField.setText(String.valueOf(_x));
        _valueChanging = false;
    }

    public void setRoiY(int y)
    {
        if( _valueChanging )
            return;

        _valueChanging = true;
        _y = y;
        if( !_fieldChanging )
            _yField.setText(String.valueOf(_y));
        _valueChanging = false;
    }

    public void setRoiWidth(int width)
    {
        if( _valueChanging )
            return;

        _valueChanging = true;
        _width = width;
        if( !_fieldChanging )
            _widthField.setText(String.valueOf(_width));
        _valueChanging = false;
    }

    public void setRoiHeight(int height)
    {
        if( _valueChanging )
            return;

        _valueChanging = true;
        _height = height;
        if( !_fieldChanging )
            _heightField.setText(String.valueOf(_height));
        _valueChanging = false;
    }

    public void setDelegate(ActionDelegate delegate)
    {
        _delegate = delegate;
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        if( _delegate != null )
        {
            _delegate.performAction(e.getActionCommand());
        }
        else
        {
            System.err.println("***"+e.getActionCommand()+": delegate not set");
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        handleUpdate(e);
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        handleUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
        handleUpdate(e);
    }

    private void handleUpdate(DocumentEvent e)
    {
        Document src = e.getDocument();
        if( src == _xField.getDocument() )
        {
            try {
                _fieldChanging = true;
                setRoiX(Integer.parseInt(_xField.getText()));
            } catch (NumberFormatException e1) {
                // do nothing (for now)
            } finally {
                _fieldChanging = false;
            }
        } else if( src == _yField.getDocument() )
        {
            try {
                _fieldChanging = true;
                setRoiY(Integer.parseInt(_yField.getText()));
            } catch (NumberFormatException e2) {
                // do nothing (for now)
            } finally {
                _fieldChanging = false;
            }
        } else if( src == _widthField.getDocument() )
        {
            try {
                _fieldChanging = true;
                setRoiWidth(Integer.parseInt(_widthField.getText()));
            } catch (NumberFormatException e2) {
                // do nothing (for now)
            } finally {
                _fieldChanging = false;
            }
        } else if( src == _heightField.getDocument() )
        {
            try {
                _fieldChanging = true;
                setRoiHeight(Integer.parseInt(_heightField.getText()));
            } catch (NumberFormatException e2) {
                // do nothing (for now)
            } finally {
                _fieldChanging = false;
            }
        }
    }

    private void addUI(JComponent comp, GridBagLayout layout, int x, int y, int width, int weight, int anchor)
    {
        GridBagConstraints con = new GridBagConstraints();
        con.gridx       = x;
        con.gridy       = y;
        con.gridwidth   = width;
        con.gridheight  = 1;
        con.weightx     = weight;
        con.weighty     = 1;
        con.anchor      = anchor;

        layout.setConstraints(comp, con);
        add(comp);
    }

    private void setupUI()
    {
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        _readButton.setActionCommand(CURRENT_ROI);
        _readButton.addActionListener(this);
        _resetButton.setActionCommand(RESET_ROI);
        _resetButton.addActionListener(this);
        _writeButton.setActionCommand(EXPORT_ROI);
        _writeButton.addActionListener(this);

        _xField.getDocument().addDocumentListener(this);
        _yField.getDocument().addDocumentListener(this);
        _widthField.getDocument().addDocumentListener(this);
        _heightField.getDocument().addDocumentListener(this);

        addUI(new JLabel("X: "),        layout, 0, 0, 1, 1, GridBagConstraints.LINE_END);
        addUI(new JLabel("Y: "),        layout, 0, 1, 1, 1, GridBagConstraints.LINE_END);
        addUI(new JLabel("Width: "),    layout, 0, 2, 1, 1, GridBagConstraints.LINE_END);
        addUI(new JLabel("Height: "),   layout, 0, 3, 1, 1, GridBagConstraints.LINE_END);
        addUI(_xField,                  layout, 1, 0, 2, 3, GridBagConstraints.LINE_START);
        addUI(_yField,                  layout, 1, 1, 2, 3, GridBagConstraints.LINE_START);
        addUI(_widthField,              layout, 1, 2, 2, 3, GridBagConstraints.LINE_START);
        addUI(_heightField,             layout, 1, 3, 2, 3, GridBagConstraints.LINE_START);
        addUI(_readButton,              layout, 2, 0, 1, 1, GridBagConstraints.LINE_END);
        addUI(_writeButton,             layout, 2, 1, 1, 1, GridBagConstraints.LINE_END);
        addUI(_resetButton,             layout, 2, 4, 1, 1, GridBagConstraints.LINE_END);
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new ROIControl());
        frame.setSize(200, 200);
        frame.setVisible(true);
    }
}


