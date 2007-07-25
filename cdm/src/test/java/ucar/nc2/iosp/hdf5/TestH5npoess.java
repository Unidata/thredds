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

import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * @author caron
 * @since Jul 18, 2007
 */
public class TestH5npoess extends TestCase {

  public void test1() throws InvalidRangeException, IOException {
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    TestH5read.readAllData(TestAll.upcShareTestDataDir+"hdf5/npoess/ExampleFiles/AVAFO_NPP_d2003125_t10109_e101038_b9_c2005829155458_devl_Tst.h5");
  }

  public void test2() throws InvalidRangeException, IOException {
    H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/reference"));
    NetcdfFile ncfile = TestH5.openH5("npoess/ExampleFiles/GDNBF-VNCCO_NPP_d2003125_t101038_e10116_b9_c2005829162517_dev.h5");
    Variable dset = ncfile.findVariable("Data_Products/VIIRS-DNB-FGEO/VIIRS-DNB-FGEO_Aggr");
    assert(null != dset );    
  }

}
