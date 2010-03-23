// $Id: ComboBox.java,v 1.6 2005/08/22 01:12:28 caron Exp $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.util.prefs.ui;

import thredds.ui.IndependentDialog;
import thredds.ui.IndependentWindow;
import ucar.util.prefs.*;
import java.util.*;
import java.util.List;
import java.io.IOException;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

//import thredds.ui.PopupMenu;

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
 * When listening for change events, generally key on type comboBoxChanged, and you
 *  must explicitly decide to save it in the list:
 *
 * <pre>
 *  cb.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("comboBoxChanged")) {
          Object select = cb.getSelectedItem());
          if (isOK( select))
            cb.addItem( select);
        }
      }
    });
  </pre>

 * @see Field.TextCombo
 * @see PrefPanel#addTextComboField
 *
 * @author John Caron
 * @version $Id: ComboBox.java,v 1.6 2005/08/22 01:12:28 caron Exp $
 */

public class ComboBox extends JComboBox {
  private static final String LIST = "ComboBoxList";
  private boolean deleting = false;

  private PersistenceManager prefs;
  private int nkeep = 20;

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

    if (prefs != null) {
      ArrayList list = (ArrayList) prefs.getList(LIST, null);
      setItemList(list);
    }

    addContextMenu();
  }

  public JComponent getDeepEditComponent() { 
    return (JComponent) getEditor().getEditorComponent();
  }

  private JPopupMenu popupMenu;
  public void addContextMenu() {
    Component editComp = getEditor().getEditorComponent();
    popupMenu = new JPopupMenu();
    editComp.addMouseListener( new PopupTriggerListener() {
      public void showPopup(java.awt.event.MouseEvent e) {
        popupMenu.show(ComboBox.this, e.getX(), e.getY());
      }
    });

    AbstractAction deleteAction = new AbstractAction() {
      public void actionPerformed( java.awt.event.ActionEvent e) {
        final JList delComp= new JList();
        delComp.setModel( getModel());
        delComp.addListSelectionListener(new ListSelectionListener() {
          public void valueChanged(ListSelectionEvent e) {
            int index = delComp.getSelectedIndex();
            deleting = true;
            if (index >= 0)
              removeItemAt( index);
            deleting = false;
          }
        });

        IndependentDialog iw = new IndependentDialog(null, true, "delete items", delComp);
        iw.show();
      }
    };
    deleteAction.putValue( Action.NAME, "Delete");
    popupMenu.add( deleteAction);

    AbstractAction deleteAllAction = new AbstractAction() {
      public void actionPerformed( java.awt.event.ActionEvent e) {
        setItemList( new ArrayList());
      }
    };
    deleteAllAction.putValue( Action.NAME, "Delete All");
    popupMenu.add( deleteAllAction);

  }

  private static abstract class PopupTriggerListener extends MouseAdapter {
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
  public void addItem( Object item) {
    if (item == null) return;
    for (int i=0; i<getItemCount(); i++) {
      if (item.equals( getItemAt(i))) {
        if (i == 0) return; // already there
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
   * @return ArrayList of items, may be any Object type.
   */
  public ArrayList getItemList() {
    ArrayList list = new ArrayList();
    for (int i=0; i< getItemCount() && i < nkeep; i++)
      list.add( getItemAt(i));
    return list;
  }

  /**
   * Use this to set the list of items.
   * @param list of items, may be any Object type.
   */
  public void setItemList(Collection list) {
    if (list == null) return;
    setModel( new DefaultComboBoxModel( list.toArray()));

    if (list.size() > 0)
      setSelectedIndex(0);
  }

  /** Set the number of items to keep */
  public void setNkeep( int nkeep) { this.nkeep = nkeep; }
  /** Get the number of items to keep */
  public int getNkeep() { return nkeep; }

  /** Get value from Store, will be an ArrayList or null */
  protected Object getStoreValue(Object defValue) {
    if (prefs == null)
      return defValue;
    return ((PreferencesExt)prefs).getBean(LIST, defValue);
  }

  /** Put new value into Store, must be a List of Strings */
  protected void setStoreValue(List newValue) {
    if (prefs != null)
      prefs.putList(LIST, newValue);
  }

  // debug
  private static long lastEvent;
  public static void main(String args[]) throws IOException {

    JFrame frame = new JFrame("Test");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    final ComboBox cb = new ComboBox( null);
    cb.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("**** cb event="+e);
        if (e.getActionCommand().equals("comboBoxChanged")) {
          System.out.println("cb.getSelectedItem="+cb.getSelectedItem());
          cb.addItem( cb.getSelectedItem());
        }
      }
    });
    cb.getEditor().getEditorComponent().setForeground(Color.red);

    /* JButton butt = new JButton("accept");
   butt.addActionListener( new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("butt accept");
        cb.accept();
     }
   }); */

    JPanel main = new JPanel();
    main.add(cb);
    // main.add(butt);

    frame.getContentPane().add(main);
    // cb.setPreferredSize(new java.awt.Dimension(500, 200));

    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);
  }

}