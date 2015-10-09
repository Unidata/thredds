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
package ucar.nc2.ui.widget;

import ucar.nc2.ui.util.Resource;

import java.awt.*;
import javax.swing.*;


/**
 * Button, Action and Menu utilities:
 * static stationHelper methods for building ucar.unidata.UI's.
 *
 * <p> Example for Toggle Action
 * <pre>
 * AbstractAction dsAction =  new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
     Boolean state = (Boolean) getValue( BAMutil.STATE);
     addCoords = state.booleanValue();
     String tooltip = addCoords ? "add Coordinates in ON" : "add Coordinates is OFF";
     dsButt.setToolTipText(tooltip);
    }
   };
   BAMutil.setActionProperties( dsAction, "Dataset", "add Coordinates is OFF", true, 'D', -1);
   addCoords = prefs.getBoolean( "dsState", false);
   dsAction.putValue(BAMutil.STATE, new Boolean(addCoords));
   AbstractButton dsButt = BAMutil.addActionToContainer(buttPanel, dsAction);

   ...
   prefs.putBoolean("dsState", dsButt.getModel().isSelected());


    </pre>
 *
 * @author John Caron
 */
public class BAMutil {
    /** Action Property specifies Selected icon name */
  public static final String SELECTED_ICON = "SelectedIcon";
    /** Action Property specifies is its a toggle */
  public static final String TOGGLE = "isToggle";
    /** Action Property specifies menu mneumonic */
  public static final String MNEMONIC = "mnemonic";
    /** Action Property specifies menu accelerator */
  public static final String ACCEL = "accelerator";
    /** the state of "toggle" actions = Boolean */
  public static final String STATE = "state";

  static private String defaultResourcePath = "/resources/nj22/ui/icons/";

  // Check if on a mac
  static private final String osName = System.getProperty("os.name").toLowerCase();
  static private final boolean isMacOs = osName.startsWith("mac os x");

  /**
   * Set the resource path for icons, images, cursors.
   * @param path reletive to the classpath
   */
  static public void setResourcePath( String path) { defaultResourcePath = path; }
  static public String getResourcePath( ) { return defaultResourcePath; }

  static final private int META_KEY = (isMacOs) ? java.awt.Event.META_MASK
                                                    : java.awt.Event.CTRL_MASK;

  static private boolean debug = false, debugToggle = false;

  /** Get the named Icon from the default resource (jar file).
    * @param name name of the Icon ( will look for <name>.gif)
    * @param errMsg true= print error message if not found
    * @return the Icon or null if not found
    */
  public static ImageIcon getIcon( String name, boolean errMsg) {
    return Resource.getIcon(defaultResourcePath+name+".gif", errMsg);
  }

  /** Get the named Image from the default resource (jar file).
    * @param name name of the Image ( will look for <name>.gif)
    * @return the Image or null if not found
    */
  public static Image getImage( String name) {
    return Resource.getImage(defaultResourcePath+name+".gif");
  }

  /** Make a cursor from the named Image in the default resource (jar file)
    * @param name name of the Image ( will look for <name>.gif)
    * @return the Cursor or null if failure
    */
  public static Cursor makeCursor( String name) {
    return Resource.makeCursor(defaultResourcePath+name+".gif");
  }

    /** Make a "buttcon" = button with an Icon
       @param icon the normal Icon
       @param selected the selected Icon
       @param tooltip the tooltip
       @param is_toggle if true, make JToggleButton, else JButton
       @return the buttcon (JButton or JToggleButton)
    */
  public static AbstractButton makeButtcon( Icon icon, Icon selected, String tooltip, boolean is_toggle) {
    AbstractButton butt;
    if (is_toggle)
      butt = new JToggleButton();
    else
      butt = new JButton();

    if (debug) System.out.println("   makeButtcon"+ icon+ " "+ selected+ " "+ tooltip+ " "+ is_toggle);

    if (icon != null)
      butt.setIcon(icon);

    if (selected != null) {
      if (is_toggle) {
        butt.setSelectedIcon( selected);
      } else {
        butt.setRolloverIcon(selected);
        butt.setRolloverSelectedIcon(selected);
        butt.setPressedIcon(selected);
        butt.setRolloverEnabled(true);
      }
    }

    butt.setMaximumSize(new Dimension(28,28));       // kludge
    butt.setPreferredSize(new Dimension(28,28));
    butt.setToolTipText(tooltip);
    butt.setFocusPainted(false);

    return butt;
  }

   /** Make a "buttcon" = button with an Icon
       @param iconName name of the Icon ( will look for <name>.gif)
       @param tooltip the tooltip
       @param is_toggle if true, make JToggleButton, else JButton
       @return the buttcon (JButton or JToggleButton)
    */
  public static AbstractButton makeButtcon( String iconName, String tooltip, boolean is_toggle) {
    Icon icon  = getIcon( iconName, false);
    Icon iconSel  = getIcon( iconName+"Sel", false);
    return makeButtcon( icon, iconSel, tooltip, is_toggle);
  }

   private static JMenuItem makeMenuItem( Icon icon, Icon rollover, String menu_cmd, boolean is_toggle,
      int mnemonic, int accel) {
    JMenuItem mi;
    if (is_toggle)
      mi = new JCheckBoxMenuItem(menu_cmd);
    else
      mi = new JMenuItem(menu_cmd);

    if (icon != null)
      mi.setIcon(icon);
    if (rollover != null) {
      mi.setRolloverIcon(rollover);
      mi.setRolloverSelectedIcon(rollover);
      mi.setPressedIcon(rollover);
      mi.setRolloverEnabled(true);
    }

    mi.setHorizontalTextPosition( SwingConstants.LEFT );
    if (mnemonic != 0)
      mi.setMnemonic( mnemonic);
    if (accel != 0)
      mi.setAccelerator(KeyStroke.getKeyStroke( accel, META_KEY));

    return mi;
  }


    // NB: doesnt add action to MenuItem
  private static JMenuItem makeMenuItemFromAction( Action act) {
    // this prevents null pointer exception if user didnt call setProperties()
    Boolean tog = (Boolean) act.getValue( BAMutil.TOGGLE);
    boolean is_toggle = (tog == null ) ? false : tog.booleanValue();
    Integer mnu = (Integer) act.getValue( BAMutil.MNEMONIC);
    int mnemonic = (tog == null ) ? -1 : mnu.intValue();
    Integer acc = (Integer) act.getValue( BAMutil.ACCEL);
    int accel = (acc == null ) ? 0 : acc.intValue();

    return makeMenuItem(
        (Icon) act.getValue( Action.SMALL_ICON),
        (Icon) act.getValue( BAMutil.SELECTED_ICON),
        (String) act.getValue( Action.SHORT_DESCRIPTION),
        is_toggle, mnemonic, accel < 0 ? 0 : accel);
  }


  /** creates a MenuItem using the given Action and adds it to the given Menu.
    Uses Properties that have been set on the Action (see setActionProperties()). All
    are optional except for Action.SHORT_DESCRIPTION:  <pre>
        Action.SHORT_DESCRIPTION   String     MenuItem text (required)
        Action.SMALL_ICON          Icon       the Icon to Use
        BAMutil.SELECTED_ICON      Icon       the Icon when selected (optional)
        BAMutil.TOGGLE             Boolean    true if its a toggle
        BAMutil.MNEMONIC           Integer    menu item shortcut
        BAMutil.ACCEL              Integer    menu item global keyboard accelerator
    </pre><br>
    The Action is triggered when the MenuItem is selected. Enabling and disabling the Action
    does the same for the MenuItem. For toggles, state is maintained in the Action,
    and MenuItem state changes when the Action state changes. <br><br>
    The point of all this is that once you set it up, you work exclusively with the action object,
    and all changes are automatically reflected in the UI.

    @param menu  add to this menu
    @param act the Action to make it out of
    @param menuPos if >= 0, add at this position, otherwise append
    @return the MenuItem created
  */
   public static JMenuItem addActionToMenu( JMenu menu, Action act, int menuPos) {
    JMenuItem mi = makeMenuItemFromAction( act);
    if (menuPos >= 0)
      menu.add( mi, menuPos);
    else
      menu.add( mi);

    Boolean tog = (Boolean) act.getValue( BAMutil.TOGGLE);
    boolean is_toggle = (tog == null ) ? false : tog.booleanValue();

    // set state for toggle buttons
    if (is_toggle) {
      if (debugToggle)
        System.out.println("addActionToMenu: "+ act.getValue( Action.SHORT_DESCRIPTION)+" "+act.getValue( BAMutil.STATE));
      Boolean state = (Boolean) act.getValue( BAMutil.STATE);
      if (state == null) state = Boolean.FALSE;
      act.putValue(BAMutil.STATE, state);
      mi.setSelected(state.booleanValue());
    }

      // add event listeners
    Action myAct = is_toggle ? new ToggleAction( act) : act;
    mi.addActionListener( myAct);
    act.addPropertyChangeListener(new myActionChangedListener(mi));

    return mi;
  }

  public static JMenuItem addActionToMenu( JMenu menu, Action act) {
    return addActionToMenu( menu, act, -1);
  }

  public static JMenuItem addActionToPopupMenu( JPopupMenu pmenu, Action act) {
    JMenuItem mi = makeMenuItemFromAction( act);
    pmenu.add( mi);

    Boolean tog = (Boolean) act.getValue( BAMutil.TOGGLE);
    boolean is_toggle = (tog == null ) ? false : tog.booleanValue();

    // set state for toggle buttons
    if (is_toggle) {
      if (debugToggle)
        System.out.println("addActionToMenu: "+ act.getValue( Action.SHORT_DESCRIPTION)+" "+act.getValue( BAMutil.STATE));
      Boolean state = (Boolean) act.getValue( BAMutil.STATE);
      if (state == null) state = Boolean.FALSE;
      act.putValue(BAMutil.STATE, state);
      mi.setSelected(state.booleanValue());
    }

      // add event listeners
    Action myAct = is_toggle ? new ToggleAction( act) : act;
    mi.addActionListener( myAct);
    act.addPropertyChangeListener(new myActionChangedListener(mi));

    return mi;
  }

    // NB: doesnt add action to button
  private static AbstractButton _makeButtconFromAction( Action act) {
    // this prevents null pointer exception if user didnt call setProperties()
    Boolean tog = (Boolean) act.getValue( BAMutil.TOGGLE);
    boolean is_toggle = (tog == null ) ? false : tog.booleanValue();

    return makeButtcon(
        (Icon) act.getValue( Action.SMALL_ICON),
        (Icon) act.getValue( BAMutil.SELECTED_ICON),
        (String) act.getValue( Action.SHORT_DESCRIPTION),
        is_toggle);
  }

  /** creates an AbstractButton using the given Action and adds it to the given Container at the position..
    Uses Properties that have been set on the Action (see setActionProperties()). All
    are optional except for Action.SMALL_ICON:  <pre>
        Action.SMALL_ICON          Icon       the Icon to Use (required)
        BAMutil.SELECTED_ICON      Icon       the Icon when selected (optional)
        Action.SHORT_DESCRIPTION   String     tooltip
        BAMutil.TOGGLE             Boolean    true if its a toggle
    </pre><br>
    The Action is triggered when the Button is selected. Enabling and disabling the Action
    does the same for the Button. For toggles, state is maintained in the Action,
    and the Button state changes when the Action state changes. <br><br>
    The point of all this is that once you set it up, you work exclusively with the action object,
    and all changes are automatically reflected in the UI.

    @param c  add to this Container
    @param act the Action to make it out of
    @param pos add to the container at this position (if pos < 0, add at the end)
    @return the AbstractButton created  (JButton or JToggleButton)
  */

  public static AbstractButton addActionToContainerPos( Container c, Action act, int pos) {
    AbstractButton butt = _makeButtconFromAction( act);
    if (pos < 0)
      c.add( butt);
    else
      c.add(butt, pos);

    if (debug) System.out.println(" addActionToContainerPos "+ act+ " "+ butt+ " "+ pos);

    Boolean tog = (Boolean) act.getValue( BAMutil.TOGGLE);
    boolean is_toggle = (tog == null ) ? false : tog.booleanValue();

      // set state for toggle buttons
    if (is_toggle) {
      if (debugToggle)
        System.out.println("addActionToContainerPos: "+ act.getValue( Action.SHORT_DESCRIPTION)+" "+act.getValue( BAMutil.STATE));
      Boolean state = (Boolean) act.getValue( BAMutil.STATE);
      if (state == null) state = Boolean.FALSE;
      act.putValue(BAMutil.STATE, state);
      butt.setSelected(state.booleanValue());
    }

      // add event listsners
    Action myAct = is_toggle ? new ToggleAction( act) : act;
    butt.addActionListener( myAct);
    act.addPropertyChangeListener(new myActionChangedListener(butt));

    return butt;
  }

  public static AbstractButton makeButtconFromAction( Action act) {
    AbstractButton butt = _makeButtconFromAction( act);

    Boolean tog = (Boolean) act.getValue( BAMutil.TOGGLE);
    boolean is_toggle = (tog == null ) ? false : tog.booleanValue();

      // set state for toggle buttons
    if (is_toggle) {
      if (debugToggle)
        System.out.println("addActionToContainerPos: "+ act.getValue( Action.SHORT_DESCRIPTION)+" "+act.getValue( BAMutil.STATE));
      Boolean state = (Boolean) act.getValue( BAMutil.STATE);
      if (state == null) state = Boolean.FALSE;
      act.putValue(BAMutil.STATE, state);
      butt.setSelected(state.booleanValue());
    }

      // add event listsners
    Action myAct = is_toggle ? new ToggleAction( act) : act;
    butt.addActionListener( myAct);
    act.addPropertyChangeListener(new myActionChangedListener(butt));

    return butt;
  }

  /** Same as addActionToContainerPos, but add to end of Container
   * @param c add to this container
   * @param act add this action
   * @return the Button that triggers this action, in case you want to decorate it
   */
  public static AbstractButton addActionToContainer( Container c, Action act) {
    return addActionToContainerPos( c, act, -1);
  }

 /** Standard way to set Properties for Actions.
  * This also looks for an Icon "<icon_name>Sel" and if it exists:
  *   1) sets SelectedIcon if its a toggle, or
  *   2) sets the Icon when selected (optional) if its not a toggle
  *
  *  If is_toggle, a toggle button is created (in addActionToContainer()), default state false
  *  To get or set the state of the toggle button:
  *    Boolean state = (Boolean) action.getValue(BAMutil.STATE);
  *    action.putValue(BAMutil.STATE, new Boolean(true/false));
  *
  * @param act  add properties to this action
  * @param icon_name name of icon (or null).
  * @param action_name menu name / tooltip
  * @param is_toggle true if its a toggle
  * @param mnemonic  menu item shortcut
  * @param accel menu item global keyboard accelerator
  **/
  public static void setActionProperties( AbstractAction act, String icon_name, String action_name,
    boolean is_toggle, int mnemonic, int accel ) {
    if (icon_name != null) {
      act.putValue( Action.SMALL_ICON, getIcon( icon_name, true));
      act.putValue( BAMutil.SELECTED_ICON, getIcon( icon_name + "Sel", false));
    }
    act.putValue( Action.SHORT_DESCRIPTION, action_name);
    act.putValue( Action.LONG_DESCRIPTION, action_name);
    act.putValue( BAMutil.TOGGLE, new Boolean(is_toggle));
    act.putValue( BAMutil.MNEMONIC, new Integer(mnemonic));
    act.putValue( BAMutil.ACCEL, new Integer(accel));
  }

 /** Standard way to set Properties and state for "Toggle" Actions.  *
  * @param act  add properties to this action
  * @param icon_name  name of icon (or null).
  * @param action_name menu name / tooltip
  * @param toggleValue default value of toggle
  * @param mnemonic  menu item shortcut
  * @param accel menu item global keyboard accelerator
  **/
  public static void setActionPropertiesToggle( AbstractAction act, String icon_name, String action_name,
    boolean toggleValue, int mnemonic, int accel ) {
    setActionProperties( act, icon_name, action_name, true, mnemonic, accel);
    act.putValue( BAMutil.STATE, new Boolean(toggleValue));
  }

  /*
  public static void addActionToMenuAndToolbar( JMenu menu, JToolBar toolbar, Action act,
      String name, String icon_name, boolean is_toggle, int mnemonic, int accel) {

    AbstractButton butt = addAction( toolbar, act, name, icon_name, is_toggle);
    JMenuItem mi = addActionToMenu( menu, act, name, icon_name, is_toggle, mnemonic, accel);
  }


  public static void addActionToMenuAndToolbar2( JMenu menu, JComponent toolbar,
      int pos, Action act) {
    String name = (String) act.getValue( Action.SHORT_DESCRIPTION);
    Icon icon = (Icon) act.getValue( Action.SMALL_ICON);

    JMenuItem mi = new JMenuItem(name);
    mi.setIcon(icon);
    mi.setHorizontalTextPosition( SwingConstants.LEFT );
    mi.addActionListener( act);
    menu.add(mi);

    JButton butt = new JButton();
    butt.setIcon(icon);
    butt.setMaximumSize(new Dimension(24,24));       // kludge
    butt.setPreferredSize(new Dimension(24,24));
    butt.setToolTipText(name);
    butt.setFocusPainted(false);
    butt.addActionListener( act);
    toolbar.add(butt, pos);
  }   */


  // used by PopopMenu
 /** This wraps a regular action and makes it into a "toggle action",
   * and associates it with an AbstractButton.
   * Fetch/set the toggle state on the <U>original</u> action using:
   * <pre>Boolean state = (Boolean) act.getValue(BAMutil.STATE);
   * act.putValue(BAMutil.STATE, new Boolean(state)); </pre>
   * It will automatically change the button state if the action state changes.
   */
  public static class ActionToggle extends AbstractAction {
    private Action orgAct;
    private AbstractButton button;

    public ActionToggle(Action oa, AbstractButton b) {
      this.orgAct = oa;
      this.button = b;
      orgAct.putValue(STATE, new Boolean(true)); // state is kept with original action

      orgAct.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
          String propertyName = e.getPropertyName();
          if (debugToggle) System.out.println("propertyChange "+ propertyName+ " "+ ((Boolean) e.getNewValue()));
          if (propertyName.equals(Action.NAME)) {
            String text = (String) e.getNewValue();
            button.setText(text);
            button.repaint();
          } else if (propertyName.equals("enabled")) {
            Boolean enabledState = (Boolean) e.getNewValue();
            button.setEnabled(enabledState.booleanValue());
            button.repaint();
          } else if (propertyName.equals(STATE)) {
            Boolean state = (Boolean) e.getNewValue();
            button.setSelected(state.booleanValue());
            button.repaint();
          }
        }
      });
    }

    public void actionPerformed( java.awt.event.ActionEvent e) {
      Boolean state = (Boolean) orgAct.getValue(BAMutil.STATE);
      orgAct.putValue(STATE, new Boolean(!state.booleanValue()));
      orgAct.actionPerformed(e);
    }
  }


   private static class ToggleAction extends AbstractAction {
    private Action orgAct;

    ToggleAction(Action orgAct) {
      this.orgAct = orgAct;
      // orgAct.putValue(STATE, new Boolean(false)); // state is kept with original action
    }

    public void actionPerformed( java.awt.event.ActionEvent e) {
      Boolean state = (Boolean) orgAct.getValue(STATE);
      orgAct.putValue(STATE, new Boolean(!state.booleanValue()));
      if (debugToggle)
        System.out.println("ToggleAction: "+ orgAct.getValue( Action.SHORT_DESCRIPTION)+" "+orgAct.getValue( BAMutil.STATE));
      orgAct.actionPerformed(e);
    }
  }

  private static class myActionChangedListener implements java.beans.PropertyChangeListener {
    private AbstractButton button;

    myActionChangedListener(AbstractButton b) {
      button = b;
    }
    public void propertyChange(java.beans.PropertyChangeEvent e) {
      String propertyName = e.getPropertyName();
      if (debugToggle) {
        System.out.print("myActionChangedListener "+ propertyName+ " "+ ((Boolean) e.getNewValue()));
        AbstractAction act = (AbstractAction) button.getAction();
        System.out.print(" "+ ((act == null) ? "" : act.getValue( Action.SHORT_DESCRIPTION)));
      }

      if (propertyName.equals(Action.NAME)) {
        String text = (String) e.getNewValue();
        button.setText(text);
        button.repaint();
      } else if (propertyName.equals("enabled")) {
        Boolean enabledState = (Boolean) e.getNewValue();
        button.setEnabled(enabledState.booleanValue());
        button.repaint();
      } else if (propertyName.equals(STATE)) {
        Boolean state = (Boolean) e.getNewValue();
        button.setSelected(state.booleanValue());
        button.repaint();
      }
    }
  }

}
