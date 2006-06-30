// $Id: ZebraConvention.java,v 1.8 2006/01/14 22:15:02 caron Exp $
/*
 * Copyright 1997-2000 Unidata Program Center/University Corporation for
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

package ucar.nc2.dataset.conv;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;

import java.io.IOException;

/**
 * Zebra ATD files.
 */

public class ZebraConvention extends ATDRadarConvention {

  /** return true if we think this is a ZebraConvention file. */
  public static boolean isMine( NetcdfFile ncfile) {
    String s =  ncfile.findAttValueIgnoreCase(null, "Convention", "none");
    return s.startsWith("Zebra");
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    this.conventionName = "Zebra";
    NcMLReader.wrapNcMLresource( ds, CoordSysBuilder.resourcesDir+"Zebra.ncml", cancelTask);

    // special time handling
    // the time coord var is created in the NcML
    // set its values = base_time + time_offset(time)
    Dimension timeDim = ds.findDimension("time");
    Variable base_time = ds.findVariable("base_time");
    Variable time_offset = ds.findVariable("time_offset");
    Variable time = ds.findVariable("time");
    if ((timeDim == null) || (base_time == null) || (time_offset == null) || (time == null))
      return;

    Attribute att = base_time.findAttribute("units");
    String units = (att != null) ? att.getStringValue() : "seconds since 1970-01-01 00:00 UTC";
    time.addAttribute( new Attribute("units", units));

    Array data;
    try {
      double baseValue = base_time.readScalarDouble();

      data = time_offset.read();
      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext())
        iter.setDoubleCurrent( iter.getDoubleNext() + baseValue);

    } catch (java.io.IOException ioe) {
      parseInfo.append("ZebraConvention failed to create time Coord Axis for file "+
        ds.getLocation()+"\n"+ioe+"\n");
      return;
    }

    time.setCachedData( data, true);
  }

  /**
   * Search for netcdf variables with "coord_axis" pointing to a Dimension
   * construct coordAxes from them, add to collection of CoordAxisImpl.
   *
  protected void addCoordAxesFromAliasVariables() {

    // look for aliased variables
    Iterator vars = netcdf.getVariableIterator();
    while (vars.hasNext()) {
      Variable ncvar = (Variable) vars.next();
      String dimName = netcdf.findAttValueIgnoreCase(ncvar, "coord_axis", null);
      if (null == dimName)
        continue;
      Dimension dim = netcdf.findDimension( dimName);
      if (null != dim)
        addCoordAxisFromVariable( dim, ncvar);
    }
  }


  // look for an coord_alias attribute
  private String findAlias( DimCoordAxis dc) {
    if (dc.mid != null)
      return netcdf.findAttValueIgnoreCase(dc.mid, "coord_alias", "");
    if (dc.edge != null)
      return netcdf.findAttValueIgnoreCase(dc.edge, "coord_alias", "");
    return "";
  } */


}

/**
 * $Log: ZebraConvention.java,v $
 * Revision 1.8  2006/01/14 22:15:02  caron
 * Use CoordSysBuilderIF
 *
 * Revision 1.7  2006/01/11 16:15:45  caron
 * syncExtend
 * N3iosp, FileWriter writes by record
 *
 * Revision 1.6  2005/02/20 00:37:00  caron
 * reorganize resources
 *
 * Revision 1.5  2005/01/05 22:47:13  caron
 * no message
 *
 * Revision 1.4  2004/12/10 17:04:17  caron
 * *** empty log message ***
 *
 * Revision 1.3  2004/12/07 01:29:29  caron
 * redo convention parsing, use _Coordinate encoding.
 *
 * Revision 1.2  2004/12/01 05:53:40  caron
 * ncml pass 2, new convention parsing
 *
 * Revision 1.1  2004/08/16 20:53:50  caron
 * 2.2 alpha (2)
 *
 * Revision 1.1  2003/04/08 15:06:28  caron
 * nc2 version 2.1
 *
 * Revision 1.1  2001/09/06 17:12:17  caron
 * zebra hack
 *
 *
 */