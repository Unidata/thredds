// $Id: TestDBBean.java,v 1.2 2003/01/06 19:37:08 john Exp $
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

import java.beans.*;
import java.io.*;
import java.util.*;


/** simulate a bean that might look like a DB row, say 20 fields */
public class TestDBBean {

  public static void main(String args[]) {
    //System.getProperty("ucar.util.prefs.PreferencesExtFactory");
    TestDBBean pf = new TestDBBean();
    long start, end;
    int nbeans = 2000;

    start = System.currentTimeMillis();
    pf.writeDirect(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeDirect = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    pf.readDirect(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readDirect = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    pf.writeBean(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeBean = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    pf.readBean(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readBean = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    pf.writeBeanCollection(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeBeanCollection = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    pf.readBeanCollection(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readBeanCollection = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    pf.writeBeanObject(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeBeanObject = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    pf.readBeanObject(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readBeanObject = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    pf.writeNode(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeNode = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    pf.readNode(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readNode = "+(end-start)+" msecs");

  }

  void writeDirect(int nbeans) {
    XMLEncoder en = null;
    try {
      en = new XMLEncoder( new BufferedOutputStream( new FileOutputStream(TestAllPrefs.dir+"dbBeanDirect.xml")));
    } catch (IOException e) {
      System.out.println("XMLEncoder Creation failed "+e);
      System.exit(1);
    }

    for (int i=0; i< nbeans; i++) {
      DBBean db = makeBean();
      en.writeObject(db);
    }

    try {
      en.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  void readDirect(int nbeans) {
    XMLDecoder en = null;
    try {
      en = new XMLDecoder( new BufferedInputStream( new FileInputStream(TestAllPrefs.dir+"dbBeanDirect.xml")));
    } catch (IOException e) {
      System.out.println("XMLDecoder Creation failed "+e);
      System.exit(1);
    }

    for (int i=0; i< nbeans; i++) {
      DBBean db = (DBBean) en.readObject();
    }

    try {
      en.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  void writeBean(int nbeans) {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      store = XMLStore.createFromFile(TestAllPrefs.dir+"dbBean.xml", null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    PreferencesExt prefs = (PreferencesExt) userRoot.node("dbBeans");
    for (int i=0; i< nbeans; i++) {
      DBBean db = makeBean();
      prefs.putBean("bean"+i, db);
    }

    try {
      store.save();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  void readBean(int nbeans) {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      store = XMLStore.createFromFile(TestAllPrefs.dir+"dbBean.xml", null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    PreferencesExt prefs = (PreferencesExt) userRoot.node("dbBeans");
    for (int i=0; i< nbeans; i++) {
      DBBean db = (DBBean) prefs.getBean("bean"+i, null);
    }

  }

  void writeBeanCollection(int nbeans) {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      store = XMLStore.createFromFile(TestAllPrefs.dir+"dbBeanCollection.xml", null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    ArrayList beans = new ArrayList();
    PreferencesExt prefs = (PreferencesExt) userRoot.node("dbBeans");
    for (int i=0; i< nbeans; i++) {
      DBBean db = makeBean();
      beans.add(db);
    }
    prefs.putBeanCollection("collection name", beans);

    try {
      store.save();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  void readBeanCollection(int nbeans) {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      store = XMLStore.createFromFile(TestAllPrefs.dir+"dbBeanCollection.xml", null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    PreferencesExt prefs = (PreferencesExt) userRoot.node("dbBeans");
    Collection beans = (Collection) prefs.getBean("collection name", null);
    for (Iterator i = beans.iterator(); i.hasNext(); ) {
      DBBean db = (DBBean) i.next();
    }
  }


  void writeBeanObject(int nbeans) {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      store = XMLStore.createFromFile(TestAllPrefs.dir+"dbBeanObject.xml", null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    PreferencesExt prefs = (PreferencesExt) userRoot.node("dbBeans");
    for (int i=0; i< nbeans; i++) {
      DBBean db = makeBean();
      prefs.putBeanObject("bean"+i, db);
    }

    try {
      store.save();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  void readBeanObject(int nbeans) {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      store = XMLStore.createFromFile(TestAllPrefs.dir+"dbBeanObject.xml", null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    PreferencesExt prefs = (PreferencesExt) userRoot.node("dbBeans");
    for (int i=0; i< nbeans; i++) {
      DBBean db = (DBBean) prefs.getBean("bean"+i, null);
    }

  }

  void writeNode(int nbeans) {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      store = XMLStore.createFromFile(TestAllPrefs.dir+"dbBeanNode.xml", null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    PreferencesExt prefs = (PreferencesExt) userRoot.node("dbBeans");
    for (int i=0; i< nbeans; i++) {
      PreferencesExt node = (PreferencesExt) prefs.node("bean"+i);
      DBBean db = makeBean();
      node.put("fld0", db.getFld0());
      node.put("fld1", db.getFld1());
      node.put("fld2", db.getFld2());
      node.put("fld3", db.getFld3());
      node.put("fld4", db.getFld4());
      node.put("fld5", db.getFld5());
      node.put("fld6", db.getFld6());
      node.put("fld7", db.getFld7());
      node.put("fld8", db.getFld8());
      node.put("fld9", db.getFld9());
    }

    try {
      store.save();
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  void readNode(int nbeans) {
    XMLStore store = null;
    PreferencesExt userRoot = null;
    try {
      store = XMLStore.createFromFile(TestAllPrefs.dir+"dbBeanNode.xml", null);
      userRoot = store.getPreferences();
    } catch (IOException e) {
      System.out.println("XMLStore Creation failed "+e);
      System.exit(1);
    }

    PreferencesExt prefs = (PreferencesExt) userRoot.node("dbBeans");
    for (int i=0; i< nbeans; i++) {
      PreferencesExt node = (PreferencesExt) prefs.node("bean"+i);
      DBBean db = new DBBean();
      db.setFld0(node.get("fld0", null));
      db.setFld1(node.get("fld1", null));
      db.setFld2(node.get("fld2", null));
      db.setFld3(node.get("fld3", null));
      db.setFld4(node.get("fld4", null));
      db.setFld5(node.get("fld5", null));
      db.setFld6(node.get("fld6", null));
      db.setFld7(node.get("fld7", null));
      db.setFld8(node.get("fld8", null));
      db.setFld9(node.get("fld9", null));
    }

  }


  private static java.util.Random r = new java.util.Random();
  public static DBBean makeBean() {
    DBBean db = new DBBean();
    byte[] b = new byte[30];
    db.setV("This is a V");
    db.setFld0( new String( "random string "+r.nextDouble()));
    db.setFld1( new String( "random string "+r.nextDouble()));
    db.setFld2( new String( "random string "+r.nextDouble()));
    db.setFld3( new String( "random string "+r.nextDouble()));
    db.setFld4( new String( "random string "+r.nextDouble()));
    db.setFld5( new String( "random string "+r.nextDouble()));
    db.setFld6( new String( "random string "+r.nextDouble()));
    db.setFld7( new String( "random string "+r.nextDouble()));
    db.setFld8( new String( "random string "+r.nextDouble()));
    db.setFld9( new String( "random string "+r.nextDouble()));

    return db;
  }



  private static final int nflds = 20;
  public static class DBBean {
    private static final int nflds = 20;
    private String fld0, fld1, fld2, fld3, fld4, fld5, fld6, fld7, fld8, fld9;
    private String v;

    public DBBean( ) { }

    public String getV() { return v; }
    public void setV( String val) { v = val; }

    public String getFld0( ) { return fld0; }
    public void setFld0( String val) { fld0 = val; }

    public String getFld1( ) { return fld1; }
    public void setFld1( String val) { fld1 = val; }

    public String getFld2( ) { return fld2; }
    public void setFld2( String val) { fld2 = val; }

    public String getFld3( ) { return fld3; }
    public void setFld3( String val) { fld3 = val; }

    public String getFld4( ) { return fld4; }
    public void setFld4( String val) { fld4 = val; }

    public String getFld5( ) { return fld5; }
    public void setFld5( String val) { fld5 = val; }

    public String getFld6( ) { return fld6; }
    public void setFld6( String val) { fld6 = val; }

    public String getFld7( ) { return fld7; }
    public void setFld7( String val) { fld7 = val; }

    public String getFld8( ) { return fld8; }
    public void setFld8( String val) { fld8 = val; }

    public String getFld9( ) { return fld9; }
    public void setFld9( String val) { fld9 = val; }

    public String toString() {
      return v
        +"\n 0="+fld0
        +"\n 9="+fld9;
    }
  }

}
/* Change History:
   $Log: TestDBBean.java,v $
   Revision 1.2  2003/01/06 19:37:08  john
   new tests

   Revision 1.1  2002/12/24 22:04:52  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/