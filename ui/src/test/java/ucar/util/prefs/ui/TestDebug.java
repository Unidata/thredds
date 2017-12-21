// $Id: TestDebug.java,v 1.1.1.1 2002/12/20 16:40:27 john Exp $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
