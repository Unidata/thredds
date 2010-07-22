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

package ucar.nc2.iosp.grid;


import ucar.ma2.Array;
import ucar.ma2.DataType;

import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.units.SimpleUnit;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.util.GaussianLatitudes;
import ucar.unidata.util.StringUtil;
import ucar.grid.GridTableLookup;
import ucar.grid.GridDefRecord;
import ucar.grib.grib2.Grib2GridTableLookup;
import ucar.grib.grib2.Grib2Tables;
import ucar.grib.grib1.Grib1GridTableLookup;

import java.util.*;


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
 */
public class GridHorizCoordSys {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GridHorizCoordSys.class);

  /**
   * GridVariables that have this GridHorizCoordSys
   */
  Map<String, GridVariable> varHash = new HashMap<String, GridVariable>(200);

  /**
   * List of GridVariable, sorted by product desc
   */
  Map<String, List<GridVariable>> productHash = new HashMap<String, List<GridVariable>>(100);

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
   * GridVertCoordSys
   */
  //HashMap vcsHash = new HashMap(30); // GridVertCoordSys

  /**
   * starting. incr values of prjection coordinates, used to generate the projection coordinate axis values
   */
  private double startx, starty, incrx, incry;

  /**
   * projection
   */
  private ProjectionImpl proj;

  /**
   * list of attributes
   */
  private List<Attribute> attributes = new ArrayList<Attribute>();

  /**
   * Create a new GridHorizCoordSys with the grid definition and lookup
   *
   * @param gds    grid definition
   * @param lookup lookup table for names
   * @param g      Group for this coord system
   */
  public GridHorizCoordSys(GridDefRecord gds, GridTableLookup lookup, Group g) {
    this.gds = gds;
    this.lookup = lookup;
    this.g = g;

    this.grid_name = AbstractIOServiceProvider.createValidNetcdfObjectName(
        lookup.getGridName(gds));
    this.shape_name = lookup.getShapeName(gds);

    isLatLon = lookup.isLatLon(gds);
    grid_name = StringUtil.replace(grid_name, ' ', "_");
    id = (g == null)
        ? grid_name
        : g.getName();

    if (isLatLon && (lookup.getProjectionType(gds) == GridTableLookup.GaussianLatLon)) {
      isGaussian = true;

      double np = gds.getDouble(GridDefRecord.NP); // # lats between pole and equator  (octet 26/27)
      np = (Double.isNaN(np)) ? 90 : np;
      //System.out.println( "np ="+np );
      gds.addParam(GridDefRecord.DY, String.valueOf(np));  // fake - need to get actual gaussian calculation here
    }
  }

  /**
   * Get the ID
   *
   * @return the ID
   */
  public String getID() {
    return id;
  }  // unique within the file

  /**
   * Get the grid name
   *
   * @return the grid name
   */
  public String getGridName() {
    return grid_name;
  }  // used in CF-1 attributes

  /**
   * Get the group
   *
   * @return the group
   */
  public Group getGroup() {
    return g;
  }

  /**
   * Is this a lat/lon grid
   *
   * @return true if is lat/lon
   */
  public boolean isLatLon() {
    return isLatLon;
  }

  /**
   * Get the number of x points
   *
   * @return the number of x points
   */
  public int getNx() {
    return gds.getInt(GridDefRecord.NX);
  }

  /**
   * Get the number of Y points
   *
   * @return the number of Y points
   */
  public int getNy() {
    return gds.getInt(GridDefRecord.NY);
  }

  /**
   * Get the X spacing in kilometers
   *
   * @return the X spacing in kilometers
   */
  public double getDxInKm() {
    return getGridSpacingInKm(GridDefRecord.DX);
    //return dx;
  }

  /**
   * Get the Y spacing in kilometers
   *
   * @return the Y spacing in kilometers
   */
  public double getDyInKm() {
    return getGridSpacingInKm(GridDefRecord.DY);
    //return dy;
  }

  /**
   * Get the grid spacing in kilometers
   * @param type
   * @return the grid spacing in kilometers
   */
  private double getGridSpacingInKm(String type) {
    double value = gds.getDouble(type);
    if (Double.isNaN(value)) return value;
    String gridUnit = gds.getParam(GridDefRecord.GRID_UNITS);
    SimpleUnit unit = null;

    if (gridUnit == null || gridUnit.equals("")) {
      unit = SimpleUnit.meterUnit;
    } else {
      unit = SimpleUnit.factory(gridUnit);
    }
    if (unit != null && SimpleUnit.isCompatible(unit.getUnitString(), "km")) {
      value = unit.convertTo(value, SimpleUnit.kmUnit);
    }
    return value;

  }

  /**
   * Add the dimensions associated with this coord sys to the netCDF file
   *
   * @param ncfile netCDF file to add to
   */
  void addDimensionsToNetcdfFile(NetcdfFile ncfile) {

    if (isLatLon) {
      ncfile.addDimension(g, new Dimension("lat", gds.getInt(GridDefRecord.NY), true));
      ncfile.addDimension(g, new Dimension("lon", gds.getInt(GridDefRecord.NX), true));
    } else {
      ncfile.addDimension(g, new Dimension("y", gds.getInt(GridDefRecord.NY), true));
      ncfile.addDimension(g, new Dimension("x", gds.getInt(GridDefRecord.NX), true));
    }
  }

  /**
   * Add the variables to the netCDF file
   *
   * @param ncfile the netCDF file
   */
  void addToNetcdfFile(NetcdfFile ncfile) {

    if (isLatLon) {
      double dy;
      if (gds.getDouble(GridDefRecord.DY) == GridDefRecord.UNDEFINED) {
        dy = setLatLonDxDy();
      } else {
        dy = (gds.getDouble(GridDefRecord.LA2) < gds.getDouble(GridDefRecord.LA1))
            ? -gds.getDouble(GridDefRecord.DY) : gds.getDouble(GridDefRecord.DY);
      }
      if (isGaussian) {
        addGaussianLatAxis(ncfile, "lat", "degrees_north",
            "latitude coordinate", "latitude", AxisType.Lat);
      } else {
        addCoordAxis(ncfile, "lat", gds.getInt(GridDefRecord.NY),
            gds.getDouble(GridDefRecord.LA1), dy, "degrees_north",
            "latitude coordinate", "latitude", AxisType.Lat);
      }
      addCoordAxis(ncfile, "lon", gds.getInt(GridDefRecord.NX),
          gds.getDouble(GridDefRecord.LO1), gds.getDouble(GridDefRecord.DX), "degrees_east",
          "longitude coordinate", "longitude", AxisType.Lon);
      addCoordSystemVariable(ncfile, "latLonCoordSys", "time lat lon");

    } else {
      if (makeProjection(ncfile)) {
        double[] yData, xData;
        if (lookup.getProjectionType(gds) == GridTableLookup.RotatedLatLon) {
          double dy = (gds.getDouble("La2") < gds.getDouble(GridDefRecord.LA1)
              ? -gds.getDouble(GridDefRecord.DY) : gds.getDouble(GridDefRecord.DY));

          yData = addCoordAxis(ncfile, "y", gds.getInt(GridDefRecord.NY),
              gds.getDouble(GridDefRecord.LA1), dy, "degrees",
              "y coordinate of projection", "projection_y_coordinate", AxisType.GeoY);
          xData = addCoordAxis(ncfile, "x", gds.getInt(GridDefRecord.NX),
              gds.getDouble(GridDefRecord.LO1), gds.getDouble(GridDefRecord.DX), "degrees",
              "x coordinate of projection", "projection_x_coordinate", AxisType.GeoX);

        } else if (lookup.getProjectionType(gds) == GridTableLookup.Orthographic) {

          yData = addCoordAxis(ncfile, "y", gds.getInt(GridDefRecord.NY), starty, incry, "km",  // fake km - really pixel
              "y coordinate of projection", "projection_y_coordinate", AxisType.GeoY);     // dunno what the 3 is
          xData = addCoordAxis(ncfile, "x", gds.getInt(GridDefRecord.NX), startx, incrx, "km",
              "x coordinate of projection", "projection_x_coordinate", AxisType.GeoX);
        } else if (lookup.getProjectionType(gds) ==   GridTableLookup.Curvilinear ) {
          yData = null;
          xData = null;
          curvilinearAxis( ncfile );
        } else {
          yData = addCoordAxis(ncfile, "y", gds.getInt(GridDefRecord.NY),
              starty, getDyInKm(), "km", "y coordinate of projection",
              "projection_y_coordinate", AxisType.GeoY);
          xData = addCoordAxis(ncfile, "x", gds.getInt(GridDefRecord.NX),
              startx, getDxInKm(), "km", "x coordinate of projection",
              "projection_x_coordinate", AxisType.GeoX);
        }
        if (GridServiceProvider.addLatLon)
          addLatLon2D(ncfile, xData, yData);
      }
    }
  }

  // release memory after init
  void empty() {
    gds = null;
    varHash = null;
    productHash = null;
    //vcsHash= null;
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
                                      String units, String desc, String standard_name, AxisType axis) {

    double np = gds.getDouble(GridDefRecord.NUMBERPARALLELS);
    if (Double.isNaN(np))
      np = gds.getDouble("Np");
    if (Double.isNaN(np)) {
      throw new IllegalArgumentException(
          "Gaussian LAt/Lon grid must have NumberParallels parameter");
    }
    double startLat = gds.getDouble(GridDefRecord.LA1);
    double endLat = gds.getDouble(GridDefRecord.LA2);

    int nlats = (int) (2 * np);
    GaussianLatitudes gaussLats = new GaussianLatitudes(nlats);

    int bestStartIndex = 0, bestEndIndex = 0;
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
    assert Math.abs(bestEndIndex - bestStartIndex + 1) == gds.getInt(GridDefRecord.NY);
    boolean goesUp = bestEndIndex > bestStartIndex;

    Variable v = new Variable(ncfile, g, null, name);
    v.setDataType(DataType.DOUBLE);
    v.setDimensions(name);

    // create the data
    int n = (int) gds.getInt(GridDefRecord.NY);
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
    Array dataArray = Array.factory(DataType.DOUBLE, new int[]{n}, data);
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

  private void addLatLon2D(NetcdfFile ncfile, double[] xData, double[] yData) {

    Variable latVar = new Variable(ncfile, g, null, "lat");
    latVar.setDataType(DataType.DOUBLE);
    latVar.setDimensions("y x");
    latVar.addAttribute(new Attribute("units", "degrees_north"));
    latVar.addAttribute(new Attribute("long_name", "latitude coordinate"));
    latVar.addAttribute(new Attribute("standard_name", "latitude"));
    latVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

    Variable lonVar = new Variable(ncfile, g, null, "lon");
    lonVar.setDataType(DataType.DOUBLE);
    lonVar.setDimensions("y x");
    lonVar.addAttribute(new Attribute("units", "degrees_east"));
    lonVar.addAttribute(new Attribute("long_name", "longitude coordinate"));
    lonVar.addAttribute(new Attribute("standard_name", "longitude"));
    lonVar.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

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
    Array latDataArray = Array.factory(DataType.DOUBLE, new int[]{ny, nx}, latData);
    latVar.setCachedData(latDataArray, false);

    Array lonDataArray = Array.factory(DataType.DOUBLE, new int[]{ny, nx}, lonData);
    lonVar.setCachedData(lonDataArray, false);

    ncfile.addVariable(g, latVar);
    ncfile.addVariable(g, lonVar);
  }

  /**
   * Make a projection and add it to the netCDF file
   *
   * @param ncfile netCDF file
   * @return true if projection was added and coordinates need to be added, false means do nothing
   */
  private boolean makeProjection(NetcdfFile ncfile) {
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
        //makeSpaceViewOrOthographic();
        makeMSGgeostationary();
        break;

      case GridTableLookup.Curvilinear:
        break;

      default:
        throw new UnsupportedOperationException("unknown projection = "
            + gds.getInt(GridDefRecord.GRID_TYPE));
    }

    Variable v = new Variable(ncfile, g, null, grid_name);
    v.setDataType(DataType.CHAR);
    v.setDimensions(""); // scalar
    char[] data = new char[]{'d'};
    Array dataArray = Array.factory(DataType.CHAR, new int[0], data);
    v.setCachedData(dataArray, false);

    for (Attribute att : attributes) {
      v.addAttribute(att);
    }

    // add CF Conventions attributes
    v.addAttribute(new Attribute(GridCF.EARTH_SHAPE, shape_name));

    // spherical earth
    double radius_spherical_earth = gds.getDouble(GridDefRecord.RADIUS_SPHERICAL_EARTH);
    // have to check both because Grib1 and Grib2 used different names
    if (Double.isNaN(radius_spherical_earth))
      radius_spherical_earth = gds.getDouble("radius_spherical_earth");

    if( ! Double.isNaN(radius_spherical_earth) ) {
      // TODO: delete if no bug reports
      //v.addAttribute(new Attribute("spherical_earth_radius_meters",
      //  new Double(radius_spherical_earth)));
      v.addAttribute(new Attribute(GridCF.EARTH_RADIUS, new Double(radius_spherical_earth)));
    } else { // oblate earth
      double major_axis = gds.getDouble( GridDefRecord.MAJOR_AXIS_EARTH );
      if (Double.isNaN( major_axis ))
        major_axis = gds.getDouble("major_axis_earth");
      double minor_axis = gds.getDouble( GridDefRecord.MINOR_AXIS_EARTH );
      if (Double.isNaN(minor_axis))
        minor_axis = gds.getDouble("minor_axis_earth");
      if ( ! Double.isNaN ( major_axis ) && ! Double.isNaN ( minor_axis )) {
        v.addAttribute(new Attribute(GridCF.SEMI_MAJOR_AXIS, new Double( major_axis )));
        v.addAttribute(new Attribute(GridCF.SEMI_MINOR_AXIS, new Double( minor_axis )));
      }
    }
    addGDSparams(v);
    ncfile.addVariable(g, v);
    return true;
  }

  /**
   * Add the GDS params to the variable as attributes
   *
   * @param v the GDS params.
   */
  private void addGDSparams(Variable v) {
    // add all the gds parameters
    List<String> keyList = new ArrayList<String>(gds.getKeys());
    Collections.sort(keyList);
    String pre =
        ((lookup instanceof Grib2GridTableLookup) ||
            (lookup instanceof Grib1GridTableLookup)) ? "GRIB" : "GDS";

    for (String key : keyList) {
      String name =
          AbstractIOServiceProvider.createValidNetcdfObjectName(pre + "_param_" + key);

      String vals = gds.getParam(key);
      try {
        int vali = Integer.parseInt(vals);
        if (key.equals(GridDefRecord.VECTOR_COMPONENT_FLAG)) {
          String cf;
          if (vali == 0) {
            cf = Grib2Tables.VectorComponentFlag.easterlyNortherlyRelative.toString();
          } else {
            cf = Grib2Tables.VectorComponentFlag.gridRelative.toString();
          }
          v.addAttribute(new Attribute(name, cf));
        } else {
          v.addAttribute(new Attribute(name, new Integer(vali)));
        }
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
  private void addCoordSystemVariable(NetcdfFile ncfile, String name, String dims) {
    Variable v = new Variable(ncfile, g, null, name);
    v.setDataType(DataType.CHAR);
    v.setDimensions(""); // scalar
    Array dataArray = Array.factory(DataType.CHAR, new int[0], new char[]{'0'});
    v.setCachedData(dataArray, false);
    v.addAttribute(new Attribute(_Coordinate.Axes, dims));
    if (isLatLon())
      v.addAttribute(new Attribute(_Coordinate.Transforms, "")); // to make sure its identified as a Coordinate System Variable
    else
      v.addAttribute(new Attribute(_Coordinate.Transforms, getGridName()));
    addGDSparams(v);
    ncfile.addVariable(g, v);
  }

  /**
   * Make a LambertConformalConic projection
   */
  private void makeLC() {
    // we have to project in order to find the origin
    proj = new LambertConformal(
        gds.getDouble(GridDefRecord.LATIN1), gds.getDouble(GridDefRecord.LOV),
        gds.getDouble(GridDefRecord.LATIN1), gds.getDouble(GridDefRecord.LATIN2));
    LatLonPointImpl startLL =
        new LatLonPointImpl(gds.getDouble(GridDefRecord.LA1), gds.getDouble(GridDefRecord.LO1));
    ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(startLL);
    startx = start.getX();
    starty = start.getY();

    if (Double.isNaN(getDxInKm())) {
      setDxDy(startx, starty, proj);
    }

    if (GridServiceProvider.debugProj) {
      System.out.println("GridHorizCoordSys.makeLC start at latlon " + startLL);

      double Lo2 = gds.getDouble(GridDefRecord.LO2);
      double La2 = gds.getDouble(GridDefRecord.LA2);
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GridHorizCoordSys.makeLC end at latlon " + endLL);

      ProjectionPointImpl endPP =
          (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + getNx() * getDxInKm();
      double endy = starty + getNy() * getDyInKm();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }

    attributes.add(new Attribute(GridCF.GRID_MAPPING_NAME, "lambert_conformal_conic"));
    if (gds.getDouble(GridDefRecord.LATIN1) == gds.getDouble(GridDefRecord.LATIN2)) {
      attributes.add(new Attribute(GridCF.STANDARD_PARALLEL,
          new Double(gds.getDouble(GridDefRecord.LATIN1))));
    } else {
      double[] data = new double[]{gds.getDouble(GridDefRecord.LATIN1),
          gds.getDouble(GridDefRecord.LATIN2)};
      attributes.add(new Attribute(GridCF.STANDARD_PARALLEL,
          Array.factory(DataType.DOUBLE, new int[]{2}, data)));
    }
    //attributes.add(new Attribute("longitude_of_central_meridian",
    attributes.add(new Attribute(GridCF.LONGITUDE_OF_CENTRAL_MERIDIAN,
        new Double(gds.getDouble(GridDefRecord.LOV))));
    //attributes.add(new Attribute("latitude_of_projection_origin",
    attributes.add(new Attribute(GridCF.LATITUDE_OF_PROJECTION_ORIGIN,
        new Double(gds.getDouble(GridDefRecord.LATIN1))));
  }

  /**
   * Make a PolarStereographic projection
   */
  private void makePS() {
    double scale = .933;

    String nproj = gds.getParam(GridDefRecord.NPPROJ);
    double latOrigin = (nproj == null || nproj.equalsIgnoreCase("true"))
        ? 90.0 : -90.0;

    // Why the scale factor?. accordining to GRIB docs:
    // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
    // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60))  [Snyder,Working Manual p157]
    // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
    proj = new Stereographic(latOrigin, gds.getDouble(GridDefRecord.LOV), scale);

    // we have to project in order to find the origin
    ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(
        new LatLonPointImpl(
            gds.getDouble(GridDefRecord.LA1), gds.getDouble(GridDefRecord.LO1)));
    startx = start.getX();
    starty = start.getY();

    if (Double.isNaN(getDxInKm())) {
      setDxDy(startx, starty, proj);
    }

    if (GridServiceProvider.debugProj) {
      System.out.println("start at proj coord " + start);
      LatLonPoint llpt = proj.projToLatLon(start);
      System.out.println("   end at lat/lon coord " + llpt);
      System.out.println("   should be lat=" + gds.getDouble(GridDefRecord.LA1)
          + " lon=" + gds.getDouble(GridDefRecord.LO1));
    }

    attributes.add(new Attribute(GridCF.GRID_MAPPING_NAME, "polar_stereographic"));
    //attributes.add(new Attribute("longitude_of_projection_origin",
    attributes.add(new Attribute(GridCF.LONGITUDE_OF_PROJECTION_ORIGIN,
        new Double(gds.getDouble(GridDefRecord.LOV))));
    //attributes.add(new Attribute("straight_vertical_longitude_from_pole",
    attributes.add(new Attribute( GridCF.STRAIGHT_VERTICAL_LONGITUDE_FROM_POLE,
        new Double(gds.getDouble(GridDefRecord.LOV))));
    //attributes.add(new Attribute("scale_factor_at_projection_origin",
    attributes.add(new Attribute(GridCF.SCALE_FACTOR_AT_PROJECTION_ORIGIN,
        new Double(scale)));
    attributes.add(new Attribute(GridCF.LATITUDE_OF_PROJECTION_ORIGIN,
        new Double(latOrigin)));
  }

  /**
   * Make a Mercator projection
   */
  private void makeMercator() {
    /**
     * Construct a Mercator Projection.
     * @param lon0 longitude of origin (degrees)
     * @param par standard parallel (degrees). cylinder cuts earth at this latitude.
     */
    double Latin = gds.getDouble(GridDefRecord.LAD);
    // name depends on Grib version 1 or 2
    if (Double.isNaN(Latin))
      Latin = gds.getDouble(GridDefRecord.LATIN);
    double Lo1 = gds.getDouble(GridDefRecord.LO1); //gds.Lo1;
    double La1 = gds.getDouble(GridDefRecord.LA1); //gds.La1;

    // put longitude origin at first point - doesnt actually matter
    proj = new Mercator(Lo1, Latin);

    // find out where
    ProjectionPoint startP = proj.latLonToProj(
        new LatLonPointImpl(La1, Lo1));
    startx = startP.getX();
    starty = startP.getY();

    if (Double.isNaN(getDxInKm())) {
      setDxDy(startx, starty, proj);
    }

    attributes.add(new Attribute(GridCF.GRID_MAPPING_NAME, "mercator"));
    attributes.add(new Attribute(GridCF.STANDARD_PARALLEL, Latin));
    attributes.add(new Attribute(GridCF.LONGITUDE_OF_PROJECTION_ORIGIN, Lo1));

    if (GridServiceProvider.debugProj) {
      double Lo2 = gds.getDouble(GridDefRecord.LO2);
      if (Lo2 < Lo1) Lo2 += 360;
      double La2 = gds.getDouble(GridDefRecord.LA2);
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GridHorizCoordSys.makeMercator: end at latlon= " +
          endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   start at proj coord " +
          new ProjectionPointImpl(startx, starty));
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + (getNx() - 1) * getDxInKm();
      double endy = starty + (getNy() - 1) * getDyInKm();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }
  }

  // RotatedLatLon
  private void makeRotatedLatLon(NetcdfFile ncfile) {

    double splat = gds.getDouble(GridDefRecord.SPLAT);
    double splon = gds.getDouble(GridDefRecord.SPLON);
    double spangle = gds.getDouble(GridDefRecord.ROTATIONANGLE);

    // Given projection coordinates, need LatLon coordinates
    proj = new RotatedLatLon(splat, splon, spangle);
    LatLonPoint startLL = proj.projToLatLon(
        new ProjectionPointImpl(
            gds.getDouble(GridDefRecord.LO1), gds.getDouble(GridDefRecord.LA1)));
    startx = startLL.getLongitude();
    starty = startLL.getLatitude();
    addCoordSystemVariable(ncfile, "latLonCoordSys", "time y x");

    // splat, splon, spangle
    attributes.add(new Attribute(GridCF.GRID_MAPPING_NAME, "rotated_latlon_grib"));
    attributes.add(new Attribute("grid_south_pole_latitude", new Double(splat)));
    attributes.add(new Attribute("grid_south_pole_longitude", new Double(splon)));
    attributes.add(new Attribute("grid_south_pole_angle", new Double(spangle)));

    if (GridServiceProvider.debugProj) {
      System.out.println("Location of pole of rotated grid:");
      System.out.println("Lon=" + splon + ", Lat=" + splat);
      System.out.println("Axial rotation about pole of rotated grid:" + spangle);

      System.out.println("Location of LL in rotated grid:");
      System.out.println("Lon=" + gds.getDouble(GridDefRecord.LO1) + ", " +
          "Lat=" + gds.getDouble(GridDefRecord.LA1));
      System.out.println("Location of LL in non-rotated grid:");
      System.out.println("Lon=" + startx + ", Lat=" + starty);

      double Lo2 = gds.getDouble(GridDefRecord.LO2);
      double La2 = gds.getDouble(GridDefRecord.LA2);
      System.out.println("Location of UR in rotated grid:");
      System.out.println("Lon=" + Lo2 + ", Lat=" + La2);
      System.out.println("Location of UR in non-rotated grid:");
      LatLonPoint endUR = proj.projToLatLon(new ProjectionPointImpl(Lo2, La2));
      System.out.println("Lon=" + endUR.getLongitude() + ", Lat=" + endUR.getLatitude());

      double dy = (La2 < gds.getDouble(GridDefRecord.LA1)) ?
          -gds.getDouble(GridDefRecord.DY) : gds.getDouble(GridDefRecord.DY);
      double endx = gds.getDouble(GridDefRecord.LO1) + (getNx() - 1) * gds.getDouble(GridDefRecord.DX);
      double endy = gds.getDouble(GridDefRecord.LA1) + (getNy() - 1) * dy;

      System.out.println("End point rotated grid should be x=" +
          endx + " y=" + endy);
    }
  }

  /**
   * Make a Space View Orthographic projection
   */
  private void makeSpaceViewOrOthographic() {
    double Lat0 = gds.getDouble(GridDefRecord.LAP);  // sub-satellite point lat
    double Lon0 = gds.getDouble(GridDefRecord.LOP);  // sub-satellite point lon

    double xp = gds.getDouble(GridDefRecord.XP);  // sub-satellite point in grid lengths
    double yp = gds.getDouble(GridDefRecord.YP);

    double dx = gds.getDouble(GridDefRecord.DX);  // apparent diameter in units of grid lengths
    double dy = gds.getDouble(GridDefRecord.DY);
    // have to check both names because Grib1 and Grib2 used different names
    double major_axis = gds.getDouble(GridDefRecord.MAJOR_AXIS_EARTH);  // km
    if (Double.isNaN(major_axis) )
       major_axis = gds.getDouble("major_axis_earth");

    double minor_axis = gds.getDouble(GridDefRecord.MINOR_AXIS_EARTH);  // km
    if (Double.isNaN(minor_axis) )
       minor_axis = gds.getDouble("minor_axis_earth");
    // Nr = altitude of camera from center, in units of radius
    double nr = gds.getDouble(GridDefRecord.NR) * 1e-6;
    double apparentDiameter = 2 * Math.sqrt((nr - 1) / (nr + 1));  // apparent diameter, units of radius (see Snyder p 173)

    // app diameter kmeters / app diameter grid lengths = m per grid length
    double gridLengthX = major_axis * apparentDiameter / dx;
    double gridLengthY = minor_axis * apparentDiameter / dy;
    // have to add to both for consistency
    gds.addParam(GridDefRecord.DX, String.valueOf(1000 * gridLengthX));  // meters
    gds.addParam(GridDefRecord.DX, new Double(1000 * gridLengthX));
    gds.addParam(GridDefRecord.DY, String.valueOf(1000 * gridLengthY));  // meters
    gds.addParam(GridDefRecord.DY, new Double(1000 * gridLengthY));
    startx = -gridLengthX * xp;  // km
    starty = -gridLengthY * yp;

    double radius = Earth.getRadius() / 1000.0;  // km

    if (nr == 1111111111.0) {  // LOOK: not sure how all ones will appear as a double, need example
      proj = new Orthographic(Lat0, Lon0, radius);

      attributes.add(new Attribute(GridCF.GRID_MAPPING_NAME, "orthographic"));
      attributes.add(new Attribute(GridCF.LONGITUDE_OF_PROJECTION_ORIGIN,
          new Double(Lon0)));
      attributes.add(new Attribute(GridCF.LATITUDE_OF_PROJECTION_ORIGIN,
          new Double(Lat0)));

    } else {  // "space view perspective"

      double height = (nr - 1.0) * radius;  // height = the height of the observing camera in km
      proj = new VerticalPerspectiveView(Lat0, Lon0, radius, height);

      attributes.add(new Attribute(GridCF.GRID_MAPPING_NAME,
          "vertical_perspective"));
      attributes.add(new Attribute(GridCF.LONGITUDE_OF_PROJECTION_ORIGIN,
          new Double(Lon0)));
      attributes.add(new Attribute(GridCF.LATITUDE_OF_PROJECTION_ORIGIN,
          new Double(Lat0)));
      attributes.add(new Attribute("height_above_earth",
          new Double(height)));
    }

    if (GridServiceProvider.debugProj) {

      double Lo2 = gds.getDouble(GridDefRecord.LO2) + 360.0;
      double La2 = gds.getDouble(GridDefRecord.LA2);
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println(
          "GridHorizCoordSys.makeOrthographic end at latlon "+ endLL);

      ProjectionPointImpl endPP =
          (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + getNx() * getDxInKm();
      double endy = starty + getNy() * getDyInKm();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }
  }

  /**
   * Make a Eumetsat MSG "Normalized Geostationary Projection" projection.
   * Fake coordinates for now, then see if this can be generalized.
   *
   * from  FM 92 GRIB-2 doc:
   *
   * Grid Definition Template 3.90: Space view perspective or orthographic
    Octet Number(s) Contents
    15 Shape of the earth (see Code Table 3.2)
    16 Scale factor of radius of spherical earth
    17-20 Scaled value of radius of spherical earth
    21 Scale factor of major axis of oblate spheroid earth
    22-25 Scaled value of major axis of oblate spheroid earth
    26 Scale factor of minor axis of oblate spheroid earth
    27-30 Scaled value of minor axis of oblate spheroid earth
    31-34 Nx - number of points along X-axis (columns)
    35-38 Ny - number of points along Y-axis (rows or lines)
    39-42 Lap - latitude of sub-satellite point
    43-46 Lop - longitude of sub-satellite point
    47 Resolution and component flags (see Flag Table 3.3)
    48-51 dx - apparent diameter of Earth in grid lengths, in X-direction
    52-55 dy - apparent diameter of Earth in grid lengths, in Y-direction
    56-59 Xp - X-coordinate of sub-satellite point (in units of 10-3 grid length expressed as an integer)
    60-63 Yp - Y-coordinate of sub-satellite point (in units of 10-3 grid length expressed as an integer)
    64 Scanning mode (flags - see Flag Table 3.4)
    65-68 Orientation of the grid; i.e., the angle between the increasing Y-axis and the meridian of the sub-satellite point in the direction of increasing latitude (see Note 3)
    69-72 Nr - altitude of the camera from the Earths centre, measured in units of the Earth (equatorial) radius multiplied by a scale factor of 10 6 (see Notes 4 and 5)
    73-76 Xo - X-coordinate of origin of sector image
    77-80 Yo - Y-coordinate of origin of sector image

    Notes:
    (1) It is assumed that the satellite is at its nominal position, i.e., it is looking directly at its sub-satellite point.
    (2) Octets 69-72 shall be set to all ones (missing) to indicate the orthographic view (from infinite distance)
    (3) It is the angle between the increasing Y-axis and the meridian 180E if the sub-satellite point is the North Pole; or the meridian 0 if the sub-satellite point is the South Pole.
    (4) The apparent angular size of the Earth will be given by 2 * Arcsin (10^6 )/Nr).
    (5) For orthographic view from infinite distance, the value of Nr should be encoded as missing (all bits set to 1).
    (6) The horizontal and vertical angular resolutions of the sensor (Rx and Ry), needed for navigation equation, can be calculated from the following:
         Rx = 2 * Arcsin (106 )/Nr)/ dx
         Ry = 2 * Arcsin (106 )/Nr)/ dy

   =======

   from  simon.elliott@eumetsat.int

   For products on a single pixel resolution grid, the scan angle is 83.84333 E-6 rad.
   So dx = 2 * arcsin(10e6/Nr) / 83.84333 E-6 = 3622.30, which encoded to the nearest integer is 3622.
   This is correctly encoded in our products.

   For products on a 3x3 pixel resolution grid, the scan angle is 3 * 83.84333 E-6 rad = 251.52999 E-6 rad.
   So dx = 2 * arcsin(10e6/Nr) / 251.52999 E-6 = 1207.43, which encoded to the nearest integer is 1207.
   This is correctly encoded in our products.

   Due to the elliptical shape of the earth, the calculation is a bit different in the y direction (Nr is in multiples of
   the equatorial radius, but the tangent point is much closer to the polar radius from the earth's centre.
   Approximating that the tangent point is actually at the polar radius from the earth's centre:
     The sine of the angle subtended by the Earths centre and the tangent point on the equator as seen from the spacecraft
     = Rp / (( Nr * Re) / 10^6) = (Rp * 10^6) / (Re * Nr)

    The angle subtended by the Earth equator as seen by the spacecraft is, by symmetry twice the inverse sine above,
      = 2 * arcsine ((Rp * 10^6) / (Re * Nr))

  For products on a single pixel resolution grid, the scan angle is 83.84333 E-6 rad.
   So dy = 2 * arcsine ((Rp * 10^6) / (Re * Nr)) / 83.84333 E-6 = 3610.06, which encoded to the nearest integer is 3610.
   This is currently encoded in our products as 3568.

  For products on a 3x3 pixel resolution grid, the scan angle is 3 * 83.84333 E-6 rad = 251.52999 E-6 rad.
   So dy = 2 * arcsine ((Rp * 10^6) / (Re * Nr)) / 251.52999 E-6 = 1203.35, which encoded to the nearest integer is 1203.
   This is currently encoded in our products as 1189.

   As you can see the dx and dy values we are using will lead to an error of around 1% in the y direction.
   I will ensure that the values are corrected to those explained here (3610 and 1203) as soon as possible.
   */
  private void makeMSGgeostationary() {
    double Lat0 = gds.getDouble(GridDefRecord.LAP);  // sub-satellite point lat
    double Lon0 = gds.getDouble(GridDefRecord.LOP);  // sub-satellite point lon

    int nx = gds.getInt(GridDefRecord.NX);
    int ny = gds.getInt(GridDefRecord.NY);
    int x_off = gds.getInt(GridDefRecord.XP);  // sub-satellite point in grid lengths
    int y_off = gds.getInt(GridDefRecord.YP);

    double dx = gds.getDouble(GridDefRecord.DX);  // apparent diameter of earth in units of grid lengths
    double dy = gds.getDouble(GridDefRecord.DY);

    // per Simon Eliot 1/18/2010, there is a bug in Eumetsat grib files,
    // we need to "correct for ellipsoidal earth"
    // (Note we should check who the originating center is
    // "Originating_center" = "EUMETSAT Operation Centre" in the GRIB id (section 1))
    // although AFAIK, eumetsat is only one using this projection.
    if (dy < 2100) {
      dx = 1207;
      dy = 1203;
    } else {
      dx = 3622;
      dy = 3610;
    }

    // have to check both names because Grib1 and Grib2 used different names
    double major_axis = gds.getDouble(GridDefRecord.MAJOR_AXIS_EARTH);  // m
    if (Double.isNaN(major_axis))
      major_axis = gds.getDouble("major_axis_earth");

    double minor_axis = gds.getDouble(GridDefRecord.MINOR_AXIS_EARTH);  // m
    if (Double.isNaN(minor_axis))
      minor_axis = gds.getDouble("minor_axis_earth");
    // Nr = altitude of camera from center, in units of radius
    double nr = gds.getDouble(GridDefRecord.NR) * 1e-6; // altitude of the camera from the Earths centre, measured in units of the Earth (equatorial) radius

          // CFAC = 2^16 / {[2 * arcsine (10^6 / Nr)] / dx }
    double as = 2 * Math.asin(1.0/nr);
    double cfac = dx / as;
    double lfac = dy / as;

    // use km, so scale by the earth radius
    double scale_factor = (nr - 1) * major_axis / 1000; // this sets the units of the projection x,y coords in km
    double scale_x = scale_factor; // LOOK fake neg need scan value
    double scale_y = -scale_factor; // LOOK fake neg need scan value
    startx = scale_factor * (1 - x_off) / cfac;
    starty = scale_factor * (y_off - ny) / lfac;
    incrx = scale_factor/cfac;
    incry = scale_factor/lfac;

    attributes.add(new Attribute(GridCF.GRID_MAPPING_NAME, "MSGnavigation"));
    attributes.add(new Attribute(GridCF.LONGITUDE_OF_PROJECTION_ORIGIN, new Double(Lon0)));
    attributes.add(new Attribute(GridCF.LATITUDE_OF_PROJECTION_ORIGIN, new Double(Lat0)));
    //attributes.add(new Attribute("semi_major_axis", new Double(major_axis)));
    //attributes.add(new Attribute("semi_minor_axis", new Double(minor_axis)));
    attributes.add(new Attribute("height_from_earth_center", new Double(nr * major_axis)));
    attributes.add(new Attribute("scale_x", new Double(scale_x)));
    attributes.add(new Attribute("scale_y", new Double(scale_y)));

    proj = new MSGnavigation(Lat0, Lon0, major_axis, minor_axis, nr * major_axis, scale_x, scale_y);

    if (GridServiceProvider.debugProj) {

      double Lo2 = gds.getDouble(GridDefRecord.LO2) + 360.0;
      double La2 = gds.getDouble(GridDefRecord.LA2);
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GridHorizCoordSys.makeMSGgeostationary end at latlon " + endLL);

      ProjectionPointImpl endPP =(ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   end at proj coord " + endPP);

      double endx = 1 + getNx();
      double endy = 1 + getNy();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }
  }

  /**
   * CurvilinearAxis
   *
   * Make lat/lon axis from variables that start with Latitude/Longitude and then
   * add the coordinates to the variables. This code is based on the ofs_atl files
   * received from Rich Signell. This code is rigid because it expects coordinate names
   * to start with Latitude/Longitude and other coordinate of depth.
   * @param ncfile  NetcdfFile
   */
  private void curvilinearAxis( NetcdfFile ncfile ) {

    List<Variable> vars = ncfile.getRootGroup().getVariables();
    String latpp = null, lonpp = null, latU = null, lonU = null, latV = null, lonV = null;
    // has to be done twice because there's no guarantee that the
    // coordinate variables will be accessed first
    List<String> timeDimLL = new ArrayList<String>();
    List<String> timeDimV = new ArrayList<String>();
    for( Variable var : vars ) {
      if( var.getName().startsWith( "Latitude") ) {
        // remove time dependency
        int[] shape = var.getShape();
        if (var.getRank() == 3 && shape[0] == 1) { // remove time dependencies - MAJOR KLUDGE
              List<Dimension> dims = var.getDimensions();
              if( ! timeDimLL.contains( dims.get( 0 ).getName() ))
                timeDimLL.add( dims.get( 0 ).getName() );
              dims.remove(0);
              var.setDimensions(dims);
        }
        // add lat attributes
        var.addAttribute(new Attribute("units", "degrees_north"));
        var.addAttribute(new Attribute("long_name", "latitude coordinate"));
        var.addAttribute(new Attribute("standard_name", "latitude"));
        var.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
        if( var.getName().contains( "U_Wind_Component")) {
          latU = var.getName();
        } else if( var.getName().contains( "V_Wind_Component")) {
          latV = var.getName();
        } else {
          latpp = var.getName();
        }

      } else if( var.getName().startsWith( "Longitude" )) {
        // remove time dependency
        int[] shape = var.getShape();
        if (var.getRank() == 3 && shape[0] == 1) { // remove time dependencies - MAJOR KLUDGE
              List<Dimension> dims = var.getDimensions();
              if( ! timeDimLL.contains( dims.get( 0 ).getName() ))
                timeDimLL.add( dims.get( 0 ).getName() );
              dims.remove(0);
              var.setDimensions(dims);
        }
        // add lon attributes
        var.addAttribute(new Attribute("units", "degrees_east"));
        var.addAttribute(new Attribute("long_name", "longitude coordinate"));
        var.addAttribute(new Attribute("standard_name", "longitude"));
        var.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

        if( var.getName().contains( "U_Wind_Component")) {
          lonU = var.getName();
        } else if( var.getName().contains( "V_Wind_Component")) {
          lonV = var.getName();
        } else {
          lonpp = var.getName();
        }
      }
   }

   // add coordinates to variables
   for( Variable var : vars ) {
     List<Dimension> dims = var.getDimensions();
     if( var.getName().startsWith( "Latitude") || var.getName().startsWith( "Longitude" )) {

      // check for other coordinate variables
     } else if( var.getName().startsWith( "time") || var.getName().startsWith( "depth")) {
        // remove time dependency if it exists
        int[] shape = var.getShape();
        if (var.getRank() == 3 && shape[0] == 1) { // remove time dependencies - MAJOR KLUDGE
              dims.remove(0);
              var.setDimensions(dims);
        }
      } else if( var.getName().startsWith( "Curvilinear") ) {
        // nothing at this time
      } else if( var.getName().startsWith( "U-component") ) {
        var.addAttribute(new Attribute("coordinates", latU +" "+ lonU));
        if( ! timeDimV.contains( dims.get( 0 ).getName() ))
                timeDimV.add( dims.get( 0 ).getName() );
      } else if( var.getName().startsWith( "V-component") ) {
        var.addAttribute(new Attribute("coordinates", latV +" "+ lonV));
        if( ! timeDimV.contains( dims.get( 0 ).getName() ))
                timeDimV.add( dims.get( 0 ).getName() );

        // rest of variables default to Pressure_Point
      } else {
        var.addAttribute(new Attribute("coordinates", latpp +" "+ lonpp));
        if( ! timeDimV.contains( dims.get( 0 ).getName() ))
                timeDimV.add( dims.get( 0 ).getName() );
     }
    }
    // remove Latitude/Longitude time dimension and variable if possible
    for( String tdLL : timeDimLL) {
      if( timeDimV.contains( tdLL ))
        continue;
      // else only used with Lat/Lon
      ncfile.getRootGroup().removeDimension( tdLL );
      ncfile.getRootGroup().removeVariable( tdLL );
    }

  }
   /**
   * Calculate the dx and dy from startx, starty and projection.
   *
   * @param startx starting x projection point
   * @param starty starting y projection point
   * @param proj   projection for transform
   */
  private void setDxDy(double startx, double starty, ProjectionImpl proj) {
    double Lo2 = gds.getDouble(GridDefRecord.LO2);
    double La2 = gds.getDouble(GridDefRecord.LA2);
    if (Double.isNaN(Lo2) || Double.isNaN(La2)) {
      return;
    }
    LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
    ProjectionPointImpl end =
        (ProjectionPointImpl) proj.latLonToProj(endLL);
    double dx = Math.abs(end.getX() - startx)
        / (gds.getInt(GridDefRecord.NX) - 1);
    double dy = Math.abs(end.getY() - starty)
        / (gds.getInt(GridDefRecord.NY) - 1);
    gds.addParam(GridDefRecord.DX, String.valueOf(dx));
    gds.addParam(GridDefRecord.DY, String.valueOf(dy));
    gds.addParam(GridDefRecord.GRID_UNITS, "km");
  }

  /**
   * Calculate  Dx Dy Lat Lon Grid
   * Note: this assumes lo1 < lo2 and dx is positive going east
   * @return dy double
   */
  private double setLatLonDxDy() {
    double lo1 = gds.getDouble(GridDefRecord.LO1);
    double la1 = gds.getDouble(GridDefRecord.LA1);
    double lo2 = gds.getDouble(GridDefRecord.LO2);
    double la2 = gds.getDouble(GridDefRecord.LA2);
    if (Double.isNaN(lo2) || Double.isNaN(la2)) {
      return Double.NaN;
    }
    if (lo2 < lo1) lo2 += 360;
    double dx = Math.abs(lo2 - lo1)
        / (gds.getInt(GridDefRecord.NX) - 1);
    double dy = Math.abs(la2 - la1)
        / (gds.getInt(GridDefRecord.NY) - 1);
    gds.addParam(GridDefRecord.DX, String.valueOf(dx));
    gds.addParam(GridDefRecord.DY, String.valueOf(dy));
    // in case someone checked on these before, we need to override
    gds.addParam(GridDefRecord.DX, new Double(dx));
    gds.addParam(GridDefRecord.DY, new Double(dy));
    gds.addParam(GridDefRecord.GRID_UNITS, "degree");

    return dy;
  }

  /**
   * returns the gds for this hcs
   * @return gds GridDefRecord
   */
  public GridDefRecord getGds() {
    return gds;
  }
}
