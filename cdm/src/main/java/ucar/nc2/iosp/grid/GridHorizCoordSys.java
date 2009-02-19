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

// $Id: GridHorizCoordSys.java 70 2006-07-13 15:16:05Z caron $

package ucar.nc2.iosp.grid;


import ucar.ma2.Array;
import ucar.ma2.DataType;

import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.GaussianLatitudes;
import ucar.unidata.util.StringUtil;
import ucar.grid.GridTableLookup;
import ucar.grid.GridDefRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


/**
 * A horizontal coordinate system created from a GridDefRecord
 * <p/>
 * <p/>
 * <p> Note on "false_easting" and "fale_northing" projection parameters:
 * <ul><li>false_easting(northing) = The value added to all x (y) values in the rectangular coordinates for a map projection.
 * This value frequently is assigned to eliminate negative numbers.
 * Expressed in the unit of measure identified in Planar Coordinate Units.
 * <li>We dont currently use, assuming that the x and y are just fine as negetive numbers.
 *
 * @author caron
 * @version $Revision: 70 $ $Date: 2006-07-13 15:16:05Z $
 */
public class GridHorizCoordSys {

  /**
   * lookup table
   */
  private GridTableLookup lookup;

  /**
   * the grid definition object
   */
  private GridDefRecord gds;

  /**
   * group for this system
   */
  private Group g;

  /**
   * grid name, shape name and id
   */
  private String grid_name, shape_name, id;

  /**
   * flags
   */
  private boolean isLatLon = true,
      isGaussian = false;

  /**
   * GridVariables that have this GridHorizCoordSys
   */
  HashMap varHash = new HashMap(200);

  /**
   * List of GridVariable, sorted by product desc
   */
  HashMap productHash = new HashMap(100);

  /**
   * GridVertCoordSys
   */
  HashMap vcsHash = new HashMap(30);

  /**
   * startx, starty
   */
  private double startx, starty;

  /**
   * projection
   */
  private ProjectionImpl proj;

  /**
   * list of attributes
   */
  private ArrayList attributes = new ArrayList();

  /**
   * Create a new GridHorizCoordSys with the grid definition and lookup
   *
   * @param gds    grid definition
   * @param lookup lookup table for names
   * @param g      Group for this coord system
   */
  GridHorizCoordSys(GridDefRecord gds, GridTableLookup lookup,
                    Group g) {
    this.gds = gds;
    this.lookup = lookup;
    this.g = g;

    this.grid_name = AbstractIOServiceProvider.createValidNetcdfObjectName(
        lookup.getGridName(gds));
    this.shape_name = lookup.getShapeName(gds);
    this.g = g;
    isLatLon = lookup.isLatLon(gds);
    grid_name = StringUtil.replace(grid_name, ' ', "_");
    id = (g == null)
        ? grid_name
        : g.getName();

    if (isLatLon
        && (lookup.getProjectionType(gds)
        == GridTableLookup.GaussianLatLon)) {
      isGaussian = true;
      double np = 90.0;
      String nps = getParamString("Np");  // # lats between pole and equator  (octet 26/27)
      if (null != nps) {
        np = Double.parseDouble(nps);
      }
      gds.addParam(gds.DX, String.valueOf(np));  // fake - need to get actual gaussian calculation here

      // hack-a-whack : who is this for ???
      // gds.dy = 2 * gds.La1 / gds.ny;
      //gds.nx = 800;
      //gds.dx = 360.0/gds.nx;
    }
  }

  /**
   * Get the ID
   *
   * @return the ID
   */
  String getID() {
    return id;
  }  // unique within the file

  /**
   * Get the grid name
   *
   * @return the grid name
   */
  String getGridName() {
    return grid_name;
  }  // used in CF-1 attributes

  /**
   * Get the group
   *
   * @return the group
   */
  Group getGroup() {
    return g;
  }

  /**
   * Is this a lat/lon grid
   *
   * @return true if is lat/lon
   */
  boolean isLatLon() {
    return isLatLon;
  }

  /**
   * Get the number of x points
   *
   * @return the number of x points
   */
  int getNx() {
    return (int) gds.getParamInt(gds.NX);
  }

  /**
   * Get the number of Y points
   *
   * @return the number of Y points
   */
  int getNy() {
    return (int) gds.getParamInt(gds.NY);
  }

  /**
   * Get the X spacing in kilometers
   *
   * @return the X spacing in kilometers
   */
  private double getDxInKm() {
    //return getParamValue(gds.DX) * .001;
    return gds.getParamDouble(gds.DX) * .001;
  }

  /**
   * Get the Y spacing in kilometers
   *
   * @return the Y spacing in kilometers
   */
  private double getDyInKm() {
    return gds.getParamDouble(gds.DY) * .001;
  }

  /**
   * Add the dimensions associated with this coord sys to the netCDF file
   *
   * @param ncfile netCDF file to add to
   */
  void addDimensionsToNetcdfFile(NetcdfFile ncfile) {

    if (isLatLon) {
      ncfile.addDimension(g, new Dimension("lat", gds.getParamInt(gds.NY), true));
      ncfile.addDimension(g, new Dimension("lon", gds.getParamInt(gds.NX), true));
    } else {
      ncfile.addDimension(g, new Dimension("y", gds.getParamInt(gds.NY), true));
      ncfile.addDimension(g, new Dimension("x", gds.getParamInt(gds.NX), true));
    }
  }

  /**
   * Add the variables to the netCDF file
   *
   * @param ncfile the netCDF file
   */
  void addToNetcdfFile(NetcdfFile ncfile) {

    if (isLatLon) {
      double dy = (gds.getParamDouble(gds.LA2)
          < gds.getParamDouble(gds.LA1))
          ? -gds.getParamDouble(gds.DY)
          : gds.getParamDouble(gds.DY);
      if (isGaussian) {
        addGaussianLatAxis(ncfile, "lat", "degrees_north",
            "latitude coordinate", "latitude",
            AxisType.Lat);
      } else {
        addCoordAxis(ncfile, "lat", (int) gds.getParamInt(gds.NY),
            gds.getParamDouble(gds.LA1), dy,
            "degrees_north", "latitude coordinate",
            "latitude", AxisType.Lat);
      }

      addCoordAxis(ncfile, "lon", (int) gds.getParamInt(gds.NX),
          gds.getParamDouble(gds.LO1),
          gds.getParamDouble(gds.DX), "degrees_east",
          "longitude coordinate", "longitude", AxisType.Lon);
      addCoordSystemVariable(ncfile, "latLonCoordSys", "time lat lon");

    } else {
      makeProjection(ncfile);
      double[] yData, xData;
      if (lookup.getProjectionType(gds) == GridTableLookup.RotatedLatLon) {
        double dy = (gds.getParamDouble("La2") < gds.getParamDouble(gds.LA1)
            ? -gds.getParamDouble("dy") : gds.getParamDouble("dy"));

        yData = addCoordAxis(ncfile, "y", gds.getParamInt(gds.NY),
            gds.getParamDouble(gds.LA1), dy, "degrees",
            "y coordinate of projection", "projection_y_coordinate", AxisType.GeoY);
        xData = addCoordAxis(ncfile, "x", gds.getParamInt(gds.NX),
            gds.getParamDouble(gds.LO1), gds.getParamDouble(gds.DX), "degrees",
            "x coordinate of projection", "projection_x_coordinate", AxisType.GeoX);
      } else {
        yData = addCoordAxis(ncfile, "y",
            (int) gds.getParamInt(gds.NY),
            starty, getDyInKm(), "km",
            "y coordinate of projection",
            "projection_y_coordinate",
            AxisType.GeoY);
        xData = addCoordAxis(ncfile, "x",
            (int) gds.getParamInt(gds.NX),
            startx, getDxInKm(), "km",
            "x coordinate of projection",
            "projection_x_coordinate",
            AxisType.GeoX);
      }
      // TODO: ?
      //if (GribServiceProvider.addLatLon) addLatLon2D(ncfile, xData, yData);
      //add2DCoordSystem(ncfile, "projectionCoordSys", "time y x"); // LOOK is this needed?
    }
  }

  /**
   * Add a coordinate axis
   *
   * @param ncfile        the netCDF file to add to
   * @param name          name of the axis
   * @param n             number of points
   * @param start         starting value
   * @param incr          increment between points
   * @param units         units
   * @param desc          description
   * @param standard_name standard name
   * @param axis          axis type
   * @return the coordinate values
   */
  private double[] addCoordAxis(NetcdfFile ncfile, String name, int n,
                                double start, double incr, String units,
                                String desc, String standard_name,
                                AxisType axis) {

    // ncfile.addDimension(g, new Dimension(name, n, true));

    Variable v = new Variable(ncfile, g, null, name);
    v.setDataType(DataType.DOUBLE);
    v.setDimensions(name);

    // create the data
    double[] data = new double[n];
    for (int i = 0; i < n; i++) {
      data[i] = start + incr * i;
    }
    Array dataArray = Array.factory(DataType.DOUBLE, new int[]{n}, data);
    v.setCachedData(dataArray, false);

    v.addAttribute(new Attribute("units", units));
    v.addAttribute(new Attribute("long_name", desc));
    v.addAttribute(new Attribute("standard_name", standard_name));
    v.addAttribute(new Attribute("grid_spacing", incr + " " + units));
    v.addAttribute(new Attribute(_Coordinate.AxisType, axis.toString()));

    ncfile.addVariable(g, v);
    return data;
  }

  /**
   * Add a gaussian lat axis
   *
   * @param ncfile        netCDF file to add to
   * @param name          name of the axis
   * @param units         units
   * @param desc          description
   * @param standard_name standard name
   * @param axis          axis type
   * @return the values
   */
  private double[] addGaussianLatAxis(NetcdfFile ncfile, String name,
                                      String units, String desc,
                                      String standard_name, AxisType axis) {

    double np = gds.getParamDouble("NumberParallels");
    if (Double.isNaN(np)) {
      np = gds.getParamDouble("Np");
    }
    if (Double.isNaN(np)) {
      throw new IllegalArgumentException(
          "Gaussian LAt/Lon grid must have NumberParallels parameter");
    }
    double startLat = gds.getParamDouble(gds.LA1);
    double endLat = gds.getParamDouble(gds.LA2);

    int nlats = (int) (2 * np);
    GaussianLatitudes gaussLats = new GaussianLatitudes(nlats);

    int bestStartIndex = 0,
        bestEndIndex = 0;
    double bestStartDiff = Double.MAX_VALUE;
    double bestEndDiff = Double.MAX_VALUE;
    for (int i = 0; i < nlats; i++) {
      double diff = Math.abs(gaussLats.latd[i] - startLat);
      if (diff < bestStartDiff) {
        bestStartDiff = diff;
        bestStartIndex = i;
      }
      diff = Math.abs(gaussLats.latd[i] - endLat);
      if (diff < bestEndDiff) {
        bestEndDiff = diff;
        bestEndIndex = i;
      }
    }
    assert Math.abs(bestEndIndex - bestStartIndex + 1)
        == gds.getParamInt(gds.NY);
    boolean goesUp = bestEndIndex > bestStartIndex;

    Variable v = new Variable(ncfile, g, null, name);
    v.setDataType(DataType.DOUBLE);
    v.setDimensions(name);

    // create the data
    int n = (int) gds.getParamInt(gds.NY);
    int useIndex = bestStartIndex;
    double[] data = new double[n];
    double[] gaussw = new double[n];
    for (int i = 0; i < n; i++) {
      data[i] = gaussLats.latd[useIndex];
      gaussw[i] = gaussLats.gaussw[useIndex];
      if (goesUp) {
        useIndex++;
      } else {
        useIndex--;
      }
    }
    Array dataArray = Array.factory(DataType.DOUBLE,
        new int[]{n}, data);
    v.setCachedData(dataArray, false);

    v.addAttribute(new Attribute("units", units));
    v.addAttribute(new Attribute("long_name", desc));
    v.addAttribute(new Attribute("standard_name", standard_name));
    v.addAttribute(new Attribute("weights", "gaussw"));
    v.addAttribute(new Attribute(_Coordinate.AxisType, axis.toString()));
    ncfile.addVariable(g, v);

    v = new Variable(ncfile, g, null, "gaussw");
    v.setDataType(DataType.DOUBLE);
    v.setDimensions(name);
    v.addAttribute(new Attribute("long_name",
        "gaussian weights (unnormalized)"));
    dataArray = Array.factory(DataType.DOUBLE,
        new int[]{n}, gaussw);
    v.setCachedData(dataArray, false);
    ncfile.addVariable(g, v);

    return data;
  }


  /**
   * Add 2D lat/lon variables (for CF compliance)
   *
   * @param ncfile netCDF file
   * @param xData  x values
   * @param yData  y values
   */
  private void addLatLon2D(NetcdfFile ncfile, double[] xData,
                           double[] yData) {

    Variable latVar = new Variable(ncfile, g, null, "lat");
    latVar.setDataType(DataType.DOUBLE);
    latVar.setDimensions("y x");
    latVar.addAttribute(new Attribute("units", "degrees_north"));
    latVar.addAttribute(new Attribute("long_name",
        "latitude coordinate"));
    latVar.addAttribute(new Attribute("standard_name", "latitude"));
    latVar.addAttribute(new Attribute(_Coordinate.AxisType,
        AxisType.Lat.toString()));

    Variable lonVar = new Variable(ncfile, g, null, "lon");
    lonVar.setDataType(DataType.DOUBLE);
    lonVar.setDimensions("y x");
    lonVar.addAttribute(new Attribute("units", "degrees_east"));
    lonVar.addAttribute(new Attribute("long_name",
        "longitude coordinate"));
    lonVar.addAttribute(new Attribute("standard_name", "longitude"));
    lonVar.addAttribute(new Attribute(_Coordinate.AxisType,
        AxisType.Lon.toString()));

    int nx = xData.length;
    int ny = yData.length;

    // create the data
    ProjectionPointImpl projPoint = new ProjectionPointImpl();
    LatLonPointImpl latlonPoint = new LatLonPointImpl();
    double[] latData = new double[nx * ny];
    double[] lonData = new double[nx * ny];
    for (int i = 0; i < ny; i++) {
      for (int j = 0; j < nx; j++) {
        projPoint.setLocation(xData[j], yData[i]);
        proj.projToLatLon(projPoint, latlonPoint);
        latData[i * nx + j] = latlonPoint.getLatitude();
        lonData[i * nx + j] = latlonPoint.getLongitude();
      }
    }
    Array latDataArray = Array.factory(DataType.DOUBLE,
        new int[]{ny,
            nx}, latData);
    latVar.setCachedData(latDataArray, false);

    Array lonDataArray = Array.factory(DataType.DOUBLE,
        new int[]{ny,
            nx}, lonData);
    lonVar.setCachedData(lonDataArray, false);

    ncfile.addVariable(g, latVar);
    ncfile.addVariable(g, lonVar);
  }

  /**
   * Make a projection and add it to the netCDF file
   *
   * @param ncfile netCDF file
   */
  private void makeProjection(NetcdfFile ncfile) {
    switch (lookup.getProjectionType(gds)) {

      case GridTableLookup.RotatedLatLon:
        makeRotatedLatLon(ncfile);
        break;
      case GridTableLookup.PolarStereographic:
        makePS();
        break;

      case GridTableLookup.LambertConformal:
        makeLC();
        break;

      case GridTableLookup.Mercator:
        makeMercator();
        break;

      case GridTableLookup.Orthographic:
        makeSpaceViewOrOthographic();
        break;

      default:
        throw new UnsupportedOperationException("unknown projection = "
            + gds.getParamInt(gds.GRID_TYPE));
    }

    Variable v = new Variable(ncfile, g, null, grid_name);
    v.setDataType(DataType.CHAR);
    v.setDimensions(new ArrayList());  // scalar
    char[] data = new char[]{'d'};
    Array dataArray = Array.factory(DataType.CHAR,
        new int[0], data);
    v.setCachedData(dataArray, false);

    for (int i = 0; i < attributes.size(); i++) {
      Attribute att = (Attribute) attributes.get(i);
      v.addAttribute(att);
    }

    v.addAttribute(new Attribute("earth_shape", shape_name));
    if (gds.getParamInt(gds.GRID_SHAPE_CODE) == 1) {
      v.addAttribute(
          new Attribute(
              "spherical_earth_radius_meters",
              new Double(
                  gds.getParamDouble(gds.RADIUS_SPHERICAL_EARTH))));
    }

    addGDSparams(v);
    ncfile.addVariable(g, v);
  }

  // TODO: This is GRIB specific - how to generalize
  /**
   * Add the GDS params to the variable as attributes
   *
   * @param v the GDS params.
   */
  private void addGDSparams(Variable v) {
    // add all the gds parameters
    java.util.Set keys = gds.getKeys();
    ArrayList keyList = new ArrayList(keys);
    Collections.sort(keyList);
    for (int i = 0; i < keyList.size(); i++) {
      String key = (String) keyList.get(i);
      String name =
          AbstractIOServiceProvider.createValidNetcdfObjectName("GridDefRecord_param_" + key);

      String vals = getParamString(key);
      try {
        int vali = Integer.parseInt(vals);
        v.addAttribute(new Attribute(name, new Integer(vali)));
      } catch (Exception e) {
        try {
          double vald = Double.parseDouble(vals);
          v.addAttribute(new Attribute(name, new Double(vald)));
        } catch (Exception e2) {
          v.addAttribute(new Attribute(name, vals));
        }
      }
    }
  }

  /**
   * Add coordinate system variable
   *
   * @param ncfile netCDF file
   * @param name   name of the variable
   * @param dims   dimensions
   */
  private void addCoordSystemVariable(NetcdfFile ncfile, String name,
                                      String dims) {
    Variable v = new Variable(ncfile, g, null, name);
    v.setDataType(DataType.CHAR);
    v.setDimensions(new ArrayList());  // scalar
    Array dataArray = Array.factory(DataType.CHAR, new int[0], new char[]{'0'});
    v.setCachedData(dataArray, false);
    v.addAttribute(new Attribute(_Coordinate.Axes, dims));
    if (!isLatLon()) {
      v.addAttribute(new Attribute(_Coordinate.Transforms,
          getGridName()));
    }

    addGDSparams(v);
    ncfile.addVariable(g, v);
  }

  // lambert conformal

  /**
   * Make a LambertConformalConic projection
   */
  private void makeLC() {
    // we have to project in order to find the origin
    proj = new LambertConformal(gds.getParamDouble(gds.LATIN1),
        gds.getParamDouble(gds.LOV),
        gds.getParamDouble(gds.LATIN1),
        gds.getParamDouble(gds.LATIN2));
    LatLonPointImpl startLL =
        new LatLonPointImpl(gds.getParamDouble(gds.LA1),
            gds.getParamDouble(gds.LO1));
    ProjectionPointImpl start =
        (ProjectionPointImpl) proj.latLonToProj(startLL);
    startx = start.getX();
    starty = start.getY();

    if (Double.isNaN(getDxInKm())) {
      setDxDy(startx, starty, proj);
    }

    if (GridServiceProvider.debugProj) {
      System.out.println("GridHorizCoordSys.makeLC start at latlon "
          + startLL);

      double Lo2 = gds.getParamDouble(gds.LO2);
      double La2 = gds.getParamDouble(gds.LA2);
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GridHorizCoordSys.makeLC end at latlon "
          + endLL);

      ProjectionPointImpl endPP =
          (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + getNx() * getDxInKm();
      double endy = starty + getNy() * getDyInKm();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }

    attributes.add(new Attribute("grid_mapping_name",
        "lambert_conformal_conic"));
    if (gds.getParamDouble(gds.LATIN1)
        == gds.getParamDouble(gds.LATIN2)) {
      attributes.add(
          new Attribute(
              "standard_parallel",
              new Double(gds.getParamDouble(gds.LATIN1))));
    } else {
      double[] data = new double[]{gds.getParamDouble(gds.LATIN1),
          gds.getParamDouble(gds.LATIN2)};
      attributes.add(
          new Attribute(
              "standard_parallel",
              Array.factory(
                  DataType.DOUBLE, new int[]{2},
                  data)));
    }
    attributes.add(
        new Attribute(
            "longitude_of_central_meridian",
            new Double(gds.getParamDouble(gds.LOV))));
    attributes.add(
        new Attribute(
            "latitude_of_projection_origin",
            new Double(gds.getParamDouble(gds.LATIN1))));
    //attributes.add( new Attribute("false_easting", new Double(startx)));
    //attributes.add( new Attribute("false_northing", new Double(starty)));
  }

  // polar stereographic

  /**
   * Make a PolarStereographic projection
   */
  private void makePS() {
    double scale = .933;

    // TODO: should this be a value instead of a boolean?
    double latOrigin = 90.0;
    String s = getParamString("NpProj");
    if ((s != null) && !s.equalsIgnoreCase("true")) {
      latOrigin = -90.0;
    }

    // Why the scale factor?. accordining to GRIB docs:
    // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
    // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60))  [Snyder,Working Manual p157]
    // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
    proj = new Stereographic(latOrigin, gds.getParamDouble(gds.LOV),
        scale);

    // we have to project in order to find the origin
    ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(
        new LatLonPointImpl(
            gds.getParamDouble(gds.LA1),
            gds.getParamDouble(gds.LO1)));
    startx = start.getX();
    starty = start.getY();

    if (Double.isNaN(getDxInKm())) {
      setDxDy(startx, starty, proj);
    }

    if (GridServiceProvider.debugProj) {
      System.out.println("start at proj coord " + start);
      LatLonPoint llpt = proj.projToLatLon(start);
      System.out.println("   end at lat/lon coord " + llpt);
      System.out.println("   should be lat="
          + gds.getParamDouble(gds.LA1) + " lon="
          + gds.getParamDouble(gds.LO1));
    }

    attributes.add(new Attribute("grid_mapping_name",
        "polar_stereographic"));
    attributes.add(
        new Attribute(
            "longitude_of_projection_origin",
            new Double(gds.getParamDouble(gds.LOV))));
    attributes.add(new Attribute("scale_factor_at_projection_origin",
        new Double(scale)));
    attributes.add(new Attribute("latitude_of_projection_origin",
        new Double(latOrigin)));
  }

  // Mercator

  /**
   * Make a Mercator projection
   */
  private void makeMercator() {
    /**
     * Construct a Mercator Projection.
     * @param lon0 longitude of origin (degrees)
     * @param par standard parallel (degrees). cylinder cuts earth at this latitude.
     */
    double Latin = gds.getParamDouble(gds.LATIN);
    double Lo1 = gds.getParamDouble(gds.LO1); //gds.Lo1;
    double La1 = gds.getParamDouble(gds.LA1); //gds.La1;

    // put longitude origin at first point - doesnt actually matter
    proj = new Mercator(Lo1, Latin);

    // find out where
    ProjectionPoint startP = proj.latLonToProj(new LatLonPointImpl(La1, Lo1));
    startx = startP.getX();
    starty = startP.getY();

    attributes.add(new Attribute("grid_mapping_name", "mercator"));
    attributes.add(new Attribute("standard_parallel", Latin));
    attributes.add(new Attribute("longitude_of_projection_origin", Lo1));

    if (GridServiceProvider.debugProj) {
      double Lo2 = gds.getParamDouble(gds.LO2);
      if (Lo2 < Lo2) Lo2 += 360;
      double La2 = gds.getParamDouble(gds.LA2);
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GribHorizCoordSys.makeMercator:   end at latlon= " + endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   start at proj coord " + new ProjectionPointImpl(startx, starty));
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + (getNx() - 1) * getDxInKm();
      double endy = starty + (getNy() - 1) * getDyInKm();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }
  }

  // RotatedLatLon
  private void makeRotatedLatLon(NetcdfFile ncfile) {
    //double splat = 0, splon = 0, spangle = 0;

    double splat = gds.getParamDouble(gds.SPLAT);
    double splon = gds.getParamDouble(gds.SPLON);
    double spangle = gds.getParamDouble(gds.ROTATIONANGLE);

    // Given projection coordinates, need LatLon coordinates
    proj = new RotatedLatLon(splat, splon, spangle);
    LatLonPoint startLL = proj.projToLatLon(
        new ProjectionPointImpl(gds.getParamDouble(gds.LO1), gds.getParamDouble(gds.LA1)));
    startx = startLL.getLongitude();
    starty = startLL.getLatitude();
    addCoordSystemVariable(ncfile, "latLonCoordSys", "time y x");

    // splat, splon, spangle
    attributes.add(new Attribute("grid_mapping_name", "rotated_lat_lon"));
    attributes.add(new Attribute("grid_south_pole_latitude", new Double(splat)));
    attributes.add(new Attribute("grid_south_pole_longitude", new Double(splon)));
    attributes.add(new Attribute("grid_south_pole_angle", new Double(spangle)));

    if (GridServiceProvider.debugProj) {
      System.out.println("Location of pole of rotated grid:");
      System.out.println("Lon=" + splon + ", Lat=" + splat);
      System.out.println("Axial rotation about pole of rotated grid:" + spangle);

      System.out.println("Location of LL in rotated grid:");
      System.out.println("Lon=" + gds.getParamDouble(gds.LO1) + ", Lat=" +
          gds.getParamDouble(gds.LA1));
      System.out.println("Location of LL in non-rotated grid:");
      System.out.println("Lon=" + startx + ", Lat=" + starty);

      double Lo2 = gds.getParamDouble(gds.LO2);
      double La2 = gds.getParamDouble(gds.LA2);
      System.out.println("Location of UR in rotated grid:");
      System.out.println("Lon=" + Lo2 + ", Lat=" + La2);
      System.out.println("Location of UR in non-rotated grid:");
      LatLonPoint endUR = proj.projToLatLon(new ProjectionPointImpl(Lo2, La2));
      System.out.println("Lon=" + endUR.getLongitude() + ", Lat=" + endUR.getLatitude());

      double dy = (La2 < gds.getParamDouble(gds.LA1)) ?
          -gds.getParamDouble(gds.DY) : gds.getParamDouble(gds.DY);
      double endx = gds.getParamDouble(gds.LO1) + (getNx() - 1) * gds.getParamDouble(gds.DX);
      double endy = gds.getParamDouble(gds.LA1) + (getNy() - 1) * dy;

      System.out.println("End point rotated grid should be x=" + endx + " y=" + endy);
    }

  }

  /**
   * Make a Space View Orthographic projection
   */
  private void makeSpaceViewOrOthographic() {
    double Lat0 = gds.getParamDouble("Lap");  // sub-satellite point lat
    double Lon0 = gds.getParamDouble("Lop");  // sub-satellite point lon

    double xp = gds.getParamDouble("Xp");  // sub-satellite point in grid lengths
    double yp = gds.getParamDouble("Yp");

    double dx = gds.getParamDouble("Dx");  // apparent diameter in units of grid lengths
    double dy = gds.getParamDouble("Dy");

    double major_axis = gds.getParamDouble("major_axis_earth");  // km
    double minor_axis = gds.getParamDouble("minor_axis_earth");  // km

    // Nr = altitude of camera from center, in units of radius
    double nr = gds.getParamDouble("Nr") * 1e-6;
    double apparentDiameter = 2 * Math.sqrt((nr - 1) / (nr + 1));  // apparent diameter, units of radius (see Snyder p 173)

    // app diameter kmeters / app diameter grid lengths = m per grid length
    double gridLengthX = major_axis * apparentDiameter / dx;
    double gridLengthY = minor_axis * apparentDiameter / dy;

    gds.addParam(gds.DX, String.valueOf(1000 * gridLengthX));  // meters
    gds.addParam(gds.DY, String.valueOf(1000 * gridLengthY));  // meters

    startx = -gridLengthX * xp;  // km
    starty = -gridLengthY * yp;

    double radius = Earth.getRadius() / 1000.0;  // km

    if (nr == 1111111111.0) {  // LOOK: not sure how all ones will appear as a double, need example
      proj = new Orthographic(Lat0, Lon0, radius);

      attributes.add(new Attribute("grid_mapping_name",
          "orthographic"));
      attributes.add(new Attribute("longitude_of_projection_origin",
          new Double(Lon0)));
      attributes.add(new Attribute("latitude_of_projection_origin",
          new Double(Lat0)));

    } else {  // "space view perspective"

      double height = (nr - 1.0) * radius;  // height = the height of the observing camera in km
      proj = new VerticalPerspectiveView(Lat0, Lon0, radius, height);

      attributes.add(new Attribute("grid_mapping_name",
          "vertical_perspective"));
      attributes.add(new Attribute("longitude_of_projection_origin",
          new Double(Lon0)));
      attributes.add(new Attribute("latitude_of_projection_origin",
          new Double(Lat0)));
      attributes.add(new Attribute("height_above_earth",
          new Double(height)));
    }

    if (GridServiceProvider.debugProj) {

      double Lo2 = gds.getParamDouble(gds.LO2) + 360.0;
      double La2 = gds.getParamDouble(gds.LA2);
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println(
          "GridHorizCoordSys.makeOrthographic end at latlon " + endLL);

      ProjectionPointImpl endPP =
          (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + getNx() * getDxInKm();
      double endy = starty + getNy() * getDyInKm();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }
  }

  /**
   * Get the value of a GridDefRecord intrinsic
   *
   * @param id a GridDefRecord intrinsic
   * @return the values
   */
  private double getParamValue(String id) {
    return gds.readDouble(id);
  }

  /**
   * Get the String of a GridDefRecord intrinsic
   *
   * @param id a GridDefRecord intrinsic
   * @return the String
   */
  private String getParamString(String id) {
    return gds.getParam(id);
  }

  /**
   * Calculate the dx and dy from startx, starty and projection.
   *
   * @param startx starting x projection point
   * @param starty starting y projection point
   * @param proj   projection for transform
   */
  private void setDxDy(double startx, double starty, ProjectionImpl proj) {
    double Lo2 = gds.getParamDouble(gds.LO2);
    double La2 = gds.getParamDouble(gds.LA2);
    if (Double.isNaN(Lo2) || Double.isNaN(La2)) {
      return;
    }
    LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
    ProjectionPointImpl end =
        (ProjectionPointImpl) proj.latLonToProj(endLL);
    double dx = 1000 * Math.abs(end.getX() - startx)
        / (gds.getParamInt(gds.NX) - 1);
    double dy = 1000 * Math.abs(end.getY() - starty)
        / (gds.getParamInt(gds.NY) - 1);
    gds.addParam(gds.DX, String.valueOf(dx));
    gds.addParam(gds.DY, String.valueOf(dy));
  }
}

