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
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.transform.WRFEtaTransformBuilder;
import ucar.unidata.util.StringUtil;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import java.io.IOException;
import java.util.*;

/**
 * WRF netcdf output files.
 * <p/>
 * Note: Apparently WRF netcdf files before version 2 didnt output the projection origin, so
 * we cant properly georeference them.
 *
 * @author caron
 */

public class WRFConvention extends CoordSysBuilder {

  static private java.text.SimpleDateFormat dateFormat;

  static {
    dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  /**
   * @param ncfile the NetcdfFile to test
   * @return true if we think this is a WRF file.
   */
  public static boolean isMine(NetcdfFile ncfile) {
    return (null != ncfile.findGlobalAttribute("MAP_PROJ")) &&
        (null != ncfile.findDimension("south_north"));
  }

  private double centerX = 0.0, centerY = 0.0;
  private ProjectionCT projCT = null;

  public WRFConvention() {
    this.conventionName = "WRF";
  }

  /* Implementation notes

    There are 2 WRFs : NMM ("Non-hydrostatic Mesoscale Model" developed at NOAA/NCEP) and
    1) NMM ("Non-hydrostatic Mesoscale Model" developed at NOAA/NCEP)
      GRIDTYPE="E"
      DYN_OPT = 4

      This is a staggered grid that requires special processing.

    2) ARW ("Advanced Research WRF", developed at MMM)
      GRIDTYPE="C"
      DYN_OPT = 2
      DX, DY grid spaceing in meters (must be equal)

      the Arakawa C staggered grid (see ARW 2.2 p 3-17)
      the + are the "non-staggered" grid:
      
      + V + V + V +
      U T U T U T U
      + V + V + V +
      U T U T U T U
      + V + V + V +
      U T U T U T U
      + V + V + V +
   */

  /* ARW Users Guide p 3-19
  <pre>
7. MAP_PROJ_NAME: Character string specifying type of map projection. Valid entries are:
  "polar" -> Polar stereographic
  "lambert" -> Lambert conformal (secant and tangent)
  "mercator" -> Mercator

8. MOAD_KNOWN_LAT/MOAD_KNOWN_LON (= CEN_LAT, CEN_LON):
  Real latitude and longitude of the center point in the grid.

9. MOAD_STAND_LATS (= TRUE_LAT1/2):
  2 real values for the "true" latitudes (where grid spacing is exact).
  Must be between -90 and 90, and the values selected depend on projection:
  Polar-stereographic: First value must be the latitude at which the grid
spacing is true. Most users will set this equal to their center latitude.
Second value must be +/-90. for NH/SH grids.
  Lambert Conformal: Both values should have the same sign as the center
latitude. For a tangential lambert conformal, set both to the same value
(often equal to the center latitude). For a secant Lambert Conformal, they
may be set to different values.
  Mercator: The first value should be set to the latitude you wish your grid
  spacing to be true (often your center latitude). Second value is not used.

10. MOAD_STAND_LONS (=STAND_LON): This is one entry specifying the longitude in degrees East (-
180->180) that is parallel to the y-axis of your grid, (sometimes referred to as the
orientation of the grid). This should be set equal to the center longitude in most cases.
*/

  public void augmentDataset(NetcdfDataset ds, CancelTask cancelTask) {
    if (null != ds.findVariable("x")) return; // check if its already been done - aggregating enhanced datasets.

    // kludge in fixing the units
    List<Variable> vlist = ds.getVariables();
    for (Variable v : vlist) {
      Attribute att = v.findAttributeIgnoreCase("units");
      if (att != null) {
        String units = att.getStringValue();
        v.addAttribute(new Attribute("units", normalize(units))); // removes the old
      }
    }

    // make projection transform
    Attribute att = ds.findGlobalAttribute("MAP_PROJ");
    int projType = att.getNumericValue().intValue();

    if (projType == 203) {

      /* centerX = centralLon;
      centerY = centralLat;
      ds.addCoordinateAxis( makeLonCoordAxis( ds, "longitude", ds.findDimension("west_east")));
      ds.addCoordinateAxis( makeLatCoordAxis( ds, "latitude", ds.findDimension("south_north")));  */

      Variable glat = ds.findVariable("GLAT");
      if (glat == null) {
        parseInfo.format("Projection type 203 - expected GLAT variable not found\n");
      } else {
        glat.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));
        glat.setDimensions("south_north west_east");
        glat.setCachedData(convertToDegrees(glat), false);
        glat.addAttribute(new Attribute("units", "degrees_north"));
      }

      Variable glon = ds.findVariable("GLON");
      if (glon == null) {
        parseInfo.format("Projection type 203 - expected GLON variable not found\n");
      } else {
        glon.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));
        glon.setDimensions("south_north west_east");
        glon.setCachedData(convertToDegrees(glon), false);
        glon.addAttribute(new Attribute("units", "degrees_east"));
      }

      VariableDS v = new VariableDS(ds, null, null, "LatLonCoordSys", DataType.CHAR, "", null, null);
      v.addAttribute(new Attribute(_Coordinate.Axes, "GLAT GLON Time"));
      Array data = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[]{}, new char[]{' '});
      v.setCachedData(data, true);
      ds.addVariable(null, v);

      Variable dataVar = ds.findVariable("LANDMASK");
      dataVar.addAttribute(new Attribute(_Coordinate.Systems, "LatLonCoordSys"));

    } else {


      double lat1 = findAttributeDouble(ds, "TRUELAT1");
      double lat2 = findAttributeDouble(ds, "TRUELAT2");
      double centralLat = findAttributeDouble(ds, "CEN_LAT");  // center of grid
      double centralLon = findAttributeDouble(ds, "CEN_LON");  // center of grid

      double standardLon = findAttributeDouble(ds, "STAND_LON"); // true longitude
      double standardLat = findAttributeDouble(ds, "MOAD_CEN_LAT");

      ProjectionImpl proj = null;
      switch (projType) {
        case 0: // for diagnostic runs with no georeferencing
          proj = new FlatEarth();
          projCT = new ProjectionCT("flat_earth", "FGDC", proj);
          // System.out.println(" using LC "+proj.paramsToString());
          break;
        case 1:
          proj = new LambertConformal(standardLat, standardLon, lat1, lat2);
          projCT = new ProjectionCT("Lambert", "FGDC", proj);
          // System.out.println(" using LC "+proj.paramsToString());
          break;
        case 2:
          // Thanks to Heiko Klein for figuring out WRF Stereographic
          double lon0 = (Double.isNaN(standardLon)) ? centralLon : standardLon;
          double scaleFactor = (1+Math.abs(Math.sin(Math.toRadians(lat1))))/2.;  // R Schmunk 9/10/07
          //double scaleFactor = (1 + Math.sin(Math.toRadians(lat1))) / 2.;
          proj = new Stereographic(lat2, lon0, scaleFactor);
          projCT = new ProjectionCT("Stereographic", "FGDC", proj);
          break;
        case 3:
          proj = new Mercator(standardLon, standardLat); // thanks to Robert Scmunk
          projCT = new ProjectionCT("Mercator", "FGDC", proj);
          // proj = new TransverseMercator(standardLat, standardLon, 1.0);
          //projCT = new ProjectionCT("TransverseMercator", "FGDC", proj);
          break;
        default:
          parseInfo.format("ERROR: unknown projection type = %s\n", projType);
          break;
      }

      if (proj != null) {
        LatLonPointImpl lpt1 = new LatLonPointImpl(centralLat, centralLon); // center of the grid
        ProjectionPoint ppt1 = proj.latLonToProj(lpt1, new ProjectionPointImpl());
        centerX = ppt1.getX();
        centerY = ppt1.getY();
        if (debug) {
          System.out.println("centerX=" + centerX);
          System.out.println("centerY=" + centerY);
        }
      }

      // make axes
      ds.addCoordinateAxis(makeXCoordAxis(ds, "x", ds.findDimension("west_east")));
      ds.addCoordinateAxis(makeXCoordAxis(ds, "x_stag", ds.findDimension("west_east_stag")));
      ds.addCoordinateAxis(makeYCoordAxis(ds, "y", ds.findDimension("south_north")));
      ds.addCoordinateAxis(makeYCoordAxis(ds, "y_stag", ds.findDimension("south_north_stag")));
      ds.addCoordinateAxis(makeZCoordAxis(ds, "z", ds.findDimension("bottom_top")));
      ds.addCoordinateAxis(makeZCoordAxis(ds, "z_stag", ds.findDimension("bottom_top_stag")));

      if (projCT != null) {
        VariableDS v = makeCoordinateTransformVariable(ds, projCT);
        v.addAttribute(new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
        ds.addVariable(null, v);
      }
    }

    // time coordinate variations
    if (ds.findVariable("Time") == null) { // Can skip this if its already there, eg from NcML
      CoordinateAxis taxis = makeTimeCoordAxis(ds, "Time", ds.findDimension("Time"));
      if (taxis == null)
        taxis = makeTimeCoordAxis(ds, "Time", ds.findDimension("Times"));
      if (taxis != null)
        ds.addCoordinateAxis(taxis);
    }

    ds.addCoordinateAxis(makeSoilDepthCoordAxis(ds, "ZS"));

    ds.finish();
  }

  private Array convertToDegrees(Variable v) {
    Array data;
    try {
      data = v.read();
      data = data.reduce();
    } catch (IOException ioe) {
      throw new RuntimeException("data read failed on " + v.getName() + "=" + ioe.getMessage());
    }
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      ii.setDoubleCurrent(Math.toDegrees(ii.getDoubleNext()));
    }
    return data;
  }

  // pretty much WRF specific
  private String normalize(String units) {
    if (units.equals("fraction")) units = "";
    else if (units.equals("dimensionless")) units = "";
    else if (units.equals("NA")) units = "";
    else if (units.equals("-")) units = "";
    else {
      units = StringUtil.substitute(units, "**", "^");
      units = StringUtil.remove(units, '}');
      units = StringUtil.remove(units, '{');
    }
    return units;
  }
  /////////////////////////////////////////////////////////////////////////

  protected void makeCoordinateTransforms(NetcdfDataset ds) {
    if (projCT != null) {
      VarProcess vp = findVarProcess(projCT.getName());
      vp.isCoordinateTransform = true;
      vp.ct = projCT;
    }
    super.makeCoordinateTransforms(ds);
  }

  protected AxisType getAxisType(NetcdfDataset ds, VariableEnhanced ve) {
    Variable v = (Variable) ve;
    String vname = v.getName();

    if (vname.equalsIgnoreCase("x") || vname.equalsIgnoreCase("x_stag"))
      return AxisType.GeoX;

    if (vname.equalsIgnoreCase("lon"))
      return AxisType.Lon;

    if (vname.equalsIgnoreCase("y") || vname.equalsIgnoreCase("y_stag"))
      return AxisType.GeoY;

    if (vname.equalsIgnoreCase("lat"))
      return AxisType.Lat;

    if (vname.equalsIgnoreCase("z") || vname.equalsIgnoreCase("z_stag"))
      return AxisType.GeoZ;

    if (vname.equalsIgnoreCase("Z"))
      return AxisType.Height;

    if (vname.equalsIgnoreCase("time") || vname.equalsIgnoreCase("times"))
      return AxisType.Time;

    String unit = ve.getUnitsString();
    if (unit != null) {
      if (SimpleUnit.isCompatible("millibar", unit))
        return AxisType.Pressure;

      if (SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;
    }


    return null;
  }

  /**
   * Does increasing values of Z go vertical  up?
   *
   * @param v for thsi axis
   * @return "up" if this is a Vertical (z) coordinate axis which goes up as coords get bigger,
   *         else return "down"
   */
  public String getZisPositive(CoordinateAxis v) {
    return "down"; //eta coords decrease upward
  }

  //////////////////////////////////////////////////////////////////////////////////////////////

  private CoordinateAxis makeLonCoordAxis(NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    double dx = 4 * findAttributeDouble(ds, "DX");
    int nx = dim.getLength();
    double startx = centerX - dx * (nx - 1) / 2;

    CoordinateAxis v = new CoordinateAxis1D(ds, null, axisName, DataType.DOUBLE, dim.getName(), "degrees_east", "synthesized longitude coordinate");
    ds.setValues(v, nx, startx, dx);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "Lon"));
    if (!axisName.equals(dim.getName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    return v;
  }

  private CoordinateAxis makeLatCoordAxis(NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    double dy = findAttributeDouble(ds, "DY");
    int ny = dim.getLength();
    double starty = centerY - dy * (ny - 1) / 2;

    CoordinateAxis v = new CoordinateAxis1D(ds, null, axisName, DataType.DOUBLE, dim.getName(), "degrees_north", "synthesized latitude coordinate");
    ds.setValues(v, ny, starty, dy);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "Lat"));
    if (!axisName.equals(dim.getName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    return v;
  }

  private CoordinateAxis makeXCoordAxis(NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    double dx = findAttributeDouble(ds, "DX") / 1000.0; // km ya just gotta know
    int nx = dim.getLength();
    double startx = centerX - dx * (nx - 1) / 2; // ya just gotta know
    //System.out.println(" originX= "+originX+" startx= "+startx);

    CoordinateAxis v = new CoordinateAxis1D(ds, null, axisName, DataType.DOUBLE, dim.getName(), "km", "synthesized GeoX coordinate from DX attribute");
    ds.setValues(v, nx, startx, dx);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoX"));
    if (!axisName.equals(dim.getName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    //ADD: is staggered grid being dealt with?
    return v;
  }

  private CoordinateAxis makeYCoordAxis(NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    double dy = findAttributeDouble(ds, "DY") / 1000.0;
    int ny = dim.getLength();
    double starty = centerY - dy * (ny - 1) / 2; // - dy/2; // ya just gotta know
    //System.out.println(" originY= "+originY+" starty= "+starty);

    CoordinateAxis v = new CoordinateAxis1D(ds, null, axisName, DataType.DOUBLE, dim.getName(), "km", "synthesized GeoY coordinate from DY attribute");
    ds.setValues(v, ny, starty, dy);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoY"));
    if (!axisName.equals(dim.getName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getName()));
    //ADD: is staggered grid being dealt with?
    return v;
  }

  private CoordinateAxis makeZCoordAxis(NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;

    String fromWhere = axisName.endsWith("stag") ? "ZNW" : "ZNU";

    CoordinateAxis v = new CoordinateAxis1D(ds, null, axisName, DataType.DOUBLE, dim.getName(), "", "eta values from variable " + fromWhere);
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoZ"));
    if (!axisName.equals(dim.getName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    // create eta values from file variables: ZNU, ZNW
    // But they are a function of time though the values are the same in the sample file
    // NOTE: Use first time sample assuming all are the same!
    Variable etaVar = ds.findVariable(fromWhere);
    if (etaVar == null) return makeFakeCoordAxis(ds, axisName, dim);

    int n = etaVar.getShape(1); //number of eta levels
    int[] origin = new int[]{0, 0};
    int[] shape = new int[]{1, n};
    try {
      Array array = etaVar.read(origin, shape);//read first time slice
      ArrayDouble.D1 newArray = new ArrayDouble.D1(n);
      IndexIterator it = array.getIndexIterator();
      int count = 0;
      while (it.hasNext()) {
        double d = it.getDoubleNext();
        newArray.set(count++, d);
      }
      v.setCachedData(newArray, true);
    } catch (Exception e) {
      e.printStackTrace();
    }//ADD: error?

    return v;
  }

  private CoordinateAxis makeFakeCoordAxis(NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    CoordinateAxis v = new CoordinateAxis1D(ds, null, axisName, DataType.SHORT, dim.getName(), "", "synthesized coordinate: only an index");
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoZ"));
    if (!axisName.equals(dim.getName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    ds.setValues(v, dim.getLength(), 0, 1);
    return v;
  }

  private CoordinateAxis makeTimeCoordAxis(NetcdfDataset ds, String axisName, Dimension dim) {
    if (dim == null) return null;
    int nt = dim.getLength();
    Variable timeV = ds.findVariable("Times");
    if (timeV == null) return null;

    Array timeData;
    try {
      timeData = timeV.read();
    } catch (IOException ioe) {
      return null;
    }

    ArrayDouble.D1 values = new ArrayDouble.D1(nt);
    int count = 0;

    if (timeData instanceof ArrayChar) {
      ArrayChar.StringIterator iter = ((ArrayChar) timeData).getStringIterator();
      while (iter.hasNext()) {
        String dateS = iter.next();
        try {
          Date d = dateFormat.parse(dateS);
          values.set(count++, (double) d.getTime() / 1000);
        } catch (java.text.ParseException e) {
          parseInfo.format("ERROR: cant parse Time string = <%s> err= %s\n",dateS, e.getMessage());

          // one more try
          String startAtt = ds.findAttValueIgnoreCase(null, "START_DATE", null);
          if ((nt == 1) && (null != startAtt)) {
            try {
              Date d = dateFormat.parse(startAtt);
              values.set(0, (double) d.getTime() / 1000);
            } catch (java.text.ParseException e2) {
              parseInfo.format("ERROR: cant parse global attribute START_DATE = <%s> err=%s\n", startAtt, e2.getMessage());
            }
          }
        }
      }
    } else {
      IndexIterator iter = timeData.getIndexIterator();
      while (iter.hasNext()) {
        String dateS = (String) iter.next();
        try {
          Date d = dateFormat.parse(dateS);
          values.set(count++, (double) d.getTime() / 1000);
        } catch (java.text.ParseException e) {
          parseInfo.format("ERROR: cant parse Time string = %s\n", dateS);
        }
      }

    }

    CoordinateAxis v = new CoordinateAxis1D(ds, null, axisName, DataType.DOUBLE, dim.getName(),
        "secs since 1970-01-01 00:00:00", "synthesized time coordinate from Times(time)");
    v.addAttribute(new Attribute(_Coordinate.AxisType, "Time"));
    if (!axisName.equals(dim.getName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    v.setCachedData(values, true);
    return v;
  }

  private VariableDS makeSoilDepthCoordAxis(NetcdfDataset ds, String coordVarName) {
    Variable coordVar = ds.findVariable(coordVarName);
    if (null == coordVar)
      return null;

    Dimension soilDim = null;
    List<Dimension> dims = coordVar.getDimensions();
    for (Dimension d : dims) {
      if (d.getName().startsWith("soil_layers"))
        soilDim = d;
    }
    if (null == soilDim)
      return null;

    if (coordVar.getRank() == 1) {
      coordVar.addAttribute(new Attribute(_Coordinate.AxisType, "GeoZ"));
      if (!coordVarName.equals(soilDim.getName()))
        coordVar.addAttribute(new Attribute(_Coordinate.AliasForDimension, soilDim.getName()));
      return (VariableDS) coordVar;
    }

    String units = ds.findAttValueIgnoreCase(coordVar, "units", "");

    CoordinateAxis v = new CoordinateAxis1D(ds, null, "soilDepth", DataType.SHORT, soilDim.getName(), units, "soil depth");
    v.addAttribute(new Attribute(_Coordinate.AxisType, "GeoZ"));
    v.addAttribute(new Attribute("units", "units"));
    if (!v.getShortName().equals(soilDim.getName()))
      v.addAttribute(new Attribute(_Coordinate.AliasForDimension, soilDim.getName()));

    //read first time slice
    int n = coordVar.getShape(1);
    int[] origin = new int[]{0, 0};
    int[] shape = new int[]{1, n};
    try {
      Array array = coordVar.read(origin, shape);
      ArrayDouble.D1 newArray = new ArrayDouble.D1(n);
      IndexIterator it = array.getIndexIterator();
      int count = 0;
      while (it.hasNext()) {
        double d = it.getDoubleNext();
        newArray.set(count++, d);
      }
      v.setCachedData(newArray, true);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return v;
  }

  private double findAttributeDouble(NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    if (att == null) return Double.NaN;
    return att.getNumericValue().doubleValue();
  }

  /**
   * Assign CoordinateTransform objects to Coordinate Systems.
   */
  protected void assignCoordinateTransforms(NetcdfDataset ncDataset) {
    super.assignCoordinateTransforms(ncDataset);

    // any cs whose got a vertical coordinate with no units
    List<CoordinateSystem> csys = ncDataset.getCoordinateSystems();
    for (CoordinateSystem cs : csys) {
      if (cs.getZaxis() != null) {
        String units = cs.getZaxis().getUnitsString();
        if ((units == null) || (units.trim().length() == 0)) {
          VerticalCT vct = makeWRFEtaVerticalCoordinateTransform(ncDataset, cs);
          if (vct != null)
            cs.addCoordinateTransform(vct);
          parseInfo.format("***Added WRFEta verticalCoordinateTransform to %s\n",cs.getName());
        }
      }
    }
  }

  private VerticalCT makeWRFEtaVerticalCoordinateTransform(NetcdfDataset ds, CoordinateSystem cs) {
    if ((null == ds.findVariable("PH")) || (null == ds.findVariable("PHB")) ||
        (null == ds.findVariable("P")) || (null == ds.findVariable("PB")))
      return null;

    WRFEtaTransformBuilder builder = new WRFEtaTransformBuilder(cs);
    return (VerticalCT) builder.makeCoordinateTransform(ds, null);
  }

  /* private boolean isStaggered(CoordinateAxis axis) {
  	if (axis == null) return false;
  	String name = axis.getName();
  	if (name == null) return false;
  	if (name.endsWith("stag")) return true;
  	return false;
  }

  private class WRFEtaBuilder extends AbstractCoordTransBuilder {
    private CoordinateSystem cs;

    WRFEtaBuilder(CoordinateSystem cs) {
      this.cs = cs;
    }

    public CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable v) {
      VerticalCT.Type type = VerticalCT.Type.WRFEta;
      VerticalCT ct = new VerticalCT(type.toString(), conventionName, type, this);

      ct.addParameter(new Parameter("height formula", "height(x,y,z) = (PH(x,y,z) + PHB(x,y,z)) / 9.81"));
      ct.addParameter(new Parameter(WRFEta.PerturbationGeopotentialVariable, "PH"));
      ct.addParameter(new Parameter(WRFEta.BaseGeopotentialVariable, "PHB"));
      ct.addParameter(new Parameter("pressure formula", "pressure(x,y,z) = P(x,y,z) + PB(x,y,z)"));
      ct.addParameter(new Parameter(WRFEta.PerturbationPressureVariable, "P"));
      ct.addParameter(new Parameter(WRFEta.BasePressureVariable, "PB"));
      ct.addParameter(new Parameter(WRFEta.IsStaggeredX, ""+isStaggered(cs.getXaxis())));
      ct.addParameter(new Parameter(WRFEta.IsStaggeredY, ""+isStaggered(cs.getYaxis())));
      ct.addParameter(new Parameter(WRFEta.IsStaggeredZ, ""+isStaggered(cs.getZaxis())));
      ct.addParameter(new Parameter("eta", ""+cs.getZaxis().getName()));

      parseInfo.append(" added vertical coordinate transform = "+type+"\n");
      return ct;
    }

    public String getTransformName() {
      return "WRF_Eta";
    }

    public TransformType getTransformType() {
      return TransformType.Vertical;
    }

    public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
      return new WRFEta(ds, timeDim, vCT);
    }

  } */

  public static void main(String args[]) throws IOException, InvalidRangeException {
    NetcdfFile ncd = NetcdfDataset.openFile("R:/testdata/wrf/WRFOU~C@", null);

    Variable glat = ncd.findVariable("GLAT");
    Array glatData = glat.read();
    IndexIterator ii = glatData.getIndexIterator();
    while (ii.hasNext()) {
      ii.setDoubleCurrent(Math.toDegrees(ii.getDoubleNext()));
    }
    NCdump.printArray(glatData, "GLAT", System.out, null);

    Variable glon = ncd.findVariable("GLON");
    Array glonData = glon.read();
    ii = glonData.getIndexIterator();
    while (ii.hasNext()) {
      ii.setDoubleCurrent(Math.toDegrees(ii.getDoubleNext()));
    }
    NCdump.printArray(glonData, "GLON", System.out, null);


    Index index = glatData.getIndex();
    Index index2 = glatData.getIndex();

    int[] vshape = glatData.getShape();
    int ny = vshape[1];
    int nx = vshape[2];

    ArrayDouble.D1 diff_y = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[]{ny});
    ArrayDouble.D1 diff_x = (ArrayDouble.D1) Array.factory(DataType.DOUBLE, new int[]{nx});

    for (int y = 0; y < ny - 1; y++) {
      double val = glatData.getDouble(index.set(0, y, 0)) - glatData.getDouble(index2.set(0, y + 1, 0));
      diff_y.set(y, val);
    }

    for (int x = 0; x < nx - 1; x++) {
      double val = glatData.getDouble(index.set(0, 0, x)) - glatData.getDouble(index2.set(0, 0, x + 1));
      diff_x.set(x, val);
    }

    NCdump.printArray(diff_y, "diff_y", System.out, null);
    NCdump.printArray(diff_x, "diff_x", System.out, null);
    ncd.close();

  }


}