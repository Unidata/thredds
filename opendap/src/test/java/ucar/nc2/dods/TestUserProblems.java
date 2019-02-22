/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.DebugFlagsImpl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @org.junit.Test
  public void testGrid() throws IOException, InvalidRangeException {
    System.setProperty("httpservices.urlencode", "false");
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
    System.setProperty("httpservices.urlencode", "true");
  }

  @org.junit.Test
  public void testNomads() throws InvalidRangeException {
    // This server is running TDS v4.2, and there appears to be an issue with encoded urls?
    System.setProperty("httpservices.urlencode", "false");
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
      System.setProperty("httpservices.urlencode", "true");
      System.out.println("trying to open " + testfile + " " + ioe);
      // getting 403 on 2 GB request
      assert true;
    }
    System.setProperty("httpservices.urlencode", "true");
    System.out.println("---- End of File ----");
  }
}
