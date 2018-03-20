/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.conv;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;

import java.io.IOException;

/**
 * Zebra ATD files.
 *
 * @author caron
 */

public class ZebraConvention extends ATDRadarConvention {

  /**
   * @param ncfile the NetcdfFile to test
   * @return true if we think this is a Zebra file.
   */
  public static boolean isMine(NetcdfFile ncfile) {
    String s = ncfile.findAttValueIgnoreCase(null, "Convention", "none");
    return s.startsWith("Zebra");
  }

  public ZebraConvention() {
    this.conventionName = "Zebra";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    NcMLReader.wrapNcMLresource(ds, CoordSysBuilder.resourcesDir + "Zebra.ncml", cancelTask);

    // special time handling
    // the time coord var is created in the NcML
    // set its values = base_time + time_offset(time)
    Dimension timeDim = ds.findDimension("time");
    Variable base_time = ds.findVariable("base_time");
    Variable time_offset = ds.findVariable("time_offset");
    Variable time = ds.findVariable("time");
    if ((timeDim == null) || (base_time == null) || (time_offset == null) || (time == null))
      return;

    Attribute att = base_time.findAttribute(CDM.UNITS);
    String units = (att != null) ? att.getStringValue() : "seconds since 1970-01-01 00:00 UTC";
    time.addAttribute(new Attribute(CDM.UNITS, units));

    Array data;
    try {
      double baseValue = base_time.readScalarDouble();

      data = time_offset.read();
      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext())
        iter.setDoubleCurrent(iter.getDoubleNext() + baseValue);

    } catch (java.io.IOException ioe) {
      parseInfo.format("ZebraConvention failed to create time Coord Axis for file %s err= %s%n", ds.getLocation(), ioe);
      return;
    }

    time.setCachedData(data, true);
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