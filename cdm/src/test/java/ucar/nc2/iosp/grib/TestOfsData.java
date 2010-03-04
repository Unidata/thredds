/*
 * Copyright 1999-2010 University Corporation for Atmospheric Research/Unidata
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
 * Date: Mar 4, 2010
 * Time: 1:18:26 PM
 */

package ucar.nc2.iosp.grib;

import junit.framework.TestCase;
import ucar.nc2.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestOfsData extends TestCase {

  public TestOfsData(String name) {
    super(name);
  }

  public void testCompare() throws IOException {
    File where = new File("C:/data/grib/richSection");
    if (where.exists()) {
      String[] args = new String[1];
      args[0] = where.getPath();
      doAll(args);
    } else {
      doAll(null);
    }
  }

  void doAll(String args[]) throws IOException {

    String dirB1;
    if (args == null || args.length < 1) {
      dirB1 = TestAll.testdataDir + "grid/grib/grib2/section/20091122";
    } else {
      dirB1 = args[0] + "/section";
    }
    File dir = new File(dirB1);
    if (dir.isDirectory()) {
      System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
      String[] children = dir.list();
      for (String child : children) {
        //System.out.println( "children i ="+ children[ i ]);
        File aChild = new File(dir, child);
        //System.out.println( "child ="+ child.getName() );
        if (aChild.isDirectory()) {
          // skip index *gbx and inventory *xml files
        } else if (
            child.length() == 0 ||
                child.endsWith("ncml") ||
                child.endsWith("gbx") ||
                child.endsWith("gbx8") ||
                child.endsWith("xml") ||
                child.endsWith("nc") ||
                child.startsWith("ls")) {

        } else {
          System.out.println("\n\nReading File " + child);
          long start = System.currentTimeMillis();

          NetcdfFile ncfile = NetcdfFile.open(dirB1 + "/" + child);
          List<Variable> vars = ncfile.getVariables();
          for (Variable var : vars) {
            List<Dimension> dims = var.getDimensions();
            if (var.getName().startsWith("Latitude") || var.getName().startsWith("Longitude")) {
              assert (var.getRank() == 2);
            } else if (var.getName().startsWith("U-component")) {
              assert (var.getRank() == 4);
              assert (dims.get(0).getName().startsWith("time"));
            } else if (var.getName().startsWith("V-component")) {
              assert (var.getRank() == 4);
              assert (dims.get(0).getName().startsWith("time"));
            } else if (var.getName().startsWith("time") || var.getName().startsWith("depth")) {
              assert (var.getRank() == 1 );
            } else if (var.getName().startsWith("Curvilinear")) {
            } else {
              assert (var.getRank() > 2);
              assert (dims.get(0).getName().startsWith("time"));
            }
          }
          ncfile.close();
        }
      }
    } else {
    }
  }

  static public void main(String args[]) throws IOException {
    TestOfsData od = new TestOfsData("");
    od.testCompare();
  }

}
