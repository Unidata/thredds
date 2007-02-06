// $Id: TesterBean.java,v 1.2 2002/12/24 22:04:54 john Exp $
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