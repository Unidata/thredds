// $Id: NUWGConvention.java,v 1.21 2005/12/10 00:34:10 caron Exp $
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

package ucar.nc2.dataset.conv;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.unidata.util.Format;
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
 * @version $Revision: 1.21 $ $Date: 2005/12/10 00:34:10 $
 */

public class NUWGConvention extends CoordSysBuilder {
  private NavInfoList navInfo = new NavInfoList();
  private String xaxisName = "", yaxisName = "";
  private Grib1 grib;

  private final boolean debugProj = false, dumpNav = false, debugPoint = false, debug = false;

  public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) {
    this.conventionName = "NUWG";

    // find all variables that have the nav dimension
    // put them into a NavInfoList
    // make their data into metadata
    Iterator vars = ds.getVariables().iterator();
    while (vars.hasNext()) {
      Variable v = (Variable) vars.next();
      if (0 <= v.findDimensionIndex("nav")) {

        if (dumpNav) parseInfo.append("NUWG has NAV var = " + v + "\n");
        try {
          navInfo.add(new NavInfo(v));
        } catch (IOException ex) {
          parseInfo.append("ERROR NUWG reading NAV var = " + v + "\n");
        }
      }
    }
    java.util.Collections.sort( navInfo, new NavComparator());
    parseInfo.append(navInfo+"\n\n");

    // is this pathetic or what ?
    // problem is NUWG doesnt identify the x, y coords.
    // so we get to hack it in here
    int mode = 1; // default is projection coords
    try {
      mode = navInfo.getInt( "grid_type_code");
      if (mode == 0) {
        xaxisName = navInfo.getString( "i_dim");
        yaxisName = navInfo.getString( "j_dim");
      } else {
        xaxisName = navInfo.getString( "x_dim");
        yaxisName = navInfo.getString( "y_dim");
      }
    } catch (NoSuchElementException e) {
    }
    grib = new Grib1( mode);

    if (null == ds.findVariable( xaxisName)) {
      grib.makeXCoordAxis( ds, xaxisName);
      parseInfo.append( "Generated x axis from NUWG nav="+xaxisName+"\n");

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
          parseInfo.append( "ERROR lon axis is not monotonic, regen from nav\n");
          grib.makeXCoordAxis( ds, xaxisName);
        }
      } catch (IOException ioe) {}
    }

    if (null == ds.findVariable( yaxisName)) {
      grib.makeYCoordAxis( ds, yaxisName);
      parseInfo.append( "Generated y axis from NUWG nav="+yaxisName+"\n");
    }

      // "referential" variables
    Iterator dims = ds.getRootGroup().getDimensions().iterator();
    while (dims.hasNext()) {
      Dimension dim = (Dimension) dims.next();
      String dimName = dim.getName();
      if (null != ds.findVariable( dimName)) // already has a coord axis
        continue;
      ArrayList ncvars = searchAliasedDimension( ds, dim);
      if ((ncvars == null) || (ncvars.size() == 0)) // no alias
          continue;

      if (ncvars.size() == 1) {
        Variable ncvar = (Variable) ncvars.get(0);
        if (!(ncvar instanceof VariableDS))
          continue; // cant be a structure
        if (makeCoordinateAxis( ncvar, dim)) {
          parseInfo.append("Added referential coordAxis = ");
          ncvar.getNameAndDimensions(parseInfo, true, false);
          parseInfo.append("\n");
        } else {
          parseInfo.append("Couldnt add referential coordAxis = "+ncvar.getName());
        }

      } else if (ncvars.size() == 2) {

        if (dimName.equals("record")) {
          // LOOK: anything else to do besides hard code it ?
          Variable ncvar = (Variable) ncvars.get(1);
          if (!(ncvar instanceof VariableDS)) continue; // cant be a structure

          if (makeCoordinateAxis( ncvar, dim)) {
            parseInfo.append("Added referential coordAxis (2) = ");
            ncvar.getNameAndDimensions(parseInfo, true, false);
            parseInfo.append("\n");
          } else {
            parseInfo.append("Couldnt add referential coordAxis = "+ncvar.getName());
          }

          //ncvar = (Variable) ncvars.get(0);
          //CoordinateAxis refTime = ds.addCoordinateAxis( ncvar);
          //vaidTime.setAuxilary( refTime);
          //parseInfo.append("Added referential record coordAxis = "+ncvar.getNameAndDimensions()+"\n");

        } else {
          // lower(?) bound
          Variable ncvar = (Variable) ncvars.get(0);
          if (!(ncvar instanceof VariableDS)) continue; // cant be a structure

          if (makeCoordinateAxis( ncvar, dim)) {
            parseInfo.append("Added referential boundary coordAxis (2) = ");
            ncvar.getNameAndDimensions(parseInfo, true, false);
            parseInfo.append("\n");
          } else {
            parseInfo.append("Couldnt add referential coordAxis = "+ncvar.getName());
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
      v.addAttribute( new Attribute("_CoordinateAxes", xaxisName+" "+yaxisName));
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
      ncvar.addAttribute( new Attribute("_CoordinateVariableAlias", dim.getName()));

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
  private ArrayList searchAliasedDimension( NetcdfDataset ds, Dimension dim) {
    String dimName = dim.getName();
    String alias = ds.findAttValueIgnoreCase(null, dimName, null);
    if (alias == null)
      return null;

    ArrayList vars = new ArrayList();
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

  private StringBuffer buf = new StringBuffer(2000);
  public String extraInfo() {
    buf.setLength(0);
    buf.append( navInfo+"\n");
    return buf.toString();
  }


  protected void makeCoordinateTransforms( NetcdfDataset ds) {
    if (grib.ct != null) {
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
    if ((dim != null) && dim.getName().equalsIgnoreCase("record"))
      return AxisType.Time;

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

  /**  @return "up" if this is a Vertical (z) coordinate axis which goes up as coords get bigger */
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

  private class NavComparator implements java.util.Comparator {
    public int compare(Object o1, Object o2) {
      NavInfo n1 = (NavInfo) o1;
      NavInfo n2 = (NavInfo) o2;
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
        parseInfo.append("Nav variable "+getName()+" not a scalar");
      }
      ArrayList values = new ArrayList();
      values.add( getStringValue());
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

    private StringBuffer buf = new StringBuffer(200);
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

  private class NavInfoList extends ArrayList {

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

    private StringBuffer buf = new StringBuffer(2000);
    public String toString() {
      buf.setLength(0);
      buf.append("\nNav Info\n");
      buf.append("Name___________Value_____________________Description\n");
      Iterator iter = iterator();
      while (iter.hasNext()) {
        NavInfo nava = (NavInfo) iter.next();
        buf.append( nava+"\n");
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
      v.addAttribute( new Attribute( "_CoordinateAxisType", (0 == grid_code) ? "Lon" : "GeoX"));
      ds.setValues( v, nx, startx, dx);
      ds.addCoordinateAxis( v);
      return v;
    }

    CoordinateAxis makeYCoordAxis( NetcdfDataset ds, String yname) {
      CoordinateAxis v = new CoordinateAxis1D( ds, null, yname, DataType.DOUBLE, yname,
            ((0 == grid_code) ? "degrees_north" : "km"), "synthesized Y coord");
      v.addAttribute( new Attribute( "_CoordinateAxisType", (0 == grid_code) ? " Lat" : "GeoY"));
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

      ProjectionCT rs = new ProjectionCT(grid_name, "FGDC", lc);
      return rs;
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

      ProjectionCT rs = new ProjectionCT(grid_name, "FGDC", ps);
      return rs;
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
