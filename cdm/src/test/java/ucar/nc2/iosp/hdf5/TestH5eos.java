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

import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestH5eos extends TestCase {

   public TestH5eos(String name) {
    super(name);
  }

  public void testStructMetadata() {
    //NetcdfFile ncfile = TestH5.open("c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    NetcdfFile ncfile = TestH5.open(TestAll.upcShareTestDataDir+"hdf5/auraData/HIRDLS2-Aura73p_b029_2000d275.he5");

    Group root = ncfile.getRootGroup();
    Group g = root.findGroup("HDFEOS_INFORMATION");
    Variable dset = g.findVariable("StructMetadata.0");
    assert(null != dset );
    assert(dset.getDataType() == DataType.CHAR);

    // read entire array
    Array A;
    try {
      A = dset.read();
    }
    catch (IOException e) {
      System.err.println("ERROR reading file");
      assert false;
      return;
    }
    assert(A.getRank() == 1);
    assert (A instanceof ArrayChar);

    ArrayChar ca = (ArrayChar) A;
    String sval = ca.getString();
    System.out.println(dset.getName());
    System.out.println(" Length = "+sval.length());
    System.out.println(" Value = "+sval);
  }

  public void test1() {
    NetcdfFile ncfile = TestH5.openH5("auraData/HIR2ARSP_c3_na.he5");
    assert ncfile.findDimension("nChans") != null;

    System.out.println(ncfile);
  }

  public void test2() {
    NetcdfFile ncfile = TestH5.openH5("auraData/HIRDLS1_v4.0.2a-aIrix-c2_2003d106.he5");
    System.out.println(ncfile);
  }


}
