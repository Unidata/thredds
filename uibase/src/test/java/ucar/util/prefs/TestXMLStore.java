/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.prefs;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Assert2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

@RunWith(JUnit4.class)
public class TestXMLStore {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  static {
      System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  private String storeFile;

  @Before
  public void setup() throws IOException {
    storeFile = tempFolder.newFile().getAbsolutePath();
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    PreferencesExt prefs = store.getPreferences();
    prefs.putDouble("testD", 3.14157);
    prefs.putFloat("testF", 1.23456F);
    prefs.putLong("testL", 12345678900L);
    prefs.putInt("testI", 123456789);
    prefs.put("testS", "youdBeDeadbyNow");
    prefs.putBoolean("testB", true);

    byte[] barr = new byte[3];
    barr[0] = 1;
    barr[1] = 2;
    barr[2] = 3;
    prefs.putByteArray("testBA", barr);

    Preferences subnode = prefs.node("SemperUbi");
    subnode.putDouble("testD", 3.14158);
    subnode.putFloat("testF", 1.23457F);
    subnode.putLong("testL", 12345678901L);
    subnode.putInt("testI", 123456780);
    subnode.put("testS", "youdBeLivebyNow");
    subnode.putBoolean("testB", false);

    byte[] barr2 = new byte[3];
    barr2[0] = 2;
    barr2[1] = 3;
    barr2[2] = 4;
    subnode.putByteArray("testBA", barr2);

    store.save();
  }

  @Test
  public void testPersistence() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);

    PreferencesExt prefs = store.getPreferences();

    double d = prefs.getDouble("testD", 0.0);
    Assert2.assertNearlyEquals(d, 3.14157);

    float f = prefs.getFloat("testF", 0.0F);
    Assert2.assertNearlyEquals(f, 1.23456F);

    long ll = prefs.getLong("testL", 0);
    assert ll == 12345678900L : "long failed";

    int ii = prefs.getInt("testI", 0);
    assert ii == 123456789 : "int failed";

    String s = prefs.get("testS", "");
    assert s.equals("youdBeDeadbyNow") : "String failed";

    boolean b = prefs.getBoolean("testB", false);
    assert b : "boolean failed";

    byte[] barr = new byte[3];
    byte[] barr2 = new byte[3];
    barr[0] = 1;
    barr[1] = 2;
    barr[2] = 3;
    byte[] ba = prefs.getByteArray("testBA", barr2);
    for (int i=0; i<3; i++)
      assert ba[i] == barr[i] : "BA failed";
  }

  @Test
  public void testPersistenceSubnode() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences prefs = store.getPreferences().node("SemperUbi");

    double d = prefs.getDouble("testD", 0.0);
    Assert2.assertNearlyEquals(d, 3.14158);

    float f = prefs.getFloat("testF", 0.0F);
    Assert2.assertNearlyEquals(f, 1.23457F);

    long ll = prefs.getLong("testL", 0);
    assert ll == 12345678901L : "long failed";

    int ii = prefs.getInt("testI", 0);
    assert ii == 123456780 : "int failed";

    String s = prefs.get("testS", "");
    assert s.equals("youdBeLivebyNow") : "String failed";

    boolean b = prefs.getBoolean("testB", true);
    assert !b : "boolean failed";

    byte[] barr = new byte[3];
    byte[] barr2 = new byte[3];
    barr[0] = 2;
    barr[1] = 3;
    barr[2] = 4;
    byte[] ba = prefs.getByteArray("testBA", barr2);
    for (int i=0; i<3; i++)
      assert ba[i] == barr[i] : "BA failed";
  }

  @Test
  public void testPersistenceChange() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences prefs = store.getPreferences().node("SemperUbi");

    String s = prefs.get("testS", "");
    assert s.equals("youdBeLivebyNow") : "testPersistenceChange failed 1";

    prefs.put("testS", "NewBetter");
    store.save();

    XMLStore store2 = XMLStore.createFromFile(storeFile, null);
    Preferences prefs2 = store2.getPreferences().node("SemperUbi");

    s = prefs2.get("testS", "");
    assert s.equals("NewBetter") : "testPersistenceChange failed 2";

    prefs.put("testS", "youdBeDeadbyNow");
    store.save();

    XMLStore store3 = XMLStore.createFromFile(storeFile, null);
    Preferences prefs3 = store3.getPreferences().node("SemperUbi");

    s = prefs3.get("testS", "");
    assert s.equals("youdBeDeadbyNow") : "testPersistenceChange failed 3";
  }

  @Test
  public void testPersistenceAddRemove() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences prefs = store.getPreferences().node("SemperUbi");

    String s = prefs.get("testS2", "def");
    assert s.equals("def") : "testPersistenceAddRemove failed 1";

    prefs.put("testS2", "WayBetter");
    store.save();

    XMLStore store2 =XMLStore.createFromFile(storeFile, null);
    Preferences prefs2 = store2.getPreferences().node("SemperUbi");

    s = prefs2.get("testS2", "");
    assert s.equals("WayBetter") : "testPersistenceAddRemove failed 2";

    prefs.remove("testS2");
    store.save();

    XMLStore store3 = XMLStore.createFromFile("E:/dev/prefs/test/store/prefs2.xml", null);
    Preferences prefs3 = store3.getPreferences().node("SemperUbi");

    s = prefs3.get("testS2", "deff");
    assert s.equals("deff") : "testPersistenceAddRemove failed 3";
  }

  @Test
  public void testPersistenceDefaults() throws IOException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences newNode = store.getPreferences().node("SemperUbi/SubSemperUbi2");

    String s = newNode.get("testS2", "def");
    assert s.equals("def") : "testPersistenceDefaults failed 1";

    s = newNode.get("testS2", "def2");
    assert s.equals("def2") : "testPersistenceDefaults failed 2";
  }

  @Test
  public void testPersistenceAddRemoveNode() throws IOException, BackingStoreException {
    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences newNode = store.getPreferences().node("SemperUbi/SubSemperUbi2");

    String s = newNode.get("testS2", "def");
    assert s.equals("def") : "testPersistenceAddRemoveNode failed 1";

    newNode.put("testS2", "WayBetterValue");
    store.save();

    XMLStore store2 = XMLStore.createFromFile(storeFile, null);
    Preferences prefs2 = store2.getPreferences().node("SemperUbi/SubSemperUbi2");

    s = prefs2.get("testS2", "");
    assert s.equals("WayBetterValue") : "testPersistenceAddRemoveNode failed 2";

    prefs2.removeNode();
    store2.save();

    XMLStore store3 = XMLStore.createFromFile(storeFile, null);
    Preferences prefs3 = store3.getPreferences().node("SemperUbi/SubSemperUbi2");

    s = prefs3.get("testS2", "deff");
    assert s.equals("deff") : "testPersistenceAddRemoveNode failed 3 " +s;
  }

  @Test
  public void testXMLencoding() throws IOException {
    String bad = "><';&\r\"\n";

    XMLStore store = XMLStore.createFromFile(storeFile, null);
    Preferences prefs = store.getPreferences().node("badchars");

    prefs.put("baddog", bad);
    store.save();

    XMLStore store2 = XMLStore.createFromFile(storeFile, null);
    Preferences pref2 = store2.getPreferences().node("badchars");

    String s = pref2.get("baddog", null);
    assert s.equals(bad) : "bad==="+s+"===";
  }
}
