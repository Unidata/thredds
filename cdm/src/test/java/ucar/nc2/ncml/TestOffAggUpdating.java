/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;
import java.io.File;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Updating dataset
 *
 * @author caron
 * @since Jul 24, 2009
 */

public class TestOffAggUpdating extends TestCase {

  public TestOffAggUpdating( String name) {
    super(name);
  }

  public void testUpdate() throws IOException, InvalidRangeException, InterruptedException {
    String dir = TestAll.cdmUnitTestDir + "agg/updating";
    File dirFile = new File(dir);
    assert dirFile != null;
    assert dirFile.exists();
    assert dirFile.isDirectory();

    // make sure that the extra file is not in the agg
    for (File f : dirFile.listFiles()) {
      if (f.getName().equals("extra.nc")) {
        if (!f.renameTo( new File(dirFile, "extra.wait"))) {
          System.out.printf("Rename fails on %s.extra.nc %n", dirFile);
        }
        break;
      }
    }

    String ncml =
      "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "       <aggregation dimName='time' type='joinExisting' recheckEvery='1 nsec'>\n" +
      "         <scan location='"+dir+"' suffix='*.nc' />\n" +
      "         <variable name='depth'>\n" +
      "           <attribute name='coordinates' value='lon lat'/>\n" +
      "         </variable>\n" +
      "         <variable name='wvh'>\n" +
      "           <attribute name='coordinates' value='lon lat'/>\n" +
      "         </variable>\n" +
      "       </aggregation>\n" +
      "       <attribute name='Conventions' type='String' value='CF-1.0'/>\n" +
      "</netcdf>";

    String location = dir + "agg/updating.ncml";
    System.out.println(" TestOffAggExistingTimeUnitsChange.testNarrGrib=\n"+ ncml);
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), location, null);

    check(ncfile, 12);

    // make sure that the extra file is  in the agg
    for (File f : dirFile.listFiles()) {
      if (f.getName().equals("extra.wait")) {
        if (!f.renameTo( new File(dirFile, "extra.nc")))
          System.out.println(" rename fails on "+ f.getPath());
        break;
      }
    }

    ncfile.sync();
    check(ncfile, 18);

    ncfile.close();    
  }

  private void check(NetcdfFile ncfile, int n) throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable("time");
    assert v != null;
    System.out.printf(" time= %s%n", v.getNameAndDimensions());
    assert v.getSize() == n : v.getSize();

    v = ncfile.findVariable("eta");
    assert v != null;
    assert v.getRank() == 3 : v.getRank();
  }
}

