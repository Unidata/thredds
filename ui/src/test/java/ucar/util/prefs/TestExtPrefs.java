/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestExtPrefs.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $


package ucar.util.prefs;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.prefs.*;
import java.io.*;

public class TestExtPrefs  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  static {
      System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  public static void main(String args[]) {
    //System.getProperty("ucar.util.prefs.PreferencesExtFactory");
    TestExtPrefs pf = new TestExtPrefs();
    pf.run();
  }

  // Preference keys for this package
  private static final String NUM_ROWS = "num_rows";
  private static final String NUM_COLS = "num_cols";

  void run() {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      store = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    Preferences prefs = userRoot.node("extApp");
    prefs.putInt(NUM_ROWS, 140);
    prefs.putInt(NUM_COLS, 180);

    prefs.putDouble("TestDoubleInt", 111);
    prefs.putDouble("TestDouble", 3.14156);
    prefs.putBoolean("TestBoolean", false);

    Preferences subnode = prefs.node("subnode");
    subnode.put("an entry", "value entry");
    PreferencesExt beanNode = (PreferencesExt) subnode.node("beanNode");
    beanNode.putObject("myBeanName", new java.awt.Rectangle(1,2,3,4));
    beanNode.putObject("myBeanToo", new java.awt.Rectangle(5,6,7,8));

    PreferencesExt beanNode2 = (PreferencesExt) subnode.node("beanNode2");
    beanNode2.putObject("myColor", new java.awt.Color(1,2,3));
    beanNode2.putObject("blue", java.awt.Color.BLUE);

    try {
      //OutputStream os = new FileOutputStream("standardPrefs.xml");
      store.save();
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}
/* Change History:
   $Log: TestExtPrefs.java,v $
   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/
