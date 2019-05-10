/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.prefs;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

import javax.swing.JMenu;
import javax.swing.JCheckBoxMenuItem;

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
 */

public class Debug {
  private static Preferences store = null;
  private static boolean debug = false, debugEvents = false;

  /** Set the persistent data. You must call this before any other call. */
  public static void setStore(Preferences debugStore) {
    store = debugStore;
  }

  /** Return the value of the named flag. If it doesnt exist, it will be added
   *  to the store and the menu with a value of "false".
   */
  public static boolean isSet(String flagName) {
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
  public static void set(String flagName, boolean value) {
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
  public static void removeAll() {
    try {
      removeAll(store, false);
    } catch (BackingStoreException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static void removeAll(Preferences prefs, boolean delete) throws BackingStoreException {

    String[] kidName = prefs.childrenNames();
    for (String aKidName : kidName) {
      Preferences pkid = prefs.node(aKidName);
      removeAll(pkid, true);
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
  public static void constructMenu(JMenu topMenu) {
    if (debug) System.out.println("Debug.constructMenu ");

    if (topMenu.getItemCount() > 0)
      topMenu.removeAll();

    try {
      addToMenu( topMenu, store); // recursive
    } catch (BackingStoreException e) { }

    topMenu.revalidate();
  }

    // recursive menu adding
    private static void addToMenu( JMenu menu, Preferences prefs) throws BackingStoreException {
    if (debug) System.out.println(" addMenu "+ prefs.name());

    String[] keys = prefs.keys();
    for (String key : keys) {
      boolean bval = prefs.getBoolean(key, false);
      String fullname = prefs.absolutePath() + "/" + key;
      menu.add(new DebugMenuItem(fullname, key, bval)); // menu leaf
      if (debug) System.out.println("   leaf= <" + key + "><" + fullname + ">");
    }

    String[] kidName = prefs.childrenNames();
    for (String aKidName : kidName) {
      Preferences pkid = prefs.node(aKidName);
      JMenu subMenu = new JMenu(pkid.name());
      menu.add(subMenu);
      addToMenu(subMenu, pkid);
    }
  }

  private static NamePart partit( String name) {
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
      if (fullname.startsWith("/Debug/")) {
        fullname = fullname.substring(7);
      }

      addChangeListener(e -> {
          if (debugEvents) { System.out.println("DebugMenuItem "+getText()+" "+getState()); }
          Debug.set(fullname, getState());
      });
    }
  }

}
