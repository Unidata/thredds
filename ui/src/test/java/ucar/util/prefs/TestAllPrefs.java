// $Id: TestAll.java,v 1.6 2005/08/22 17:13:59 caron Exp $
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

import ucar.util.prefs.ui.*;
import junit.framework.*;

import java.io.*;

/**
 * TestSuite that runs all the sample tests
 */
public class TestAllPrefs {
  public static boolean show = false;
  public static String dir = "test/data/";

  public static junit.framework.Test suite() {
    // needed in TestBasis testWho
    System.setProperty("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory");

    TestSuite suite = new TestSuite();

    suite.addTest(new TestSuite(TestBasic.class));
    suite.addTest(new TestSuite(TestInputMunger.class));
    suite.addTest(new TestSuite(TestEvents.class));
    suite.addTest(new TestSuite(TestXMLStore.class));
    suite.addTest(new TestSuite(TestXMLStoreChains.class));
    suite.addTest(new TestSuite(TestBeans.class));

    // test ui classes
    suite.addTest(new TestSuite(TestDebug.class)); //
    suite.addTest(new TestSuite(TestPanel.class));
    suite.addTest(new TestSuite(TestPanel2.class)); //
    suite.addTest(new TestSuite(TestPanelStore.class)); // */
    suite.addTest(new TestSuite(TestField.class));
    suite.addTest(new TestSuite(TestFieldInput.class)); // */

    return suite;
  }

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