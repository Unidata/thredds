// $Id: TestInputMunger.java,v 1.4 2005/08/22 01:15:07 caron Exp $
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