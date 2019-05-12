/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.prefs;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.*;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.*;

/** simulate a bean that might look like a DB row, say 20 fields */
@RunWith(JUnit4.class)
public class TestDBBean {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  static {
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");
  }

  @Test
  @Ignore("Broken - please have a look.")
  public void testDBrow() {
    //System.getProperty("ucar.util.prefs.PreferencesExtFactory");
    long start, end;
    int nbeans = 2000;

    start = System.currentTimeMillis();
    writeDirect(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeDirect = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    readDirect(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readDirect = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    writeBean(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeBean = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    readBean(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readBean = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    writeBeanCollection(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeBeanCollection = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    readBeanCollection(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readBeanCollection = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    writeBeanObject(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeBeanObject = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    readBeanObject(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readBeanObject = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    writeNode(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeNode = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    readNode(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readNode = "+(end-start)+" msecs");
  }

  void writeDirect(int nbeans) {
    XMLEncoder en = null;
    try {
      en = new XMLEncoder( new BufferedOutputStream( new FileOutputStream(tempFolder.newFile())));
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
      en = new XMLDecoder(new BufferedInputStream(new FileInputStream(tempFolder.newFile())));
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
      store = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
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
      store = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
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
      store = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
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
      store = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
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
      store = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
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
      store = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
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
      store = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
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
      store = XMLStore.createFromFile(tempFolder.newFile().getAbsolutePath(), null);
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
