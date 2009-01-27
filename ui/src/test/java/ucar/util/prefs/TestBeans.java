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

import junit.framework.*;

import java.awt.Rectangle;

public class TestBeans extends TestCase {

  public TestBeans( String name) {
    super(name);
  }

  public void testDefault() {
    System.out.println("***TestBeans");
    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      TesterBean tbean = new TesterBean();
      prefs.putBean( "default", tbean);
      prefs.putBeanObject( "defaultObject", tbean);

      store2.save();

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }

    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      TesterBean tbean =  (TesterBean) prefs.getBean( "default", null);
      TesterBean tbeano =  (TesterBean) prefs.getBean( "defaultObject", null);

      assert tbean.getB() == tbeano.getB() : "boolean failed";
      assert tbean.getByte() == tbeano.getByte() : "byte failed";
      assert tbean.getShort() == tbeano.getShort() : "short failed";
      assert tbean.getI() == tbeano.getI() : "int failed";
      assert tbean.getL() == tbeano.getL() : "long failed";
      assert tbean.getF() == tbeano.getF() : "float failed";
      assert tbean.getD() == tbeano.getD() : "double failed";
      assert tbean.getS().equals(tbeano.getS()) : "string failed";

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void testNonDefault() {
    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      TesterBean tbean = new TesterBean(false, 9999, (short) 666, 123456789, .99f, .00001099, "nondefault");
      prefs.putBean( "nondefault", tbean);
      prefs.putBeanObject( "nondefaultObject", tbean);

      store2.save();

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }

    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      TesterBean tbean =  (TesterBean) prefs.getBean( "default", null);
      TesterBean tbeano =  (TesterBean) prefs.getBean( "defaultObject", null);

      assert tbean.getB() == tbeano.getB() : "boolean failed";
      assert tbean.getByte() == tbeano.getByte() : "byte failed";
      assert tbean.getShort() == tbeano.getShort() : "short failed";
      assert tbean.getI() == tbeano.getI() : "int failed";
      assert tbean.getL() == tbeano.getL() : "long failed";
      assert tbean.getF() == tbeano.getF() : "float failed";
      assert tbean.getD() == tbeano.getD() : "double failed";
      assert tbean.getS().equals(tbeano.getS()) : "string failed";

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }
  }

  public void testChangedBean() {
    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      TesterBean tbean = new TesterBean(false, 9999, (short) 666, 123456789, .99f, .00001099, "orig");
      prefs.putBean( "changeableBean", tbean);
      prefs.putBeanObject( "changeableBeanObject", tbean);

      store2.save();

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }

    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      TesterBean tbean =  (TesterBean) prefs.getBean( "changeableBean", null);
      TesterBean tbeano =  (TesterBean) prefs.getBean( "changeableBeanObject", null);

      assert tbean.getS().equals("orig");
      assert tbeano.getS().equals("orig");

      // change the objects
      tbean.setS("changed");
      tbeano.setS("changedo");

      // note putBean not called
      store2.save();
    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }

    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      TesterBean tbean =  (TesterBean) prefs.getBean( "changeableBean", null);
      TesterBean tbeano =  (TesterBean) prefs.getBean( "changeableBeanObject", null);

      assert tbean.getS().equals("changed");
      assert tbeano.getS().equals("changedo");

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }

  }

  public void testBadChars() {
    String baddies =   "q>w<'e;&t\rl\"\nv";
    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      TesterBean tbean = new TesterBean();
      tbean.setS( baddies);
      prefs.putBean( "bad", tbean);
      prefs.putBeanObject( "bado", tbean);

      store2.save();

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }

    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      TesterBean tbean =  (TesterBean) prefs.getBean( "bad", null);
      TesterBean tbeano =  (TesterBean) prefs.getBean( "bado", null);

      assert tbean.getS().equals(baddies) : "bean encoding failed" + tbean.getS();
      assert tbeano.getS().equals(baddies) : "beanObject encoding failed" + tbeano.getS();

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }
  }



  public void testNonBean() {
    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testNBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      Rectangle r = new Rectangle(1, 2);
      prefs.putBean( "rect", r);
      prefs.putBeanObject( "recto", r);

      store2.save();

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }

    try {
      XMLStore store2 = XMLStore.createFromFile(TestAllPrefs.dir+"testNBeans.xml", null);
      PreferencesExt prefs = store2.getPreferences();

      // just looking for exceptions
      Rectangle r =  (Rectangle) prefs.getBean( "rect", null);
      Rectangle ro =  (Rectangle) prefs.getBean( "recto", null);

    } catch (Exception e) {
      assert false;
      System.out.println(e);
      e.printStackTrace();
    }
  }


  boolean closeD( double d1, double d2) {
    if (Math.abs(d1) > 1.0E-7)
      return (Math.abs(d1-d2) / d1) < 1.0E-7;
    else
      return (Math.abs(d1-d2)) < 1.0E-7;
  }
  boolean closeF( float d1, float d2) { return (Math.abs(d1-d2) / d1) < 1.0E-7; }

}
/* Change History:
   $Log: TestBeans.java,v $
   Revision 1.2  2002/12/24 22:04:52  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/