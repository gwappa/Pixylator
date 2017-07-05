/**
*   CSVOutput.java
*   @author Keisuke Sehara
*/

package lab.proj.chaos.colortrack;

import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import ij.IJ;
import ij.io.SaveDialog;

public class CSVOutput
    implements MeasurementOutput
{
    static final boolean    DEBUG           = false;
    static final String     OUTPUT_NAME     = "CSV file output";
    static final String     OUTPUT_KEY      = "csv_output";
    static final String     EXT             = ".csv";
    static final String     ATTR_SEP        = "_";
    static final String     COL_SEP         = ",";

    PrintWriter     out     = null;
    boolean headerWritten   = false;
    List<String>    headers = new LinkedList<String>();
    List<String>    values  = new LinkedList<String>();

    @Override
    public String getParamName()
    {
        return OUTPUT_NAME;
    }

    @Override
    public String getParamKey()
    {
        return OUTPUT_KEY;
    }

    @Override
    public void startOutput(String title, int width, int height, int nslice) throws IOException
    {
        SaveDialog  dialog      = new SaveDialog("Save results as CSV...", PREFIX+FileNameFunctions.getBaseName(title), EXT);
        String      filename    = dialog.getFileName();
        if( filename == null ){
            throw new IOException("CSVOutput canceled Pixylation");
        }
        String      path        = new java.io.File(dialog.getDirectory(), filename).getAbsolutePath();

        out = new PrintWriter(path);
        headers.clear();
        values.clear();
        headerWritten = false;
    }

    @Override
    public void nextSlice(int slice) throws IOException
    {
        flush();
        if( !headerWritten ){
            headers.add("Slice");
        }
        values.add(String.valueOf(slice));

        if( DEBUG ){
            IJ.log("CSVOutput: starting slice #"+slice+"...");
        }
    }

    @Override
    public void endOutput(int slices) throws IOException
    {
        flush();
        out.close();
        out = null;
        headerWritten = false;
        if( DEBUG ){
            IJ.log("CSVOutput: closed.");
        }
    }

    @Override
    public void print(String category, String attrname, double value) throws IOException
    {
        if( !headerWritten ){
            headers.add(category+ATTR_SEP+attrname);
        }
        values.add(String.valueOf(value));
    }

    @Override
    public void print(String category, String attrname, int value) throws IOException
    {
        if( !headerWritten ){
            headers.add(category+ATTR_SEP+attrname);
        }
        values.add(String.valueOf(value));
    }

    protected void printlnList(PrintWriter out, String sep, List<String> iterable)
    {
        Iterator<String> it = iterable.iterator();
        boolean first = true;
        while(it.hasNext()){
            if( first ){
                first = false;
            } else {
                out.print(sep);
            }
            out.print(it.next());
        }
        out.println();
    }

    protected void flush()
    {
        if( headers.size() == 0 ){
            // no value yet
            return;
        }
        if( !headerWritten ){
            // write headers
            printlnList(out, COL_SEP, headers);
            headerWritten = true;
        }
        // write values
        printlnList(out, COL_SEP, values);
        values.clear();
        out.flush();
    }
}
