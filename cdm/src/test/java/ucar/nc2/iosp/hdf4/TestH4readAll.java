/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.nc2.iosp.hdf4;

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.FileFilter;
import java.io.File;

import junit.framework.TestCase;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestH4readAll extends TestCase {

  public TestH4readAll(String name) {
    super(name);
  }

  public void testReadAll() throws IOException {
    //readandCountAllInDir(testDir, null);
    int count = TestAll.readAllDir("D:/hdf4/", null);
    System.out.println("***READ "+count+" files");
    count = TestAll.readAllDir("R:/testdata/hdf4/", new MyFileFilter());
    System.out.println("***READ "+count+" files");
  }

  class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return pathname.getName().endsWith(".hdf") || pathname.getName().endsWith(".eos");
    }
  }

  public void testProblems() throws IOException {
    //readandCountAllInDir(testDir, null);
    int count = TestAll.readAllDir("E:/problem/", null);
    System.out.println("***READ "+count+" files");
  }


  public void problem() throws IOException {
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/tag1 H4header/tagDetail H4header/linked H4header/construct"));
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/tag2 H4header/tagDetail H4header/construct"));
    //H4header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H4header/linked"));

    //TestAll.readAll("E:/problem/MAC021S0.A2007287.1920.002.2007289002404.hdf");

    NetcdfFile ncfile = NetcdfFile.open("E:/problem/MAC021S0.A2007287.1920.002.2007289002404.hdf");
    //Variable v = ncfile.findVariable("L1B_AIRS_Cal_Subset/Data Fields/radiances");
    //assert v != null;
    //v.read();
    ncfile.close();
  }
}
