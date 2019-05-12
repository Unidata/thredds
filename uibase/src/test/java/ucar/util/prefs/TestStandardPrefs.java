/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.prefs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.prefs.*;
import java.io.*;

@RunWith(JUnit4.class)
public class TestStandardPrefs  {
  // Preference keys for this package
  private static final String NUM_ROWS = "num_rows";
  private static final String NUM_COLS = "num_cols";

  @Test
  public void testPersistence() throws IOException, BackingStoreException {
    Preferences userRoot = Preferences.userRoot();
    Preferences prefs = userRoot.node("myApp");

    System.out.println("rows = " + prefs.getInt(NUM_ROWS, 40));
    System.out.println("cols = " + prefs.getInt(NUM_COLS, 80));

    prefs.putInt(NUM_ROWS, 140);
    prefs.putInt(NUM_COLS, 180);

    prefs.putDouble("TestDoubleInt", 111);
    prefs.putDouble("TestDouble", 3.14156);
    prefs.putBoolean("TestBoolean", false);

    System.out.println("*rows = " + prefs.getInt(NUM_ROWS, 40));
    System.out.println("cols = " + prefs.getInt(NUM_COLS, 80));

    Preferences subnode = prefs.node("subnode");
    subnode.put("an entry", "value entry");
    Preferences subsubnode = subnode.node("subsubnode");

    userRoot.exportSubtree(System.out);
  }
}
