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