/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package thredds.servlet;

import java.util.TreeMap;
import java.util.Map;

/**
 * A minimal implementation of a globally-accessible set of Debug flags.
 */

public class Debug {
  static private Map<String, Boolean> map = new TreeMap<String, Boolean>();
  static private boolean debug = false, changed = true;

  /**
   * See if this flag is set
   * @param flagName : name of flag
   * @return : true if set
   */
  static public boolean isSet(String flagName) {
    Object val;
    if (null == (val = map.get(flagName))) {
      //if (debug) println("Debug.isSet new "+ flagName);
      map.put(flagName, false);
      changed = true;
      return false;
    }

    return (Boolean) val;
  }

  /**
   * Set a named flag.
   * @param flagName : set this flag
   * @param value : to this boolean value
   */
  static public void set(String flagName, boolean value) {
    if (null == map.get(flagName)) {
      changed = true;
    }
    map.put(flagName, value);
    //if (debug) println("  Debug.set "+ flagName+" "+value);
  }

  /**
   * Clear all flags; none are defined after this is called.
   */
  static public void clear() {
   map = new TreeMap<String, Boolean>();
  }

  /**
   * Get the set of flag names as a Set of Strings.
   * @return Set of flag names.
   */
  static public java.util.Set<String> keySet() { return map.keySet(); }

  /*
   * Send a global log message if flag is set.
   * @param flagName
   * @param s : message
   * @see Log#printlnG
   *
  static public void printIfSet(String flagName, String s) {
    if (isSet(flagName)) println(flagName +": "+ s);
  }

  /**
   * Send a global log message.
   * @param s : message
   * @see Log#printlnG
   *
  static public void println( String s) {
    Log.printlnG(s);
  } */

}