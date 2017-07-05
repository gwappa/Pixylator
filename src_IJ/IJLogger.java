/**
*   IJLogger.java
*   @author Keisuke Sehara
*/
package lab.proj.chaos.colortrack;

import java.io.StringWriter;
import java.io.PrintWriter;

import ij.IJ;

public class IJLogger
{
    public static void logError(Throwable e)
    {
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        IJ.log(w.toString());
    }
}
