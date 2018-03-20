/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestDebug.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $

package ucar.util.prefs.ui;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestDebug {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private static PreferencesExt store;
  private static XMLStore xstore;

  @Before
  public void setup() throws IOException {
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");

    xstore = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
    store = xstore.getPreferences();
    //store = new PreferencesExt(null,"");
    Debug.setStore( store.node("Debug"));
  }

  @Test
  public void testDebug() {
    Debug.set("testit", true);
    assert( Debug.isSet("testit"));

    Debug.set("fart/allow", true);
    //assert( Debug.isSet("fart.allow"));
    assert( Debug.isSet("fart/allow"));

    Debug.set("fart/allow", false);
    assert( !Debug.isSet("fart/allow"));

    assert( !Debug.isSet("fart/notSet"));
    try {
      assert( !store.nodeExists("fart"));
      assert( store.nodeExists("/Debug/fart"));
      assert( store.nodeExists("Debug/fart"));
    } catch (Exception e) {
      assert (false);
    }
  }

  @Test
  public void testMenu() {
    Debug.constructMenu( new javax.swing.JMenu());
    try {
      xstore.save();
    } catch (java.io.IOException e) {
      assert(false);
    }
  }
}
