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

package ucar.nc2.iosp.hdf4;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.TestAll;
import ucar.nc2.iosp.hdf5.TestH5;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;

import java.io.IOException;
import java.io.File;

import junit.framework.*;

/**
 * Class Description.
 *
 * @author caron
 * @since Oct 15, 2008
 */
public class TestH4eos extends TestCase {

  public TestH4eos(String name) {
    super(name);
  }

  class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      return pathname.getName().endsWith(".hdf") || pathname.getName().endsWith(".eos");
    }
  }

  public void testReadAll() throws IOException {
    //readandCountAllInDir(testDir, null);
    int count = 0;
    count += TestAll.actOnAll("R:/testdata/hdf4/", new MyFileFilter(), new MyAct());
    System.out.println("***READ " + count + " files");
    count += TestAll.actOnAll("D:/hdf4/", new MyFileFilter(), new MyAct());
    System.out.println("***READ " + count + " files");
  }

  private class MyAct implements TestAll.Act {

    public int doAct(String filename) throws IOException {
      NetcdfFile ncfile = null;

      try {
        ncfile = NetcdfFile.open(filename);
        Group root = ncfile.getRootGroup();
        Group g = root.findGroup("HDFEOS INFORMATION");
        if (g == null) g = ncfile.getRootGroup();

        Variable dset = g.findVariable("StructMetadata.0");
        if (dset != null) {
          System.out.println("EOS file=" + filename);
          return 1;
        }

        System.out.println("NOT EOS file=" + filename);
        return 0;
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }

  }
}
