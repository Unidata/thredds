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
package ucar.nc2.iosp.hdf5;

import junit.framework.*;


import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author caron
 * @since Jul 17, 2007
 */
public class TestOddTypes extends TestCase {

  public void testOpaque() throws InvalidRangeException, IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfFile ncfile = TestH5.openH5("samples/opaque.h5");
    System.out.println( "\n"+ncfile);
    Variable v2 = ncfile.findVariable("Opaque");
    assert v2 != null;

    Array data = v2.read();
    assert data.getElementType() == ByteBuffer.class : data.getElementType();
    System.out.println( "data size= "+new Section(data.getShape()));
    NCdump.printArray(data, "Opaque data", System.out, null);


    Array odata = v2.read(new Section("1:20"));
    assert odata.getElementType() == ByteBuffer.class;
    assert odata.getSize() == 20;
  }

  public void testEnum() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestH5.openH5("support/enum.h5");
    Variable v2 = ncfile.findVariable("enum");
    assert v2 != null;

    Array data = v2.read();
    assert data.getElementType() == int.class;

    NetcdfDataset ncd = TestH5.openH5dataset("support/enum.h5");
    v2 = ncd.findVariable("enum");
    assert v2 != null;

    data = v2.read();
    assert data.getElementType() == String.class;
  }

  public void testEnum2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = NetcdfDataset.openFile("D:/netcdf4/tst_enum_data.nc", null);
    Variable v2 = ncfile.findVariable("primary_cloud");
    assert v2 != null;

    Array data = v2.read();
    assert data.getElementType() == byte.class;

    NetcdfDataset ncd = NetcdfDataset.openDataset("D:/netcdf4/tst_enum_data.nc");
    v2 = ncd.findVariable("primary_cloud");
    assert v2 != null;

    data = v2.read();
    assert data.getElementType() == String.class;
  }

  public void testTime() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = TestAll.testdataDir + "hdf5/support/time.h5";
    NetcdfFile ncfile = TestNC2.open( filename);
    Variable v = ncfile.findVariable("dset");
    Array data = v.read();
    NCdump.printArray(data, "dset", System.out, null);
    System.out.println( "\n**** testReadNetcdf4 done\n\n"+ncfile);
    ncfile.close();
  }

  // not supporting bitfield, poor documentation
  public void testBitfield() throws InvalidRangeException, IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfFile ncfile = TestH5.openH5("samples/bitfield.h5");
  }

  // attribute vlen String
  public void testVlenStrings() throws InvalidRangeException, IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfFile ncfile = TestH5.openH5("support/vlstra.h5");
    System.out.println( "\n**** testReadNetcdf4 done\n\n"+ncfile);
  }

   public void testAttString() throws InvalidRangeException, IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfFile ncfile = TestH5.openH5("support/attstr.h5");
  }

  public void testCompoundString() throws InvalidRangeException, IOException {
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    TestH5read.readAllData(TestAll.testdataDir + "hdf5/support/cstr.h5");
  }

  public void testCompoundEnum() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = TestAll.testdataDir + "hdf5/support/cenum.h5";
    NetcdfFile ncfile = TestNC2.open( filename);
    Variable v = ncfile.findVariable("enum");
    Array data = v.read();
    NCdump.printArray(data, "enum", System.out, null);
    System.out.println( "\n**** testReadNetcdf4 done\n\n"+ncfile);
    ncfile.close();
  }

  public void misc() {
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));

    // bitfields, opaque
    NetcdfFile ncfile = TestH5.openH5("support/bitop.h5");
    System.out.println( "\n"+ncfile);
  }

  public void testMisc() {
    byte[] heapId = new byte[] { 0, 22, 32, 0, 0, 0, -19, 5};
    int offset = makeIntFromBytes(heapId, 1, 5);
    System.out.printf("%d%n", offset);
  }

  private int makeIntFromBytes(byte[] bb, int start, int n) {
    int result = 0;
    for (int i = start + n - 1; i >= start; i--) {
      result <<= 8;
      byte b = bb[i];
      result += (b < 0) ? b + 256 : b;
    }
    return result;
  }
}
