/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.prefs;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
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


/** test  XMLDecoder */
@RunWith(JUnit4.class)
public class TestDecoder {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private File file;

  @Before
  public void setup() throws IOException {
    this.file = tempFolder.newFile();
    assertThat(this.file).isNotNull();
  }

  @Test
  @Ignore("Broken - please have a look.")
  public void testDecoder() throws FileNotFoundException {
    long start, end;
    int nbeans = 1;

    start = System.currentTimeMillis();
    writeDirect(nbeans);
    end = System.currentTimeMillis();
    System.out.println("writeDirect = "+(end-start)+" msecs");

    start = System.currentTimeMillis();
    readDirect(nbeans);
    end = System.currentTimeMillis();
    System.out.println("readDirect = "+(end-start)+" msecs");
  }

  private void writeDirect(int nbeans) throws FileNotFoundException {
    try (XMLEncoder en = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(file)))) {
      for (int i = 0; i < nbeans; i++) {
        DBBean db = makeBean();
        en.writeObject(db);
      }
      en.flush();
    }
  }

  private void readDirect(int nbeans) throws FileNotFoundException {
    assertThat(this.file).isNotNull();
    try (XMLDecoder en = new XMLDecoder( new BufferedInputStream( new FileInputStream(this.file)), this,
        e -> {
          System.out.println("***XMLStore.read() got Exception= "+e.getClass().getName()+" "+e.getMessage());
        })) {

      // loop till you die
      try {
        while (true) {
          Object o = en.readObject();
          System.out.println("beanObject= " + o.getClass().getName() + "\n " + o);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  private static java.util.Random r = new java.util.Random();
  private static DBBean makeBean() {
    DBBean db = new DBBean();
    byte[] b = new byte[30];
    db.setV("This is a V");
    db.setFld0("random string " + r.nextDouble());
    db.setFld1(  "random string "+r.nextDouble());
    db.setFld2(  "random string "+r.nextDouble());
    db.setFld3(  "random string "+r.nextDouble());
    db.setFld4(  "random string "+r.nextDouble());
    db.setFld5(  "random string "+r.nextDouble());
    db.setFld6(  "random string "+r.nextDouble());
    db.setFld7(  "random string "+r.nextDouble());
    db.setFld8(  "random string "+r.nextDouble());
    db.setFld9(  "random string "+r.nextDouble());

    return db;
  }

  private static final int nflds = 20;
  private static class DBBean {
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
