/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.util.prefs;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.prefs.*;
import java.io.*;

@RunWith(JUnit4.class)
public class TestExtPrefs  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  static {
      System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }
  // Preference keys for this package
  private static final String NUM_ROWS = "num_rows";
  private static final String NUM_COLS = "num_cols";

  @Test
  public void testPersistence() throws IOException, BackingStoreException {

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