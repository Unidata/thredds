// $Id: TestDebug.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $
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
package ucar.util.prefs.ui;

import junit.framework.*;
import ucar.util.prefs.*;

public class TestDebug extends TestCase {
  private static PreferencesExt store = new PreferencesExt(null,"");
  private static XMLStore xstore;

  public TestDebug( String name) {
    super(name);
    try {
      xstore = XMLStore.createFromFile(TestAllPrefs.dir+"testDebug.xml", null);
      store = xstore.getPreferences();
    } catch (java.io.IOException e) {}
    //store = new PreferencesExt(null,"");
    Debug.setStore( store.node("Debug"));
  }

  public void testDebug() {
    System.out.println("***TestDebug");
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

  public void testMenu() {
    Debug.constructMenu( new javax.swing.JMenu());
    try {
      xstore.save();
    } catch (java.io.IOException e) {
      assert(false);
    }
  }

}
/* Change History:
   $Log: TestDebug.java,v $
   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/