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
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.nc2.units.SimpleUnit;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.ma2.ArrayByte;
import ucar.ma2.DataType;

import java.util.*;

/**
 * AWIPS netcdf output.
 *
 * @author caron
 *
 * @see <a href="http://www-md.fsl.noaa.gov/eft/AWIPS/16c/onlinehelp/ifpServerSatelliteNETCDF.html">http://www-md.fsl.noaa.gov/eft/AWIPS/16c/onlinehelp/ifpServerSatelliteNETCDF.html</a>
 * @see <a href="http://www.nws.noaa.gov/mdl/awips/aifmdocs/sec_4_e.htm">http://www.nws.noaa.gov/mdl/awips/aifmdocs/sec_4_e.htm</a>
 */

public class AWIPSsatConvention extends CoordSysBuilder {

  /**
   * @param ncfile the NetcdfFile to test
   * @return true if we think this is a AWIPSsatConvention file.
   */
  public static boolean isMine( NetcdfFile ncfile) {
    return (null != ncfile.findGlobalAttribute("projName")) &&
       (null != ncfile.findGlobalAttribute("lon00")) &&
       (null != ncfile.findGlobalAttribute("lat00")) &&
       (null != ncfile.findGlobalAttribute("lonNxNy")) &&
       (null != ncfile.findGlobalAttribute("latNxNy")) &&
       (null != ncfile.findGlobalAttribute("centralLon")) &&
       (null != ncfile.findGlobalAttribute("centralLat")) &&
       (null != ncfile.findDimension("x")) &&
       (null != ncfile.findDimension("y")) &&
       (null != ncfile.findVariable("image"));
  }

  private final boolean debugProj = false;

  private ProjectionCT projCT = null;
  private double startx, starty, dx, dy;

  public AWIPSsatConvention() {
    this.conventionName = "AWIPS-Sat";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) {
    if (null != ds.findVariable("x")) return; // check if its already been done - aggregating enhanced datasets.

    Dimension dimx = ds.findDimension("x");
    int nx = dimx.getLength();

    Dimension dimy = ds.findDimension("y");
    int ny = dimy.getLength();

    String projName = ds.findAttValueIgnoreCase(null, "projName", "none");
    if (projName.equalsIgnoreCase("LAMBERT_CONFORMAL"))
      projCT = makeLCProjection(ds, projName, nx, ny);

    if (projName.equalsIgnoreCase("MERCATOR"))
      projCT = makeMercatorProjection(ds, projName, nx, ny);

    ds.addCoordinateAxis( makeXCoordAxis( ds, nx, "x"));
    ds.addCoordinateAxis( makeYCoordAxis( ds, ny, "y"));

    // long_name; LOOK: not sure of units
    Variable datav = ds.findVariable("image");
    String long_name = ds.findAttValueIgnoreCase(null, "channel", null);
    if (null != long_name)
      datav.addAttribute( new Attribute("long_name", long_name));

    // missing values
    ArrayByte.D1 missing_values = new ArrayByte.D1(2);
    missing_values.set(0, (byte) 0);
    missing_values.set(1, (byte) -127);
    datav.addAttribute( new Attribute("missing_values", missing_values));
    datav.addAttribute( new Attribute("_unsigned", "true"));

    if (projCT != null) {
        VariableDS v = makeCoordinateTransformVariable(ds, projCT);
        v.addAttribute( new Attribute(_Coordinate.Axes, "x y"));
        ds.addVariable(null, v);
    }

    ds.finish();
 }


  /////////////////////////////////////////////////////////////////////////


  protected AxisType getAxisType( NetcdfDataset ds, VariableEnhanced ve) {
    Variable v = (Variable) ve;
    String vname = v.getName();

   if (vname.equalsIgnoreCase("x"))
      return AxisType.GeoX;

    if (vname.equalsIgnoreCase("lon"))
      return AxisType.Lon;

    if (vname.equalsIgnoreCase("y"))
      return AxisType.GeoY;

    if (vname.equalsIgnoreCase("lat"))
      return AxisType.Lat;

    if (vname.equalsIgnoreCase("record"))
      return AxisType.Time;
    Dimension dim = v.getDimension(0);
    if ((dim != null) && dim.getName().equalsIgnoreCase("record"))
      return AxisType.Time;

    String unit = ve.getUnitsString();
    if (unit != null) {
      if ( SimpleUnit.isCompatible("millibar", unit))
        return AxisType.Pressure;

      if ( SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;
    }

    return AxisType.GeoZ;
  }

  protected void makeCoordinateTransforms( NetcdfDataset ds) {
    if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName());
      vp.isCoordinateTransform = true;
      vp.ct = projCT;
    }
    super.makeCoordinateTransforms( ds);
  }

  private ProjectionCT makeLCProjection(NetcdfDataset ds, String name, int nx, int ny) throws NoSuchElementException {
    double centralLat = findAttributeDouble( ds, "centralLat");
    double centralLon = findAttributeDouble( ds, "centralLon");
    double rotation = findAttributeDouble( ds, "rotation");

    // lat0, lon0, par1, par2
    LambertConformal proj = new LambertConformal(rotation, centralLon, centralLat, centralLat);
    // we have to project in order to find the origin
    double lat0 = findAttributeDouble( ds, "lat00");
    double lon0 = findAttributeDouble( ds, "lon00");
    ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj( new LatLonPointImpl( lat0, lon0));
    if (debugProj) parseInfo.append("getLCProjection start at proj coord ").append(start).append("\n");
    startx = start.getX();
    starty = start.getY();

        // we will use the end to compute grid size LOOK may be wrong
    double latN = findAttributeDouble( ds, "latNxNy");
    double lonN = findAttributeDouble( ds, "lonNxNy");
    ProjectionPointImpl end = (ProjectionPointImpl) proj.latLonToProj( new LatLonPointImpl( latN, lonN));
    dx = (end.getX() - startx) / nx;
    dy = (end.getY() - starty) / ny;

    if (debugProj) {
      parseInfo.append("  makeProjectionLC start at proj coord ").append(startx).append(" ").append(starty).append("\n");
      parseInfo.append("  makeProjectionLC end at proj coord ").append(end.getX()).append(" ").append(end.getY()).append("\n");
      double fdx = findAttributeDouble(ds, "dxKm");
      double fdy = findAttributeDouble(ds, "dyKm");
      parseInfo.append("  makeProjectionLC calc dx= ").append(dx).append(" file dx= ").append(fdx).append("\n");
      parseInfo.append("  makeProjectionLC calc dy= ").append(dy).append(" file dy= ").append(fdy).append("\n");
    }

    return new ProjectionCT(name, "FGDC", proj);
  }

 private ProjectionCT makeMercatorProjection(NetcdfDataset ds, String name, int nx, int ny) throws NoSuchElementException {
    double centralLat = findAttributeDouble( ds, "centralLat");
    // Center longitude for the mercator projection, where the mercator projection is parallel to the Earth's surface.
    // from this, i guess is actually transverse mercator
    double centralLon = findAttributeDouble( ds, "centralLon");
    // lat0, central meridian, scale factor
    // TransverseMercator proj = new TransverseMercator(centralLat, centralLon, 1.0);

    double latDxDy = findAttributeDouble( ds, "latDxDy");
    double lonDxDy = findAttributeDouble( ds, "lonDxDy");

    // lat0, lon0, par
    Mercator proj = new Mercator(latDxDy, lonDxDy, latDxDy);

    // we have to project in order to find the start LOOK may be wrong
    double lat0 = findAttributeDouble( ds, "lat00");
    double lon0 = findAttributeDouble( ds, "lon00");
    ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj( new LatLonPointImpl( lat0, lon0));
    startx = start.getX();
    starty = start.getY();

    // we will use the end to compute grid size
    double latN = findAttributeDouble( ds, "latNxNy");
    double lonN = findAttributeDouble( ds, "lonNxNy");
    ProjectionPointImpl end = (ProjectionPointImpl) proj.latLonToProj( new LatLonPointImpl( latN, lonN));
    dx = (end.getX() - startx) / nx;
    dy = (end.getY() - starty) / ny;

    if (debugProj) {
      parseInfo.append("  makeProjectionMercator start at proj coord ").append(startx).append(" ").append(starty).append("\n");
      parseInfo.append("  makeProjectionMercator end at proj coord ").append(end.getX()).append(" ").append(end.getY()).append("\n");
      double fdx = findAttributeDouble(ds, "dxKm");
      double fdy = findAttributeDouble(ds, "dyKm");
      parseInfo.append("  makeProjectionMercator calc dx= ").append(dx).append(" file dx= ").append(fdx).append("\n");
      parseInfo.append("  makeProjectionMercator calc dy= ").append(dy).append(" file dy= ").append(fdy).append("\n");
    }

    return new ProjectionCT(name, "FGDC", proj);
  }

  private CoordinateAxis makeXCoordAxis(NetcdfDataset ds, int nx, String xname) {
    CoordinateAxis v = new CoordinateAxis1D(ds, null, xname, DataType.DOUBLE, xname, "km", "x on projection");
    ds.setValues(v, nx, startx, dx);

    parseInfo.append("Created X Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo);
    parseInfo.append("\n");

    if (debugProj)
      parseInfo.append("  makeXCoordAxis ending x ").append(startx + nx * dx).append(" nx= ").append(nx).append(" dx= ").append(dx).append("\n");
    return v;
  }

  private CoordinateAxis makeYCoordAxis( NetcdfDataset ds, int ny, String yname) {
    CoordinateAxis v = new CoordinateAxis1D( ds, null, yname, DataType.DOUBLE, yname, "km", "y on projection");
    ds.setValues( v, ny, starty, dy);

    parseInfo.append("Created Y Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo);
    parseInfo.append("\n");

    if (debugProj)
      parseInfo.append("  makeYCoordAxis ending y ").append(starty + ny * dy).append(" ny= ").append(ny).append(" dy= ").append(dy).append("\n");
    return v;
  }

  private double findAttributeDouble( NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    return att.getNumericValue().doubleValue();
  }


}
