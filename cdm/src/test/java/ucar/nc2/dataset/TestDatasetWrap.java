// $Id: TestDatasetWrap.java,v 1.2 2005/11/17 00:48:21 caron Exp $
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
package ucar.nc2.dataset;

import junit.framework.TestCase;
import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestCompare;
import ucar.nc2.dataset.NetcdfDatasetCache;

import java.io.File;

public class TestDatasetWrap extends TestCase {

  public TestDatasetWrap( String name) {
    super(name);
  }

  public void testDatasetWrap() throws Exception {
    doOne(TestAll.testdataDir+ "grid/netcdf/nuwg/eta.nc");
    //readAllDir( TestAll.testdataDir+ "grid/netcdf");
  }

  void readAllDir(String dirName) throws Exception {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();

    for (int i = 0; i < allFiles.length; i++) {
      String name = allFiles[i].getAbsolutePath();
      if (name.endsWith(".nc"))
        doOne(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }

  private void doOne(String filename) throws Exception {
    NetcdfFile ncfile = NetcdfDataset.acquireFile(filename, null);
    NetcdfDataset ncWrap = new NetcdfDataset( ncfile, true);

    NetcdfDataset ncd = NetcdfDatasetCache.acquire(filename, null);
    System.out.println(" dataset wraps= "+filename);

    TestCompare.compareFiles(ncd, ncWrap);
    ncd.close();
    ncWrap.close();
  }
}
