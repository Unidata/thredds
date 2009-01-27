// $Id: PopupMenu.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.ui;

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
    act.putValue(BAMutil.STATE, new Boolean(state));
    add( mi);
  }

  public JComponent getParentComponent() { return parent; }

  public static abstract class PopupTriggerListener extends MouseAdapter {
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
/* Change History:
   $Log: PopupMenu.java,v $
   Revision 1.4  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.3  2004/09/28 21:39:09  caron
   *** empty log message ***

   Revision 1.2  2004/09/24 03:26:34  caron
   merge nj22

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.1.1.1  2002/02/15 00:01:48  caron
   import sources

*/
