
package lab.proj.chaos.colortrack;

public interface ParameterModel
{
    void addParameterListener(ParameterListener l, String role);
    void removeParameterListener(ParameterListener l);
}
