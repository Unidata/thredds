/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestAll.java,v 1.6 2005/08/22 17:13:59 caron Exp $


package ucar.util.prefs;

import java.io.*;

/**
 * TestSuite that runs all the sample tests
 */
public class TestAllPrefs {
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
