/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

/** Convenience class for constructing popup menus.
 *
 * Example: add to a JTable:
 *
 *    JTable jtable = table.getJTable();
      PopupMenu csPopup = new PopupMenu(jtable, "Options");
      csPopup.addAction("Show Declaration", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          showDeclaration(table);
        }
      });

 *
 */

public class PopupMenu extends JPopupMenu {
  private JComponent parent;

  /** Constructor.
    *
    * @param parent A MouseListener is added to this JComponent.
    * @param menuTitle title of the popup menu.
    */
   public PopupMenu(JComponent parent, String menuTitle) {
     this( parent, menuTitle, false);
   }

  /** Constructor.
    *
    * @param parent A MouseListener is added to this JComponent.
    * @param menuTitle title of the popup menu.
   *  @param anyButton if true, any button activates, otherwise use MouseEvent.isPopupTrigger
    */
   public PopupMenu(JComponent parent, String menuTitle, boolean anyButton) {
     super(menuTitle);
     this.parent = parent;
     setBorderPainted( true);
     setLabel( menuTitle);

     parent.addMouseListener( new PopupTriggerListener(anyButton) {
       public void showPopup(java.awt.event.MouseEvent e) {
         show(PopupMenu.this.parent, e.getX(), e.getY());
       }
     });
   }

   /** Add an action to the popup menu.
   * Note that the menuName is made the NAME value of the action.
   * @param menuName name of the action on the menu.
   * @param act the Action.
   */
  public void addAction( String menuName, Action act) {
    act.putValue( Action.NAME, menuName);
    super.add(act);
  }

  /** Add an action to the popup menu, with an icon.
   * Note that the menuName is made the NAME value of the action.
   * @param menuName name of the action on the menu.
   * @param act the Action.
   */
  public void addAction( String menuName, String iconName, Action act) {
    addAction( menuName, BAMutil.getIcon( iconName, true), act);
  }

  /** Add an action to the popup menu, with an icon.
   * Note that the menuName is made the NAME value of the action.
   * @param menuName name of the action on the menu.
   * @param act the Action.
   */
  public void addAction( String menuName, ImageIcon icon, Action act) {
    act.putValue( Action.NAME, menuName);
    act.putValue( Action.SMALL_ICON, icon);
    JMenuItem mi = add(act);
    mi.setHorizontalTextPosition( SwingConstants.LEFT );
  }

  /** Add an action to the popup menu, using a JCheckBoxMenuItem.
   * Fetch the toggle state using:
   * <pre>Boolean state = (Boolean) act.getValue(BAMutil.STATE); </pre>
   * @param menuName name of the action on the menu.
   * @param act the Action.
   * @param state : initial state of the checkbox
   */
  public void addActionCheckBox( String menuName, AbstractAction act, boolean state) {
    JMenuItem mi = new JCheckBoxMenuItem(menuName, state);
    mi.addActionListener( new BAMutil.ActionToggle(act, mi));
    act.putValue(BAMutil.STATE, state);
    add( mi);
  }

  public JComponent getParentComponent() { return parent; }

  public abstract static class PopupTriggerListener extends MouseAdapter {
    private boolean anyButton = false;
    PopupTriggerListener( boolean anyButton) {
      this.anyButton = anyButton;
    }

    PopupTriggerListener( ) {}

    public void mousePressed (MouseEvent e) {
      // System.out.println( "PopupTriggerListener "+e);
      if(anyButton || e.isPopupTrigger())
        showPopup(e);
    }
    public void mouseClicked (MouseEvent e) {
      if(anyButton || e.isPopupTrigger())
        showPopup(e);
    }
    public void mouseReleased (MouseEvent e) {
      if(anyButton || e.isPopupTrigger())
        showPopup(e);
    }

    public abstract void showPopup(MouseEvent e);
  }
}
