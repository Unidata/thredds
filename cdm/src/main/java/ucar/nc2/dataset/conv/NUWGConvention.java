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
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil;
import ucar.nc2.units.SimpleUnit;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import java.io.IOException;
import java.util.*;

/**
 * NUWG Convention (ad hoc).
 * see http://www.unidata.ucar.edu/packages/netcdf/NUWG/
 *
 * @author caron
 */

public class NUWGConvention extends CoordSysBuilder {
  private NavInfoList navInfo = new NavInfoList();
  private String xaxisName = "", yaxisName = "";
  private Grib1 grib;

  private final boolean dumpNav = false;

  public NUWGConvention() {
    this.conventionName = "NUWG";
  }

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) {
    if (null != ds.findGlobalAttribute("_enhanced")) return; // check if its already been done - aggregating enhanced datasets.
    ds.addAttribute(null, new Attribute("_enhanced", "")); // LOOK
    
    // find all variables that have the nav dimension
    // put them into a NavInfoList
    // make their data into metadata
    List<Variable> vars = ds.getVariables();
    for (Variable v : vars) {
      if (0 <= v.findDimensionIndex("nav")) {

        if (dumpNav) parseInfo.format("NUWG has NAV var = %s\n", v);
        try {
          navInfo.add(new NavInfo(v));
        } catch (IOException ex) {
          parseInfo.format("ERROR NUWG reading NAV var = %s\n", v);
        }
      }
    }
    java.util.Collections.sort( navInfo, new NavComparator());
    parseInfo.format("%s\n\n", navInfo);

    // is this pathetic or what ?
    // problem is NUWG doesnt identify the x, y coords.
    // so we get to hack it in here
    int mode = 3; // default is LambertConformal
    try {
      mode = navInfo.getInt( "grid_type_code");
    } catch (NoSuchElementException e) {
      log.warn("No mode in navInfo - assume 3");
    }

    try {
     if (mode == 0) {
        xaxisName = navInfo.getString( "i_dim");
        yaxisName = navInfo.getString( "j_dim");
      } else {
        xaxisName = navInfo.getString( "x_dim");
        yaxisName = navInfo.getString( "y_dim");
      }
    } catch (NoSuchElementException e) {
      log.warn("No mode in navInfo - assume = 1");
      // could match variable grid_type, data = "tangential lambert conformal  "
    }
    grib = new Grib1( mode);

    if (null == ds.findVariable( xaxisName)) {
      grib.makeXCoordAxis( ds, xaxisName);
      parseInfo.format("Generated x axis from NUWG nav= %s\n", xaxisName);

    } else if (xaxisName.equalsIgnoreCase("lon")) {

      try {
          // check monotonicity
        boolean ok = true;
        Variable dc = ds.findVariable( xaxisName);
        Array coordVal = dc.read();
        IndexIterator coordIndex = coordVal.getIndexIterator();

        double coord1 = coordIndex.getDoubleNext();
        double coord2 = coordIndex.getDoubleNext();
        boolean increase = coord1 > coord2;
        coord1 = coord2;
        while (coordIndex.hasNext()) {
          coord2 = coordIndex.getDoubleNext();
          if ((coord1 > coord2) ^ increase) {
            ok = false;
            break;
          }
          coord1 = coord2;
        }

        if (!ok) {
          parseInfo.format( "ERROR lon axis is not monotonic, regen from nav\n");
          grib.makeXCoordAxis( ds, xaxisName);
        }
      } catch (IOException ioe) {
        log.warn("IOException when reading xaxis = "+xaxisName);
      }
    }

    if (null == ds.findVariable( yaxisName)) {
      grib.makeYCoordAxis( ds, yaxisName);
      parseInfo.format("Generated y axis from NUWG nav=%s\n", yaxisName);
    }

      // "referential" variables
    List<Dimension> dims = ds.getRootGroup().getDimensions();
    for (Dimension dim : dims) {
      String dimName = dim.getName();
      if (null != ds.findVariable( dimName)) // already has a coord axis
        continue;
      List<Variable> ncvars = searchAliasedDimension( ds, dim);
      if ((ncvars == null) || (ncvars.size() == 0)) // no alias
          continue;

      if (ncvars.size() == 1) {
        Variable ncvar = ncvars.get(0);
        if (!(ncvar instanceof VariableDS))
          continue; // cant be a structure
        if (makeCoordinateAxis( ncvar, dim)) {
          parseInfo.format("Added referential coordAxis = ");
          ncvar.getNameAndDimensions(parseInfo, true, false);
          parseInfo.format("\n");
        } else {
          parseInfo.format("Couldnt add referential coordAxis = %s\n", ncvar.getName());
        }

      } else if (ncvars.size() == 2) {

        if (dimName.equals("record")) {
          Variable ncvar0 = ncvars.get(0);
          Variable ncvar1 = ncvars.get(1);
          Variable ncvar = ncvar0.getName().equalsIgnoreCase("valtime") ? ncvar0 : ncvar1;

          if (makeCoordinateAxis( ncvar, dim)) {
            parseInfo.format("Added referential coordAxis (2) = ");
            ncvar.getNameAndDimensions(parseInfo, true, false);
            parseInfo.format("\n");

            // the usual crap - clean up time units
            String units = ncvar.getUnitsString();
            if (units != null) {
              units = StringUtil.remove(units, '(');
              units = StringUtil.remove(units, ')');
              ncvar.addAttribute(new Attribute("units", units));
            }
          } else {
            parseInfo.format("Couldnt add referential coordAxis = %s\n", ncvar.getName());
          }

          //ncvar = (Variable) ncvars.get(0);
          //CoordinateAxis refTime = ds.addCoordinateAxis( ncvar);
          //vaidTime.setAuxilary( refTime);
          //parseInfo.append("Added referential record coordAxis = "+ncvar.getNameAndDimensions()+"\n");

        } else {
          // lower(?) bound
          Variable ncvar = ncvars.get(0);
          if (!(ncvar instanceof VariableDS)) continue; // cant be a structure

          if (makeCoordinateAxis( ncvar, dim)) {
            parseInfo.format("Added referential boundary coordAxis (2) = ");
            ncvar.getNameAndDimensions(parseInfo, true, false);
            parseInfo.format("\n");
          } else {
            parseInfo.format("Couldnt add referential coordAxis = %s\n", ncvar.getName());
          }

          /*  CoordinateAxis bound1 = ds.addCoordinateAxis( (VariableDS) ncvar);
          parseInfo.append("Added referential boundary coordAxis = ");
          ncvar.getNameAndDimensions(parseInfo, true, false);
          parseInfo.append("\n");

           // upper(?) bound
          ncvar = (Variable) ncvars.get(1);
          if (!(ncvar instanceof VariableDS)) continue; // cant be a structure
          CoordinateAxis bound2 = ds.addCoordinateAxis( (VariableDS) ncvar);
          //bound1.setAuxilary( bound2);

          parseInfo.append("Added referential boundary coordAxis = ");
          ncvar.getNameAndDimensions(parseInfo, true, false);
          parseInfo.append("\n");

          /* DimCoordAxis dc = addCoordAxisFromTopBotVars( dim, (Variable) ncvars.get(0),
              (Variable) ncvars.get(1));
          if (null != dc)
            dc.isReferential = true; */
        }
      } // 2
    } // loop over dims

    if (grib.ct != null) {
      VariableDS v = makeCoordinateTransformVariable(ds, grib.ct);
      v.addAttribute( new Attribute(_Coordinate.Axes, xaxisName+" "+yaxisName));
      ds.addVariable(null, v);
    }

    ds.finish();
  }

  private boolean makeCoordinateAxis( Variable ncvar, Dimension dim) {
    if (ncvar.getRank() != 1)
      return false;
    Dimension vdim = ncvar.getDimension(0);
    if (!vdim.equals(dim))
      return false;

    if (!dim.getName().equals(ncvar.getShortName())) {
      ncvar.addAttribute( new Attribute(_Coordinate.AliasForDimension, dim.getName()));

    }

    /* if (dim.getCoordinateVariables().size() == 1) {
      dim.addCoordinateVariable( ncvar);
      ncvar.setIsCoordinateAxis( true);
    } */
    return true;
  }

  /* private int getDimensionIndex( Variable v, String dimName) {
    Iterator iter = v.getDimensions().iterator();
    int count = 0;
    while (iter.hasNext()) {
      Dimension dim = (Dimension) iter.next();
      if (dimName.equalsIgnoreCase( dim.getName()))
        return count;
      count++;
    }
    return -1;
  } */

  /** Search for an aliased coord that may have multiple variables
   *   :dimName = alias1, alias2;
   *   Variable alias1(dim);
   *   Variable alias2(dim);
   * @param ds search in this dataset
   * @param dim: look for this dimension name
   * @return Collection of nectdf variables, or null if none
   */
  private List<Variable> searchAliasedDimension( NetcdfDataset ds, Dimension dim) {
    String dimName = dim.getName();
    String alias = ds.findAttValueIgnoreCase(null, dimName, null);
    if (alias == null)
      return null;

    List<Variable> vars = new ArrayList<Variable>();
    StringTokenizer parser = new StringTokenizer(alias, " ,");
    while (parser.hasMoreTokens()) {
      String token = parser.nextToken();
      Variable ncvar = ds.findVariable( token);
      if (ncvar == null)
        continue;
      if (ncvar.getRank() != 1)
        continue;
      Iterator dimIter = ncvar.getDimensions().iterator();
      Dimension dim2 = (Dimension) dimIter.next();
      if (dimName.equals(dim2.getName())) {
        vars.add(ncvar);
        if (debug) System.out.print(" "+token);
      }
    }
    if (debug) System.out.println();

    return vars;
  }

  private StringBuilder buf = new StringBuilder(2000);
  public String extraInfo() {
    buf.setLength(0);
    buf.append(navInfo).append("\n");
    return buf.toString();
  }


  protected void makeCoordinateTransforms( NetcdfDataset ds) {
    if ((grib != null) && (grib.ct != null)) {
      VarProcess vp = findVarProcess(grib.ct.getName());
      vp.isCoordinateTransform = true;
      vp.ct = grib.ct;
    }
    super.makeCoordinateTransforms( ds);
  }

  protected AxisType getAxisType( NetcdfDataset ds, VariableEnhanced ve) {
    Variable v = (Variable) ve;
    String vname = v.getName();

    if (vname.equalsIgnoreCase("lat"))
      return AxisType.Lat;

    if (vname.equalsIgnoreCase("lon"))
      return AxisType.Lon;

    if (vname.equalsIgnoreCase(xaxisName))
      return AxisType.GeoX;

    if (vname.equalsIgnoreCase(yaxisName))
      return AxisType.GeoY;

    if (vname.equalsIgnoreCase("record"))
      return AxisType.Time;
    Dimension dim = v.getDimension(0);
    if ((dim != null) && dim.getName().equalsIgnoreCase("record")) { // wow thats bad!
      return AxisType.Time;
    }

    String unit = ve.getUnitsString();
    if (unit != null) {
      if ( SimpleUnit.isCompatible("millibar", unit))
        return AxisType.Pressure;

      if ( SimpleUnit.isCompatible("m", unit))
        return AxisType.Height;

      if ( SimpleUnit.isCompatible("sec", unit))
        return null;     
    }

    return AxisType.GeoZ; // AxisType.GeoZ;
  }

  /**  @return "up" if this is a Vertical (z) coordinate axis which goes up as coords get bigger
   * @param v for this axis
   */
  public String getZisPositive( CoordinateAxis v) {

     // gotta have a length unit
    String unit = v.getUnitsString();
    if ((unit != null) && SimpleUnit.isCompatible("m", unit))
      return "up";

    return "down";

    // lame NUWG COnventions! units of millibar might be "millibars above ground" !
    // heres a kludge that should work
    // return v.getName().equalsIgnoreCase("fhg") ? "up" : "down";
  }

  private class NavComparator implements java.util.Comparator<NavInfo> {
    public int compare(NavInfo n1, NavInfo n2) {
      return n1.getName().compareTo( n2.getName());
    }
    public boolean equals(Object obj) {
      return (this == obj);
    }
  }

  private class NavInfo {
    Variable ncvar;
    DataType valueType;
    String svalue;
    byte bvalue;
    int ivalue;
    double dvalue;

    public NavInfo( Variable ncvar) throws IOException {
      this.ncvar = ncvar;
      valueType = ncvar.getDataType();
      try {
        if ((valueType == DataType.CHAR) || (valueType == DataType.STRING))
          svalue =  ncvar.readScalarString();
        else if (valueType == DataType.BYTE)
          bvalue = ncvar.readScalarByte();
        else if ((valueType == DataType.INT) || (valueType == DataType.SHORT))
          ivalue = ncvar.readScalarInt();
        else
          dvalue = ncvar.readScalarDouble();
      } catch (java.lang.UnsupportedOperationException e) {
        parseInfo.format("Nav variable %s  not a scalar\n", getName());
      }
      //List<String> values = new ArrayList<String>();
      //values.add( getStringValue());
      // ncDataset.setValues( ncvar, values); // WHY?
    }

    public String getName() { return ncvar.getName(); }
    public String getDescription() {
      Attribute att = ncvar.findAttributeIgnoreCase("long_name");
      return (att == null) ? getName() : att.getStringValue();
    }
    public DataType getValueType() { return valueType; }

    public String getStringValue(){
      if ((valueType == DataType.CHAR) || (valueType == DataType.STRING))
        return svalue;
      else if (valueType == DataType.BYTE)
        return Byte.toString(bvalue);
      else if ((valueType == DataType.INT) || (valueType == DataType.SHORT))
        return Integer.toString(ivalue);
      else
        return Double.toString(dvalue);
    }

    private StringBuilder buf = new StringBuilder(200);
    public String toString() {
      buf.setLength(0);
      buf.append(getName());
      buf.append(" ");
      Format.tab(buf, 15, true);
      buf.append(getStringValue());
      buf.append(" ");
      Format.tab(buf, 35, true);
      buf.append(getDescription());
      return buf.toString();
    }
  }

  private class NavInfoList extends ArrayList<NavInfo> {

    public NavInfo findInfo( String name) {
      Iterator iter = iterator();
      while (iter.hasNext()) {
        NavInfo nav = (NavInfo) iter.next();
        if (name.equalsIgnoreCase(nav.getName()))
          return nav;
      }
      return null;
    }

    public double getDouble( String name) throws NoSuchElementException {
      NavInfo nav = findInfo( name);
      if (nav == null)
        throw new NoSuchElementException("GRIB1 "+name);

      if ((nav.valueType == DataType.DOUBLE) || (nav.valueType == DataType.FLOAT))
        return nav.dvalue;
      else if ((nav.valueType == DataType.INT) || (nav.valueType == DataType.SHORT))
        return (double) nav.ivalue;
      else if (nav.valueType == DataType.BYTE)
        return (double)  nav.bvalue;

      throw new IllegalArgumentException("NUWGConvention.GRIB1.getDouble "+name+" type = "+nav.valueType);
    }

    public int getInt( String name) throws NoSuchElementException {
      NavInfo nav = findInfo( name);
      if (nav == null)
        throw new NoSuchElementException("GRIB1 "+name);

      if ((nav.valueType == DataType.INT) || (nav.valueType == DataType.SHORT))
        return nav.ivalue;
      else if ((nav.valueType == DataType.DOUBLE) || (nav.valueType == DataType.FLOAT))
        return (int) nav.dvalue;
      else if (nav.valueType == DataType.BYTE)
        return (int)  nav.bvalue;

      throw new IllegalArgumentException("NUWGConvention.GRIB1.getInt "+name+" type = "+nav.valueType);
    }

    public String getString( String name) throws NoSuchElementException {
      NavInfo nav = findInfo( name);
      if (nav == null)
        throw new NoSuchElementException("GRIB1 "+name);
      return nav.svalue;
    }

    private StringBuilder buf = new StringBuilder(2000);
    public String toString() {
      buf.setLength(0);
      buf.append("\nNav Info\n");
      buf.append("Name___________Value_____________________Description\n");
      Iterator iter = iterator();
      while (iter.hasNext()) {
        NavInfo nava = (NavInfo) iter.next();
        buf.append(nava).append("\n");
      }
      buf.append("\n");
      return buf.toString();
    }

  }

  // encapsolates GRIB-specific processing
  private class Grib1 {
    private String grid_name;
    private int grid_code = 0;
    private ProjectionCT ct;

    private  int nx, ny;
    private double startx, starty;
    private double dx, dy;

    Grib1( int mode) {
      // horiz system
      grid_name = "Projection";
      if (grid_name.length() == 0) grid_name = "grid_var";

      grid_code = mode;
      if (0 == grid_code)
         processLatLonProjection();
       else if (3 == grid_code)
         ct = makeLCProjection();
       else if (5 == grid_code)
         ct = makePSProjection();
       else
         throw new IllegalArgumentException("NUWGConvention: unknown grid_code= "+grid_code);

      // vertical system
    }

    CoordinateAxis makeXCoordAxis( NetcdfDataset ds, String xname) {
      CoordinateAxis v = new CoordinateAxis1D( ds, null, xname, DataType.DOUBLE, xname,
          (0 == grid_code) ? "degrees_east" : "km", "synthesized X coord");
      v.addAttribute( new Attribute( _Coordinate.AxisType, (0 == grid_code) ? AxisType.Lon.toString() : AxisType.GeoX.toString()));
      ds.setValues( v, nx, startx, dx);
      ds.addCoordinateAxis( v);
      return v;
    }

    CoordinateAxis makeYCoordAxis( NetcdfDataset ds, String yname) {
      CoordinateAxis v = new CoordinateAxis1D( ds, null, yname, DataType.DOUBLE, yname,
            ((0 == grid_code) ? "degrees_north" : "km"), "synthesized Y coord");
      v.addAttribute( new Attribute( _Coordinate.AxisType, (0 == grid_code) ? AxisType.Lat.toString() : AxisType.GeoY.toString()));
      ds.setValues( v, ny, starty, dy);
      ds.addCoordinateAxis( v);
      return v;
    }

    private ProjectionCT makeLCProjection() throws NoSuchElementException {
      double latin1 = navInfo.getDouble( "Latin1");
      double latin2 = navInfo.getDouble( "Latin2");
      double lov = navInfo.getDouble( "Lov");
      double la1 = navInfo.getDouble( "La1");
      double lo1 = navInfo.getDouble( "Lo1");

      // we have to project in order to find the origin
      LambertConformal lc = new LambertConformal(latin1, lov, latin1, latin2);
      ProjectionPointImpl start = (ProjectionPointImpl) lc.latLonToProj( new LatLonPointImpl( la1, lo1));
      if (debug) System.out.println("start at proj coord "+start);
      startx = start.getX();
      starty = start.getY();

      nx = navInfo.getInt( "Nx");
      ny = navInfo.getInt( "Ny");
      dx = navInfo.getDouble( "Dx")/1000.0; // need to be km : unit conversion LOOK;
      dy = navInfo.getDouble( "Dy")/1000.0; // need to be km : unit conversion LOOK;

      return new ProjectionCT(grid_name, "FGDC", lc);
    }

    // polar stereographic
    private ProjectionCT makePSProjection() throws NoSuchElementException {
      double lov = navInfo.getDouble( "Lov");
      double la1 = navInfo.getDouble( "La1");
      double lo1 = navInfo.getDouble( "Lo1");

      // Why the scale factor?. accordining to GRID docs:
      // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
      // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60))  [Snyder,Working Manual p157]
      // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
      Stereographic ps = new Stereographic(90.0, lov, .933);

      // we have to project in order to find the origin
      ProjectionPointImpl start = (ProjectionPointImpl) ps.latLonToProj( new LatLonPointImpl( la1, lo1));
      if (debug) System.out.println("start at proj coord "+start);
      startx = start.getX();
      starty = start.getY();

      nx = navInfo.getInt( "Nx");
      ny = navInfo.getInt( "Ny");
      dx = navInfo.getDouble( "Dx")/1000.0;
      dy = navInfo.getDouble( "Dy")/1000.0;

      return new ProjectionCT(grid_name, "FGDC", ps);
    }

    private void processLatLonProjection() throws NoSuchElementException {
        // get stuff we need to construct axes
      starty = navInfo.getDouble( "La1");
      startx = navInfo.getDouble( "Lo1");
      nx = navInfo.getInt( "Ni");
      ny = navInfo.getInt( "Nj");
      dx = navInfo.getDouble( "Di");
      dy = navInfo.getDouble( "Dj");
    }

  } // GRIB1 */

}

/*

  private void showPoint( int ix, int iy) {
    ProjectionPointImpl pt = new ProjectionPointImpl(xaxis.getCoordValue(ix), yaxis.getCoordValue(iy));
    if (debugPoint) System.out.println( ix+ " "+iy+" "+pt+ " --> " +proj.projToLatLon(pt));
  }

  private void showProj( double lat, double lon) {
    LatLonPointImpl llpt = new LatLonPointImpl(lat,lon);
    if (debugProj) System.out.println( llpt+ " --> " +proj.latLonToProj(llpt));
  }

  private void showLat( double x, double y) {
    ProjectionPointImpl pt = new ProjectionPointImpl(x, y);
    LatLonPoint llpt = proj.projToLatLon(pt);
    if (debugPoint) System.out.println( pt+ " --> " +llpt.getLatitude()+" "+llpt.getLongitude());
  }

*/
