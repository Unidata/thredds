/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.util.prefs;

import java.beans.ExceptionListener;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.prefs.BackingStoreException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestObjectEncode {

  public static class TesterBean {
    private boolean b = true;
    private byte by = 100;
    private int i = 42;
    private short sh = 44;
    private long l = 393838484;
    private float f = .77F;
    private double d = 1.23;
    private String s = "default";

    public TesterBean() {
    }

    public TesterBean(boolean b, int i, short sh, long l, float f, double d, String s) {
      this.b = b;
      this.by = by;
      this.i = i;
      this.sh = sh;
      this.l = l;
      this.f = f;
      this.d = d;
      this.s = s;
    }

    public double getD() {
      return d;
    }

    public void setD(double d) {
      this.d = d;
    }

    public int getI() {
      return i;
    }

    public void setI(int i) {
      this.i = i;
    }

    public boolean getB() {
      return b;
    }

    public void setB(boolean i) {
      this.b = i;
    }

    public byte getByte() {
      return by;
    }

    public void setByte(byte i) {
      this.by = i;
    }

    public long getL() {
      return l;
    }

    public void setL(long i) {
      this.l = i;
    }

    public float getF() {
      return f;
    }

    public void setF(float i) {
      this.f = i;
    }

    public short getShort() {
      return sh;
    }

    public void setShort(short sh) {
      this.sh = sh;
    }

    public String getS() {
      return s;
    }

    public void setS(String s) {
      this.s = s;
    }

    public String toString() {
      return "TesterBean " + i + " " + d;
    }
  }

  @Test
  public void testNodeChange() throws BackingStoreException, IOException {

    String filename = File.createTempFile("foo", "bar").getAbsolutePath();
    OutputStream objOS = new BufferedOutputStream(new FileOutputStream(filename, false));

    XMLEncoder beanEncoder = new XMLEncoder(objOS);
    beanEncoder.setExceptionListener(new ExceptionListener() {
      public void exceptionThrown(Exception exception) {
        System.out.println("XMLStore.save()");
        exception.printStackTrace();
      }
    });

    beanEncoder.writeObject(new java.awt.Rectangle(100, 200));
    beanEncoder.writeObject(new TesterBean());
    beanEncoder.close();
  }

}
