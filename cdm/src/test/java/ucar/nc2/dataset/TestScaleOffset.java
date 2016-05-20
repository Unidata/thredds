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
package ucar.nc2.dataset;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.ma2.*;

import java.io.IOException;

public class TestScaleOffset {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testWrite() throws Exception {
    String filename = tempFolder.newFile("scaleOffset.nc").getAbsolutePath();
    ArrayDouble unpacked;
    MAMath.ScaleOffset so;
    Array packed;

    try (NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename)) {
      // define dimensions
      Dimension latDim = ncfile.addDimension("lat", 200);
      Dimension lonDim = ncfile.addDimension("lon", 300);
      int       n      = lonDim.getLength();

      // create an array
      unpacked = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
      Index ima = unpacked.getIndex();

      for (int i = 0; i < latDim.getLength(); i++) {
        for (int j = 0; j < lonDim.getLength(); j++) {
          unpacked.setDouble(ima.set(i, j), (i * n + j) + 30.0);
        }
      }

      boolean isUnsigned   = true;
      double  missingValue = -9999;
      int     nbits        = 16;

      // convert to packed form
      so = MAMath.calcScaleOffsetSkipMissingData(unpacked, missingValue, nbits, isUnsigned);
      ncfile.addVariable("unpacked", DataType.DOUBLE, "lat lon");

      ncfile.addVariable("packed", DataType.SHORT, "lat lon");
      if (isUnsigned) {
        ncfile.addVariableAttribute("packed", CDM.UNSIGNED, "true");
      }
      //ncfile.addVariableAttribute("packed", CDM.MISSING_VALUE, new Short( (short) -9999));
      ncfile.addVariableAttribute("packed", CDM.SCALE_FACTOR, so.scale);
      ncfile.addVariableAttribute("packed", "add_offset", so.offset);

      // create the file
      ncfile.create();

      ncfile.write("unpacked", unpacked);

      packed = MAMath.convert2packed(unpacked, missingValue, nbits, isUnsigned, DataType.SHORT);
      ncfile.write("packed", packed);
    }

    Array readPacked;

    // read the packed form, compare to original
    try (NetcdfFile ncfileRead = NetcdfFile.open(filename)) {
      Variable v = ncfileRead.findVariable("packed");
      assert v != null;
      readPacked = v.read();
      ucar.unidata.util.test.CompareNetcdf.compareData(readPacked, packed);
    }

    Array readEnhanced;

    // read the packed form, enhance using scale/offset, compare to original
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(filename)) {
      Variable vs = ncd.findVariable("packed");
      assert vs != null;
      readEnhanced = vs.read();
      //TestCompare.compareData(readEnhanced, unpacked);
      closeEnough(packed, unpacked, readEnhanced, 1.0 / so.scale);
    }

    Array cnvertPacked = MAMath.convert2Unpacked(readPacked, so);
    //TestCompare.compareData(readUnpacked, unpacked);
    closeEnough(packed, cnvertPacked, readEnhanced, 1.0 / so.scale);

    doSubset(filename);
  }

  void closeEnough(Array packed, Array data1, Array data2, double close) {
    IndexIterator iterp = packed.getIndexIterator();
    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    while (iter1.hasNext()) {
      double v1 = iter1.getDoubleNext();
      double v2 = iter2.getDoubleNext();
      double p = iterp.getDoubleNext();
      double diff = Math.abs(v1 - v2);
      assert (diff < close) : v1 + " != " + v2 + " index=" + iter1+" packed="+p;
      //System.out.println(v1 + " == " + v2 + " index=" + iter1+" packed="+p);
    }
  }

  // check section of scale/offset only applies it once
  private void doSubset(String filename) throws IOException, InvalidRangeException {
    // read the packed form, enhance using scale/offset, compare to original
    try (NetcdfDataset ncd = NetcdfDataset.openDataset(filename)) {
      Variable vs = ncd.findVariable("packed");
      assert vs != null;

      Section s            = new Section().appendRange(1, 1).appendRange(1, 1);
      Array   readEnhanced = vs.read(s);
      NCdumpW.printArray(readEnhanced);

      Variable sec         = vs.section(s);
      Array    readSection = sec.read();
      NCdumpW.printArray(readSection);

      ucar.unidata.util.test.CompareNetcdf.compareData(readEnhanced, readSection);
    }
  }
}
