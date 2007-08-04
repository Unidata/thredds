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

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.nc2.units.SimpleUnit;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.StringUtil;

import java.io.IOException;
import java.util.*;

/**
 * AWIPS netcdf output.
 *
 * @author caron
 */

public class AWIPSConvention extends CoordSysBuilder {

  /**
   * @param ncfile the NetcdfFile to test
   * @return true if we think this is a AWIPS file.
   */
  public static boolean isMine( NetcdfFile ncfile) {
    return (null != ncfile.findGlobalAttribute("projName")) &&
       (null != ncfile.findDimension("charsPerLevel")) &&
       (null != ncfile.findDimension("x")) &&
       (null != ncfile.findDimension("y"));
  }

  private final boolean debugProj = false;
  private final boolean debugBreakup = false;

  private List<Variable> mungedList = new ArrayList<Variable>();
  private ProjectionCT projCT = null;
  private double startx, starty;

  public AWIPSConvention() {
    this.conventionName = "AWIPS";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) {

    Dimension dimx = ds.findDimension("x");
    int nx = dimx.getLength();

    Dimension dimy = ds.findDimension("y");
    int ny = dimy.getLength();

    String projName = ds.findAttValueIgnoreCase(null, "projName", "none");
    if (projName.equalsIgnoreCase("LATLON")) {
      ds.addCoordinateAxis( makeLonCoordAxis( ds, nx, "x"));
      ds.addCoordinateAxis( makeLatCoordAxis( ds, ny, "y"));
    } else if (projName.equalsIgnoreCase("LAMBERT_CONFORMAL")) {
      projCT = makeLCProjection(ds, projName);
      ds.addCoordinateAxis( makeXCoordAxis( ds, nx, "x"));
      ds.addCoordinateAxis( makeYCoordAxis( ds, ny, "y"));
    } else if (projName.equalsIgnoreCase("STEREOGRAPHIC")) {
      projCT = makeStereoProjection(ds, projName);
      ds.addCoordinateAxis( makeXCoordAxis( ds, nx, "x"));
      ds.addCoordinateAxis( makeYCoordAxis( ds, ny, "y"));
    }


    CoordinateAxis timeCoord = makeTimeCoordAxis( ds);
    if (timeCoord != null) {
      ds.addCoordinateAxis( timeCoord);
      Dimension d =  timeCoord.getDimension(0);
      if (!d.getName().equals( timeCoord.getShortName()) )
        timeCoord.addAttribute( new Attribute(_Coordinate.AliasForDimension, d.getName()));
    }

    // AWIPS cleverly combines multiple z levels into a single variable (!!)
    for (Variable ncvar : ds.getVariables()) {
      String levelName = ncvar.getName() + "Levels";
      Variable levelVar = ds.findVariable(levelName);
      if (levelVar == null) continue;
      if (levelVar.getRank() != 2) continue;
      if (levelVar.getDataType() != DataType.CHAR) continue;

      List<Dimension> levels = breakupLevels(ds, levelVar);
      try {
        createNewVariables(ds, ncvar, levels, levelVar.getDimension(0));
      }
      catch (InvalidRangeException ex) {
        parseInfo.append("createNewVariables InvalidRangeException\n");
      }
      mungedList.add(ncvar);
    }

    if (projCT != null) {
        VariableDS v = makeCoordinateTransformVariable(ds, projCT);
        v.addAttribute( new Attribute(_Coordinate.Axes, "x y"));
        ds.addVariable(null, v);
    }


    ds.finish();

      // kludge in fixing the units
    List<Variable> vlist = ds.getVariables();
    for (Variable v : vlist) {
      Attribute att = v.findAttributeIgnoreCase("units");
      if (att != null) {
        String units = att.getStringValue();
        v.addAttribute(new Attribute("units", normalize(units))); // removes the old
      }
    }

  }

  // pretty much WRF specific
  private String normalize( String units) {
    if (units.equals("/second")) units="1/sec";
    if (units.equals("degrees K")) units="K";
    else {
      units = StringUtil.substitute( units, "**", "^");
      units = StringUtil.remove( units, ')');
      units = StringUtil.remove( units, '(');
    }
    return units;
  }

  // take a combined level variable and create multiple levels out of it
  // return the list of Dimensions that were created
  private List<Dimension> breakupLevels( NetcdfDataset ds, Variable levelVar) {
    if (debugBreakup) parseInfo.append("breakupLevels = ").append(levelVar.getName()).append("\n");
    List<Dimension> dimList = new ArrayList<Dimension>();

    ArrayChar levelVarData;
    try {
      levelVarData = (ArrayChar) levelVar.read();
    } catch (IOException ioe) {
      return dimList;
    }

    List<String> values = null;
    String currentUnits = null;
    ArrayChar.StringIterator iter = levelVarData.getStringIterator();
    while (iter.hasNext()) {
      String s = iter.next();
      if (debugBreakup) parseInfo.append("   ").append(s).append("\n");
      StringTokenizer stoke = new StringTokenizer(s);

      // first token is the unit
      String units = stoke.nextToken().trim();
      if (!units.equals(currentUnits)) {
        if (values != null)
          dimList.add( makeZCoordAxis(ds, values, currentUnits));
        values = new ArrayList<String>();
        currentUnits = units;
      }

      // next token is the value
      if (stoke.hasMoreTokens())
        values.add(stoke.nextToken());
      else
        values.add("0");
    }
    if (values != null)
      dimList.add(makeZCoordAxis(ds, values, currentUnits));

    if (debugBreakup) parseInfo.append("  done breakup"+"\n");

    return dimList;
  }

  // make a new variable out of the list in "values"
  private Dimension makeZCoordAxis( NetcdfDataset ds, List<String> values, String units) {
    int len = values.size();
    String name = makeZCoordName( units);
    if (len > 1)
      name = name + Integer.toString(len);
    else
      name = name + values.get(0);
    StringUtil.replace(  name, ' ', "-");

    // LOOK replace with check against actual values !!!
    Dimension dim;
    if (null != (dim = ds.getRootGroup().findDimension(name))) {
      if (dim.getLength() == len) {
        if (debugBreakup) parseInfo.append("  use existing dim").append(dim);
        return dim;
      }
    }

    // create new one
    dim = new Dimension(name, len, true);
    ds.addDimension( null, dim);
    if (debugBreakup) parseInfo.append("  make Dimension = ").append(name).append(" length = ").append(len).append("\n");

    // if (len < 2) return dim; // skip 1D

    if (debugBreakup) {
      parseInfo.append("  make ZCoordAxis = ").append(name).append(" length = ").append(len).append("\n");
    }

    CoordinateAxis v = new CoordinateAxis1D( ds, null, name, DataType.DOUBLE, name,
       makeUnitsName( units), makeLongName(name));
    String positive = getZisPositive( ds, v);
    if (null != positive)
      v.addAttribute( new Attribute(_Coordinate.ZisPositive, positive));

    ds.setValues( v, values);
    ds.addCoordinateAxis(v);

    parseInfo.append("Created Z Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.append("\n");

    return dim;
  }

  private String makeZCoordName(String units) {
    if (units.equalsIgnoreCase("MB")) return "PressureLevels";
    if (units.equalsIgnoreCase("K")) return "PotTempLevels";
    if (units.equalsIgnoreCase("BL")) return "BoundaryLayers";

    if (units.equalsIgnoreCase("FHAG")) return "FixedHeightAboveGround";
    if (units.equalsIgnoreCase("FH")) return "FixedHeight";
    if (units.equalsIgnoreCase("SFC")) return "Surface";
    if (units.equalsIgnoreCase("MSL")) return "MeanSeaLevel";
    if (units.equalsIgnoreCase("FRZ")) return "FreezingLevel";
    if (units.equalsIgnoreCase("TROP")) return "Tropopause";
    if (units.equalsIgnoreCase("MAXW")) return "MaxWindLevel";
    return units;
  }

  private String makeUnitsName(String units) {
    if (units.equalsIgnoreCase("MB")) return "hPa";
    if (units.equalsIgnoreCase("BL")) return "hPa";
    if (units.equalsIgnoreCase("FHAG")) return "m";
    if (units.equalsIgnoreCase("FH")) return "m";
    return "";
  }

  private String makeLongName(String name) {
    if (name.equalsIgnoreCase("PotTempLevels")) return "Potential Temperature Level";
    if (name.equalsIgnoreCase("BoundaryLayers")) return "BoundaryLayer hectoPascals above ground";
    else return name;
  }

  // create new variables as sections of ncVar
  private void createNewVariables( NetcdfDataset ds, Variable ncVar, List<Dimension> newDims,
     Dimension levelDim) throws InvalidRangeException {

    List<Dimension> dims = ncVar.getDimensions();
    int newDimIndex = dims.indexOf(levelDim);
    //String shapeS = ncVar.getShapeS();

    int[] origin = new int[ncVar.getRank()];
    int[] shape = ncVar.getShape();
    int count = 0;
    for (Dimension dim : newDims) {
      String name = ncVar.getName() + "-" + dim.getName();

      origin[newDimIndex] = count;
      shape[newDimIndex] = dim.getLength();

      Variable varNew = ncVar.section(new Section(origin, shape));
      varNew.setName(name);
      varNew.setDimension(newDimIndex, dim);

      // synthesize long name
      String long_name = ds.findAttValueIgnoreCase(ncVar, "long_name", ncVar.getName());
      long_name = long_name + "-" + dim.getName();
      ds.addVariableAttribute(varNew, new Attribute("long_name", long_name));

      ds.addVariable(null, varNew);

      parseInfo.append("Created New Variable as section = ");
      varNew.getNameAndDimensions(parseInfo, true, false);
      parseInfo.append("\n");

      count += dim.getLength();
    }
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

  private String getZisPositive( NetcdfDataset ds, CoordinateAxis v) {

    String attValue = ds.findAttValueIgnoreCase(v, "positive", null);
    if (null != attValue)
      return attValue.equalsIgnoreCase("up") ? "up" : "down";

    String unit = v.getUnitsString();
    if ((unit != null) && SimpleUnit.isCompatible("millibar", unit))
      return "down";
    if ((unit != null) && SimpleUnit.isCompatible("m", unit))
      return "up";

      // dunno
    return null;
  }

  private ProjectionCT makeLCProjection(NetcdfDataset ds, String name) throws NoSuchElementException {
    double centralLat = findAttributeDouble( ds, "centralLat");
    double centralLon = findAttributeDouble( ds, "centralLon");
    double rotation = findAttributeDouble( ds, "rotation");

    // we have to project in order to find the origin
    LambertConformal lc = new LambertConformal(rotation, centralLon, centralLat, centralLat);
    double lat0 = findAttributeDouble( ds, "lat00");
    double lon0 = findAttributeDouble( ds, "lon00");
    ProjectionPointImpl start = (ProjectionPointImpl) lc.latLonToProj( new LatLonPointImpl( lat0, lon0));
    if (debugProj) parseInfo.append("getLCProjection start at proj coord ").append(start).append("\n");
    startx = start.getX();
    starty = start.getY();

    return new ProjectionCT(name, "FGDC", lc);
  }

  private ProjectionCT makeStereoProjection(NetcdfDataset ds, String name) throws NoSuchElementException {
    double centralLat = findAttributeDouble( ds, "centralLat");
    double centralLon = findAttributeDouble( ds, "centralLon");
    double rotation = findAttributeDouble( ds, "rotation");

    // scale factor at lat = k = 2*k0/(1+sin(lat))  [Snyder,Working Manual p157]
    // then to make scale = 1 at lat, k0 = (1+sin(lat))/2
    double latDxDy = findAttributeDouble( ds, "latDxDy");
    double latR = Math.toRadians( latDxDy);
    double scale = (1.0 + Math.sin( latR)) / 2;

    // Stereographic(double latt, double lont, double scale)

    Stereographic proj = new Stereographic(centralLat, centralLon, scale);
    // we have to project in order to find the origin
    double lat0 = findAttributeDouble( ds, "lat00");
    double lon0 = findAttributeDouble( ds, "lon00");
    ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj( new LatLonPointImpl( lat0, lon0));
    startx = start.getX();
    starty = start.getY();

    // projection info
    parseInfo.append("---makeStereoProjection start at proj coord ").append(start).append("\n");

    double latN = findAttributeDouble( ds, "latNxNy");
    double lonN = findAttributeDouble( ds, "lonNxNy");
    ProjectionPointImpl pt = (ProjectionPointImpl) proj.latLonToProj( new LatLonPointImpl( latN, lonN));
    parseInfo.append("                        end at proj coord ").append(pt).append("\n");
    parseInfo.append("                        scale= ").append(scale).append("\n");

    return new ProjectionCT(name, "FGDC", proj);
  }

  private CoordinateAxis makeXCoordAxis(NetcdfDataset ds, int nx, String xname) {
    double dx = findAttributeDouble(ds, "dxKm");
    CoordinateAxis v = new CoordinateAxis1D(ds, null, xname, DataType.DOUBLE, xname, "km", "x on projection");
    ds.setValues(v, nx, startx, dx);

    parseInfo.append("Created X Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.append("\n");

    return v;
  }

  private CoordinateAxis makeYCoordAxis( NetcdfDataset ds, int ny, String yname) {
    double dy = findAttributeDouble( ds, "dyKm");
    CoordinateAxis v = new CoordinateAxis1D( ds, null, yname, DataType.DOUBLE, yname, "km", "y on projection");
    ds.setValues( v, ny, starty, dy);

    parseInfo.append("Created Y Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.append("\n");

    return v;
  }

  private CoordinateAxis makeLonCoordAxis(NetcdfDataset ds, int n, String xname) {
    double min = findAttributeDouble(ds, "xMin");
    double max = findAttributeDouble(ds, "xMax");
    double d = findAttributeDouble(ds, "dx");
    CoordinateAxis v = new CoordinateAxis1D(ds, null, xname, DataType.DOUBLE, xname, "degrees_east", "longitude");
    ds.setValues(v, n, min, d);
    v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

    double maxCalc = min + d * n;
    parseInfo.append("Created Lon Coordinate Axis (max calc= ").append(maxCalc).append(" should be ").append(max).append(") ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.append("\n");

    return v;
  }

  private CoordinateAxis makeLatCoordAxis(NetcdfDataset ds, int n, String xname) {
    double min = findAttributeDouble(ds, "yMin");
    double max = findAttributeDouble(ds, "yMax");
    double d = findAttributeDouble(ds, "dy");
    CoordinateAxis v = new CoordinateAxis1D(ds, null, xname, DataType.DOUBLE, xname, "degrees_north", "latitude");
    ds.setValues(v, n, min, d);
    v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

    double maxCalc = min + d * n;
    parseInfo.append("Created Lat Coordinate Axis (max calc= ").append(maxCalc).append(" should be ").append(max).append(") ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.append("\n");

    return v;
  }

  private CoordinateAxis makeTimeCoordAxis( NetcdfDataset ds) {
    Variable timeVar = ds.findVariable("valtimeMINUSreftime");
    Dimension recordDim = ds.findDimension("record");
    Array vals;

    try {
      vals = timeVar.read();
    } catch (IOException ioe) {
      return null;
    }

    // it seems that the record dimension does not always match valtimeMINUSreftime dimension!!
    // HAHAHAHAHAHAHAHA !
    int recLen = recordDim.getLength();
    int valLen = (int) vals.getSize();
    if (recLen != valLen) {
      try {
        vals = vals.sectionNoReduce(new int[] {0}, new int[] {recordDim.getLength()}, null);
        parseInfo.append(" corrected the TimeCoordAxis length\n");
      } catch (InvalidRangeException e) {
        parseInfo.append("makeTimeCoordAxis InvalidRangeException\n");
      }
    }

    // create the units out of the filename if possible
    String units = makeTimeUnitFromFilename(ds.getLocation());
    if (units == null) // ok that didnt work, try something else
      return makeTimeCoordAxisFromReference(ds, timeVar, vals);

    // create the coord axis
    String desc = "synthesized time coordinate from valtimeMINUSreftime and filename YYYYMMDD_HHMM";
    CoordinateAxis1D timeCoord = new CoordinateAxis1D( ds, null, "timeCoord", DataType.INT, "record", units, desc);

    timeCoord.setCachedData( vals, true);

    parseInfo.append("Created Time Coordinate Axis = ");
    timeCoord.getNameAndDimensions(parseInfo, true, false);
    parseInfo.append("\n");

    return timeCoord;
  }

  private String makeTimeUnitFromFilename( String dsName) {
    dsName = dsName.replace('\\','/');

    // posFirst: last '/' if it exists
    int posFirst = dsName.lastIndexOf('/');
    if (posFirst < 0) posFirst = 0;

     // posLast: next '.' if it exists
     int posLast = dsName.indexOf(".", posFirst);
     if (posLast < 0)
       dsName = dsName.substring(posFirst+1);
    else
       dsName = dsName.substring(posFirst+1, posLast);

    // gotta be YYYYMMDD_HHMM
    if (dsName.length() != 13)
      return null;

    String year = dsName.substring(0,4);
    String mon = dsName.substring(4,6);
    String day = dsName.substring(6,8);
    String hour = dsName.substring(9,11);
    String min = dsName.substring(11,13);

    return "seconds since "+year+"-"+mon+"-"+day+" "+hour+":"+min+":0";
  }

  // construct time coordinate from reftime variable
  private CoordinateAxis makeTimeCoordAxisFromReference( NetcdfDataset ds, Variable timeVar, Array vals) {
    Variable refVar = ds.findVariable("reftime");
    if (refVar == null) return null;
    double refValue;
    try {
      Array refArray = refVar.read();
      refValue = refArray.getDouble(refArray.getIndex()); // get the first value
    } catch (IOException ioe) {
      return null;
    }
    // construct the values array - make it a double to be safe
    Array dvals = Array.factory(double.class, vals.getShape());
    IndexIterator diter = dvals.getIndexIterator();
    IndexIterator iiter = vals.getIndexIterator();
    while (iiter.hasNext())
      diter.setDoubleNext( iiter.getDoubleNext() + refValue); // add reftime to each of the values

    String units = ds.findAttValueIgnoreCase(refVar, "units", "seconds since 1970-1-1 00:00:00");
    units = normalize(units);
    String desc = "synthesized time coordinate from reftime, valtimeMINUSreftime";
    CoordinateAxis1D timeCoord = new CoordinateAxis1D( ds, null, "timeCoord", DataType.DOUBLE, "record", units, desc);

    timeCoord.setCachedData( dvals, true);

    parseInfo.append("Created Time Coordinate Axis From Reference = ");
    timeCoord.getNameAndDimensions(parseInfo, true, false);
    parseInfo.append("\n");

    return timeCoord;
  }

  private double findAttributeDouble( NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    return att.getNumericValue().doubleValue();
  }


}

