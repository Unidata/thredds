// $Id: TestInputMunger.java,v 1.4 2005/08/22 01:15:07 caron Exp $
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

import java.io.*;

public class TestInputMunger extends TestCase {

  public TestInputMunger(String name) {
    super(name);
  }

  protected void setUp() {
    TestExtPrefs pf = new TestExtPrefs();
    //pf.run();
  }

  public void testBean() throws IOException {
    System.out.println("***TestInputMunger");

    String s = "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<java version='1.4.1_01' class='java.beans.XMLDecoder'>\n" +
      "<object class=\"ucar.util.prefs.TesterBean\"/></java>\n";
    byte[] sb = s.getBytes();

    //doOne(TestAll.dir+"testMunger.xml", sb);
  }

  public void testBufferSize() throws IOException {
    //doOneFile(TestAll.dir+"testMunger.xml");
    //doOneFile(TestAll.dir+"extPrefs2.xml");
    //doOneFile(TestAll.dir+"testBeanObject.xml");
    //doOneFile(TestAll.dir+"dataViewer.xml");
  }

  public void testRead() {
    try {
      XMLStore xstore = XMLStore.createFromFile(TestAllPrefs.dir+"dataViewer.xml", null);
    } catch (java.io.IOException e) {
      assert false;
      e.printStackTrace();
    }
  }

  public void doOneFile(String filename) throws IOException {
    File f = new File( filename);
    long size = f.length();

    XMLStore store = XMLStore.createFromFile(TestAllPrefs.dir+"fake", null);
    XMLStore.InputMunger im = store.new InputMunger(new FileInputStream( filename), (int) size + 10);

    ByteArrayOutputStream bos = new ByteArrayOutputStream((int)size+10);
    TestAllPrefs.copy(im, bos);
    byte[] sb = bos.toByteArray();

    doOne(filename, sb);
  }

  private void doOne(String filename, byte[] sb) throws IOException {

    XMLStore store = XMLStore.createFromFile(TestAllPrefs.dir+"fake", null);
    byte c;
    int bmin = 200;
    int bmax = Math.min(1025, sb.length);
    if (TestAllPrefs.show) {
      System.out.println("\n*****doOne= "+filename);
      System.out.write(sb);
      System.out.println("\n*****");
      System.out.println(filename+ ": test buffer sizes from "+bmin+" to "+bmax);
    }

    for (int i=bmin; i<bmax; i++) {
      int count = 0;
      XMLStore.InputMunger im = store.new InputMunger(new FileInputStream( filename), i);
      while (0 <= (c = (byte) im.read())) {
        byte w = sb[count];
        assert c == w : "pos= "+count +
          " want="+Character.toString((char)w)+"("+w+")" +
          " got="+ Character.toString((char)c)+"("+c+") size= "+i;
        count++;
        //System.out.println(count+": "+Character.toString((char)c)+"("+c+")");
      }
    }
    if (TestAllPrefs.show) System.out.println("\n*****OK");
 }

}
/* Change History:
   $Log: TestInputMunger.java,v $
   Revision 1.4  2005/08/22 01:15:07  caron
   no message

   Revision 1.3  2003/01/06 19:37:08  john
   new tests

   Revision 1.2  2002/12/24 22:04:53  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:27  john
   start new cvs root: prefs

*/