
package lab.proj.chaos.colortrack;

public class FileNameFunctions
{
    public static String getBaseName(String title)
    {
        int dot = title.lastIndexOf(".");
        if( dot == -1 ){
            return title;
        } else {
            return title.substring(0, dot);
        }
    }
}
