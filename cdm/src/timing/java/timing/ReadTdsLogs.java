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

package timing;

import java.io.BufferedReader;
import java.io.*;
import java.util.StringTokenizer;

/**
 * Class Description.
 *
 * @author caron
 * @since Apr 10, 2008
 */
public class ReadTdsLogs {
  int maxLines = 100;

  void read(String filename) throws IOException {
    InputStream ios = new FileInputStream(filename);

    BufferedReader dataIS = new BufferedReader(new InputStreamReader(ios));
    int count = 0;
    while ((maxLines < 0) || (count < maxLines)) {
      String line = dataIS.readLine();
      if (line == null) break;
      parseLine(line);;
      count++;
    }
    ios.close();
  }

  void parseLine(String line) {
    String[] result = line.split("\\s");
    System.out.println(" ntokes="+result.length);
  }

  public static void main(String args[]) throws IOException {
    new ReadTdsLogs().read("C:/TEMP/threddsLogs/access.2008-03.log");
  }

}
