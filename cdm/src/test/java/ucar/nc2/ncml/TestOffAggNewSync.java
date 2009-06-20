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

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.io.StringReader;

import ucar.nc2.*;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;

public class TestOffAggNewSync extends TestCase {

  public TestOffAggNewSync(String name) {
    super(name);
  }

  String dataDir = "//shemp/data/testdata/image/testSync/";

  public void testMove() throws IOException, InterruptedException {
    move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    String filename = "file:./" + TestNcML.topDir + "offsite/aggExistingSync.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 7);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    ncfile = NcMLReader.readNcML(filename, null);
    testAggCoordVar(ncfile, 8);
    ncfile.close();
  }


  private String aggExistingSync =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
            "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
            "  <aggregation  dimName='time' type='joinExisting' recheckEvery='1 sec' >\n" +
            "    <variableAgg name='IR_WV'/>\n" +
            "    <scan location='//shemp/data/testdata/image/testSync' suffix='.gini' dateFormatMark='SUPER-NATIONAL_8km_WV_#yyyyMMdd_HHmm'/>\n" +
            "  </aggregation>\n" +
            "</netcdf>";

  public void testRemove() throws IOException, InterruptedException {
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, 8);
    System.out.println("");
    ncfile.close();

    boolean ok = move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    int nfiles = ok ? 7 : 8;  // sometimes fails

    ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, nfiles);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
  }

  public void testSync() throws IOException, InterruptedException {
    move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");

    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, 7);

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    Thread.sleep(2000);

    ncfile.sync();
    testAggCoordVar(ncfile, 8);
    ncfile.close();
  }

  public void testSyncRemove() throws IOException, InterruptedException {
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(aggExistingSync), "aggExistingSync", null);
    testAggCoordVar(ncfile, 8);
    System.out.println("");

    boolean ok = move(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
    int nfiles = ok ? 7 : 8;  // sometimes fails
    Thread.sleep(2000);

    ncfile.sync();
    testAggCoordVar(ncfile, nfiles);
    ncfile.close();

    moveBack(dataDir + "SUPER-NATIONAL_8km_WV_20051128_2100.gini");
  }


  public void testAggCoordVar(NetcdfFile ncfile, int n) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == n : time.getSize() +" != " + n;
    assert time.getShape()[0] == n;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == n;
    assert data.getShape()[0] == n;
    assert data.getElementType() == double.class;

    double prev = Double.NaN;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      double dval = dataI.getDoubleNext();
      System.out.println(" coord=" + dval);
      assert (Double.isNaN(prev) || dval > prev);
      prev = dval;
    }
  }

  boolean move(String filename) {
    File f = new File(filename);
    if (f.exists())
      return f.renameTo(new File(filename + ".save"));
    return false;
  }

  void moveBack(String filename) {
    File f = new File(filename + ".save");
    f.renameTo(new File(filename));
  }

}


