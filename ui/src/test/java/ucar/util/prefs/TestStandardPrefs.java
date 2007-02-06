// $Id: TestStandardPrefs.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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
package ucar.util.prefs;

import java.util.prefs.*;
import java.io.*;

public class TestStandardPrefs  {

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