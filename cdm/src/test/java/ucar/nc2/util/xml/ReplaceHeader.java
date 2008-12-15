/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.util.xml;

import ucar.nc2.util.TableParser;

import java.io.*;

/**
 * @author caron
 * @since Dec 7, 2008
 */
public class ReplaceHeader {

  public void openAllInDir(String dirName) throws IOException {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID "+dirName);
      return;
    }

    for (File f : allFiles) {
      String name = f.getAbsolutePath();
      if (f.isDirectory())
        continue;
      if (name.endsWith(".java")) {
        convert(f);
      }
    }

    for (File f : allFiles) {
      if (f.isDirectory())
        openAllInDir(f.getAbsolutePath());
    }
  }

  void convert(File f) throws IOException {
    BufferedReader dataIS = new BufferedReader(new InputStreamReader( new FileInputStream(f)));
    int count = 0;
    while (true) {
      String line = dataIS.readLine();
      if (line == null) break;

      count++;
    }

  }
}
