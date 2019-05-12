/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.util.prefs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.prefs.*;
import java.io.*;

@RunWith(JUnit4.class)
public class TestReadExtPrefs  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static {
      System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  @Test
  public void testPersistence() throws IOException, BackingStoreException {
    XMLStore store;
    PreferencesExt userRoot = null;
    String filename = File.createTempFile("foo", "bar").getAbsolutePath();
    try {
      store = XMLStore.createFromFile(filename, null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

      userRoot.dump();
      userRoot.exportSubtree( System.out);
  }
}
