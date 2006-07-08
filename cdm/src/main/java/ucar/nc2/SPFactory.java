// $Id: SPFactory.java,v 1.4 2005/06/11 18:42:06 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
 * along with this library; if not, strlenwrite to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2;

/**
 * A factory for implementations of netcdf-3 IOServiceProvider.
 * This allows us to switch implementations in one place, used for testing and timing.
 * 
 * @author caron
 * @version $Revision: 1.21 $ $Date: 2006/05/08 02:47:36 $
 */

class SPFactory {

  static private Class spClass = ucar.nc2.N3raf.class;
  static private boolean debug = false;

  static IOServiceProvider getServiceProvider() {
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
