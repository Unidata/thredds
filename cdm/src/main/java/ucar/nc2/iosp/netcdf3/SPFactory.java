/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.netcdf3;

import ucar.nc2.iosp.netcdf3.N3raf;
import ucar.nc2.iosp.IOServiceProvider;

/**
 * A factory for implementations of netcdf-3 IOServiceProvider.
 * This allows us to switch implementations in one place, used for testing and timing.
 * 
 * @author caron
 */

public class SPFactory {

  static private Class spClass = N3raf.class;
  static private boolean debug = false;

  static public IOServiceProvider getServiceProvider() {
    try {
      if (debug) System.out.println("**********using Service Provider Class = "+spClass.getName());
      return (IOServiceProvider) spClass.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return null;
  }

  static public void setServiceProvider(String spName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    spClass = Class.forName(spName);
    spClass.newInstance(); // fail fast
    if (debug) System.out.println("**********NetcCDF Service Provider Class set to = "+spName);
  }

}
