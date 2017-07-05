
package lab.proj.chaos.colortrack;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

public class ImageSelector
    implements javax.swing.ComboBoxModel<String>, PixylatorDirectives
{
    static final boolean DEBUG = false;

    List<ListDataListener> uiListeners = new LinkedList<ListDataListener>();
    List<ImageSelectionListener> imListeners = new LinkedList<ImageSelectionListener>();

    List<String> imageTitles = new LinkedList<String>();
    String selection = null;

    public ImageSelector()
    {
        update();
    }

    /**
    *   updates with the list of currently open images.
    *   changes selection if the previous selection is not available anymore.
    */
    public void update(){
        String[] titles = WindowManager.getImageTitles();
        imageTitles.clear();
        for(String title: titles){
            if( title.equals(PREVIEW_WINDOW_TITLE) ){
                continue;
            }
            imageTitles.add(title);
        }

        if( imageTitles.size() == 0 ){
            setSelectedIndex(-1);
        } else if ( (selection == null) || !imageTitles.contains(selection) ){
            ImagePlus im = WindowManager.getCurrentImage();
            if( im != null ){
                selection = im.getTitle();
                fireSelectionChanged();
            } else {
                setSelectedIndex(0);
            }
        } else {
            fireContentsChanged(); // just notify with the contents change
        }
    }

    public ImagePlus getSelectedImage(){
        return (selection == null)? null: WindowManager.getImage(selection);
    }

    /**
    *   notifies when only the contents change, whereas selection is unaltered.
    */
    protected void fireContentsChanged(){
        ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, imageTitles.size());
        Iterator<ListDataListener> uit = new LinkedList<ListDataListener>(uiListeners).iterator();
        while(uit.hasNext())
            uit.next().contentsChanged(e);
    }

    /**
    *   notifies if the selection has been changed (it implies the contents change i.e. fireContentsChanged() is also called).
    */
    protected void fireSelectionChanged(){
        try {
            // notify ListDataListener's
            fireContentsChanged();

            // notify ImageSelectionListener's
            Iterator<ImageSelectionListener> it = imListeners.iterator();
            ImagePlus im = null;
            if( selection != null ){
                im = WindowManager.getImage(selection);
            }
            while(it.hasNext())
                it.next().selectedImageChanged(im);
        } catch (Throwable t) {
            IJLogger.logError(t);
        }
    }

    public void setSelectedIndex(int i){
        if( (i>=0) && (i<imageTitles.size()) ){
            selection = imageTitles.get(i);
            if( DEBUG ){
                IJ.log("image selection changed: "+selection);
            }
            fireSelectionChanged();
        } else if (i == -1) {
            selection = null;
            if( DEBUG ){
                IJ.log("no image is selected.");
            }
            fireSelectionChanged();
        } else {
            IJ.log("ImageSelector: cannot handle index #"+i);
        }
    }

    public int getSelectedIndex(){
        if( selection == null ){
            return -1;
        } else {
            return imageTitles.indexOf(selection);
        }
    }

    @Override
    public Object getSelectedItem(){
        return selection;
    }

    @Override
    public void setSelectedItem(Object item){
        if( item == null ){
            selection = null;
            fireSelectionChanged();

        } else if (item instanceof String) {
            String newvalue = (String)item;
            if( imageTitles.contains(newvalue) ){
                selection = newvalue;
                fireSelectionChanged();
            }
        }
    }

    @Override
    public int getSize(){
        return imageTitles.size();
    }

    @Override
    public String getElementAt(int index){
        return imageTitles.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l){
        uiListeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l){
        uiListeners.remove(l);
    }

    public void addImageSelectionListener(ImageSelectionListener l){
        imListeners.add(l);
    }

    public void removeImageSelectionListener(ImageSelectionListener l){
        imListeners.remove(l);
    }
}
