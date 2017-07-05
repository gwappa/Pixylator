
package lab.proj.chaos.colortrack;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import java.util.Properties;

/**
*   a UI class for controlling frame range.
*/
public class FrameControl extends JPanel
    implements  java.awt.event.ActionListener,
                javax.swing.event.DocumentListener,
                ParameterModel
{
    private static final long serialVersionUID = 10L;
    private static final String PANEL_TITLE    = "Frames";

    // used for ParameterListener keys
    public static final String FRAME_START      = "frame_start";
    public static final String FRAME_STOP       = "frame_stop";

    // used for ActionDelegate commands
    public static final String SET_FRAME_START  = "set_frame_start";    // action command issued when "Current" is clicked for Start
    public static final String SET_FRAME_STOP   = "set_frame_stop";     // action command issued when "Current" is clicked for Stop
    public static final String SET_FRAME_ALL    = "set_all_frames";     // action command issued when "Current" is clicked for All frames

    ActionDelegate _delegate    = null;
    ParameterNotifier _notifier = null;

    int         _start          = 1;
    int         _stop           = 1;
    boolean     _valueChanging  = false;
    boolean     _fieldChanging  = false;

    JButton     _startButton    = new JButton("Current");
    JButton     _stopButton     = new JButton("Current");
    JButton     _allButton      = new JButton("All frames");
    JTextField  _startField     = new JTextField(String.valueOf(_start), 6);
    JTextField  _stopField      = new JTextField(String.valueOf(_stop),  6);

    public FrameControl()
    {
        _notifier = new ParameterNotifier(this);
        setupUI();
        setBorder(BorderFactory.createTitledBorder(PANEL_TITLE));
    }

    public boolean saveConfigs(Properties properties)
    {
        properties.setProperty("frame.start", String.valueOf(_start));
        properties.setProperty("frame.stop", String.valueOf(_stop));
        return true;
    }

    public boolean loadConfigs(Properties properties)
    {
        try {
            setStart(Integer.parseInt(properties.getProperty("frame.start")));
            setStop(Integer.parseInt(properties.getProperty("frame.stop")));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void setEnabled(boolean value)
    {
        super.setEnabled(value);
        for( Component comp: getComponents() )
            comp.setEnabled(value);
    }

    public int getStart()
    {
        return _start;
    }

    public int getStop()
    {
        return _stop;
    }

    public void setDelegate(ActionDelegate delegate)
    {
        _delegate = delegate;
    }

    public void setStart(int start)
    {
        if( _valueChanging )
            return;

        _valueChanging = true;
        _start = start;
        if( !_fieldChanging )
            _startField.setText(String.valueOf(_start));
        _valueChanging = false;

        _notifier.notifyParameterUpdate(FRAME_START, _start);
    }

    public void setStop(int stop)
    {
        if( _valueChanging )
            return;

        _valueChanging = true;
        _stop = stop;
        if( !_fieldChanging )
            _stopField.setText(String.valueOf(_stop));
        _valueChanging = false;

        _notifier.notifyParameterUpdate(FRAME_STOP, _stop);
    }

    @Override
    public void addParameterListener(ParameterListener l, String role){
        _notifier.addParameterListener(l, role);
    }

    @Override
    public void removeParameterListener(ParameterListener l){
        _notifier.removeParameterListener(l);
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        if( _delegate != null )
        {
            _delegate.performAction(e.getActionCommand());
        } else
        {
            System.err.println("***command '"+e.getActionCommand()+"' will not get issued because there is no delegate.");
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
        if( src == _startField.getDocument() )
        {
            try {
                _fieldChanging = true;
                setStart(Integer.parseInt(_startField.getText()));
            } catch (NumberFormatException e1) {
                // do nothing (for now)
            } finally {
                _fieldChanging = false;
            }
        } else if( src == _stopField.getDocument() )
        {
            try {
                _fieldChanging = true;
                setStop(Integer.parseInt(_stopField.getText()));
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

        _startButton.setActionCommand(SET_FRAME_START);
        _startButton.addActionListener(this);
        _stopButton.setActionCommand(SET_FRAME_STOP);
        _stopButton.addActionListener(this);
        _allButton.setActionCommand(SET_FRAME_ALL);
        _allButton.addActionListener(this);

        _startField.getDocument().addDocumentListener(this);
        _stopField.getDocument().addDocumentListener(this);

        addUI(new JLabel("Start: "),    layout, 0, 0, 1, 1, GridBagConstraints.LINE_END);
        addUI(new JLabel("Stop: "),     layout, 0, 1, 1, 1, GridBagConstraints.LINE_END);
        addUI(_startField,              layout, 1, 0, 2, 2, GridBagConstraints.CENTER);
        addUI(_stopField,               layout, 1, 1, 2, 2, GridBagConstraints.CENTER);
        addUI(_startButton,             layout, 3, 0, 1, 1, GridBagConstraints.LINE_START);
        addUI(_stopButton,              layout, 3, 1, 1, 1, GridBagConstraints.LINE_START);
        addUI(_allButton,               layout, 2, 2, 2, 2, GridBagConstraints.FIRST_LINE_END);
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new FrameControl());
        frame.setSize(250, 120);
        frame.setVisible(true);
    }
}
