// $Id: TestBeans.java,v 1.2 2002/12/24 22:04:52 john Exp $
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

package ucar.util.prefs;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestBeans {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
  String prefsFilename;

  static {
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  @Before
  public void setup() throws IOException {
    prefsFilename = tempFolder.newFile().getAbsolutePath();
  }

  @Test
  public void testDefault() throws IOException {
    XMLStore store1 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs1 = store1.getPreferences();

    TesterBean tbean1 = new TesterBean();
    prefs1.putBean( "default", tbean1);
    prefs1.putBeanObject( "defaultObject", tbean1);

    store1.save();


    XMLStore store2 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs = store2.getPreferences();

    TesterBean tbean =  (TesterBean) prefs.getBean( "default", null);
    TesterBean tbeano =  (TesterBean) prefs.getBean( "defaultObject", null);
    assert tbean != null;
    assert tbeano != null;

    assert tbean.getB() == tbeano.getB() : "boolean failed";
    assert tbean.getByte() == tbeano.getByte() : "byte failed";
    assert tbean.getShort() == tbeano.getShort() : "short failed";
    assert tbean.getI() == tbeano.getI() : "int failed";
    assert tbean.getL() == tbeano.getL() : "long failed";
    assert tbean.getF() == tbeano.getF() : "float failed";
    assert tbean.getD() == tbeano.getD() : "double failed";
    assert tbean.getS().equals(tbeano.getS()) : "string failed";
  }

  @Test
  public void testNonDefault() throws IOException {
    XMLStore store1 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs1 = store1.getPreferences();

    TesterBean tbean1 = new TesterBean(false, 9999, (short) 666, 123456789, .99f, .00001099, "nondefault");
    prefs1.putBean( "nondefault", tbean1);
    prefs1.putBeanObject( "nondefaultObject", tbean1);

    store1.save();


    XMLStore store2 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs = store2.getPreferences();

    TesterBean tbean =  (TesterBean) prefs.getBean( "nondefault", null);
    TesterBean tbeano =  (TesterBean) prefs.getBean( "nondefaultObject", null);

    assert tbean.getB() == tbeano.getB() : "boolean failed";
    assert tbean.getByte() == tbeano.getByte() : "byte failed";
    assert tbean.getShort() == tbeano.getShort() : "short failed";
    assert tbean.getI() == tbeano.getI() : "int failed";
    assert tbean.getL() == tbeano.getL() : "long failed";
    assert tbean.getF() == tbeano.getF() : "float failed";
    assert tbean.getD() == tbeano.getD() : "double failed";
    assert tbean.getS().equals(tbeano.getS()) : "string failed";
  }

  @Test
  public void testChangedBean() throws IOException {
    XMLStore store1 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs1 = store1.getPreferences();

    TesterBean tbean1 = new TesterBean(false, 9999, (short) 666, 123456789, .99f, .00001099, "orig");
    prefs1.putBean( "changeableBean", tbean1);
    prefs1.putBeanObject( "changeableBeanObject", tbean1);

    store1.save();


    XMLStore store2 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs2 = store2.getPreferences();

    TesterBean tbean2 =  (TesterBean) prefs2.getBean( "changeableBean", null);
    TesterBean tbeano2 =  (TesterBean) prefs2.getBean( "changeableBeanObject", null);

    assert tbean2.getS().equals("orig");
    assert tbeano2.getS().equals("orig");

    // change the objects
    tbean2.setS("changed");
    tbeano2.setS("changedo");

    // note putBean not called
    store2.save();


    XMLStore store3 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs = store3.getPreferences();

    TesterBean tbean =  (TesterBean) prefs.getBean( "changeableBean", null);
    TesterBean tbeano =  (TesterBean) prefs.getBean( "changeableBeanObject", null);

    assert tbean.getS().equals("changed");
    assert tbeano.getS().equals("changedo");
  }

  @Test
  public void testBadChars() throws IOException {
    String baddies =   "q>w<'e;&t\rl\"\nv";

    XMLStore store1 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs1 = store1.getPreferences();

    TesterBean tbean1 = new TesterBean();
    tbean1.setS( baddies);
    prefs1.putBean( "bad", tbean1);
    prefs1.putBeanObject( "bado", tbean1);

    store1.save();


    XMLStore store2 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs = store2.getPreferences();

    TesterBean tbean =  (TesterBean) prefs.getBean( "bad", null);
    TesterBean tbeano =  (TesterBean) prefs.getBean( "bado", null);

    assert tbean.getS().equals(baddies) : "bean encoding failed" + tbean.getS();
    assert tbeano.getS().equals(baddies) : "beanObject encoding failed" + tbeano.getS();
  }

  @Test
  public void testNonBean() throws IOException {
    XMLStore store1 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs1 = store1.getPreferences();

    Rectangle r1 = new Rectangle(1, 2);
    prefs1.putBean( "rect", r1);
    prefs1.putBeanObject( "recto", r1);

    store1.save();


    XMLStore store2 = XMLStore.createFromFile(prefsFilename, null);
    PreferencesExt prefs = store2.getPreferences();

    // just looking for exceptions
    Rectangle r =  (Rectangle) prefs.getBean( "rect", null);
    Rectangle ro =  (Rectangle) prefs.getBean( "recto", null);
  }
}
