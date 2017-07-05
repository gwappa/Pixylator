/**
*   ResultsTableOutput.java
*   @author Keisuke Sehara
*/

package lab.proj.chaos.colortrack;

import java.io.IOException;
import java.util.List;

import ij.IJ;
import ij.measure.ResultsTable;

public class ResultsTableOutput
    implements MeasurementOutput
{
    static final boolean DEBUG  = false;

    static final String NAME            = "ImageJ results table";
    static final String KEY             = "results_table";
    static final String COL_SLICE       = "Slice";
    static final String DEFAULT_NAME    = "Results";
    static final String SEP             = "_";

    String          tableName   = DEFAULT_NAME;
    ResultsTable    table       = null;
    int             current     = 0;

    @Override
    public String getParamName(){
        return NAME;
    }

    @Override
    public String getParamKey(){
        return KEY;
    }

    @Override
    public void startOutput(String title, int width, int height, int slices) throws IOException
    {
        String base = FileNameFunctions.getBaseName(title);
        tableName   = PREFIX + base;
        table       = new ResultsTable();
        if( DEBUG ){
            IJ.log("ResultsTableOutput: created a table for: "+tableName);
        }
    }

    @Override
    public void endOutput(int slices) throws IOException
    {
        table.showRowNumbers(false);
        table.show(tableName);
        table       = null;
        tableName   = DEFAULT_NAME;
        current     = 0;
    }

    @Override
    public void nextSlice(int slice) throws IOException
    {
        table.incrementCounter();
        current = table.getCounter();
        table.addValue(COL_SLICE, String.valueOf(current));
        if( DEBUG ){
            IJ.log("ResultsTableOutput: starting frame #"+current+"...");
        }
    }

    @Override
    public void print(String category, String attrname, double value) throws IOException
    {
        if( DEBUG ){
            IJ.log(String.format("%s: %s.%s -> %f", "ResultsTableOutput", category, attrname, value));
        }
        table.addValue(category+SEP+attrname, value);
    }

    @Override
    public void print(String category, String attrname, int value) throws IOException
    {
        if( DEBUG ){
            IJ.log(String.format("%s: %s.%s -> %d", "ResultsTableOutput", category, attrname, value));
        }
        table.addValue(category+SEP+attrname, String.valueOf(value));
    }
}
