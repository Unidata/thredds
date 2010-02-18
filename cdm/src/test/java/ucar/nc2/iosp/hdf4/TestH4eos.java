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
import ucar.nc2.util.CompareNetcdf;
import ucar.nc2.iosp.hdf5.TestH5;
import ucar.ma2.*;

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

  String testDir = TestAll.testdataDir + "hdf4/";
  public void testReadAll() throws IOException {
    int count = 0;
    count = TestAll.actOnAll(testDir, new MyFileFilter(), new MyAct());
    System.out.println("***READ " + count + " files");
    //count = TestAll.actOnAll("D:/formats/hdf4/", new MyFileFilter(), new MyAct());
    //System.out.println("***READ " + count + " files");
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

  public void testUnsigned() throws IOException, InvalidRangeException {
    String filename = testDir + "MOD021KM.A2004328.1735.004.2004329164007.hdf";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    String vname = "/MODIS_SWATH_Type_L1B/Data Fields/EV_250_Aggr1km_RefSB";
    Variable v = ncfile.findVariable(vname);
    assert v != null : filename+" "+vname;

    Array data = v.read();
    System.out.printf(" sum =          %f%n", MAMath.sumDouble(data));

    double sum2 = 0;
    double sum3 = 0;
    int[] varShape = v.getShape();
    int[] origin = new int[3];
    int[] size = new int[]{1, varShape[1], varShape[2]};
    for (int i = 0; i < varShape[0]; i++) {
      origin[0] = i;
      Array data2D = v.read(origin, size);

      double sum = MAMath.sumDouble(data2D);
      System.out.printf("  %d sum3D =        %f%n", i, sum);
      sum2 += sum;

//      assert data2D.getRank() == 2;
      sum = MAMath.sumDouble(data2D.reduce(0));
      System.out.printf("  %d sum2D =        %f%n", i, sum);
      sum3 += sum;

      CompareNetcdf.compareData(data2D, data2D.reduce(0));
    }
    System.out.printf(" sum2D =        %f%n", sum2);
    System.out.printf(" sum2D.reduce = %f%n", sum3);
    assert sum2 == sum3;
  }

  public void testUnsigned2() throws IOException, InvalidRangeException {
    int nz = 1;
    int ny = 2030;
    int nx = 1354;
    int size = nz*ny*nx;

    short[] vals = new short[size];
    for (int i=0; i<size; i++ )
      vals[i] = (short) i;

    Array data = Array.factory(DataType.SHORT, new int[] {nz,ny,nx}, vals);
    data.setUnsigned(true);
    System.out.printf(" sum =          %f%n", MAMath.sumDouble(data));
    System.out.printf(" sum2 =          %f%n", MAMath.sumDouble(data.reduce(0)));
  }
}
