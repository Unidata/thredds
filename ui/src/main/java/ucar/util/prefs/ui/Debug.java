// $Id: Debug.java,v 1.3 2004/08/26 17:55:17 caron Exp $
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

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.event.*;

/**
 * Provides static methods for managing persistent lists of debugging flags that
 * can be set by the user at runtime. A new debug flag is added to the Store
 * whenever isSet() is called and the flag name does not already exist.
 * The flags can be set dynamically at runtime through a JMenu, with the
 * name of the flag used for the menu label, and the / separator indicating menu
 * levels. Note that this class does not depend on using PreferencesExt,
 * and so could also be used with the default Preferences implementations.
 *
 * <p> To use, make sure that you call setStore() before anything else; it is a
 * good idea to use a separate node to avoid name collisions:
   <pre> Debug.setStore(prefs.node("/debugFlags")); </pre>

 *  <p> To allow user control at runtime, add a JMenu instance variable to your main menu,
 *  which constructs itself whenever it is called using the current set of flags, e.g.:
 <pre>
  JRootPane rootPane = getRootPaneContainer().getRootPane();
  JMenuBar mb = new JMenuBar();
  rootPane.setJMenuBar(mb);
  JMenu sysMenu = new JMenu("System");
  mb.add(sysMenu);

  JMenu debugMenu = (JMenu) sysMenu.add(new JMenu("Debug"));
  debugMenu.addMenuListener( new MenuListener() {
    public void menuSelected(MenuEvent e) { Debug.constructMenu(debugMenu);}
    public void menuDeselected(MenuEvent e) {}
    public void menuCanceled(MenuEvent e) {}
  });
  sysMenu.add(debugMenu);
  </pre>
 *
 * Then in your application code, check to see if a debug flag is turned on, for example:
  <pre>
   if (Debug.isSet("util/configure/stuff")) do_good_stuff();
  </pre>
 *
 * <p> The first time this code is called, this flag will be added to the Store and
 * to the menu (with a value of false). The user can then turn it on using the menu system.
 * This allows you to add fine-grained debugging control without having to manage
 * a separate list of flags. Since these are all static methods, you do not have to pass
 * around a global Debug object. In this example, the top menu item to be added will be c
 * alled "util" the second item called "configure", and the flag itself will be called
 * "stuff". The menu may nest to arbitrary depth. Similarly, a substore of the main
 * debug Store (the one passed into setStore()) is created called "util", which has a
 * substore called "configure", which has a boolean value whose key is "stuff".
 * Thus name collisions should be easy to avoid.
 *
 * @see java.util.prefs.Preferences
 * @author John Caron
 * @version $Revision: 1.3 $ $Date: 2004/08/26 17:55:17 $
 */

public class Debug {
  static private boolean changed = true;
  static private Preferences store = null;
  static private boolean debug = false, debugEvents = false;

  /** Set the persistent data. You must call this before any other call. */
  static public void setStore(Preferences debugStore) {
    store = debugStore;
  }

  /** Return the value of the named flag. If it doesnt exist, it will be added
   *  to the store and the menu with a value of "false".
   */
  static public boolean isSet(String flagName) {
    if (store == null) return false;

    NamePart np = partit( flagName);
    if (debug) {
      try {
        if ((np.storeName.length() > 0) && !store.nodeExists(np.storeName))
          System.out.println("Debug.isSet create node = "+ flagName+" "+np);
        else if (null == store.node(np.storeName).get( np.keyName, null))
          System.out.println("Debug.isSet create flag = "+ flagName+" "+np);
      } catch (BackingStoreException e) { }
    }
    // add it if it doesnt already exist
    boolean value =  store.node(np.storeName).getBoolean( np.keyName, false);
    store.node(np.storeName).putBoolean( np.keyName, value);
    return value;
  }

  /** Set the value of the named flag. If it doesnt exist, it will be added
   *  to the store and the menu.
   */
  static public void set(String flagName, boolean value) {
    NamePart np = partit( flagName);
    if (debug) {
      try {
        if ((np.storeName.length() > 0) && !store.nodeExists(np.storeName))
          System.out.println("Debug.set create node = "+ flagName+" "+np);
        else if (null == store.node(np.storeName).get( np.keyName, null))
          System.out.println("Debug.set create flag = "+ flagName+" "+np);
      } catch (BackingStoreException e) { }
    }
    store.node(np.storeName).putBoolean( np.keyName, value);
  }

  /** Clear all flags (set to false).
   */
  static public void removeAll() {
    try {
      removeAll(store, false);
    }
    catch (BackingStoreException ex) { }
  }

  static private void removeAll(Preferences prefs, boolean delete) throws BackingStoreException {

    String[] kidName = prefs.childrenNames();
    for (int i=0; i<kidName.length; i++) {
      Preferences pkid = (Preferences) prefs.node(kidName[i]);
      removeAll( pkid, true);
    }

    if (delete)
      prefs.removeNode();
    else
      prefs.clear();
  }

  /* static public void clear() {
   map = new TreeMap();
  } */

  /** Construct cascading pull-aside menus using the values of the debug flags
   *  in the Preferences object.
   *  @param topMenu attach the menus as children of this one.
   */
  static public void constructMenu(JMenu topMenu) {
    if (debug) System.out.println("Debug.constructMenu ");

    if (topMenu.getItemCount() > 0)
      topMenu.removeAll();

    try {
      addToMenu( topMenu, store); // recursive
    } catch (BackingStoreException e) { }

    topMenu.revalidate();
  }

    // recursive menu adding
  static private void addToMenu( JMenu menu, Preferences prefs) throws BackingStoreException {
    if (debug) System.out.println(" addMenu "+ prefs.name());

    String[] keys = prefs.keys();
    for (int i=0; i<keys.length; i++) {
      boolean bval = prefs.getBoolean(keys[i], false);
      String fullname = prefs.absolutePath()+"/"+keys[i];
      menu.add( new DebugMenuItem( fullname, keys[i], bval)); // menu leaf
      if (debug) System.out.println("   leaf= <"+ keys[i]+"><"+fullname+">");
    }

    String[] kidName = prefs.childrenNames();
    for (int i=0; i<kidName.length; i++) {
      Preferences pkid = (Preferences) prefs.node(kidName[i]);
      JMenu subMenu = new JMenu(pkid.name());
      menu.add( subMenu);
      addToMenu( subMenu, pkid);
    }
  }

  static private NamePart partit( String name) {
    NamePart np = new NamePart();

    //name = name.replace('.', '/');
    int pos = name.lastIndexOf('/');
    if (pos >= 0) {
      np.storeName = name.substring(0, pos);
      np.keyName = name.substring(pos+1);
      if (np.storeName.startsWith("/"))
        np.storeName = np.storeName.substring(1);
    } else
       np.keyName = name;

    return np;
  }

  private static class NamePart{
    String storeName = "", keyName = "";
    public String toString() { return "<"+storeName+"> <"+keyName+">"; }
  }

  private static class DebugMenuItem extends JCheckBoxMenuItem {
    private String fullname;

    DebugMenuItem( String foolName, String menuName, boolean val) {
      super(menuName, val);

      fullname = foolName;
      if (fullname.startsWith("/Debug/"))
        fullname = fullname.substring(7);

      addChangeListener( new ChangeListener() {
        public void stateChanged(ChangeEvent evt) {
          if (debugEvents) System.out.println("DebugMenuItem "+getText()+" "+getState());
          Debug.set(fullname, getState());
        }
      });
    }
  }

}

/* Change History:
   $Log: Debug.java,v $
   Revision 1.3  2004/08/26 17:55:17  caron
   no message

   Revision 1.2  2003/05/29 23:33:28  john
   latest release

   Revision 1.1.1.1  2002/12/20 16:40:25  john
   start new cvs root: prefs

   Revision 1.1.1.1  2001/11/10 16:01:23  caron
   checkin prefs

*/
