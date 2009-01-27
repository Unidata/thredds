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
