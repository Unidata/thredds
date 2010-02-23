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

import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.ma2.DataType;
import ucar.ma2.Array;

import java.io.IOException;

/**
 * NsslRadarMosaicConvention Convention.
 *
 * @see "http://www.nmq.nssl.noaa.gov/~qpeverif/blog/?page_id=12"
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
    String cs = ncfile.findAttValueIgnoreCase(null, "Conventions", null);
    if (cs != null) return false;

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

    ds.finish();
  }

  private void augment3D(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    ds.addAttribute(null, new Attribute("Conventions", "NSSL National Reflectivity Mosaic"));

    addLongName(ds, "mrefl_mosaic", "3-D reflectivity mosaic grid");
    addCoordinateAxisType(ds, "Height", AxisType.Height);  
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
    Array missingData = Array.factory(DataType.SHORT.getPrimitiveClassType(), new int[] {2}, new short[] {-990, -9990});
    var.addAttribute(new Attribute("missing_value", missingData));    
    var.addAttribute(new Attribute(_Coordinate.Axes, "Height Lat Lon"));
  }

  private void augment2D(NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    ds.addAttribute(null, new Attribute("Conventions", "NSSL National Reflectivity Mosaic"));

    addLongName(ds, "cref", "composite reflectivity");
    addLongName(ds, "hgt_cref", "height associated with the composite reflectivity");
    addLongName(ds, "etp18", "scho top");
    addLongName(ds, "shi", "csevere hail index");
    addLongName(ds, "posh", "probability of severe hail");
    addLongName(ds, "mehs", "maximum estimated hail size");
    addLongName(ds, "hsr", "hybrid scan reflectivity");
    addLongName(ds, "hsrh", "height associated with the hybrid scan reflectivity");
    addLongName(ds, "vil", "vertically integrated liquid");
    addLongName(ds, "vilD", "vertically integrated liquid density");
    addLongName(ds, "pcp_flag", "Radar precipitation flag");
    addLongName(ds, "pcp_type", "Surface precipitation type");

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

  private void addLongName(NetcdfDataset ds, String varName, String longName) {
    Variable v = ds.findVariable(varName);
    if (v != null)
      v.addAttribute(new Attribute("long_name", longName));
  }

    private void addCoordinateAxisType(NetcdfDataset ds, String varName, AxisType type) {
    Variable v = ds.findVariable(varName);
    if (v != null)
      v.addAttribute(new Attribute(_Coordinate.AxisType, type.name()));
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
    ds.addCoordinateAxis( v);

    // add lon
    v = new CoordinateAxis1D(ds, null, "Lon", DataType.FLOAT, "Lon", "degrees_east", "longitude coordinate");
    ds.setValues(v, nlon, lon, dlon);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
    ds.addCoordinateAxis( v);

    // add time
    ds.addDimension(null, new Dimension("Time", 1));
    v = new CoordinateAxis1D(ds, null, "Time", DataType.INT, "Time", "seconds since 1970-1-1 00:00:00", "time coordinate");
    ds.setValues(v, 1, time, 1);
    v.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));
    ds.addCoordinateAxis( v);
  }

}