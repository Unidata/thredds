/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestXMLStoreChains.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $

package ucar.util.prefs;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.Misc;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.prefs.Preferences;

public class TestXMLStoreChains {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private String storeFile = tempFolder.newFile().getAbsolutePath();
  private String chain2File = tempFolder.newFile().getAbsolutePath();
  private String jarFile = "/auxdata/chain3.xml";  // file lost, disable tests

  public TestXMLStoreChains() throws IOException {
  }

  public void testBasic() {
    System.out.println("***TestXMLStoreChains");
    try {
      XMLStore store2 = XMLStore.createFromFile(storeFile, null);
      PreferencesExt prefs = store2.getPreferences();
      System.out.println("-------------");
      prefs.exportSubtree( System.out);

      Preferences node = prefs.node("/myApp");
      int ival  = node.getInt("extraOne", 0);
      assert ival == 1 : "testBasic fail 1" + ival;

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }

  }

  public void testBean() {
    try {
      XMLStore store2 = XMLStore.createFromFile(storeFile, null);

      PreferencesExt prefs = store2.getPreferences();
      Preferences node = prefs.node("/myApp");
      int ival  = node.getInt("extraOne", 0);
      assert ival == 1 : "testBasic fail 1" + ival;

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }

    //System.out.println("testBasic");
  }

  public void testChain() {
    try {
      XMLStore store1 = XMLStore.createFromFile(chain2File, null);
      XMLStore store = XMLStore.createFromFile(storeFile, store1);
      PreferencesExt prefs = store.getPreferences();
      prefs.exportSubtree( System.out);

      Preferences node = prefs.node("/myApp");
      int ival  = node.getInt("extraOne", 0);
      assert ival == 1 : "testChain fail 1 " + ival;

      ival  = node.getInt("extraTwo", 0);
      assert ival == 2 : "testChain fail 1 " + ival;

      double dval  = node.getDouble("TestDouble", 0.0);
      assert Misc.nearlyEquals(dval, 3.14159) : "testChain fail 2 " + dval;

      node.putDouble("TestDouble", 3.14159);
      node.putDouble("TestFloat", -999.0);
      node.putInt("extraTwo", 2);
      //prefs.exportSubtree( System.out);
      store.save();

      // things only get written to top store
      store = XMLStore.createFromFile(chain2File, null);
      prefs = store.getPreferences();
      node = prefs.node("/myApp");
      dval  = node.getDouble("TestDouble", 0.0);
      assert Misc.nearlyEquals(dval, 0.0) : "testChain fail 2 " + dval;

      // but not if they are in the default
      store = XMLStore.createFromFile(storeFile, null);
      prefs = store.getPreferences();
      node = prefs.node("/myApp");
      ival  = node.getInt("extraTwo", 0);
      assert ival == 0 : "testChain fail 3 " + ival;


    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }

    System.out.println("testChain");
  }

  void show( Preferences prefs) {
    try {
      System.out.println("node  <"+prefs.name()+">");

      String[] kidName = prefs.childrenNames();
      System.out.println(" # children= "+kidName.length);
      for (int i=0; i<kidName.length; i++) {
        System.out.println("  "+kidName[i]);
      }

      String[] keyName = prefs.keys();
      System.out.println(" # Keys= "+keyName.length);
      for (int i=0; i<keyName.length; i++) {
        System.out.println("  "+keyName[i]);
      }
    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }

  }

      //////////////////////////////////
  public void testFailureChain() {
    // System.out.println("  testFailureChain");
    String filename = "chain3.xml";
    PreferencesExt prefs = null;
    XMLStore store = null;
    try {
      XMLStore store3 = XMLStore.createFromResource("/auxdata/bogus.xml", null);
      assert false;
    } catch (Exception e) {
      assert true;
    }
  }


    //////////////////////////////////
  boolean debug=false, debugWhichRead = false;
  public void testStandardChain() {
    XMLStore store = null;
    try {
      XMLStore store3 = XMLStore.createFromResource(jarFile, null);
      XMLStore store2 = XMLStore.createFromFile(chain2File, store3);
      store = XMLStore.createFromFile(storeFile, store2);
    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }
    PreferencesExt prefs = store.getPreferences();

    Preferences node = prefs.node("/myApp");
    int ival  = node.getInt("extraOne", 0);
    assert ival == 1 : "testStandardChain fail 1 " + ival;

    ival  = node.getInt("extraTwo", 0);
    assert ival == 2 : "testStandardChain fail 2 " + ival;

    ival  = node.getInt("num_rows", 0);
    assert ival == 128 : "testStandardChain fail 3 " + ival;

    ival  = node.getInt("missing", 0);
    assert ival == 0 : "testStandardChain fail 4 " + ival;

    boolean bval  = node.getBoolean("TestBoolean", false);
    assert bval: "testStandardChain fail 5 " + bval;

    node.putDouble("TestFloat", -999.0F);
    node.putDouble("TestDouble", 3.14159);
    //prefs.exportSubtree( System.out);
    try {
      store.save();
      store = XMLStore.createFromFile(storeFile, null);
      prefs = store.getPreferences();
      prefs.exportSubtree( System.out);
    } catch (Exception e) {
      e.printStackTrace();
    }

    node = prefs.node("/myApp");
    double dval  = node.getDouble("TestDouble", 0.0);
    assert Misc.nearlyEquals(dval, 3.14159) : "testStandardChain fail 6 " + dval;

    float fval  = node.getFloat("TestFloat", 0.0F);
    assert Misc.nearlyEquals(fval, -999.0F) : "testStandardChain fail 7 " + fval;

  }

  public InputStream getFileResource( String fullName) {
    InputStream is = getClass().getResourceAsStream(fullName);

    if (is != null) {
      if (debug) System.out.println("Resource.getResourceAsStream ok on "+fullName);
      return is;
    } else if (debug)
      System.out.println("Resource.getResourceAsStream failed on <"+fullName+">");

    try {
      is =  new FileInputStream(fullName);
      if (debug) System.out.println("Resource.FileInputStream ok on "+fullName);
    } catch (FileNotFoundException e) {
      if (debug)  System.out.println("  FileNotFoundException: Resource.getFile failed on "+fullName);
    } catch (java.security.AccessControlException e) {
      if (debug)  System.out.println("  AccessControlException: Resource.getFile failed on "+fullName);
    }

    return is;
  }

}
