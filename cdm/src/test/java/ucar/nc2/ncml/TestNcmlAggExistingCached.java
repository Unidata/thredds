// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Date;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.DiskCache2;

/** Test TestNcml - AggExisting  in the JUnit framework. */

public class TestNcmlAggExistingCached extends TestCase {

  public TestNcmlAggExistingCached( String name) {
    super(name);
  }

  public void setUp() {
    Aggregation.setPersistenceCache( new DiskCache2("/.unidata/cachePersist", true, 60 * 24 * 30, 60));
    NetcdfFileCache.init(50, 70, 20 * 60);
  }

      // String filename = "file:"+TestNcML.topDir + "aggExisting4.ncml";
    String filename = "dods://localhost:8080/thredds/dodsC/aggCacheTest/aggExisting4.ncml";

  public void testNcmlDirect() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfDataset.openDataset(filename, false, null);
    System.out.println("\n TestNcmlAggExistingCached.open "+ filename);
    //System.out.println(" "+ ncfile);

    testAggCoordVar( ncfile);
    testAggCoordVarSubset( ncfile);
    testAggCoordVarSubsetDefeatLocalCache( ncfile);

    ncfile.close();
  }

  public void testNcmlCached() throws IOException, InvalidRangeException {
    System.out.println("\n TestNcmlAggExistingCached.acquire at "+ new Date());
    NetcdfFile ncfile = NetcdfDataset.acquireDataset(filename, null);
    testAggCoordVar( ncfile);
    ncfile.close();

    System.out.println("\n TestNcmlAggExistingCached.acquire again at "+ new Date());
    ncfile = NetcdfDataset.acquireDataset(filename, null);
    testAggCoordVar( ncfile);
    ncfile.close();

    try {
      Thread.currentThread().sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    System.out.println("\n TestNcmlAggExistingCached.acquire after sleeping "+ new Date());
    ncfile = NetcdfDataset.acquireDataset(filename, null);
    testAggCoordVar( ncfile);
    ncfile.close();

    NetcdfFileCache.clearCache(false);
    System.out.println("\n TestNcmlAggExistingCached.acquire after flushing cache "+ new Date());
    ncfile = NetcdfDataset.acquireDataset(filename, null);
    testAggCoordVar( ncfile);
    testAggCoordVarSubset( ncfile);
    ncfile.close();

  }

  double[] result = new double[] {1.1496816E9, 1.1496852E9, 1.1496888E9  };
  public void testAggCoordVar(NetcdfFile ncfile) {
    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == double.class;

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        assert TestAll.closeEnough(dataI.getDoubleNext(), result[count]);
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  public void testAggCoordVarSubset(NetcdfFile ncfile) throws InvalidRangeException, IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read("1:2");
    assert data.getRank() == 1;
    assert data.getSize() == 2;
    assert data.getShape()[0] == 2;
    assert data.getElementType() == double.class;

    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      assert TestAll.closeEnough(dataI.getDoubleNext(), result[count+1]);
      count++;
    }

    data = time.read("0:2:2");
    assert data.getRank() == 1;
    assert data.getSize() == 2;
    assert data.getShape()[0] == 2;
    assert data.getElementType() == double.class;

    count = 0;
    dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      assert TestAll.closeEnough(dataI.getDoubleNext(), result[count*2]);
      count++;
    }
  }
  public void testAggCoordVarSubsetDefeatLocalCache(NetcdfFile ncfile) throws InvalidRangeException, IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    time.setCachedData(null, false);
    Array data = time.read("1:2");
    assert data.getRank() == 1;
    assert data.getSize() == 2;
    assert data.getShape()[0] == 2;
    assert data.getElementType() == double.class;

    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      assert TestAll.closeEnough(dataI.getDoubleNext(), result[count+1]);
      count++;
    }

    time.setCachedData(null, false);
    data = time.read("0:2:2");
    assert data.getRank() == 1;
    assert data.getSize() == 2;
    assert data.getShape()[0] == 2;
    assert data.getElementType() == double.class;

    count = 0;
    dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      assert TestAll.closeEnough(dataI.getDoubleNext(), result[count*2]);
      count++;
    }
  }


}
