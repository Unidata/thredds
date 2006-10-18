// $Id: GribHorizCoordSys.java 70 2006-07-13 15:16:05Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.grib;

import ucar.grib.*;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.conv._Coordinate;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

/**
 * A horizontal coordinate system created from a Grib2GridDefinitionSection.
 *
 *         <p/>
 *         <p> Note on "false_easting" and "fale_northing" projection parameters:
 *         <ul><li>false_easting(northing) = The value added to all x (y) values in the rectangular coordinates for a map projection.
 *         This value frequently is assigned to eliminate negative numbers.
 *         Expressed in the unit of measure identified in Planar Coordinate Units.
 *         <li>We dont currently use, assuming that the x and y are just fine as negetive numbers.
 *
 * @author caron
 * @version $Revision: 70 $ $Date: 2006-07-13 15:16:05Z $
 */
public class GribHorizCoordSys {
  private TableLookup lookup;
  private Index.GdsRecord gdsIndex;
  private Group g;

  private String grid_name, shape_name, id;
  private boolean isLatLon = true;
  HashMap varHash = new HashMap(200); // GribVariables that have this GribHorizCoordSys
  HashMap productHash = new HashMap(100); // List of GribVariable, sorted by product desc
  HashMap vcsHash = new HashMap(30); // GribVertCoordSys

  private double startx, starty;
  private ProjectionImpl proj;
  private ArrayList attributes = new ArrayList();

  GribHorizCoordSys(Index.GdsRecord gdsIndex, TableLookup lookup, Group g) {
    this.gdsIndex = gdsIndex;
    this.lookup = lookup;
    this.g = g;

    this.grid_name = NetcdfFile.createValidNetcdfObjectName(lookup.getGridName(gdsIndex));
    this.shape_name = lookup.getShapeName(gdsIndex);
    this.g = g;
    isLatLon = lookup.isLatLon(gdsIndex);
    grid_name = StringUtil.replace(grid_name, ' ', "_");
    id = (g == null) ? grid_name : g.getName();

    if (isLatLon && (lookup.getProjectionType(gdsIndex) == TableLookup.GaussianLatLon)) {
        // hack-a-whack
      gdsIndex.dy = 2 * gdsIndex.La1 / gdsIndex.ny;
      gdsIndex.nx = 800;
      gdsIndex.dx = 360.0/gdsIndex.nx;
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
    return gdsIndex.nx;
  }

  int getNy() {
    return gdsIndex.ny;
  }

  double getDx() {
    return gdsIndex.dx * .001;
  }

  double getDy() {
    return gdsIndex.dy * .001;
  }

  void addDimensionsToNetcdfFile(NetcdfFile ncfile) {

    if (isLatLon) {
      ncfile.addDimension(g, new Dimension("lat", gdsIndex.ny, true));
      ncfile.addDimension(g, new Dimension("lon", gdsIndex.nx, true));
    } else {
      ncfile.addDimension(g, new Dimension("y", gdsIndex.ny, true));
      ncfile.addDimension(g, new Dimension("x", gdsIndex.nx, true));
    }
  }

  void addToNetcdfFile(NetcdfFile ncfile) {

    if (isLatLon) {
      double dy = (gdsIndex.readDouble("La2") < gdsIndex.La1) ? -gdsIndex.dy : gdsIndex.dy;
      addCoordAxis(ncfile, "lat", gdsIndex.ny, gdsIndex.La1, dy, "degrees_north", "latitude coordinate", "latitude", AxisType.Lat);
      addCoordAxis(ncfile, "lon", gdsIndex.nx, gdsIndex.Lo1, gdsIndex.dx, "degrees_east", "longitude coordinate", "longitude", AxisType.Lon);
      add2DCoordSystem(ncfile, "latLonCoordSys", "time lat lon");

    } else {
      computeProjection(ncfile);
      double[] yData = addCoordAxis(ncfile, "y", gdsIndex.ny, starty, getDy(), "km",
              "y coordinate of projection", "projection_y_coordinate", AxisType.GeoY);
      double[] xData = addCoordAxis(ncfile, "x", gdsIndex.nx, startx, getDx(), "km",
              "x coordinate of projection", "projection_x_coordinate", AxisType.GeoX);
      if (GribServiceProvider.addLatLon) addLatLon2D(ncfile, xData, yData);
      //add2DCoordSystem(ncfile, "projectionCoordSys", "time y x"); // LOOK is this needed?
    }
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
    Array dataArray = Array.factory(DataType.DOUBLE.getClassType(), new int []{n}, data);
    v.setCachedData(dataArray, false);

    v.addAttribute(new Attribute("units", units));
    v.addAttribute(new Attribute("long_name", desc));
    v.addAttribute(new Attribute("standard_name", standard_name));
    v.addAttribute(new Attribute("grid_spacing", incr + " " + units));
    v.addAttribute(new Attribute(_Coordinate.AxisType, axis.toString()));

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
    Array latDataArray = Array.factory(DataType.DOUBLE.getClassType(), new int []{ny, nx}, latData);
    latVar.setCachedData(latDataArray, false);

    Array lonDataArray = Array.factory(DataType.DOUBLE.getClassType(), new int []{ny, nx}, lonData);
    lonVar.setCachedData(lonDataArray, false);

    ncfile.addVariable(g, latVar);
    ncfile.addVariable(g, lonVar);
  }

  private void computeProjection(NetcdfFile ncfile) {
    switch (lookup.getProjectionType(gdsIndex)) {
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
        makeOrthographic();
        break;
      default:
        throw new UnsupportedOperationException("unknown projection = " + gdsIndex.grid_type);
    }

    Variable v = new Variable(ncfile, g, null, grid_name);
    v.setDataType(DataType.CHAR);
    v.setDimensions(new ArrayList()); // scalar
    char[] data = new char[]{'d'};
    Array dataArray = Array.factory(DataType.CHAR.getClassType(), new int[0], data);
    v.setCachedData(dataArray, false);

    for (int i = 0; i < attributes.size(); i++) {
      Attribute att = (Attribute) attributes.get(i);
      v.addAttribute(att);
    }

    v.addAttribute(new Attribute("earth_shape", shape_name));
    //v.addAttribute(new Attribute("GRIB_earth_shape_code", new Integer(gdsIndex.grid_shape_code)));
    if (gdsIndex.grid_shape_code == 1) {
      v.addAttribute(new Attribute("spherical_earth_radius_meters", new Double(gdsIndex.radius_spherical_earth)));
    }

    // add all the gds parameters
    java.util.Set keys = gdsIndex.params.keySet();
    ArrayList keyList = new ArrayList( keys);
    Collections.sort( keyList);
    for (int i = 0; i < keyList.size(); i++) {
      String key = (String) keyList.get(i);
      String name = NetcdfFile.createValidNetcdfObjectName("GRIB_param_"+key);

      String vals = (String) gdsIndex.params.get(key);
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

    ncfile.addVariable(g, v);
  }

  private void add2DCoordSystem(NetcdfFile ncfile, String name, String dims) {
    Variable v = new Variable(ncfile, g, null, name);
    v.setDataType(DataType.CHAR);
    v.setDimensions(new ArrayList()); // scalar
    Array dataArray = Array.factory(DataType.CHAR.getClassType(), new int[0], new char[]{'0'});
    v.setCachedData(dataArray, false);
    v.addAttribute(new Attribute(_Coordinate.Axes, dims));
    if (!isLatLon())
      v.addAttribute(new Attribute(_Coordinate.Transforms, getGridName()));
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

      double endx = startx + getNx() * getDx();
      double endy = starty + getNy() * getDy();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }

    attributes.add(new Attribute("grid_mapping_name", "lambert_conformal_conic"));
    if (gdsIndex.latin1 == gdsIndex.latin2)
      attributes.add(new Attribute("standard_parallel", new Double(gdsIndex.latin1)));
    else {
      double[] data = new double[]{gdsIndex.latin1, gdsIndex.latin2};
      attributes.add(new Attribute("standard_parallel",
              Array.factory(DataType.DOUBLE.getClassType(), new int[]{2}, data)));
    }
    attributes.add(new Attribute("longitude_of_central_meridian", new Double(gdsIndex.LoV)));
    attributes.add(new Attribute("latitude_of_projection_origin", new Double(gdsIndex.latin1)));
    //attributes.add( new Attribute("false_easting", new Double(startx)));
    //attributes.add( new Attribute("false_northing", new Double(starty)));
  }

  // polar stereographic
  private void makePS() {
    double scale = .933;

    // Why the scale factor?. accordining to GRIB docs:
    // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
    // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60))  [Snyder,Working Manual p157]
    // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
    proj = new Stereographic(90.0, gdsIndex.LoV, scale);

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
    attributes.add(new Attribute("longitude_of_projection_origin", new Double(gdsIndex.LoV)));
    attributes.add(new Attribute("scale_factor_at_projection_origin", new Double(scale)));
    //attributes.add( new Attribute("false_easting", new Double(startx)));
    //attributes.add( new Attribute("false_northing", new Double(starty)));
  }

  // Mercator
  private void makeMercator() {
    /**
     * Construct a Mercator Projection.
     * @param lat0 latitude of origin (degrees)
     * @param lon0 longitude of origin (degrees)
     * @param par standard parallel (degrees). cylinder cuts earth at this latitude.
     */
    double Latin = gdsIndex.readDouble("Latin") * .001;
    double La1 = gdsIndex.La1;
    double Lo1 = gdsIndex.Lo1;

    // put projection origin at La1, Lo1
    proj = new Mercator(La1, Lo1, Latin);
    startx = 0;
    starty = 0;

    attributes.add(new Attribute("grid_mapping_name", "mercator"));
    attributes.add(new Attribute("standard_parallel", new Double(Latin)));
    attributes.add(new Attribute("longitude_of_projection_origin", new Double(Lo1)));
    attributes.add(new Attribute("latitude_of_projection_origin", new Double(La1)));

    if (GribServiceProvider.debugProj) {
      double Lo2 = gdsIndex.readDouble("Lo2") + 360.0;
      double La2 = gdsIndex.readDouble("La2");
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GribHorizCoordSys.makeMercator end at latlon " + endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + getNx() * getDx();
      double endy = starty + getNy() * getDy();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }
  }

  private void makeOrthographic() {
    double Lat0 = gdsIndex.readDouble("Latitude of sub-satellite point");
    double Lon0 = gdsIndex.readDouble("Longitude of sub-satellite pt");

    double xp = gdsIndex.readDouble("X-coordinateSub-satellite");
    double yp = gdsIndex.readDouble("Y-coordinateSub-satellite");

    startx = gdsIndex.readDouble("X-coordinateOrigin") - xp;
    starty = gdsIndex.readDouble("Y-coordinateOrigin") - yp;

    proj = new Orthographic(Lat0, Lon0);

    attributes.add(new Attribute("grid_mapping_name", "orthographic"));
    attributes.add(new Attribute("longitude_of_projection_origin", new Double(Lon0)));
    attributes.add(new Attribute("latitude_of_projection_origin", new Double(Lat0)));

    if (GribServiceProvider.debugProj) {

      double Lo2 = gdsIndex.readDouble("Lo2") + 360.0;
      double La2 = gdsIndex.readDouble("La2");
      LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
      System.out.println("GribHorizCoordSys.makeMercator end at latlon " + endLL);

      ProjectionPointImpl endPP = (ProjectionPointImpl) proj.latLonToProj(endLL);
      System.out.println("   end at proj coord " + endPP);

      double endx = startx + getNx() * getDx();
      double endy = starty + getNy() * getDy();
      System.out.println("   should be x=" + endx + " y=" + endy);
    }
  }

}