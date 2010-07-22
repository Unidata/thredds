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
package ucar.nc2.iosp.grib;

import ucar.grib.*;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.GaussianLatitudes;

import java.util.*;

/**
 * A horizontal coordinate system created from a Grib2GridDefinitionSection.
 * <p/>
 * <p/>
 * <p> Note on "false_easting" and "fale_northing" projection parameters:
 * <ul><li>false_easting(northing) = The value added to all x (y) values in the rectangular coordinates for a map projection.
 * This value frequently is assigned to eliminate negative numbers.
 * Expressed in the unit of measure identified in Planar Coordinate Units.
 * <li>We dont currently use, assuming that the x and y are just fine as negetive numbers.
 * @deprecated
 * @author caron
 */
public class GribHorizCoordSys {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribHorizCoordSys.class);

  private TableLookup lookup;
  private Index.GdsRecord gdsIndex; // becomes null after init
  private Group g;
  int nx, ny;
  double dx, dy;

  private String grid_name, shape_name, id;
  private boolean isLatLon = true, isGaussian = false;
  Map<String,GribVariable> varHash = new HashMap<String,GribVariable>(200); // GribVariables that have this GribHorizCoordSys
  Map<String, List<GribVariable>> productHash = new HashMap<String, List<GribVariable>>(100); // List of GribVariable, sorted by product desc
  HashMap vcsHash = new HashMap(30); // GribVertCoordSys

  private double startx, starty;
  private ProjectionImpl proj;
  private List<Attribute> attributes = new ArrayList<Attribute>();

  GribHorizCoordSys(Index.GdsRecord gdsIndex, TableLookup lookup, Group g) {
    this.gdsIndex = gdsIndex;
    this.lookup = lookup;
    this.g = g;

    this.nx = gdsIndex.nx;
    this.ny = gdsIndex.ny;
    this.dx = gdsIndex.dx * .001;
    this.dy = gdsIndex.dy * .001;

    this.grid_name = lookup.getGridName(gdsIndex);
    grid_name = StringUtil.replace(grid_name, ' ', "_");
    this.shape_name = lookup.getShapeName(gdsIndex);
    this.g = g;
    isLatLon = lookup.isLatLon(gdsIndex);
    id = (g == null) ? grid_name : g.getName();

    if (isLatLon && (lookup.getProjectionType(gdsIndex) == TableLookup.GaussianLatLon)) {
      isGaussian = true;
      double np = 90.0;
      String nps = (String) gdsIndex.params.get("Np"); // # lats between pole and equator  (octet 26/27)
      if (null != nps) {
        np = Double.parseDouble(nps);
      }
      gdsIndex.dy = np; // fake - need to get actual gaussian calculation here

      // hack-a-whack : who is this for ???
      // gdsIndex.dy = 2 * gdsIndex.La1 / gdsIndex.ny;
      //gdsIndex.nx = 800;
      //gdsIndex.dx = 360.0/gdsIndex.nx;
    }
  }

  String getID() {
    return id;
  } // unique within the file

  String getGridName() {
    return grid_name;
  } // used in CF-1 attributes

  Group getGroup() {
    return g;
  }

  boolean isLatLon() {
    return isLatLon;
  }

  int getNx() {
    return nx;
  }

  int getNy() {
    return ny;
  }

  private double getDxInKm() {
    return dx;
  }

  private double getDyInKm() {
    return dy;
  }

  void addDimensionsToNetcdfFile(NetcdfFile ncfile) {
    if (isLatLon) {
      ncfile.addDimension(g, new Dimension("lat", ny));
      ncfile.addDimension(g, new Dimension("lon", nx));
    } else {
      ncfile.addDimension(g, new Dimension("y", ny));
      ncfile.addDimension(g, new Dimension("x", nx));
    }
  }

  void addToNetcdfFile(NetcdfFile ncfile) {
     if (isLatLon ) {
      double dy = (gdsIndex.readDouble("La2") < gdsIndex.La1) ? -gdsIndex.dy : gdsIndex.dy;
      if (isGaussian)
        addGaussianLatAxis(ncfile, "lat", "degrees_north", "latitude coordinate", "latitude", AxisType.Lat);
      else
        addCoordAxis(ncfile, "lat", gdsIndex.ny, gdsIndex.La1, dy, "degrees_north", "latitude coordinate", "latitude", AxisType.Lat);

      addCoordAxis(ncfile, "lon", gdsIndex.nx, gdsIndex.Lo1, gdsIndex.dx, "degrees_east", "longitude coordinate", "longitude", AxisType.Lon);
      addCoordSystemVariable(ncfile, "latLonCoordSys", "time lat lon");

    } else {
      boolean hasProj = makeProjection(ncfile);
      if (hasProj) {
        double[] yData, xData;
        if ( lookup.getProjectionType(gdsIndex) == TableLookup.RotatedLatLon) {
          double dy = (gdsIndex.readDouble("La2") < gdsIndex.La1) ? -gdsIndex.dy : gdsIndex.dy;
          yData = addCoordAxis(ncfile, "y", gdsIndex.ny, gdsIndex.La1, dy, "degrees",
                  "y coordinate of projection", "projection_y_coordinate", AxisType.GeoY);
          xData = addCoordAxis(ncfile, "x", gdsIndex.nx, gdsIndex.Lo1, gdsIndex.dx, "degrees",
                 "x coordinate of projection", "projection_x_coordinate", AxisType.GeoX);
        } else {
           yData = addCoordAxis(ncfile, "y", gdsIndex.ny, starty, getDyInKm(), "km",
                  "y coordinate of projection", "projection_y_coordinate", AxisType.GeoY);
           xData = addCoordAxis(ncfile, "x", gdsIndex.nx, startx, getDxInKm(), "km",
                  "x coordinate of projection", "projection_x_coordinate", AxisType.GeoX);
        }
        if (GribServiceProvider.addLatLon) addLatLon2D(ncfile, xData, yData);
      } else {
        log.warn("Unknown grid type= "+gdsIndex.grid_type+"; no projection found");
      }
    }
  }

  // release memory after init
  void empty() {
    gdsIndex = null;
    varHash = null;
    productHash = null;
    vcsHash= null;
  }

  private double[] addCoordAxis(NetcdfFile ncfile, String name, int n, double start, double incr, String units,
          String desc, String standard_name, AxisType axis) {

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

  private double[] addGaussianLatAxis(NetcdfFile ncfile, String name, String units,
          String desc, String standard_name, AxisType axis) {

    double np = gdsIndex.readDouble("NumberParallels");
    if (Double.isNaN(np)) 
      np = gdsIndex.readDouble("Np");
    if (Double.isNaN(np))
      throw new IllegalArgumentException("Gaussian Lat/Lon grid must have NumberParallels parameter");
    double startLat = gdsIndex.La1;
    double endLat = gdsIndex.readDouble("La2");

    int nlats = (int) (2 * np);
    GaussianLatitudes gaussLats = new GaussianLatitudes(nlats);

    int bestStartIndex = 0, bestEndIndex = 0;
    double bestStartDiff = Double.MAX_VALUE;
    double bestEndDiff = Double.MAX_VALUE;
    for (int i=0; i<nlats; i++) {
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
    assert Math.abs(bestEndIndex - bestStartIndex + 1) == gdsIndex.ny;
    boolean goesUp = bestEndIndex > bestStartIndex;

    Variable v = new Variable(ncfile, g, null, name);
    v.setDataType(DataType.DOUBLE);
    v.setDimensions(name);

    // create the data
    int n = gdsIndex.ny;
    int useIndex = bestStartIndex;
    double[] data = new double[n];
    double[] gaussw = new double[n];
    for (int i = 0; i < n; i++) {
      data[i] = gaussLats.latd[useIndex];
      gaussw[i] = gaussLats.gaussw[useIndex];
      if (goesUp) useIndex++; else useIndex--;
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
    v.addAttribute(new Attribute("long_name", "gaussian weights (unnormalized)"));
    dataArray = Array.factory(DataType.DOUBLE, new int[]{n}, gaussw);
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

  private boolean makeProjection(NetcdfFile ncfile) {
    switch (lookup.getProjectionType(gdsIndex)) {
      case TableLookup.RotatedLatLon:
        makeRotatedLatLon( ncfile );
        break;
      case TableLookup.PolarStereographic:
        makePS();
        break;
      case TableLookup.LambertConformal:
        makeLC();
        break;
      case TableLookup.Mercator:
        makeMercator();
        break;
      case TableLookup.Orthographic:
        makeSpaceViewOrOthographic();
        break;
      default:
        return false;
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

    v.addAttribute(new Attribute("earth_shape", shape_name));
    //v.addAttribute(new Attribute("GRIB_earth_shape_code", new Integer(gdsIndex.grid_shape_code)));
    if (gdsIndex.grid_shape_code == 1) {
      v.addAttribute(new Attribute("spherical_earth_radius_meters", gdsIndex.radius_spherical_earth));
    }

    addGDSparams(v);
    ncfile.addVariable(g, v);

    return true;
  }

  private void addGDSparams(Variable v) {
    // add all the gds parameters
    java.util.Set<String> keys = gdsIndex.params.keySet();
    List<String> keyList = new ArrayList<String>(keys);
    Collections.sort(keyList);
    for (String key : keyList) {
      String name = "GRIB_param_" + key;

      String vals = (String) gdsIndex.params.get(key);
      try {
        int vali = Integer.parseInt(vals);
        v.addAttribute(new Attribute(name, vali));
      } catch (Exception e) {
        try {
          double vald = Double.parseDouble(vals);
          v.addAttribute(new Attribute(name, vald));
        } catch (Exception e2) {
          v.addAttribute(new Attribute(name, vals));
        }
      }
    }
  }

  private void addCoordSystemVariable(NetcdfFile ncfile, String name, String dims) {
    Variable v = new Variable(ncfile, g, null, name);
    v.setDataType(DataType.CHAR);
    v.setDimensions(""); // scalar
    Array dataArray = Array.factory(DataType.CHAR, new int[0], new char[]{'0'});
    v.setCachedData(dataArray, false);
    v.addAttribute(new Attribute(_Coordinate.Axes, dims));
    if (!isLatLon())
      v.addAttribute(new Attribute(_Coordinate.Transforms, getGridName()));

    addGDSparams(v);
    ncfile.addVariable(g, v);
  }

  // lambert conformal
  private void makeLC() {
    // we have to project in order to find the origin
    proj = new LambertConformal(gdsIndex.latin1, gdsIndex.LoV, gdsIndex.latin1, gdsIndex.latin2);
    LatLonPointImpl startLL = new LatLonPointImpl(gdsIndex.La1, gdsIndex.Lo1);
    ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(startLL);
    startx = start.getX();
    starty = start.getY();

    if (GribServiceProvider.debugProj) {
      System.out.println("GribHorizCoordSys.makeLC start at latlon " + startLL);

      double Lo2 = gdsIndex.readDouble("Lo2");
      double La2 = gdsIndex.readDouble("La2");
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GribHorizCoordSys.makeLC end at latlon " + endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + getNx() * getDxInKm();
      double endy = starty + getNy() * getDyInKm();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }

    attributes.add(new Attribute("grid_mapping_name", "lambert_conformal_conic"));
    if (gdsIndex.latin1 == gdsIndex.latin2)
      attributes.add(new Attribute("standard_parallel", gdsIndex.latin1));
    else {
      double[] data = new double[]{gdsIndex.latin1, gdsIndex.latin2};
      attributes.add(new Attribute("standard_parallel",
              Array.factory(DataType.DOUBLE, new int[]{2}, data)));
    }
    attributes.add(new Attribute("longitude_of_central_meridian", gdsIndex.LoV));
    attributes.add(new Attribute("latitude_of_projection_origin", gdsIndex.latin1));
    //attributes.add( new Attribute("false_easting", new Double(startx)));
    //attributes.add( new Attribute("false_northing", new Double(starty)));
  }

  // polar stereographic
  private void makePS() {
    double scale = .933;

    double latOrigin = 90.0;
    String s = (String) gdsIndex.params.get("NpProj");
    if (s != null && !s.equalsIgnoreCase("true"))
      latOrigin = -90.0;

    // Why the scale factor?. accordining to GRIB docs:
    // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
    // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60))  [Snyder,Working Manual p157]
    // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
    proj = new Stereographic(latOrigin, gdsIndex.LoV, scale);

    // we have to project in order to find the origin
    ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(new LatLonPointImpl(gdsIndex.La1, gdsIndex.Lo1));
    startx = start.getX();
    starty = start.getY();

    if (GribServiceProvider.debugProj) {
      System.out.println("start at proj coord " + start);
      LatLonPoint llpt = proj.projToLatLon(start);
      System.out.println("   end at lat/lon coord " + llpt);
      System.out.println("   should be lat=" + gdsIndex.La1 + " lon=" + gdsIndex.Lo1);
    }

    attributes.add(new Attribute("grid_mapping_name", "polar_stereographic"));
    attributes.add(new Attribute("longitude_of_projection_origin", gdsIndex.LoV));
    attributes.add(new Attribute("straight_vertical_longitude_from_pole", gdsIndex.LoV));
    attributes.add(new Attribute("scale_factor_at_projection_origin", scale));
    attributes.add(new Attribute("latitude_of_projection_origin", latOrigin));
  }

  // Mercator
  private void makeMercator() {
    /**
     * Construct a Mercator Projection.
     * @param lon0 longitude of origin (degrees)
     * @param par standard parallel (degrees). cylinder cuts earth at this latitude.
     */
    double Latin = gdsIndex.readDouble("LaD");
    // name depends on Grib version 1 or 2
    if (Double.isNaN(Latin) )
       Latin = gdsIndex.readDouble("Latin");
    double Lo1 = gdsIndex.Lo1;
    double La1 = gdsIndex.La1;

    // put longitude origin at first point - doesnt actually matter
    proj = new Mercator(Lo1, Latin);

    // find out where
    LatLonPointImpl startLL = new LatLonPointImpl(La1, Lo1);
    ProjectionPoint startP = proj.latLonToProj( startLL);
    startx = startP.getX();
    starty = startP.getY();

    attributes.add(new Attribute("grid_mapping_name", "mercator"));
    attributes.add(new Attribute("standard_parallel", Latin));
    attributes.add(new Attribute("longitude_of_projection_origin", Lo1));

    if (GribServiceProvider.debugProj) {
      double Lo2 = gdsIndex.readDouble("Lo2");
      if (Lo2 < Lo2) Lo2 += 360;
      double La2 = gdsIndex.readDouble("La2");
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GribHorizCoordSys.makeMercator: start at latlon= " + startLL);
      System.out.println("                                  end at latlon= " + endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   start at proj coord " + new ProjectionPointImpl(startx,starty));
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + (getNx()-1) * getDxInKm();
      double endy = starty + (getNy()-1) * getDyInKm();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }
  }

  // RotatedLatLon
  private void makeRotatedLatLon( NetcdfFile ncfile ) {
    double splat = 0, splon = 0, spangle = 0;
    String spLat = (String) gdsIndex.params.get("SpLat");
    if (null != spLat) {
      splat = Double.parseDouble(spLat);
    }
    String spLon = (String) gdsIndex.params.get("SpLon");
    if (null != spLon) {
      splon = Double.parseDouble(spLon);
    }
    String spAngle = (String) gdsIndex.params.get("RotationAngle");
    if (null != spAngle) {
      spangle = Double.parseDouble(spAngle);
    }
    // Given projection coordinates, need LatLon coordinates
    proj = new RotatedLatLon( splat, splon, spangle );
    LatLonPoint startLL = proj.projToLatLon( new ProjectionPointImpl(gdsIndex.Lo1,gdsIndex.La1));
    startx = startLL.getLongitude();
    starty = startLL.getLatitude();
    addCoordSystemVariable(ncfile, "latLonCoordSys", "time y x");

    // splat, splon, spangle
    attributes.add(new Attribute("grid_mapping_name", "rotated_latlon_grib"));
    attributes.add( new Attribute("grid_south_pole_latitude", new Double(splat)));
    attributes.add( new Attribute("grid_south_pole_longitude", new Double(splon)));
    attributes.add( new Attribute("grid_south_pole_angle", new Double(spangle)));

    if ( GribServiceProvider.debugProj) {
      System.out.println("Location of pole of rotated grid:");
      System.out.println("Lon="+ splon +", Lat="+ splat );
      System.out.println("Axial rotation about pole of rotated grid:"+ spangle );

      System.out.println("Location of LL in rotated grid:" );
      System.out.println("Lon="+ gdsIndex.Lo1 +", Lat="+ gdsIndex.La1 );
      System.out.println("Location of LL in non-rotated grid:" );
      System.out.println("Lon="+ startx +", Lat="+ starty );

      double Lo2 = gdsIndex.readDouble("Lo2");
      double La2 = gdsIndex.readDouble("La2");
      System.out.println("Location of UR in rotated grid:" );
      System.out.println("Lon="+ Lo2 +", Lat="+ La2 );
      System.out.println("Location of UR in non-rotated grid:" );
      LatLonPoint endUR = proj.projToLatLon( new ProjectionPointImpl(Lo2,La2));
      System.out.println("Lon="+ endUR.getLongitude() +", Lat="+ endUR.getLatitude());

      double dy = ( La2 < gdsIndex.La1) ? -gdsIndex.dy : gdsIndex.dy;
      double endx = gdsIndex.Lo1 + (getNx() -1) * gdsIndex.dx;
      double endy = gdsIndex.La1 + (getNy() -1) * dy;

      System.out.println("End point rotated grid should be x=" + endx + " y=" + endy);
    }
  }

  private void makeSpaceViewOrOthographic() {
    double Lat0 = gdsIndex.readDouble("Lap"); // sub-satellite point lat
    double Lon0 = gdsIndex.readDouble("Lop");  // sub-satellite point lon

    double xp = gdsIndex.readDouble("Xp"); // sub-satellite point in grid lengths
    double yp = gdsIndex.readDouble("Yp");

    double dx = gdsIndex.readDouble("Dx"); // apparent diameter in units of grid lengths
    double dy = gdsIndex.readDouble("Dy");

    double major_axis = gdsIndex.readDouble("major_axis_earth"); // km
    if ( Double.isNaN( major_axis  ))
       major_axis  = gdsIndex.readDouble("grid_major_axis_earth");

    double minor_axis = gdsIndex.readDouble("minor_axis_earth"); // km
    if ( Double.isNaN( minor_axis ))
       minor_axis = gdsIndex.readDouble("grid_minor_axis_earth");

    // Nr = altitude of camera from center, in units of radius
    double nr = gdsIndex.readDouble("Nr") * 1e-6;
    double apparentDiameter = 2 * Math.sqrt((nr - 1) / (nr + 1)); // apparent diameter, units of radius (see Snyder p 173)

    // app diameter kmeters / app diameter grid lengths = m per grid length
    double gridLengthX = major_axis * apparentDiameter / dx;
    double gridLengthY = minor_axis * apparentDiameter / dy;

    gdsIndex.dx = 1000 * gridLengthX; // meters
    gdsIndex.addParam( "Dx", String.valueOf(1000 * gridLengthX));
    gdsIndex.dy = 1000 * gridLengthY;
    gdsIndex.addParam( "Dy", String.valueOf(1000 * gridLengthY));

    startx = -gridLengthX * xp;  // km
    starty = -gridLengthY * yp;

    double radius = Earth.getRadius() / 1000.0; // km

    if (nr == 1111111111.0) { // LOOK: not sure how all ones will appear as a double, need example
      proj = new Orthographic(Lat0, Lon0, radius);

      attributes.add(new Attribute("grid_mapping_name", "orthographic"));
      attributes.add(new Attribute("longitude_of_projection_origin", Lon0));
      attributes.add(new Attribute("latitude_of_projection_origin", Lat0));

    } else { // "space view perspective"

      double height = (nr - 1.0) * radius;  // height = the height of the observing camera in km
      proj = new VerticalPerspectiveView(Lat0, Lon0, radius, height);

      attributes.add(new Attribute("grid_mapping_name", "vertical_perspective"));
      attributes.add(new Attribute("longitude_of_projection_origin", Lon0));
      attributes.add(new Attribute("latitude_of_projection_origin", Lat0));
      attributes.add(new Attribute("height_above_earth", height));
    }

    if (GribServiceProvider.debugProj) {

      double Lo2 = gdsIndex.readDouble("Lo2") + 360.0;
      double La2 = gdsIndex.readDouble("La2");
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GribHorizCoordSys.makeOrthographic end at latlon " + endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + getNx() * getDxInKm();
      double endy = starty + getNy() * getDyInKm();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }
  }

}