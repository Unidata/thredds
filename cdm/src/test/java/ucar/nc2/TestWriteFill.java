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
package ucar.nc2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.*;
import ucar.nc2.iosp.netcdf3.N3iosp;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Test writing with fill values
 */
public class TestWriteFill {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testCreateWithFill() throws IOException {
    String filename = tempFolder.newFile("testWriteFill.nc").getAbsolutePath();

    try (NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename, true)) {
      // define dimensions
      Dimension latDim  = ncfile.addDimension("lat", 6);
      Dimension lonDim  = ncfile.addDimension("lon", 12);
      Dimension timeDim = ncfile.addDimension("time", 0, true, true, false);

      ArrayList dims = new ArrayList();
      dims.add(latDim);
      dims.add(lonDim);

      ArrayList rdims = new ArrayList();
      rdims.add(timeDim);
      rdims.add(latDim);
      rdims.add(lonDim);

      // define Variables
      ncfile.addVariable("temperature", DataType.DOUBLE, dims);
      ncfile.addVariableAttribute("temperature", "units", "K");
      ncfile.addVariableAttribute("temperature", "_FillValue", new Double(-999.9));

      // define Variables
      ncfile.addVariable("lat", DataType.DOUBLE, new Dimension[] { latDim });
      ncfile.addVariable("lon", DataType.FLOAT, new Dimension[] { latDim });
      ncfile.addVariable("shorty", DataType.SHORT, new Dimension[] { latDim });

      ncfile.addVariable("rtemperature", DataType.INT, rdims);
      ncfile.addVariableAttribute("rtemperature", "units", "K");
      ncfile.addVariableAttribute("rtemperature", "_FillValue", new Integer(-9999));

      ncfile.addVariable("rdefault", DataType.INT, rdims);

      // add string-valued variables
      Dimension svar_len = ncfile.addDimension("svar_len", 80);
      dims = new ArrayList();
      dims.add(svar_len);
      ncfile.addVariable("svar", DataType.CHAR, dims);
      ncfile.addVariable("svar2", DataType.CHAR, dims);

      // string array
      Dimension names = ncfile.addDimension("names", 3);
      ArrayList dima  = new ArrayList();
      dima.add(names);
      dima.add(svar_len);

      ncfile.addVariable("names", DataType.CHAR, dima);
      ncfile.addVariable("names2", DataType.CHAR, dima);

      // create the file
      try {
        ncfile.create();
      } catch (IOException e) {
        System.err.println("ERROR creating file " + ncfile.getLocation() + "\n" + e);
        assert (false);
      }

      // write some data
      ArrayDouble A   = new ArrayDouble.D3(1, latDim.getLength(), lonDim.getLength() / 2);
      int         i, j;
      Index       ima = A.getIndex();
      // write
      for (i = 0; i < latDim.getLength(); i++) {
        for (j = 0; j < lonDim.getLength() / 2; j++) {
          A.setDouble(ima.set(0, i, j), (double) (i * 1000000 + j * 1000));
        }
      }

      int[] origin = new int[3];
      try {
        ncfile.write("rtemperature", origin, A);
      } catch (IOException e) {
        System.err.println("ERROR writing file");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      //////////////////////////////////////////////////////////////////////
      // test reading, checking for fill values

      Variable temp = ncfile.findVariable("temperature");
      assert (null != temp);

      Array tA = temp.read();
      assert (tA.getRank() == 2);

      ima = tA.getIndex();
      int[] shape = tA.getShape();

      for (i = 0; i < shape[0]; i++) {
        for (j = shape[1]; j < shape[1]; j++) {
          assert (tA.getDouble(ima.set(i, j)) == -999.9);
        }
      }

      Variable rtemp = ncfile.findVariable("rtemperature");
      assert (null != rtemp);

      Array rA = rtemp.read();
      assert (rA.getRank() == 3);

      ima = rA.getIndex();
      int[] rshape = rA.getShape();

      for (i = 0; i < rshape[1]; i++) {
        for (j = rshape[2] / 2 + 1; j < rshape[2]; j++) {
          assert (rA.getDouble(ima.set(0, i, j)) == -9999.0) : rA.getDouble(ima);
        }
      }

      Variable v = ncfile.findVariable("lat");
      assert (null != v);
      Array         data = v.read();
      IndexIterator ii   = data.getIndexIterator();
      while (ii.hasNext()) {
        assert ii.getDoubleNext() == N3iosp.NC_FILL_DOUBLE;
      }

      v = ncfile.findVariable("lon");
      assert (null != v);
      data = v.read();
      ii = data.getIndexIterator();
      while (ii.hasNext()) {
        assert ii.getFloatNext() == N3iosp.NC_FILL_FLOAT;
      }

      v = ncfile.findVariable("shorty");
      assert (null != v);
      data = v.read();
      ii = data.getIndexIterator();
      while (ii.hasNext()) {
        assert ii.getShortNext() == N3iosp.NC_FILL_SHORT;
      }

      v = ncfile.findVariable("rdefault");
      assert (null != v);
      data = v.read();
      ii = data.getIndexIterator();
      while (ii.hasNext()) {
        assert ii.getIntNext() == N3iosp.NC_FILL_INT;
      }
    }
  }
}
