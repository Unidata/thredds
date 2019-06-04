/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.ui.prefs;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import ucar.ui.widget.ActionListenerAdapter;
import ucar.ui.widget.IndependentDialog;
import ucar.util.prefs.PersistenceManager;
import ucar.util.prefs.PreferencesExt;

/**
 * A simple extension to JComboBox, which persists the n latest values.
 * The JComboBox is editable; user can add a new String, then if acceptable,
 * the calling routine should transform it to correct object type and call addItem().
 * When item is added, it is placed on the top of the list, so it is more likely to be saved.
 * <p> The items in the list can be any Object type with these caveats:
 *  <ul>
 *   <li> item.toString() used as display name
 *   <li> item.equals() used for object equality
 *   <li> prefs.putBeanObject() used for storage, so XMLEncoder used, so object must have no-arg Constructor.
 *  </ul>
 *
 * When listening for change events, typically use addChangeListener(), which only throws an event on "comboBoxChanged".
 * You must explicitly decide to save the selected Item in the list, eg on success.
 * This moves it up the top of the saved list.
 *
 * <pre>
 *  cb.addChangeListener((e) -> {
      Object select = cb.getSelectedItem());
      if (isOK(select)) cb.addItem( select);
    });
  </pre>
 * @author John Caron
 */
public class ComboBox<E> extends JComboBox<E> {
  private static final String LIST = "ComboBoxList";
  private boolean deleting = false;

  private PersistenceManager prefs;
  private int nkeep;

  public ComboBox() {
    this( null, 20);
  }

  /**
   * Constructor.
   * @param prefs get/put list here; may be null.
   */
  public ComboBox(PersistenceManager prefs) {
    this( prefs, 20);
  }

  /**
   * Constructor.
   * @param prefs get/put list here; may be null.
   * @param nkeep keep this many when you save.
   */
  public ComboBox(PersistenceManager prefs, int nkeep) {
    super();
    this.prefs = prefs;
    this.nkeep = nkeep;
    setEditable(true);
    setPreferences(prefs);
    addContextMenu();
  }

  /**
   * Add a listener that gets called only when the selected item changes.
   */
  public void addChangeListener(ActionListener successListener) {
    addActionListener(new ActionListenerAdapter("comboBoxChanged", successListener));
  }

  public void setPreferences(PersistenceManager prefs) {
    this.prefs = prefs;
    if (prefs != null) {
      List<E> list = (List<E>) prefs.getList(LIST, null);
      setItemList(list);
      setSelectedIndex(-1); // nothing is selected, so first selection causes change event.
    }
  }

  public JComponent getDeepEditComponent() {
    return (JComponent) getEditor().getEditorComponent();
  }

  private JPopupMenu popupMenu;
  private void addContextMenu() {
    Component editComp = getEditor().getEditorComponent();
    popupMenu = new JPopupMenu();
    editComp.addMouseListener( new PopupTriggerListener() {
      public void showPopup(java.awt.event.MouseEvent e) {
        popupMenu.show(ComboBox.this, e.getX(), e.getY());
      }
    });

    AbstractAction deleteAction = new AbstractAction() {
      public void actionPerformed( java.awt.event.ActionEvent e) {
        final JList<E> delComp= new JList<>();
        delComp.setModel( getModel());
        delComp.addListSelectionListener(e2 -> {
            int index = delComp.getSelectedIndex();
            deleting = true;
            if (index >= 0) {
              removeItemAt( index);
            }
            deleting = false;
        });

        IndependentDialog iw = new IndependentDialog(null, true, "delete items", delComp);
        iw.setVisible(true);
      }
    };
    deleteAction.putValue( Action.NAME, "Delete");
    popupMenu.add( deleteAction);

    AbstractAction deleteAllAction = new AbstractAction() {
      public void actionPerformed( java.awt.event.ActionEvent e) {
        setItemList( new ArrayList<>());
      }
    };
    deleteAllAction.putValue( Action.NAME, "Delete All");
    popupMenu.add( deleteAllAction);

  }

  private abstract static class PopupTriggerListener extends MouseAdapter {
    public void mouseReleased (MouseEvent e) { if(e.isPopupTrigger()) showPopup(e); }
    public abstract void showPopup(MouseEvent e);
  }

  protected void fireActionEvent() {
    if (deleting) return; // no events while deleting
    super.fireActionEvent();
  }

  /**
   * Add the item to the top of the list. If it already exists, move it to the top.
   * @param item to be added.
   */
  public void addItem(E item) {
    if (item == null) return;
    for (int i=0; i<getItemCount(); i++) {
      if (item.equals( getItemAt(i))) {
        if (i == 0) {
          setSelectedIndex(0);
          return; // already there
        }
        removeItemAt(i);
      }
    }

    // add as first in the list
    insertItemAt( item, 0);
    setSelectedIndex(0);
  }

  /** Save the last n items to PreferencesExt. */
  public void save() {
    if (prefs != null)
      prefs.putList(LIST, getItemList());
  }

  /**
   * Use this to obtain the list of items.
   * @return ArrayList of items, of type E.
   */
  public List<E> getItemList() {
    ArrayList<E> list = new ArrayList<>();
    for (int i=0; i< getItemCount() && i < nkeep; i++)
      list.add( getItemAt(i));
    return list;
  }

  /**
   * Use this to set the list of items.
   * @param list of items of type E.
   */
  public void setItemList(Collection<E> list) {
    if (list == null) return;
    setModel( new DefaultComboBoxModel<>( (E[]) list.toArray()));
  }

  /** Set the number of items to keep */
  public void setNkeep( int nkeep) { this.nkeep = nkeep; }
  /** Get the number of items to keep */
  public int getNkeep() { return nkeep; }

  /** Get value from Store, will be an ArrayList or null */
  protected Object getStoreValue(Object defValue) {
    if (prefs == null) return defValue;
    return ((PreferencesExt)prefs).getBean(LIST, defValue);
  }

  /** Put new value into Store, must be a List of Strings */
  protected void setStoreValue(List<E> newValue) {
    if (prefs != null)
      prefs.putList(LIST, newValue);
  }
}
