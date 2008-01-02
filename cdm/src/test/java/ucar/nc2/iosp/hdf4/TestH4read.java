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
package ucar.nc2.iosp.hdf4;

import junit.framework.TestCase;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.nc2.TestAll;

import java.io.IOException;

/**
 * @author caron
 * @since Jan 1, 2008
 */
public class TestH4read extends TestCase {
  static public String testDir = "C:/data/hdf4/";
  public TestH4read(String name) {
    super(name);
  }

  public void testReadAndCount() throws IOException {
    System.out.println("  dims  vars gatts  atts structs");
    read(testDir+"17766010.hdf", 0, 1, 3, 0, 1);
    read(testDir+"balloon_sonde.o3_knmi000_de.bilt_s2_20060905t112100z_002.hdf", 4, 11, 33, 286, 0);
    read(testDir+"TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF", 2, 3, 5, 9, 0);
    read(testDir+"f13_owsa_04010_09A.hdf", 0, 7, 3, 35, 0);
    read(testDir+"c402_rp_02.diag.sfc.20020122_0130z.hdf", 3, 86, 6, 590, 0);
    read(testDir+"olslit1995.oct_digital_12.hdf", 0, 0, 0, 0, 0); // missing lots : multiple strips plus a raster - crappy
    //read(testDir+"MOP03M-200501-L3V81.0.1.hdf", 0, 0, 0, 0, 0); 
    read(testDir+"96108_08.hdf", 5, 44, 39, 39, 0); // "MODIS Airborne Simulator (MAS) Level-1B Data" Swath data 810 x 716 x 50 channels
  }

  private void read(String filename, int ndims, int nvars, int ngatts, int natts, int nstructs) throws IOException {
    NetcdfFile ncfile = NetcdfFile.open(filename);
    int hdims = ncfile.getDimensions().size();
    int hvars = ncfile.getVariables().size();
    int hgatts = ncfile.getGlobalAttributes().size();
    int hatts = 0;
    int hstructs = 0;
    for (Variable v : ncfile.getVariables()) {
      hatts += v.getAttributes().size();
      if (v instanceof Structure)
        hstructs++;
    }
    print(ndims, hdims);
    print(nvars, hvars);
    print(ngatts, hgatts);
    print(natts, hatts);
    print(nstructs, hstructs);
    System.out.println("   "+filename);
    ncfile.close();
  }

  private void print(int want, int have) {
    System.out.format("%5d", have);
    System.out.print( (want != have) ? "*" : " ");
  }

  public void readAll() {
    TestAll.readAllDir(testDir, null);
  }

  public void problem() {
    TestAll.readAll(testDir+"f13_owsa_04010_09A.hdf");
  }

}
