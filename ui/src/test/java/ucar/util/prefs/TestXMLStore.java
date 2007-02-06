// $Id: TestXMLStore.java,v 1.2 2002/12/24 22:04:53 john Exp $
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

import junit.framework.*;

import java.util.prefs.Preferences;

public class TestXMLStore extends TestCase {

  private boolean debug = false;
  private String storeFile = TestAllPrefs.dir+"TestXMLStore.xml";
  private String storeFile2 = TestAllPrefs.dir+"TestXMLStore2.xml";

  public TestXMLStore( String name) {
    super(name);
  }

  public void testMake() {
    System.out.println("***TestXMLStore");
    try {
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
    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }


  public void testPersistence() {
    try {
      XMLStore store = XMLStore.createFromFile(storeFile, null);
      //store.writeToStream( System.out);

      PreferencesExt prefs = store.getPreferences();

      double d = prefs.getDouble("testD", 0.0);
      assert closeD(d, 3.14157) : "double failed";

      float f = prefs.getFloat("testF", 0.0F);
      assert closeF(f, 1.23456F) : "float failed";

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

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void testPersistenceSubnode() {
    try {
      XMLStore store = XMLStore.createFromFile(storeFile, null);
      Preferences prefs = store.getPreferences().node("SemperUbi");

      double d = prefs.getDouble("testD", 0.0);
      assert closeD(d, 3.14158) : "double failed";

      float f = prefs.getFloat("testF", 0.0F);
      assert closeF(f, 1.23457F) : "float failed";

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

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }


  public void testPersistenceChange() {
    try {
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

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }


  public void testPersistenceAddRemove() {
    try {
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

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }


  public void testPersistenceDefaults() {
    try {
      XMLStore store = XMLStore.createFromFile(storeFile, null);
      Preferences newNode = store.getPreferences().node("SemperUbi/SubSemperUbi2");

      String s = newNode.get("testS2", "def");
      assert s.equals("def") : "testPersistenceDefaults failed 1";

      s = newNode.get("testS2", "def2");
      assert s.equals("def2") : "testPersistenceDefaults failed 2";


    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void testPersistenceAddRemoveNode() {
    try {
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

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void testRoots() {
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");

    try {
      XMLStore store = XMLStore.createFromFile(storeFile, null);
      Preferences prefs = store.getPreferences();
      PreferencesExt.setUserRoot( (PreferencesExt) prefs);
      Preferences defPrefs = Preferences.userRoot();

      assert (prefs == defPrefs) : "testRoots ";

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }

  }

  public void testXMLencoding() {
    String bad = "><';&\r\"\n";
    try {
      XMLStore store = XMLStore.createFromFile(storeFile, null);
      Preferences prefs = store.getPreferences().node("badchars");

      prefs.put("baddog", bad);
      store.save();

      XMLStore store2 = XMLStore.createFromFile(storeFile, null);
      Preferences pref2 = store2.getPreferences().node("badchars");

      String s = pref2.get("baddog", null);
      assert s.equals(bad) : "bad==="+s+"===";
      //System.out.println("bad==="+s+"===");

     } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }
  }

  boolean closeD( double d1, double d2) { return (Math.abs(d1-d2) / d1) < 1.0E-7; }
  boolean closeF( float d1, float d2) { return (Math.abs(d1-d2) / d1) < 1.0E-7; }


}
/* Change History:
   $Log: TestXMLStore.java,v $
   Revision 1.2  2002/12/24 22:04:53  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/