// $Id: TesterBean.java,v 1.2 2002/12/24 22:04:54 john Exp $
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
import java.beans.*;
import java.io.*;

public class TesterBean {
    private boolean b = true;
    private byte by = 100;
    private int i = 42;
    private short sh = 44;
    private long l = 393838484;
    private float f = .77F;
    private double d = 1.23;
    private String s = "default";

    public TesterBean( ) { }
    public TesterBean( boolean b, int i, short sh, long l, float f, double d, String s) {
      this.b = b;
      this.by = by;
      this.i = i;
      this.sh = sh;
      this.l = l;
      this.f = f;
      this.d = d;
      this.s = s;
    }
    public double getD() { return d; }
    public void setD( double d) { this.d = d; }

    public int getI() { return i; }
    public void setI( int i) { this.i = i; }

    public boolean getB() { return b; }
    public void setB( boolean i) { this.b = i; }

    public byte getByte() { return by; }
    public void setByte( byte i) { this.by = i; }

    public long getL() { return l; }
    public void setL( long i) { this.l = i; }

    public float getF() { return f; }
    public void setF( float i) { this.f = i; }

    public short getShort() { return sh; }
    public void setShort( short sh) { this.sh = sh; }

    public String getS() { return s; }
    public void setS( String s) { this.s = s; }

    public String toString() { return "TesterBean "+i+" "+d; }


    public static void main(String[] args) throws java.io.IOException {

      File outFile = new File("E:/testXMLEncoder.xml");
      OutputStream objOS = new BufferedOutputStream(new FileOutputStream( outFile, false));

      XMLEncoder beanEncoder = new XMLEncoder( objOS);
      beanEncoder.setExceptionListener(new ExceptionListener() {
        public void exceptionThrown(Exception exception) {
          System.out.println("XMLStore.save()");
          exception.printStackTrace();
        }
      });

      beanEncoder.writeObject( new java.awt.Rectangle(100, 200));
      beanEncoder.writeObject( new TesterBean());
      beanEncoder.close();
    }

}
/* Change History:
   $Log: TesterBean.java,v $
   Revision 1.2  2002/12/24 22:04:54  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/