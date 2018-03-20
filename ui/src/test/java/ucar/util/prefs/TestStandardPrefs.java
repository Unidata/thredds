/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestStandardPrefs.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $

package ucar.util.prefs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.prefs.*;
import java.io.*;

public class TestStandardPrefs  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String args[]) {
    //System.getProperty("ucar.util.prefs.PreferencesExtFactory");
    TestStandardPrefs pf = new TestStandardPrefs();
    pf.foo();
  }

  // Preference keys for this package
  private static final String NUM_ROWS = "num_rows";
  private static final String NUM_COLS = "num_cols";

  void foo() {
    Preferences userRoot = Preferences.userRoot();
    Preferences prefs = userRoot.node("myApp");

    System.out.println("rows = "+prefs.getInt(NUM_ROWS, 40));
    System.out.println("cols = "+prefs.getInt(NUM_COLS, 80));

    prefs.putInt(NUM_ROWS, 140);
    prefs.putInt(NUM_COLS, 180);

    prefs.putDouble("TestDoubleInt", 111);
    prefs.putDouble("TestDouble", 3.14156);
    prefs.putBoolean("TestBoolean", false);

    System.out.println("*rows = "+prefs.getInt(NUM_ROWS, 40));
    System.out.println("cols = "+prefs.getInt(NUM_COLS, 80));

    Preferences subnode = prefs.node("subnode");
    subnode.put("an entry", "value entry");
    Preferences subsubnode = subnode.node("subsubnode");

    try {
      //OutputStream os = new FileOutputStream("standardPrefs.xml");
      userRoot.exportSubtree( System.out);
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
/* Change History:
   $Log: TestStandardPrefs.java,v $
   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/
