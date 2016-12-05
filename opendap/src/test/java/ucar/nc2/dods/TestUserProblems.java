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
package ucar.nc2.dods;

import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.DebugFlagsImpl;

import java.io.IOException;
import java.util.List;

/**
 * Test nc2 dods in the JUnit framework.
 * Dataset {
 * Grid {
 * ARRAY:
 * Float64 amp[10];
 * MAPS:
 * Float64 x[10];
 * } OneD;
 * } Simple;
 */
public class TestUserProblems {

  @org.junit.Test
  public void testGrid() throws IOException, InvalidRangeException {
    try (DODSNetcdfFile dodsfile = TestDODSRead.openAbs("http://iridl.ldeo.columbia.edu/SOURCES/.NOAA/.NCEP/.CPC/.GLOBAL/.daily/dods")) {

      Variable dataV = null;

      // array
      assert (null != (dataV = dodsfile.findVariable("olr")));
      assert dataV instanceof DODSVariable;

      // maps
      Variable v = null;
      assert (null != (v = dodsfile.findVariable("time")));
      assert (null != (v = dodsfile.findVariable("lat")));
      assert (null != (v = dodsfile.findVariable("lon")));

      // read data
      Array data = dataV.read("0, 0:72:1, 0:143:1");
      assert null != data;
    }
  }

  @org.junit.Test
  public void testNomads() throws InvalidRangeException {
    DODSNetcdfFile.setDebugFlags(new DebugFlagsImpl("DODS/serverCall"));
  		/* The temperature is recorded */
    String testfile = "http://nomads.ncdc.noaa.gov/thredds/dodsC/cfsr1hr/200912/tmp2m.gdas.200912.grb2";
    try (NetcdfFile ncfile = NetcdfDataset.openFile(testfile, null)) {
      System.out.printf("The GRIB file %s, temperature is displayed.", testfile);
      System.out.println();

      Variable V2 = ncfile.findVariableByAttribute(null, "units", "K");

      List<Dimension> newdim = V2.getDimensions();
      int num_of_dimension = newdim.size();


      int[] shape = V2.getShape();
      for (int sh = 0; sh < num_of_dimension; sh++) {
        System.out.print(shape[sh]);
        System.out.print(" ");
      }

      int[] origin = new int[4];//4-Dimensional

  			/*Till here the code works fine. I get the error
  			 * opendap.dap.DAP2Exception: Method failed:HTTP/1.1 403 Forbidden
  			 * for the below read() method. */
      ArrayFloat.D4 Temperature = (ArrayFloat.D4) V2.read(origin, shape).reduce();

    } catch (IOException ioe) {
      System.out.println("trying to open " + testfile + " " + ioe);
      // getting 403 on 2 GB request
      assert true;
    }
    System.out.println("---- End of File ----");
  }
}
