
package lab.proj.chaos.colortrack;

import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
*   a modular implementation of ParameterModel.
*   it takes care of handling listeners and corresponding 'role' keys for the parent object.
*/
public class ParameterNotifier
    implements ParameterModel
{
    List<ParameterListener>         listeners   = new java.util.LinkedList<ParameterListener>();
    Map<ParameterListener, String>  roles       = new java.util.Hashtable<ParameterListener, String>();
    Object parent = null;

    public ParameterNotifier(Object parent)
    {
        this.parent = parent;
    }

    public void addParameterListener(ParameterListener l, String role)
    {
        if( !listeners.contains(l) )
            listeners.add(l);
        roles.put(l, role);
    }

    public void removeParameterListener(ParameterListener l)
    {
        if( listeners.contains(l) )
            listeners.remove(l);
        if( roles.containsKey(l) )
            roles.remove(l);
    }

    public void notifyParameterUpdate(String key, int value)
    {
        Iterator<ParameterListener> it = listeners.iterator();
        ParameterListener l;
        while(it.hasNext()){
            l = it.next();
            l.parameterUpdate(parent, roles.get(l), key, value);
        }
    }
}
