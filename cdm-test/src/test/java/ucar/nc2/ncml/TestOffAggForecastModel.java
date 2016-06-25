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
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggForecastModel {
  private int nruns = 15;
  private int nfore = 11;

  static String dataDir = TestDir.cdmUnitTestDir + "ncml/nc/ncmodels/";
  static String ncml =
    "<?xml version='1.0' encoding='UTF-8'?>\n" +
    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
    "  <aggregation dimName='runtime' type='forecastModelRunCollection' recheckEvery='0 sec'>\n" +
    "    <scan location='"+dataDir+"' suffix='.nc' dateFormatMark='NAM_CONUS_80km_#yyyyMMdd_HHmm' enhance='true'/>\n" +
    "  </aggregation>\n" +
    "</netcdf>";

  @Test
  public void testForecastModel() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestDir.cdmUnitTestDir + "ncml/offsite/aggForecastModel.xml";
    System.out.println(" TestOffAggForecastModel.testForecastModel=\n"+ ncml);
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);

    testDimensions(ncfile, nruns);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile, nruns);
    testReadData(ncfile, nruns, nfore);
    testReadSlice(ncfile);

    ncfile.close();
  }

  @Test
  public void testForecastModelExtend() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestDir.cdmUnitTestDir + "ncml/offsite/aggForecastModel.xml";
    String newModel = dataDir + "NAM_CONUS_80km_20051212_1200.nc";
    String newModelsave = dataDir + "NAM_CONUS_80km_20051212_1200.nc.save";
    File newModelFile = new File(newModel);
    File newModelFileSave = new File(newModelsave);

    // remove one of the files from the scan
    if (newModelFile.exists() && !newModelFileSave.exists()) {
      boolean ok = newModelFile.renameTo(newModelFileSave);
      if (!ok) throw new IOException("cant rename file "+newModelFile);
    } else if (!newModelFile.exists() && newModelFileSave.exists()) {
      System.out.println("already renamed");
    } else if (!newModelFile.exists() && !newModelFileSave.exists()) {
      throw new IOException("missing "+newModelFile.getPath());
    } else if (newModelFile.exists() && newModelFileSave.exists()) {
      boolean ok = newModelFile.delete();
      if (!ok) throw new IOException("cant delete file "+newModelFile.getPath());
    }

    System.out.println(" TestOffAggForecastModel.testForecastModel=\n"+ ncml);
    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null)) {
      System.out.println(" TestAggForecastModel.open " + filename);

      testDimensions(ncfile, nruns - 1);
      testCoordVar(ncfile);
      testAggCoordVar(ncfile, nruns - 1);
      testReadData(ncfile, nruns - 1, nfore);
      testReadSlice(ncfile);
    }

    // new file arrives
    boolean ok = newModelFileSave.renameTo(newModelFile);
    if (!ok)
      throw new IOException("cant rename file");

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null)) {
      System.out.println(" TestAggForecastModel.open " + filename);

      testDimensions(ncfile, nruns);
      testCoordVar(ncfile);
      testAggCoordVar(ncfile, nruns);
      testReadData(ncfile, nruns, nfore);
      testReadSlice(ncfile);
    }

  }

  public void testDimensions(NetcdfFile ncfile, int nagg) {
    Dimension latDim = ncfile.findDimension("x");
    assert null != latDim;
    assert latDim.getShortName().equals("x");
    assert latDim.getLength() == 93;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("y");
    assert null != lonDim;
    assert lonDim.getShortName().equals("y");
    assert lonDim.getLength() == 65;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("run");
    assert null != timeDim;
    assert timeDim.getShortName().equals("run");
    assert timeDim.getLength() == nagg : nagg +" != "+ timeDim.getLength();
  }

 public void testCoordVar(NetcdfFile ncfile) throws IOException {

    Variable lat = ncfile.findVariable("y");
    assert null != lat;
    assert lat.getShortName().equals("y");
    assert lat.getRank() == 1;
    assert lat.getSize() == 65;
    assert lat.getShape()[0] == 65;
    assert lat.getDataType() == DataType.DOUBLE;

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension("y"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("km");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == 65;
      assert data.getShape()[0] == 65;
      assert data.getElementType() == double.class;

      IndexIterator dataI = data.getIndexIterator();
      assert Misc.closeEnough(dataI.getDoubleNext(), -832.6983183345455);
      assert Misc.closeEnough(dataI.getDoubleNext(), -751.4273183345456);
      assert Misc.closeEnough(dataI.getDoubleNext(), -670.1563183345455);

  }

  public void testAggCoordVar(NetcdfFile ncfile, int nagg) {
    Variable time = ncfile.findVariable("run");
    assert null != time;
    assert time.getShortName().equals("run");
    assert time.getRank() == 1;
    assert time.getSize() == nagg;
    assert time.getShape()[0] == nagg;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.isCoordinateVariable();

    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == nagg;
      assert data.getShape()[0] == nagg;
      assert data.getElementType() == double.class;

      NCdumpW.printArray(data);

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        double val = dataI.getDoubleNext();
        assert val == count * 12;
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;

    }
  }

  public void testReadData(NetcdfFile ncfile, int nagg, int nfore) throws IOException {
    Variable v = ncfile.findVariable("P_sfc");
    assert null != v;
    assert v.getShortName().equals("P_sfc");
    assert v.getRank() == 4;
    assert v.getShape()[0] == nagg;
    assert v.getShape()[1] == nfore : v.getShape()[1];
    assert v.getShape()[2] == 65;
    assert v.getShape()[3] == 93;
    assert v.getDataType() == DataType.FLOAT;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("run");
    assert v.getDimension(1) == ncfile.findDimension("time");
    assert v.getDimension(2) == ncfile.findDimension("y");
    assert v.getDimension(3) == ncfile.findDimension("x");

      Array data = v.read();
      assert data.getRank() == 4;
      assert data.getShape()[0] == nagg;
      assert data.getShape()[1] == nfore;
      assert data.getShape()[2] == 65;
      assert data.getShape()[3] == 93;
      assert data.getElementType() == float.class;

  }

  public void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable("P_sfc");

      Array data = v.read(origin, shape);
      assert data.getRank() == 4;
      assert data.getSize() == shape[0] * shape[1] * shape[2] * shape[3];
      assert data.getShape()[0] == shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
      assert data.getShape()[3] == shape[3];
      assert data.getElementType() == float.class;

      /* Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          //System.out.println(" "+val);
          assert Misc.closeEnough(val, 100*(i+origin[0]) + 10*j + k) : val;
        } */

  }

  public void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {14, 11, 3, 4} );
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {4, 2, 3, 2} );
    testReadSlice( ncfile, new int[] {5, 0, 0, 0}, new int[] {3, 10, 3, 4} );
    testReadSlice( ncfile, new int[] {10, 0, 0, 0}, new int[] {4, 10, 2, 3} );
   }
}

