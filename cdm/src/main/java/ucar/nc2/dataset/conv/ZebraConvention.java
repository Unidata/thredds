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

package ucar.nc2.dataset.conv;

import ucar.ma2.*;
import ucar.nc2.*;
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

    Attribute att = base_time.findAttribute("units");
    String units = (att != null) ? att.getStringValue() : "seconds since 1970-01-01 00:00 UTC";
    time.addAttribute(new Attribute("units", units));

    Array data;
    try {
      double baseValue = base_time.readScalarDouble();

      data = time_offset.read();
      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext())
        iter.setDoubleCurrent(iter.getDoubleNext() + baseValue);

    } catch (java.io.IOException ioe) {
      parseInfo.format("ZebraConvention failed to create time Coord Axis for file %s err= %s\n", ds.getLocation(), ioe);
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