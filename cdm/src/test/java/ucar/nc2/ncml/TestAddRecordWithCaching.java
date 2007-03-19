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
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

/** Test TestNcml - AggExisting  in the JUnit framework. */

public class TestAddRecordWithCaching extends TestCase {

  public TestAddRecordWithCaching( String name) {
    super(name);
  }

  public void setUp() {
    NetcdfFileCache.init(50, 70, 20 * 60);
  }

  String metarFile = TestAll.getUpcSharePath() + "/testdata/station/ldm/metar/Surface_METAR_20060325_0000.nc";
  String wrappedMetarFile = TestAll.getUpcSharePath() + "/testdata/station/ldm/metar/test.ncml";
  String addingMetarFile = "test/data/tmp/addRecords.nc";

  public void testMetarFile() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfDataset.openDataset(metarFile, false, null);
    System.out.println("\n TestAddRecordWithCaching.open "+ metarFile);
    System.out.println(" "+ ncfile);

    assert null == ncfile.findVariable("record");

    ncfile.close();
  }

  public void testMetarFileWrapped() throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfDataset.openDataset(wrappedMetarFile, false, null);
    System.out.println("\n TestAddRecordWithCaching.open "+ wrappedMetarFile);
    System.out.println(" "+ ncfile);

    assert null != ncfile.findVariable("record");

    ncfile.close();
  }

  // for this part, you must be running:
  // FileWriter -in R:/testdata/station/ldm/metar/Surface_METAR_20060325_0000.nc -out <addingMetarFile> -delay 100
  public void testNcmlCached() throws IOException, InvalidRangeException {
    String filename = "file:"+TestNcML.topDir + "addRecordSync.xml";

    doit(filename);

    try {
      Thread.currentThread().sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }

    doit(filename);

  }

  private void doit(String filename) throws IOException {
    System.out.println("\n TestAddRecordWithCaching.acquire at "+ new Date());
    NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null);
    Variable record = ncd.findVariable("record");
    assert null != record;

    long size = record.getSize();
    System.out.println("nrecs = "+ size);

    NetcdfFile ncfile = ncd.getReferencedFile();
    record = ncd.findVariable("record");
    assert null != record;

    long size2 = record.getSize();
    System.out.println("nrecs = "+ size2);

    assert (size == size2) : size+" != "+size2;
    ncfile.close();
  }
}

