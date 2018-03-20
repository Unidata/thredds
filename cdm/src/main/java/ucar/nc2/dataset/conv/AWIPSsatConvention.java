/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset.conv;

import ucar.nc2.*;
import ucar.nc2.constants.CDM;
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

  private static final boolean debugProj = false;

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
    if (projName.equalsIgnoreCase("CYLINDRICAL_EQUIDISTANT")) {
      makeLatLonProjection(ds, projName, nx, ny);

      ds.addCoordinateAxis(makeLonCoordAxis(ds, nx, "x"));
      ds.addCoordinateAxis(makeLatCoordAxis(ds, ny, "y"));

    } else {

      if (projName.equalsIgnoreCase("LAMBERT_CONFORMAL"))
        projCT = makeLCProjection(ds, projName, nx, ny);

      if (projName.equalsIgnoreCase("MERCATOR"))
        projCT = makeMercatorProjection(ds, projName, nx, ny);

      ds.addCoordinateAxis(makeXCoordAxis(ds, nx, "x"));
      ds.addCoordinateAxis(makeYCoordAxis(ds, ny, "y"));
    }

    // long_name; LOOK: not sure of units
    Variable datav = ds.findVariable("image");
    String long_name = ds.findAttValueIgnoreCase(null, "channel", null);
    if (null != long_name)
      datav.addAttribute( new Attribute(CDM.LONG_NAME, long_name));
    datav.setDataType(DataType.UBYTE);

    // missing values
    ArrayByte.D1 missing_values = new ArrayByte.D1(2, true);
    missing_values.set(0, (byte) 0);
    missing_values.set(1, (byte) -127);
    datav.addAttribute(new Attribute(CDM.MISSING_VALUE, missing_values));

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
    String vname = v.getShortName();
    String units = v.getUnitsString();

    if (units.equalsIgnoreCase(CDM.LON_UNITS))
       return AxisType.Lon;

    if (units.equalsIgnoreCase(CDM.LAT_UNITS))
       return AxisType.Lat;


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
    if ((dim != null) && dim.getShortName().equalsIgnoreCase("record"))
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
      VarProcess vp = findVarProcess(projCT.getName(), null);
      vp.isCoordinateTransform = true;
      vp.ct = projCT;
    }
    super.makeCoordinateTransforms( ds);
  }

  private void makeLatLonProjection(NetcdfDataset ds, String name, int nx, int ny) throws NoSuchElementException {
    double lat0 = findAttributeDouble( ds, "lat00");
    double lon0 = findAttributeDouble(ds, "lon00");
    double latEnd = findAttributeDouble(ds, "latNxNy");
    double lonEnd = findAttributeDouble(ds, "lonNxNy");
    if (lonEnd < lon0) lonEnd += 360;

    startx = lon0;
    starty = lat0;
    dx = (lonEnd - lon0) / nx;
    dy = (latEnd - lat0) / ny;
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
    if (debugProj) parseInfo.format("getLCProjection start at proj coord %s%n", start);
    startx = start.getX();
    starty = start.getY();

        // we will use the end to compute grid size LOOK may be wrong
    double latN = findAttributeDouble( ds, "latNxNy");
    double lonN = findAttributeDouble( ds, "lonNxNy");
    ProjectionPointImpl end = (ProjectionPointImpl) proj.latLonToProj( new LatLonPointImpl( latN, lonN));
    dx = (end.getX() - startx) / nx;
    dy = (end.getY() - starty) / ny;

    if (debugProj) {
      parseInfo.format("  makeProjectionLC start at proj coord %f %f%n", startx, starty);
      parseInfo.format("  makeProjectionLC end at proj coord %f %f%n", end.getX(),end.getY());
      double fdx = findAttributeDouble(ds, "dxKm");
      double fdy = findAttributeDouble(ds, "dyKm");
      parseInfo.format("  makeProjectionLC calc dx= %f file dx= %f%n",dx,fdx);
      parseInfo.format("  makeProjectionLC calc dy= %f file dy= %f%n",dy,fdy);
    }

    return new ProjectionCT(name, "FGDC", proj);
  }

 private ProjectionCT makeMercatorProjection(NetcdfDataset ds, String name, int nx, int ny) throws NoSuchElementException {
    // double centralLat = findAttributeDouble( ds, "centralLat");
    // Center longitude for the mercator projection, where the mercator projection is parallel to the Earth's surface.
    // from this, i guess is actually transverse mercator
    // double centralLon = findAttributeDouble( ds, "centralLon");
    // lat0, central meridian, scale factor
    // TransverseMercator proj = new TransverseMercator(centralLat, centralLon, 1.0);

    double latDxDy = findAttributeDouble( ds, "latDxDy");
    double lonDxDy = findAttributeDouble( ds, "lonDxDy");

    // lat0, lon0, par
    Mercator proj = new Mercator( lonDxDy, latDxDy);

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
      parseInfo.format("  makeProjectionMercator start at proj coord %f %f%n", startx, starty);
      parseInfo.format("  makeProjectionMercator end at proj coord %f %f%n", end.getX(), end.getY());
      double fdx = findAttributeDouble(ds, "dxKm");
      double fdy = findAttributeDouble(ds, "dyKm");
      parseInfo.format("  makeProjectionMercator calc dx= %f file dx= %f%n",dx,fdx);
      parseInfo.format("  makeProjectionMercator calc dy= %f file dy= %f%n",dy,fdy);
    }

    return new ProjectionCT(name, "FGDC", proj);
  }

  private CoordinateAxis makeXCoordAxis(NetcdfDataset ds, int nx, String xname) {
    CoordinateAxis v = new CoordinateAxis1D(ds, null, xname, DataType.DOUBLE, xname, "km", "x on projection");
    v.setValues( nx, startx, dx);

    parseInfo.format("Created X Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("%n");

    if (debugProj)
      parseInfo.format("  makeXCoordAxis ending x %f nx= %d dx= %f%n",startx + nx * dx, nx, dx);
    return v;
  }

  private CoordinateAxis makeYCoordAxis( NetcdfDataset ds, int ny, String yname) {
    CoordinateAxis v = new CoordinateAxis1D( ds, null, yname, DataType.DOUBLE, yname, "km", "y on projection");
    v.setValues( ny, starty, dy);

    parseInfo.format("Created Y Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("%n");

    if (debugProj)
      parseInfo.format("  makeYCoordAxis ending y %f ny= %d dy= %f%n",starty + ny * dy, ny, dy);
    return v;
  }

  private CoordinateAxis makeLonCoordAxis(NetcdfDataset ds, int nx, String xname) {
    CoordinateAxis v = new CoordinateAxis1D(ds, null, xname, DataType.DOUBLE, xname, CDM.LON_UNITS, "longitude");
    v.setValues(nx, startx, dx);

    parseInfo.format("Created X Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("%n");
    return v;
  }

  private CoordinateAxis makeLatCoordAxis( NetcdfDataset ds, int ny, String yname) {
    CoordinateAxis v = new CoordinateAxis1D( ds, null, yname, DataType.DOUBLE, yname, CDM.LAT_UNITS, "latitude");
    v.setValues(ny, starty, dy);

    parseInfo.format("Created Lat Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("%n");

    return v;
  }

  private double findAttributeDouble( NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    return att.getNumericValue().doubleValue();
  }


}
