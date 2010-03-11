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
/**
 * User: rkambic
 * Date: Oct 22, 2009
 * Time: 3:12:19 PM
 */

package ucar.nc2.iosp.gempak;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;

import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;

public class TestReadingGempak extends TestCase {

  public TestReadingGempak(String name) {
    super(name);
  }

  public void testCompare() throws IOException {
    File where = new File("C:/data/grib/idd");
    if (where.exists()) {
      String[] args = new String[1];
      args[0] = "C:/data/grib/idd";
      doAll(args);
    } else {
      doAll(null);
    }
  }

  void doAll(String args[]) throws IOException {

    String dirB1;
    if (args == null || args.length < 1) {
      dirB1 = TestAll.testdataDir + "grid/gempak";
    } else {
      dirB1 = args[0] + "/gempak";
    }
    File dir = new File(dirB1);
    if (dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String child : children) {
        if ( child.endsWith( ".gem" ) ) {
          if( child.startsWith( "air"))
            continue;
          System.out.println("\n\nReading File " + child);
          long start = System.currentTimeMillis();

          NetcdfFile ncfileBinary = NetcdfFile.open(dirB1 + "/" + child);
          System.out.println("Time to create Netcdf object using Gempak Iosp " +
              (System.currentTimeMillis() - start) + "  ms");
          ncfileBinary.close();
        }
      }
    } else {
    }
  }

  static public void main(String args[]) throws IOException {
    TestReadingGempak ggi = new TestReadingGempak("");
    ggi.testCompare();
  }


}
