
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.event.DocumentEvent;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;

import java.util.Properties;


/**
*   a UI class for controlling histogram.
*/
public class HistogramControl extends JPanel
    implements  java.awt.event.ActionListener,
                javax.swing.event.DocumentListener
{
    public static final int INITIAL_SAMPLE_NUMBER   = 1000000;

    public static final String RESET_SAMPLE_NUMBER  = "reset_histogram_sample_number";
    public static final String REPLOT_HISTOGRAM     = "replot_histogram";


    ActionDelegate  _delegate       = null;
    boolean         _valueChanging  = false;
    boolean         _fieldChanging  = false;

    int _number             = INITIAL_SAMPLE_NUMBER;
    JTextField _numberField = new JTextField(String.valueOf(_number), 15);
    JButton _revertButton    = new JButton("Default");
    JButton _replotButton   = new JButton("Re-plot");

    public HistogramControl()
    {
        setupUI();
    }

    public boolean saveConfigs(Properties properties)
    {
        properties.setProperty("histo.samplenumber", String.valueOf(_number));
        return true;
    }

    public boolean loadConfigs(Properties properties)
    {
        try {
            setSampleNumber(Integer.parseInt(properties.getProperty("histo.samplenumber")));
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

    public int getSampleNumber()
    {
        return _number;
    }

    public void setSampleNumber(int number)
    {
        if( _valueChanging )
            return;

        _valueChanging = true;
        _number = number;
        if( !_fieldChanging )
            _numberField.setText(String.valueOf(_number));
        _valueChanging = false;
    }

    public void setDelegate(ActionDelegate delegate)
    {
        _delegate = delegate;
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e)
    {
        String command = e.getActionCommand();
        if( command.equals(RESET_SAMPLE_NUMBER) )
        {
            setSampleNumber(INITIAL_SAMPLE_NUMBER);
        } else if( _delegate != null )
        {
            _delegate.performAction(command);
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
        if( src == _numberField.getDocument() )
        {
            try {
                _fieldChanging = true;
                setSampleNumber(Integer.parseInt(_numberField.getText()));
            } catch (NumberFormatException e1) {
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

        _revertButton.setActionCommand(RESET_SAMPLE_NUMBER);
        _revertButton.addActionListener(this);
        _replotButton.setActionCommand(REPLOT_HISTOGRAM);
        _replotButton.addActionListener(this);

        _numberField.getDocument().addDocumentListener(this);

        addUI(new JLabel("Max sample number: "), layout, 0, 0, 3, 3, GridBagConstraints.LAST_LINE_START);
        addUI(_numberField,                      layout, 0, 1, 3, 5, GridBagConstraints.FIRST_LINE_START);
        addUI(_replotButton,                     layout, 2, 2, 1, 1, GridBagConstraints.LINE_START);
        addUI(_revertButton,                      layout, 0, 2, 1, 1, GridBagConstraints.LINE_END);
    }

    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new HistogramControl());
        frame.setSize(200, 200);
        frame.setVisible(true);
    }
}









