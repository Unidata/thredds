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
  private final boolean debugBreakup = true;

  private List<Variable> mungedList = new ArrayList<Variable>();
  private ProjectionCT projCT = null;
  private double startx, starty;

  public AWIPSConvention() {
    this.conventionName = "AWIPS";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) {
    if (null != ds.findVariable("x")) return; // check if its already been done - aggregating enhanced datasets.

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

      try {
        List<Dimension> levels = breakupLevels(ds, levelVar);
        createNewVariables(ds, ncvar, levels, levelVar.getDimension(0));
      } catch (InvalidRangeException ex) {
        parseInfo.format("createNewVariables InvalidRangeException\n");
      } catch (IOException ioe) {
        parseInfo.format("createNewVariables IOException\n");
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

  // LOOK not dealing with "FHAG 0 10 ", "FHAG 0 30 "
  // take a combined level variable and create multiple levels out of it
  // return the list of Dimensions that were created
  private List<Dimension> breakupLevels( NetcdfDataset ds, Variable levelVar) throws IOException {
    if (debugBreakup) parseInfo.format("breakupLevels = %s\n", levelVar.getName());
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
      if (debugBreakup) parseInfo.format("   %s\n", s);
      StringTokenizer stoke = new StringTokenizer(s);

/* problem with blank string:
   char pvvLevels(levels_35=35, charsPerLevel=10);
"MB 1000   ", "MB 975    ", "MB 950    ", "MB 925    ", "MB 900    ", "MB 875    ", "MB 850    ", "MB 825    ", "MB 800    ", "MB 775    ", "MB 750    ",
"MB 725    ", "MB 700    ", "MB 675    ", "MB 650    ", "MB 625    ", "MB 600    ", "MB 575    ", "MB 550    ", "MB 525    ", "MB 500    ", "MB 450    ",
"MB 400    ", "MB 350    ", "MB 300    ", "MB 250    ", "MB 200    ", "MB 150    ", "MB 100    ", "BL 0 30   ", "BL 60 90  ", "BL 90 120 ", "BL 120 150",
"BL 150 180", ""
*/
      if (!stoke.hasMoreTokens())
        continue; // skip it

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

    if (debugBreakup) parseInfo.format("  done breakup\n");

    return dimList;
  }

  // make a new variable out of the list in "values"
  private Dimension makeZCoordAxis( NetcdfDataset ds, List<String> values, String units) throws IOException {
    int len = values.size();
    String name = makeZCoordName( units);
    if (len > 1)
      name = name + Integer.toString(len);
    else
      name = name + values.get(0);
    StringUtil.replace(  name, ' ', "-");

    Dimension dim;
    if (null != (dim = ds.getRootGroup().findDimension(name))) {
      if (dim.getLength() == len) {
        // check against actual values
        Variable coord = ds.getRootGroup().findVariable(name);
        Array coordData = coord.read();
        Array newData = Array.makeArray(coord.getDataType(), values);
        if (MAMath.isEqual(coordData, newData)) {
          if (debugBreakup) parseInfo.format("  use existing coord %s\n", dim);
          return dim;
        }
      }
    }

    String orgName = name;
    int count = 1;
    while (ds.getRootGroup().findDimension(name) != null) {
      name = orgName + "-"+count;
      count++;
    }

    // create new one
    dim = new Dimension(name, len);
    ds.addDimension( null, dim);
    if (debugBreakup) parseInfo.format("  make Dimension = %s length = %d\n", name, len);

    // if (len < 2) return dim; // skip 1D

    if (debugBreakup) {
      parseInfo.format("  make ZCoordAxis = = %s length = %d\n", name, len);
    }

    CoordinateAxis v = new CoordinateAxis1D( ds, null, name, DataType.DOUBLE, name,
       makeUnitsName( units), makeLongName(name));
    String positive = getZisPositive( ds, v);
    if (null != positive)
      v.addAttribute( new Attribute(_Coordinate.ZisPositive, positive));

    ds.setValues( v, values);
    ds.addCoordinateAxis(v);

    parseInfo.format("Created Z Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("\n");

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

      parseInfo.format("Created New Variable as section = ");
      varNew.getNameAndDimensions(parseInfo, true, false);
      parseInfo.format("\n");

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
    if (debugProj) parseInfo.format("getLCProjection start at proj coord %s\n",start);
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
    double scale = (1.0 + Math.abs(Math.sin( latR))) / 2;  // thanks to R Schmunk

    // Stereographic(double latt, double lont, double scale)

    Stereographic proj = new Stereographic(centralLat, centralLon, scale);
    // we have to project in order to find the origin
    double lat0 = findAttributeDouble( ds, "lat00");
    double lon0 = findAttributeDouble( ds, "lon00");
    ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj( new LatLonPointImpl( lat0, lon0));
    startx = start.getX();
    starty = start.getY();

    // projection info
    parseInfo.format("---makeStereoProjection start at proj coord %s\n", start);

    double latN = findAttributeDouble( ds, "latNxNy");
    double lonN = findAttributeDouble( ds, "lonNxNy");
    ProjectionPointImpl pt = (ProjectionPointImpl) proj.latLonToProj( new LatLonPointImpl( latN, lonN));
    parseInfo.format("                        end at proj coord %s\n", pt);
    parseInfo.format("                        scale= %f\n", scale);

    return new ProjectionCT(name, "FGDC", proj);
  }

  private CoordinateAxis makeXCoordAxis(NetcdfDataset ds, int nx, String xname) {
    double dx = findAttributeDouble(ds, "dxKm");
    CoordinateAxis v = new CoordinateAxis1D(ds, null, xname, DataType.DOUBLE, xname, "km", "x on projection");
    ds.setValues(v, nx, startx, dx);

    parseInfo.format("Created X Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("\n");

    return v;
  }

  private CoordinateAxis makeYCoordAxis( NetcdfDataset ds, int ny, String yname) {
    double dy = findAttributeDouble( ds, "dyKm");
    CoordinateAxis v = new CoordinateAxis1D( ds, null, yname, DataType.DOUBLE, yname, "km", "y on projection");
    ds.setValues( v, ny, starty, dy);

    parseInfo.format("Created Y Coordinate Axis = ");
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("\n");

    return v;
  }

  private CoordinateAxis makeLonCoordAxis(NetcdfDataset ds, int n, String xname) {
    double min = findAttributeDouble(ds, "xMin");
    double max = findAttributeDouble(ds, "xMax");
    double d = findAttributeDouble(ds, "dx");
    if (Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(d)) return null;

    CoordinateAxis v = new CoordinateAxis1D(ds, null, xname, DataType.DOUBLE, xname, "degrees_east", "longitude");
    ds.setValues(v, n, min, d);
    v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lon.toString()));

    double maxCalc = min + d * n;
    parseInfo.format("Created Lon Coordinate Axis (max calc= %f shoule be = %f)\n", maxCalc, max);
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("\n");

    return v;
  }

  private CoordinateAxis makeLatCoordAxis(NetcdfDataset ds, int n, String xname) {
    double min = findAttributeDouble(ds, "yMin");
    double max = findAttributeDouble(ds, "yMax");
    double d = findAttributeDouble(ds, "dy");
    if (Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(d)) return null;

    CoordinateAxis v = new CoordinateAxis1D(ds, null, xname, DataType.DOUBLE, xname, "degrees_north", "latitude");
    ds.setValues(v, n, min, d);
    v.addAttribute( new Attribute(_Coordinate.AxisType, AxisType.Lat.toString()));

    double maxCalc = min + d * n;
    parseInfo.format("Created Lat Coordinate Axis (max calc= %f should be = %f)\n", maxCalc, max);
    v.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("\n");

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
        parseInfo.format(" corrected the TimeCoordAxis length\n");
      } catch (InvalidRangeException e) {
        parseInfo.format("makeTimeCoordAxis InvalidRangeException\n");
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

    parseInfo.format("Created Time Coordinate Axis = ");
    timeCoord.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("\n");

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

    parseInfo.format("Created Time Coordinate Axis From Reference = ");
    timeCoord.getNameAndDimensions(parseInfo, true, false);
    parseInfo.format("\n");

    return timeCoord;
  }

  private double findAttributeDouble( NetcdfDataset ds, String attname) {
    Attribute att = ds.findGlobalAttributeIgnoreCase(attname);
    if (att == null) {
      parseInfo.format("ERROR cant find attribute= %s\n", attname);
      return Double.NaN;
    }
    return att.getNumericValue().doubleValue();
  }


}

