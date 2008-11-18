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
    String filename = TestAll.upcShareTestDataDir + "hdf5/support/time.h5";
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
    TestH5read.readAllData(TestAll.upcShareTestDataDir + "hdf5/support/cstr.h5");
  }

  public void testCompoundEnum() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = TestAll.upcShareTestDataDir + "hdf5/support/cenum.h5";
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
}
