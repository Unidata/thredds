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

package ucar.nc2.thredds.server;

import ucar.unidata.util.StringUtil;
import ucar.nc2.util.IO;

import java.io.File;
import java.io.IOException;

/**
 * Class Description.
 *
 * @author caron
 * @since Sep 22, 2008
 */
public class TestTDSerrors {

  static String server = "http://localhost:8080/thredds";
  static String path = "/catalogServices?catalog=http://localhost:9080/thredds/catalog.xml";
  public static void main(String args[]) {
    int count = 0;
    while (true) {
      try {
        String result = IO.readURLcontentsWithException(server+path);
        System.out.println(" result= "+result);
      } catch (Exception e) {
        System.out.println(" failed "+e.getMessage());
      }

      count++;
      if (count % 10 == 0) System.out.println("count = "+count);
    }
  }



}

