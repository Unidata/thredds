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
package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.ncml4.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.ma2.DataType;
import ucar.ma2.Array;

import java.io.IOException;

/**
 * NsslRadarMosaicConvention Convention
 * <p/>
 * http://www.nmq.nssl.noaa.gov/~qpeverif/blog/?page_id=12
 *
 * @author caron
 */

public class NsslRadarMosaicConvention extends CoordSysBuilder {

  /**
   * Is this my file?
   *
   * @param ncfile test this NetcdfFile
   * @return true if we think this is a NsslRadarMosaicConvention file.
   */
  public static boolean isMine(NetcdfFile ncfile) {
    String s = ncfile.findAttValueIgnoreCase(null, "DataType", null);
    if ((s == null) || !(s.equalsIgnoreCase("LatLonGrid") || s.equalsIgnoreCase("LatLonHeightGrid")))
      return false;

    if ((null == ncfile.findGlobalAttribute("Latitude")) ||
        (null == ncfile.findGlobalAttribute("Longitude")) ||
        (null == ncfile.findGlobalAttribute("LatGridSpacing")) ||
        (null == ncfile.findGlobalAttribute("LonGridSpacing")) ||
        (null == ncfile.findGlobalAttribute("Time")))
      return false;

    return true;
  }

  public NsslRadarMosaicConvention() {
    this.conventionName = "NSSL National Reflectivity Mosaic";
  }

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    if (null != ds.findVariable("Lat")) return; // check if its already been done - aggregating enhanced datasets.
    String s = ds.findAttValueIgnoreCase(null, "DataType", null);
    if (s.equalsIgnoreCase("LatLonGrid"))
      augment2D(ds, cancelTask);
    else
      augment3D(ds, cancelTask);
  }

  private void augment3D(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    NcMLReader.wrapNcMLresource(ds, CoordSysBuilder.resourcesDir + "NsslRadarMosaic3D.ncml", cancelTask);
    addCoordSystem(ds);

    Variable var = ds.findVariable("mrefl_mosaic");
    assert var != null;

    float scale_factor = Float.NaN;
    Attribute att = var.findAttributeIgnoreCase("Scale");
    if (att != null) {
      scale_factor = att.getNumericValue().floatValue();
      var.addAttribute(new Attribute("scale_factor", 1.0f / scale_factor));
    }
    att = ds.findGlobalAttributeIgnoreCase("MissingData");
    if (null != att) {
      float val = att.getNumericValue().floatValue();
      if (!Float.isNaN(scale_factor)) val *= scale_factor;
      var.addAttribute(new Attribute("missing_value", (short) val));
    }
    // hack
    Array missingData = Array.factory(DataType.SHORT.getClassType(), new int[] {2}, new short[] {-990, -9990});
    var.addAttribute(new Attribute("missing_value", missingData));    
    var.addAttribute(new Attribute(_Coordinate.Axes, "Height Lat Lon"));

  }

  private void augment2D(NetcdfDataset ds, CancelTask cancelTask) throws IOException {

    NcMLReader.wrapNcMLresource(ds, CoordSysBuilder.resourcesDir + "NsslRadarMosaic.ncml", cancelTask);
    addCoordSystem(ds);

    // fix the variable attributes
    for (Variable var : ds.getVariables()) {
      //boolean need_enhance = false;
      float scale_factor = Float.NaN;
      Attribute att = var.findAttributeIgnoreCase("Scale");
      if (att != null) {
        scale_factor = att.getNumericValue().floatValue();
        var.addAttribute(new Attribute("scale_factor", 1.0f / scale_factor));
        //need_enhance = true;
      }
      att = var.findAttributeIgnoreCase("MissingData");
      if (null != att) {
        float val = att.getNumericValue().floatValue();
        if (!Float.isNaN(scale_factor)) val *= scale_factor;
        var.addAttribute(new Attribute("missing_value", (short) val));
        //need_enhance = true;
      }
      //if (need_enhance)
      //  ((VariableDS)var).enhance();
    }

    ds.finish();
  }

  private void addCoordSystem(NetcdfDataset ds) throws IOException {

    double lat = ds.findGlobalAttributeIgnoreCase("Latitude").getNumericValue().doubleValue();
    double lon = ds.findGlobalAttributeIgnoreCase("Longitude").getNumericValue().doubleValue();
    double dlat = ds.findGlobalAttributeIgnoreCase("LatGridSpacing").getNumericValue().doubleValue();
    double dlon = ds.findGlobalAttributeIgnoreCase("LonGridSpacing").getNumericValue().doubleValue();
    int time = ds.findGlobalAttributeIgnoreCase("Time").getNumericValue().intValue();

    if (debug) System.out.println(ds.getLocation() + " Lat/Lon=" + lat + "/" + lon);

    int nlat = ds.findDimension("Lat").getLength();
    int nlon = ds.findDimension("Lon").getLength();

    // add lat
    CoordinateAxis v = new CoordinateAxis1D(ds, null, "Lat", DataType.FLOAT, "Lat", "degrees_north", "latitude coordinate");
    ds.setValues(v, nlat, lat, -dlat);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
    ds.addVariable(null, v);

    // add lon
    v = new CoordinateAxis1D(ds, null, "Lon", DataType.FLOAT, "Lon", "degrees_east", "longitude coordinate");
    ds.setValues(v, nlon, lon, dlon);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    ds.addVariable(null, v);

    // add time
    ds.addDimension(null, new Dimension("Time", 1));
    v = new CoordinateAxis1D(ds, null, "Time", DataType.INT, "Time", "seconds since 1970-1-1 00:00:00", "time coordinate");
    ds.setValues(v, 1, time, 1);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    ds.addVariable(null, v);

  }

}