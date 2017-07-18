/**
*   OutputControl.java
*   @author Keisuke Sehara
*/
package lab.proj.chaos.colortrack;

import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;

import java.util.Properties;

/**
*   the user interface class for controlling Measuerment/Mask/Preview outputs.
*/
public class OutputControl
    extends javax.swing.JPanel
{
    private static final long serialVersionUID  = 10L;
    private static final String PANEL_TITLE     = "Output";

    private static final String LABEL_RESULTS   = "Results output:";
    private static final String LABEL_MASK      = "Mask output:";
    private static final String LABEL_PREVIEW   = "Preview";

    OutputSelector<MeasurementOutput>   results = new OutputSelector<MeasurementOutput>();
    OutputSelector<MaskOutput>          mask    = new OutputSelector<MaskOutput>();

    public OutputControl(){
        // configure measurement outputs
        results.addOutput(new CSVOutput());
        results.addOutput(new ResultsTableOutput());
        results.addOutput(new NoMeasurementOutput());
        // configure mask outputs
        mask.addOutput(new H264MaskOutput());
        mask.addOutput(new ImageStackOutput());
        mask.addOutput(new NoMaskOutput());
        initUI();
    }

    public boolean saveConfigs(Properties properties)
    {
        properties.setProperty("output.measurement", results.getSelectedOutput().getElementKey());
        properties.setProperty("output.mask", mask.getSelectedOutput().getElementKey());
        return true;
    }

    public boolean loadConfigs(Properties properties)
    {
        try {
            results.setSelectedKey(properties.getProperty("output.measurement"));
            mask.setSelectedKey(properties.getProperty("output.mask"));
        } catch (RuntimeException e) {
            IJLogger.logError(e);
        }
        try {
            mask.setSelectedKey(properties.getProperty("output.mask"));
        } catch (RuntimeException e) {
            IJLogger.logError(e);
        }
        return true;
    }

    public MeasurementOutput getMeasurementOutput(){
        return results.getSelectedOutput();
    }

    public MaskOutput getMaskOutput(){
        return mask.getSelectedOutput();
    }

    protected void initUI(){
        setBorder(javax.swing.BorderFactory.createTitledBorder(PANEL_TITLE));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(new JLabel(LABEL_RESULTS), true);
        add(new JComboBox<String>(results), false);
        add(new JLabel(LABEL_MASK), true);
        add(new JComboBox<String>(mask), false);
        JCheckBox doPreview = new JCheckBox(LABEL_PREVIEW);
        doPreview.setEnabled(false);
        add(doPreview, false);
        add(Box.createVerticalGlue());
    }

    public void add(javax.swing.JComponent comp, boolean isLabel){
        comp.setAlignmentX(0.0f); // left aligned
        comp.setAlignmentY(isLabel? 1.0f:0.0f);
        super.add(comp);
    }
}
