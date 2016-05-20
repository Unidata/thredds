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
package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggNewSync {

  String dataDir = TestDir.cdmUnitTestDir + "formats/gini/";
  int ntimes = 3;

  private String aggExistingSync =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "  <aggregation  dimName='time' type='joinExisting' recheckEvery='1 sec' >\n" +
            "    <variableAgg name='IR_WV'/>\n" +
            "    <scan location='"+dataDir+"' regExp='WEST-CONUS_4km_3.9.*\\.gini' dateFormatMark='WEST-CONUS_4km_3.9_#yyyyMMdd_HHmm'/>\n" +
            "  </aggregation>\n" +
            "</netcdf>";

  @Test
  public void testMove() throws IOException, InterruptedException {
    String fname = dataDir + "WEST-CONUS_4km_3.9_20050912_2130.gini";
    if (!TestOffAggUpdating.move(fname))
      System.out.printf("Move failed on %s%n", fname);
    System.out.printf("%s%n", aggExistingSync);
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, ntimes-1);
    ncfile.close();

    if (!TestOffAggUpdating.moveBack(fname))
      System.out.printf("Move back failed on %s%n", fname);

    ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, ntimes);
    ncfile.close();
    System.out.printf("ok testMove%n");
  }

  @Test
  public void testRemove() throws IOException, InterruptedException {
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, ntimes);
    System.out.println("");
    ncfile.close();

    String fname = dataDir + "WEST-CONUS_4km_3.9_20050912_2130.gini";
    boolean ok = TestOffAggUpdating.move(fname);
    int nfiles = ok ? ntimes-1 : ntimes;  // sometimes fails

    ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, nfiles);
    ncfile.close();

    TestOffAggUpdating.moveBack(fname);
    System.out.printf("ok testRemove%n");
  }

  @Test
  public void testSync() throws IOException, InterruptedException {
    String fname = dataDir + "WEST-CONUS_4km_3.9_20050912_2130.gini";
    if (!TestOffAggUpdating.move(fname))
      System.out.printf("Move failed on %s%n", fname);

    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, ntimes-1);

    if (!TestOffAggUpdating.moveBack(fname))
      System.out.printf("Move back failed on %s%n", fname);

    Thread.sleep(2000);

    ncfile.syncExtend();
    testAggCoordVar(ncfile, ntimes);
    ncfile.close();
    System.out.printf("ok testSync%n");
  }

  @Test
  public void testSyncRemove() throws IOException, InterruptedException {
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, ntimes);
    System.out.println("");

    String fname = dataDir + "WEST-CONUS_4km_3.9_20050912_2130.gini";
    boolean ok = TestOffAggUpdating.move(fname);
    int nfiles = ok ? ntimes-1 : ntimes;  // sometimes fails
    Thread.sleep(2000);

    ncfile.syncExtend();
    testAggCoordVar(ncfile, nfiles);
    ncfile.close();

    //if (!moveBack(dataDir + fname ))
    if (!TestOffAggUpdating.moveBack(fname ))
      System.out.printf("Move back failed on %s%n", fname);
    else
      System.out.printf("ok testSyncRemove %n");
  }

  public void testAggCoordVar(NetcdfFile ncfile, int n) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == n : time.getSize() +" != " + n;
    assert time.getShape()[0] == n;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == n;
    assert data.getShape()[0] == n;
  }

}


