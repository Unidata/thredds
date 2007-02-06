// $Id: TestBasic.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $
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

import java.io.*;
import java.util.prefs.Preferences;

public class TestBasic extends TestCase {

  private boolean debug = false;

  public TestBasic( String name) {
    super(name);
  }

  public void testWho() {
    System.out.println("***TestBasic");

        // this makes PreferencesExt the SPI
    Preferences userRoot = Preferences.userRoot();
    assert userRoot instanceof PreferencesExt : "Factory not set = "+userRoot.getClass().getName();
    //assert false : "assert is on";
  }

  public void testPutGet() {
    try {

      Preferences userRoot = Preferences.userRoot();

      userRoot.putDouble("testD", 3.14157);
      double d = userRoot.getDouble("testD", 0.0);
      assert closeD(d, 3.14157) : "double failed";

      userRoot.putFloat("testF", 1.23456F);
      float f = userRoot.getFloat("testF", 0.0F);
      assert closeF(f, 1.23456F) : "float failed";

      userRoot.putLong("testL", 12345678900L);
      long ll = userRoot.getLong("testL", 0);
      assert ll == 12345678900L : "long failed";

      userRoot.putInt("testI", 123456789);
      int ii = userRoot.getInt("testI", 0);
      assert ii == 123456789 : "int failed";

      userRoot.put("testS", "youdBeDeadbyNow");
      String s = userRoot.get("testS", "");
      assert s.equals("youdBeDeadbyNow") : "String failed";

      userRoot.putBoolean("testB", true);
      boolean b = userRoot.getBoolean("testB", false);
      assert b : "boolean failed";

      byte[] barr = new byte[3];
      byte[] barr2 = new byte[3];
      barr[0] = 1;
      barr[1] = 2;
      barr[2] = 3;
      userRoot.putByteArray("testBA", barr);
      byte[] ba = userRoot.getByteArray("testBA", barr2);
      for (int i=0; i<3; i++)
        assert ba[i] == barr[i] : "BA failed";

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void testSubnode() {
    try {

      Preferences userRoot = Preferences.userRoot();
      Preferences subNode = userRoot.node("SemperUbi");

      String[] keys = userRoot.keys();
      for (int i=0; i<keys.length; i++) {
        String value =  userRoot.get(keys[i], "failed");
        subNode.put(keys[i], value);
      }

      float f = subNode.getFloat("testF", 0.0F);
      assert closeF(f, 1.23456F) : "float failed";

      long ll = subNode.getLong("testL", 0);
      assert ll == 12345678900L : "long failed";

      int ii = subNode.getInt("testI", 0);
      assert ii == 123456789 : "int failed";

      String s = subNode.get("testS", "");
      assert s.equals("youdBeDeadbyNow") : "String failed";

      boolean b = subNode.getBoolean("testB", false);
      assert b : "boolean failed";

      byte[] barr = new byte[3];
      byte[] barr2 = new byte[3];
      barr[0] = 1;
      barr[1] = 2;
      barr[2] = 3;
      byte[] ba = subNode.getByteArray("testBA", barr2);
      for (int i=0; i<3; i++)
        assert ba[i] == barr[i] : "BA failed";

    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }

  }


  public void testExport() {
      Preferences userRoot = Preferences.userRoot();
      Preferences fromNode = userRoot.node("SemperUbi");

      try {
        ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
        fromNode.exportNode( os);
        String xml = new String(os.toByteArray());
        xml = substitute(xml, "SemperUbi", "SubUbi");
        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes());
        Preferences.importPreferences( is);
        userRoot.exportSubtree( System.out);
      } catch (Exception e) {
        System.out.println(e);
        e.printStackTrace();
      }
  }

  public void testRemove() {
      Preferences userRoot = Preferences.userRoot();
      Preferences subNode = userRoot.node("SemperUbi");

      long ll = subNode.getLong("testL", 0);
      assert ll == 12345678900L : "long failed";
      subNode.remove("testL");
      ll = subNode.getLong("testL", 0);
      assert ll == 0 : "reove failed";
      subNode.putLong("testL", 12345678900L);
      ll = subNode.getLong("testL", 0);
      assert ll == 12345678900L : "long failed 2";
  }

  public void testRemoveNode() {
    try {
      Preferences userRoot = Preferences.userRoot();
      Preferences subNode = userRoot.node("SemperUbi");
      Preferences newNode = subNode.node("SubSemperUbi");

      newNode.putLong("testL", 12345678900L);
      long ll = subNode.getLong("testL", 0);
      assert ll == 12345678900L : "testRemoveNode failed";

      newNode.removeNode();
      String[] kidName = subNode.childrenNames();
      //System.out.println(" # children= "+kidName.length);
      for (int i=0; i<kidName.length; i++) {
        //System.out.println("  "+kidName[i]);
        assert !kidName[i].equals("SubSemperUbi") : "testRemoveNode failed 1";
      }


      newNode = subNode.node("SubSemperUbi");
      ll = newNode.getLong("testL", 123L);
      assert ll == 123 : "testRemoveNode failed 2 " + ll;

    } catch (java.util.prefs.BackingStoreException e) {
      e.printStackTrace();
    }
  }

  /**
   * Find all occurences of the "match" in original, and substitute the "subst" string
   * @param  original: starting string
   * @param match: string to match
   * @param subst: string to substitute
   * @return a new string with substitutions
   */
   String substitute( String original, String match, String subst) {
    String s = original;
    int pos;
    while (0 <= (pos = s.indexOf( match))) {
      StringBuffer sb = new StringBuffer( s);
      s = sb.replace( pos, pos + match.length(), subst).toString();
    }

    return s;
  }


  boolean closeD( double d1, double d2) { return (Math.abs(d1-d2) / d1) < 1.0E-7; }
  boolean closeF( float d1, float d2) { return (Math.abs(d1-d2) / d1) < 1.0E-7; }

}
