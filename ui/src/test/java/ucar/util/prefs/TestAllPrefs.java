// $Id: TestAll.java,v 1.6 2005/08/22 17:13:59 caron Exp $
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

import ucar.unidata.util.test.TestDir;
import ucar.util.prefs.ui.*;
import junit.framework.*;

import java.io.*;

/**
 * TestSuite that runs all the sample tests
 */
public class TestAllPrefs {
  public static boolean show = false;
  public static String dir = TestDir.temporaryLocalDataDir;

  /* public static junit.framework.Test suite() {
    // needed in TestBasic testWho
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");

    TestSuite suite = new TestSuite();

    suite.addTest(new TestSuite(TestBasic.class));
    // suite.addTest(new TestSuite(TestInputMunger.class));
    suite.addTest(new TestSuite(TestEvents.class));
    suite.addTest(new TestSuite(TestXMLStore.class));
    // suite.addTest(new TestSuite(TestXMLStoreChains.class));  chain.xml files are lost
    suite.addTest(new TestSuite(TestBeans.class));

    // test ui classes
    suite.addTest(new TestSuite(TestDebug.class)); //
    suite.addTest(new TestSuite(TestPanel.class));
    suite.addTest(new TestSuite(TestPanel2.class)); //
    suite.addTest(new TestSuite(TestPanelStore.class)); //
    suite.addTest(new TestSuite(TestField.class));
    suite.addTest(new TestSuite(TestFieldInput.class)); //

    return suite;
  }     */

  /**
   * copy all bytes from in to out.
   * @param in: InputStream
   * @param out: OutputStream
   * @throws java.io.IOException
   */
  static public void copy(InputStream in, OutputStream out) throws IOException {
      byte[] buffer = new byte[9000];
      while (true) {
        int bytesRead = in.read(buffer);
        if (bytesRead == -1) break;
        out.write(buffer, 0, bytesRead);
      }
  }

  static public void copyFile(String fileInName, OutputStream out) throws IOException {
    InputStream in = null;
    try {
      in = new BufferedInputStream( new FileInputStream( fileInName));
      copy( in, out);
    } finally {
      if (null != in) in.close();
    }
  }

}
