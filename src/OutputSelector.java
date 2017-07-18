/**
*   MeasurementOutputSelector.java
*   @author Keisuke Sehara
*/

package lab.proj.chaos.colortrack;

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Hashtable;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

public class OutputSelector<T extends TrackerElement>
    implements javax.swing.ComboBoxModel<String>
{
    List<ListDataListener>  listeners   = new LinkedList<ListDataListener>();
    List<String>            labels      = new LinkedList<String>();
    Map<String, T>          workers     = new Hashtable<String, T>();

    String selection = null;


    public void update(){
        if( labels.size() == 0 ){
            setSelectedItem(null);
        } else if ( selection == null ){
            setSelectedItem(labels.get(0));
        } else {
            fireContentsChanged(); // just notify with the contents change
        }
    }

    public void addOutput(T output){
        String label = output.getElementName();
        labels.add(label);
        workers.put(label, output);
        update();
    }

    public T getSelectedOutput(){
        return (selection == null)? null: workers.get(selection);
    }

    /**
    *   notifies when only the contents change, whereas selection is unaltered.
    */
    protected void fireContentsChanged(){
        ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, labels.size());
        Iterator<ListDataListener> it = listeners.iterator();
        while(it.hasNext())
            it.next().contentsChanged(e);
    }

    @Override
    public Object getSelectedItem(){
        return selection;
    }

    @Override
    public void setSelectedItem(Object item){
        if( item == null ){
            selection = null;
            fireContentsChanged();

        } else if (item instanceof String) {
            String newvalue = (String)item;
            if( labels.contains(newvalue) ){
                selection = newvalue;
                fireContentsChanged();
            }
        }
    }

    public void setSelectedKey(String key) throws RuntimeException{
        if( key == null ){
            selection = null;
            fireContentsChanged();
        } else {
            Iterator<T> it = workers.values().iterator();
            T selected = null;
            T worker;
            while(it.hasNext()){
                worker = it.next();
                if( worker.getElementKey() == key ){
                    selected = worker;
                    break;
                }
            }
            if( selected == null ){
                selection = null;
                throw new RuntimeException(String.format("The output with name '%s' was not found.", key));
            } else {
                selection = selected.getElementName();
            }
        }
    }

    @Override
    public int getSize(){
        return labels.size();
    }

    @Override
    public String getElementAt(int index){
        return labels.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l){
        listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l){
        listeners.remove(l);
    }
}
