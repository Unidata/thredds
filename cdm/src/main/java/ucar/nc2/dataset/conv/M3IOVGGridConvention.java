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
/******************************************************************************
PURPOSE: M3IOVGGridConvention.java - describes NetCDF files that follow
         M3IO (Models-3) conventions for structured (in projected, e.g.,
         Lambert space) 5D gridded scalar data.
NOTES:   These are similar to Vis5D data sets except that the data are
         cell-based/cell-centered instead of vertex-based.
         The cells are hexahedra and should be visualized as constant-
         colored (no shade colors).
         With the VG scheme, all cells at a given layer are at the same
         height varying rectillinearly based on pressure.
         The pressure-based levels are converted into approximate z in
         meters above mean sea level.
HISTORY: 2003/06/26 @author plessel.todd@epa.gov, based on WRFConvention.java
STATUS:  unreviewed, untested.
******************************************************************************/

package ucar.nc2.dataset.conv;

import java.util.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

/*
 * Describes NetCDF files that follow M3IO (Models-3) conventions for
 * structured (in projected, e.g., Lambert space) 5D gridded scalar data.
 * These are similar to Vis5D data sets except that the data are
 * cell-based/cell-centered instead of vertex-based.
 * The cells are hexahedra and should be visualized as constant-colored
 * (no shade colors). With the VG scheme, all cells at a given layer are at
 * the same height varying rectillinearly based on pressure.
 * The pressure-based levels are converted into approximate z in meters above
 * mean sea level.
 * @invariant ncfile != null implies isValidM3IOFile()
 *
 * @HISTORY: 2003/06/26
 * @author plessel.todd@epa.gov, based on WRFConvention.java
 * @author caron - convert to new CoordSysBuilder
 */
public class M3IOVGGridConvention extends CoordSysBuilder {

  // Attributes:

  // From M3IO parms3.h:
  private static final int GRDDED3 = 1;
  private static final int IDDATA3 = 3;
  private static final int PTRFLY3 = 8;
  private static final int MXLAYS3 = 100;
  private static final int MXVARS3 = 120;
  private static final int NAMLEN3 = 16;
  private static final int LATGRD3 = 1;
  private static final int LAMGRD3 = 2;
  private static final int MERGRD3 = 3;
  private static final int STEGRD3 = 4;
  private static final int UTMGRD3 = 5;
  private static final int VGSGPH3 = 1;
  private static final int VGSGPN3 = 2;
  private static final int VGSIGZ3 = 3;
  private static final int VGPRES3 = 4;
  private static final int VGZVAL3 = 5;
  private static final int VGHVAL3 = 6;
  private static final int IMISS3 = -9999;
  private static final double BADVAL3 = -9.999E36;
  private static final double AMISS3 = -9.000E36;
  private static final double SURFACE_PRESSURE_IN_MB = 1012.5;


  /*
   * Is this NetCDF file an M3IO file?
   * @param ncfile The NetCDF file to query.
   * @pre ncfile != null
   */
  public static boolean isMine( NetcdfFile ncFile ) {
    return ncFile.findGlobalAttribute( "VGLVLS" ) != null && isValidM3IOFile_( ncFile );
  }


  ///////////////////////////////
  private CoordinateTransform ct = null;
  private  NetcdfDataset ncd = null;

  public M3IOVGGridConvention() {
    this.conventionName = "M3IOVGGrid";
  }

  /*
   * Create a NetcdfDataset out of this NetcdfFile, adding coordinates, etc.
   * @param ncDataset The updated data set.
   * @pre ncDataset != null
   * @param ncFile The file of the data set.
   * @pre ncFile != null
   * @post conventionName.equals( "M3IOVGGrid" )
   */
  public void augmentDataset( NetcdfDataset ncd, CancelTask cancelTask) {
    this.ncd = ncd;
    constructCoordAxes( ncd);
    ncd.finish();
  }


  /*
   * Construct the coordinate axes and coordinate transform for the data set.
   * @param ds The data set to construct axes for.
   * @pre ds != null
   * @post ct != null
   */
  protected void constructCoordAxes( NetcdfDataset ds ) {
    if (null != ncd.findVariable("x")) return; // check if its already been done - aggregating enhanced datasets.

   // super.constructCoordAxes( ds );

    // Set the projection:
    final int gdtyp = intAttribute( "GDTYP" );
    final double p_alp = doubleAttribute( "P_ALP" );
    final double p_bet = doubleAttribute( "P_BET" );
    final double p_gam = doubleAttribute( "P_GAM" );
    final double xcent = doubleAttribute( "XCENT" );
    final double ycent = doubleAttribute( "YCENT" );
    String xUnits = gdtyp == LATGRD3 ? "degrees east"  : "m";
    String yUnits = gdtyp == LATGRD3 ? "degrees north" : "m";
    ProjectionImpl p = null;

    switch ( gdtyp ) {
    case LATGRD3:
      p = new LatLonProjection();
      break;
    case LAMGRD3:
      p = new LambertConformal( ycent, p_gam, p_alp, p_bet );
      break;
    case MERGRD3:
      p = new TransverseMercator( p_alp, p_bet, 1.0 );
      break;
    case STEGRD3:
      p = new Stereographic( p_alp, p_bet, 1.0 );
      break;
    case UTMGRD3:
      break;
    default:
      break;
    }

    if ( p != null ) {
      ct = new ProjectionCT( p.getClassName(), "FGDC", p );
      VariableDS v = makeCoordinateTransformVariable(ds, ct);
      v.addAttribute( new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
      ds.addVariable(null, v);
    }

    // Add axes:
    makeXCoordAxis( ds, xUnits );
    makeYCoordAxis( ds, yUnits );
    makeZCoordAxis( ds );
    makeTimeCoordAxis( ds );
  }


  /*
   * Create and add the x coordinate axis to the data-set.
   * @param ds The data-set to create an axis for.
   * @param units The dimension's units - e.g., "m" or "degrees".
   * @pre ds != null
   * @pre units != null
   * @pre ncfile != null
   */
  private void makeXCoordAxis( NetcdfDataset ds, String units ) {
    Dimension dim = ds.findDimension( "COL" );
    final String newUnits = units.equals( "m" ) ? "km" : units;
    String desc = "synthesized x coordinate from XORIG, XCELL global attributes";

    CoordinateAxis axis = new CoordinateAxis1D( ds, null, "x", DataType.DOUBLE, dim.getName(), newUnits, desc );

    final double scale = units.equals("m") ? 0.001 : 1.0;//IDV BUG m->km
    final double xorig = doubleAttribute( "XORIG" ) * scale;
    final double xcell = doubleAttribute( "XCELL" ) * scale;

    ds.setValues(axis, dim.getLength(), xorig, xcell);
    axis.addAttribute( new Attribute( "units", newUnits ) );
    axis.addAttribute( new Attribute( "long_name", desc ) );
    axis.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.GeoX.toString() ) );
    ds.addCoordinateAxis( axis );
  }


  /*
   * Create and add the y coordinate axis to the data-set.
   * @param ds The data-set to create an axis for.
   * @param units The dimension's units - e.g., "m" or "degrees".
   * @pre ds != null
   * @pre units != null
   * @pre ncfile != null
   */
  private void makeYCoordAxis( NetcdfDataset ds, String units ) {
    Dimension dim = ds.findDimension( "ROW" );
    final String newUnits = units.equals( "m" ) ? "km" : units;
    String desc = "synthesized y coordinate from YORIG, YCELL global attributes";

    CoordinateAxis axis = new CoordinateAxis1D( ds, null, "y", DataType.DOUBLE, dim.getName(), newUnits, desc );
    final double scale = units.equals("m") ? 0.001 : 1.0;//IDV BUG m->km
    final double yorig = doubleAttribute( "YORIG" ) * scale;
    final double ycell = doubleAttribute( "YCELL" ) * scale;
    ds.setValues(axis,  dim.getLength(), yorig, ycell );
    axis.addAttribute( new Attribute( "units", newUnits ) );
    axis.addAttribute( new Attribute( "long_name", desc ) );
    axis.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.GeoY.toString() ) );
    ds.addCoordinateAxis( axis );
  }


  /*
   * Create and add the z coordinate axis to the data-set.
   * This involves conversion from the vertical grid scheme to meters above
   * mean sea level.
   * @param ds The data-set to create an axis for.
   * @pre ds != null
   * @pre units != null
   * @pre ncfile != null
   */
  private void makeZCoordAxis( NetcdfDataset ds ) {
    Dimension dim = ds.findDimension( "LAY" );

    String desc = "synthesized z coordinate from VGTYP, VGTOP, VGLVLS global attributes";
    CoordinateAxis axis = new CoordinateAxis1D( ds, null, "z", DataType.DOUBLE, dim.getName(), "km", desc ); // IDV?
    final int vgtyp = intAttribute( "VGTYP" );
    final double vgtop = doubleAttribute( "VGTOP" );
    final double[] vglvls = doubleArrayAttribute( "VGLVLS" );
    double[] vgLevelsInMeters = convertVGLevels_( vgtyp, vgtop, vglvls );
    final int count = vgLevelsInMeters.length;
    List<String> vgArray = new ArrayList<String>( count );

    for ( int index = 0; index < count; ++index ) {
      vgArray.add( Double.toString( vgLevelsInMeters[ index ] * 0.001 ) );
    }

    ds.setValues(axis, vgArray );
    axis.addAttribute( new Attribute( "units", "km" ) );
    axis.addAttribute( new Attribute( "long_name", desc ) );
    axis.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.Height.toString() ) );
    ds.addCoordinateAxis( axis );
  }


  /*
   * Create and add the time coordinate axis to the data-set.
   * @param ds The data-set to create an axis for.
   * @pre ds != null
   */
  private void makeTimeCoordAxis( NetcdfDataset ds ) {

    // Parse the time step:

    final int hhmmss = intAttribute( "TSTEP" );
    final int hh     = hhmmss / 10000;
    final int hh0000 = hh     * 10000;
    final int mmss   = hhmmss - hh0000;
    final int mm     = mmss / 100;
    final int mm00   = mm   * 100;
    final int ss     = mmss - mm00;
    final int mm_hh = 60;
    final int ss_mm = 60;
    final int totalSeconds = hh * mm_hh * ss_mm + mm * ss_mm + ss;
    final int timeSteps = dimension_( ds, "TSTEP" );
    ArrayInt.D1 data = new ArrayInt.D1( timeSteps );

    for ( int timeStep = 0; timeStep < timeSteps; ++timeStep ) {
      data.set( timeStep, timeStep * totalSeconds );
    }

    // Create the coord axis:

    String desc = "synthesized time coordinate from SDATE, STIME, STEP global attributes";
    String units = timeUnits();
    CoordinateAxis1D axis = new CoordinateAxis1D( ds, null, "time", DataType.INT, "TSTEP", units, desc );
    axis.setCachedData( data, true);
    axis.addAttribute( new Attribute( "long_name", desc));
    axis.addAttribute( new Attribute( "units", units ));
    axis.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.Time.toString() ) );
    ds.addCoordinateAxis( axis );
  }


  // Queries:


  /*
   * The time units of the ncFile.
   * @pre ncFile != null
   * @post return != null
   */
  private String timeUnits() {
    final int yyyddd = intAttribute( "SDATE" );
    final int hhmmss = intAttribute( "STIME" );
    final int yyyy   = yyyddd / 1000;
    final int ddd    = yyyddd % 1000;
    final int hh     = hhmmss / 10000;
    final int hh0000 = hh     * 10000;
    final int mmss   = hhmmss - hh0000;
    final int mm     = mmss / 100;
    final int mm00   = mm   * 100;
    final int ss     = mmss - mm00;
    Calendar cal = new GregorianCalendar( new SimpleTimeZone( 0, "GMT" ) );
    cal.clear();
    cal.set( Calendar.YEAR, yyyy );
    cal.set( Calendar.DAY_OF_YEAR, ddd );
    cal.set( Calendar.HOUR, hh );
    cal.set( Calendar.MINUTE, mm );
    cal.set( Calendar.SECOND, ss );
    java.text.SimpleDateFormat dateFormatOut =
      new java.text.SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    dateFormatOut.setTimeZone( java.util.TimeZone.getTimeZone( "GMT" ) );
    return "seconds since " + dateFormatOut.format( cal.getTime() ) + " UTC";
  }


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

    if (vname.equalsIgnoreCase("time"))
      return AxisType.Time;

    if (vname.equalsIgnoreCase("z"))
      return AxisType.Height;

    return null;
  }

  /*
   * 
   * Return "up" if this is a Vertical (z) coordinate axis which goes up as
   * coords get bigger
   * @param axis The axis to query.
   * @pre axis != null
   * @post return != null
   * @post getUnitsString( axis ) != null
   * @post return.equals( "up " ) implies
   *       SimpleUnit.isCompatible( "m", getUnitsString( axis ) )
   */
  protected String getZPositive( CoordinateAxis axis ) {
    String unit = axis.getUnitsString( );
    boolean isup = (unit != null) && SimpleUnit.isCompatible( "m", unit );
    return  isup ? "up" : "";
  }


  /*
   * List of one CoordinateTransform.
   * @param cs The coordinate system.
   * @pre cs != null
   * @pre ct != null
   * @post return != null
   * @post return.length == 1
   * @post return[ 0 ] == ct
   */
  protected List<CoordinateTransform> getCoordinateTransforms( CoordinateSystem cs ) {
    List<CoordinateTransform> list = new ArrayList<CoordinateTransform>();

    if ( cs.getXaxis() != null && cs.getYaxis() != null ) {
      list.add( ct );
    }

    return list;
  }


  /*
   * Might the given variable have missing data points? Yes.
   * Any M3IO variable may contain missing data points.
   * @param v The variable to query.
   * @pre v != null
   * @post return == true
   */
  protected boolean hasMissingData( Variable v ) {
    return true;
  }


  /*
   * The value that denotes "missing" for a real variable.
   * @post return == BADVAL3
   */
  protected double getMissingDataValue( Variable unused ) {
    return BADVAL3;
  }


  /*
   * int value of named attribute.
   * @pre name != null
   */
  private int intAttribute( String name ) {
    return intAttribute_( ncd, name );
  }


  /*
   * double value of named attribute.
   * @pre name != null
   */
  private double doubleAttribute( String name ) {
    return doubleAttribute_( ncd, name );
  }


  /*
   * Array of double values of named attribute.
   * @pre name != null
   * @post return != null
   * @post return.length > 0
   */
  private double[] doubleArrayAttribute( String name ) {
    return doubleArrayAttribute_( ncd, name );
  }


  /*
   * int value of named attribute.
   * @param ncFile The file to query.
   * @param name The name of the attribute to check for.
   * @pre ncFile != null
   * @pre name != null
   */
  static private int intAttribute_( NetcdfFile ncFile, String name ) {
    int result = 0;
    Attribute attribute = ncFile.findGlobalAttribute( name );

    if ( attribute != null ) {
      result = attribute.getNumericValue().intValue();
    }

    return result;
  }


  /*
   * double value of named attribute.
   * @param ncFile The file to query.
   * @param name The name of the attribute to check for.
   * @pre ncFile != null
   * @pre name != null
   */
  static private double doubleAttribute_( NetcdfFile ncFile, String name ) {
    double result = 0.0;
    Attribute attribute = ncFile.findGlobalAttribute( name );

    if ( attribute != null ) {
      result = attribute.getNumericValue().doubleValue();
    }

    return result;
  }


  /*
   * Array of double values of named attribute.
   * @param ncFile The file to query.
   * @param name The name of the attribute to check for.
   * @pre ncFile != null
   * @pre name != null
   */
  static private double[] doubleArrayAttribute_( NetcdfFile ncFile,
                                                 String name ) {
    double[] result = null;
    Attribute attribute = ncFile.findGlobalAttribute( name );

    if ( attribute != null && attribute.isArray() &&
         attribute.getLength() > 1 ) {
      final int count = attribute.getLength();
      result = new double[ count ];

      for ( int index = 0; index < count; ++index ) {
        result[ index ] = attribute.getNumericValue( index ).doubleValue();
      }
    }

    return result;
  }


  /*
   * Is ncFile a valid M3IO file?
   * @param ncFile The file to check.
   * @pre ncFile != null
   */
  static private boolean isValidM3IOFile_( NetcdfFile ncFile ) {
    final int ftypes[] = { GRDDED3, IDDATA3, PTRFLY3 };
    final String[] dims = { "TSTEP", "DATE-TIME", "LAY", "VAR", "ROW", "COL"};
    boolean result = hasDimensions_( ncFile, dims );
    result = result && hasStringAttribute_( ncFile, "EXEC_ID", 80 );
    result = result && hasIntAttributeIn_( ncFile, "FTYPE", ftypes );
    result = result && hasYYYYDDDAttribute_( ncFile, "CDATE" );
    result = result && hasHHMMSSAttribute_( ncFile, "CTIME" );
    result = result && hasYYYYDDDAttribute_( ncFile, "SDATE" );
    result = result && hasHHMMSSAttribute_( ncFile, "STIME" );
    result = result && hasHHMMSSAttribute_( ncFile, "TSTEP" );
    result = result && hasIntAttribute_( ncFile, "NTHIK",
                                             1, Integer.MAX_VALUE );
    result = result && hasIntAttribute_( ncFile, "NCOLS",
                                             1, Integer.MAX_VALUE );
    result = result && hasIntAttribute_( ncFile, "NROWS",
                                             1, Integer.MAX_VALUE );
    result = result && hasIntAttribute_( ncFile, "NLAYS", 1, MXLAYS3 );
    result = result && hasIntAttribute_( ncFile, "NVARS", 1, MXVARS3 );
    result = result && hasDimension_( ncFile, "TSTEP", 1, Integer.MAX_VALUE );

    if ( result ) {
      final int mxrec = dimension_( ncFile, "TSTEP" );
      final int nvars = intAttribute_( ncFile, "NVARS" );
      final int nlays = intAttribute_( ncFile, "NLAYS" );
      final int nrows = intAttribute_( ncFile, "NROWS" );
      final int ncols = intAttribute_( ncFile, "NCOLS" );
      final int nthik = intAttribute_( ncFile, "NTHIK" );
      final int min = Math.min( nrows, ncols );
///// final int max = Integer.MAX_VALUE / MXVARS3 / mxrec / MXLAYS3 / nrows;
// Assume only one timestep of one variable must fit into memory...
      final int max = Integer.MAX_VALUE / MXLAYS3 / nrows;
      result = nthik <= min && ncols <= max;
    }

    final int gdtypes[] = { LATGRD3, LAMGRD3, MERGRD3, STEGRD3, UTMGRD3 };
    result = result && hasIntAttributeIn_( ncFile, "GDTYP", gdtypes );
    result = result && hasDoubleAttribute_( ncFile, "P_ALP", -90.0, 90.0 );
    result = result && hasDoubleAttribute_( ncFile, "P_BET", -90.0, 90.0 );
    result = result && hasDoubleAttribute_( ncFile, "P_GAM", -180.0, 180.0 );
    result = result && hasDoubleAttribute_( ncFile, "XCENT", -180.0, 180.0 );
    result = result && hasDoubleAttribute_( ncFile, "YCENT", -90.0, 90.0 );
    result = result && hasDoubleAttribute_( ncFile, "XORIG",
                                            -Float.MAX_VALUE,Float.MAX_VALUE);
    result = result && hasDoubleAttribute_( ncFile, "YORIG",
                                            -Float.MAX_VALUE,Float.MAX_VALUE);
    result = result && hasDoubleAttribute_( ncFile, "XCELL",
                                            Float.MIN_VALUE, Float.MAX_VALUE);
    result = result && hasDoubleAttribute_( ncFile, "YCELL",
                                            Float.MIN_VALUE, Float.MAX_VALUE);

    if ( result ) {
      final int gdtyp = intAttribute_( ncFile, "GDTYP" );
      final double p_alp = doubleAttribute_( ncFile, "P_ALP" );
      final double p_bet = doubleAttribute_( ncFile, "P_BET" );
      final double p_gam = doubleAttribute_( ncFile, "P_GAM" );
      final double xcent = doubleAttribute_( ncFile, "XCENT" );
      final double ycent = doubleAttribute_( ncFile, "YCENT" );
      final double xorig = doubleAttribute_( ncFile, "XORIG" );
      final double yorig = doubleAttribute_( ncFile, "YORIG" );
      final double xcell = doubleAttribute_( ncFile, "XCELL" );
      final double ycell = doubleAttribute_( ncFile, "YCELL" );
      final int nrows = intAttribute_( ncFile, "NROWS" );
      final int ncols = intAttribute_( ncFile, "NCOLS" );
      result = isValidM3IOProjection_( gdtyp, p_alp, p_bet, p_gam, xcent,
                                       ycent, xorig, yorig, xcell, ycell,
                                       nrows, ncols );
    }

    final int vgtypes[] = {VGSGPH3, VGSGPN3, VGSIGZ3, VGPRES3,VGZVAL3,VGHVAL3};
    result = result && hasIntAttributeIn_( ncFile, "VGTYP", vgtypes );
    result = result && hasDoubleAttribute_(ncFile,"VGTOP",0.,Float.MAX_VALUE);
    result = result && ncFile.findGlobalAttribute( "VGLVLS" ) != null;

    if ( result ) {
      final int nlays = intAttribute_( ncFile, "NLAYS" );
      final int vgtyp = intAttribute_( ncFile, "VGTYP" );
      final double vgtop = doubleAttribute_( ncFile, "VGTOP" );
      Attribute a = ncFile.findGlobalAttribute( "VGLVLS" );
      result = a != null && a.isArray() && a.getLength() == nlays + 1;

      if ( result ) {
        double[] vglvls = doubleArrayAttribute_( ncFile, "VGLVLS" );
        result = vglvls != null && vglvls.length == nlays + 1;
        result = result && isValidVG_( vgtyp, vgtop, vglvls );
      }
    }

    result = result && hasStringAttribute_( ncFile, "GDNAM", NAMLEN3 );
    result = result && hasStringAttribute_( ncFile, "UPNAM", NAMLEN3 );
    final int nvars = intAttribute_( ncFile, "NVARS" );
    final int varListLen = nvars * NAMLEN3;
    result = result && hasStringAttribute_( ncFile, "VAR-LIST", varListLen );
    result = result && ncFile.findGlobalAttribute( "FILEDESC" ) != null;
    result = result && ncFile.findGlobalAttribute( "HISTORY" ) != null;
    return result;
  }


  /*
   * Does ncFile contain the named dimensions?
   * @param ncFile The file to query.
   * @param dims The names of the dimensions to check for.
   * @pre ncFile != null
   * @pre dims != null
   * @pre dims.length > 0
   */
  static private boolean hasDimensions_( NetcdfFile ncFile, String[] dims ) {
    boolean result = true;
   Iterator iter = ncFile.getDimensions().iterator();
   final int count = dims.length;
   int index = 0;

    while ( result && iter.hasNext() ) {
      Dimension d = (Dimension) iter.next();
      result = index < count && d.getName().equals( dims[ index ] );
      ++index;
    }

    return result;
  }


  /*
   * Does ncFile contain the named String attribute with the given length?
   * @param ncFile The file to query.
   * @param name The name of the aatribute to check for.
   * @param length The length of the String value of the named attribute.
   * @pre ncFile != null
   * @pre name != null
   * @pre length > 0
   */
  static private boolean hasStringAttribute_( NetcdfFile ncFile, String name,
                                              int length ) {
    Attribute a = ncFile.findGlobalAttribute( name );
    return a != null && a.isString() && a.getStringValue().length()==length;
  }


  /*
   * Does ncFile contain the named double attribute within the given range?
   * @param ncFile The file to query.
   * @param name The name of the aatribute to check for.
   * @param min The minimum value of the named attribute.
   * @param max The maximum value of the named attribute.
   * @pre ncFile != null
   * @pre name != null
   * @pre min >= -Double.MAX_VALUE
   * @pre max >= min
   */
  static private boolean hasDoubleAttribute_( NetcdfFile ncFile, String name,
                                              double min, double max ) {
    boolean result = false;
    Attribute a = ncFile.findGlobalAttribute( name );

    if ( a != null ) {

      if ( a.getDataType() == DataType.FLOAT ) {
        final float v = a.getNumericValue().floatValue();
        result = v >= min && v <= max;
      } else if ( a.getDataType() == DataType.DOUBLE  ) {
        final double v = a.getNumericValue().doubleValue();
        result = v >= min && v <= max;
      }
    }

    return result;
  }


  /*
   * Does ncFile contain the named int dimension within the given range?
   * @param ncFile The file to query.
   * @param name The name of the dimension to check for.
   * @param min The minimum value of the named attribute.
   * @param max The maximum value of the named attribute.
   * @pre ncFile != null
   * @pre name != null
   * @pre min >= -Double.MAX_VALUE
   * @pre max >= min
   */
  static private boolean hasDimension_( NetcdfFile ncFile, String name,
                                        int min, int max ) {
    boolean result = false;
    Dimension d = ncFile.findDimension( name );

    if ( d != null ) {
      final int size = d.getLength();
      result = inRangeI_( size, min, max );
    }

    return result;
  }


  /*
   * Thr ncFile's named dimension length.
   * @param ncFile The file to query.
   * @param name The name of the dimension to check for.
   * @pre ncFile != null
   * @pre name != null
   * @post return > 0
   */
  static private int dimension_( NetcdfFile ncFile, String name ) {
    Dimension d = ncFile.findDimension( name );
    return d.getLength();
  }


  /*
   * Does ncFile contain the named int attribute within the given range?
   * @param ncFile The file to query.
   * @param name The name of the attribute to check for.
   * @param min The minimum value of the named attribute.
   * @param max The maximum value of the named attribute.
   * @pre ncFile != null
   * @pre name != null
   * @pre min >= -Double.MAX_VALUE
   * @pre max >= min
   */
  static private boolean hasIntAttribute_( NetcdfFile ncFile, String name,
                                           int min, int max ) {
    boolean result = false;
    Attribute a = ncFile.findGlobalAttribute( name );

    if ( a != null ) {

      if ( a.getDataType() == DataType.INT  ) {
        final int v = a.getNumericValue().intValue();
        result = v >= min && v <= max;
      }
    }

    return result;
  }


  /*
   * Does ncFile contain the named int attribute from the given set?
   * @param ncFile The file to query.
   * @param name The name of the attribute to check for.
   * @param match The values of the named attribute.
   * @pre ncFile != null
   * @pre name != null
   * @pre match != null
   */
  static private boolean hasIntAttributeIn_( NetcdfFile ncFile, String name,
                                             int[] match ) {
    boolean result = false;
    Attribute a = ncFile.findGlobalAttribute( name );

    if ( a != null && (a.getDataType() == DataType.INT)  ) {
      final int value = a.getNumericValue().intValue();
      final int count = match.length;

      for ( int index = 0; ! result && index < count; ++index ) {
        result = value == match[ index ];
      }
    }

    return result;
  }


  /*
   * Does ncFile contain the named date attribute?
   * @param ncFile The file to query.
   * @param name The name of the attribute to check for.
   * @pre ncFile != null
   * @pre name != null
   */
  static private boolean hasYYYYDDDAttribute_(NetcdfFile ncFile,String name) {
    boolean result = hasIntAttribute_( ncFile, name, 1001, 9999366 );

    if ( result ) {
      final int yyyyddd = intAttribute_( ncFile, name );
      final int ddd = yyyyddd - ( yyyyddd / 1000 ) * 1000;
      result = inRangeI_( ddd, 1, 366 );
    }

    return result;
  }


  /*
   * Does ncFile contain the named time attribute?
   * @param ncFile The file to query.
   * @param name The name of the attribute to check for.
   * @pre ncFile != null
   * @pre name != null
   */
  static private boolean hasHHMMSSAttribute_(NetcdfFile ncFile, String name) {
    boolean result = hasIntAttribute_( ncFile, name, 0, 235959 );

    if ( result ) {
      final int hhmmss = intAttribute_( ncFile, name );
      final int hh = hhmmss / 10000;
      result = isValidTimestepSize_( hhmmss ) && inRangeI_( hh, 0, 23 );
    }

    return result;
  }


  /*
   * Is hhmmss a valid time-step size?
   * @param hhmmss The time-stamp to check.
   */
  static private boolean isValidTimestepSize_( int hhmmss ) {
    final int hh     = hhmmss  / 10000;
    final int hh0000 = hh      * 10000;
    final int mmss   = hhmmss - hh0000;
    final int mm     = mmss / 100;
    final int mm00   = mm   * 100;
    final int ss     = mmss - mm00;
    return hh >= 0 && inRangeI_( mm, 0, 59 ) && inRangeI_( ss, 0, 59 );
  }


  /*
   * Does the given set of parameters constitute a valid M3IO map projection?
   * @param gdtyp The M3IO projection type, e.g., LAMGRD3.
   * @param p_alp The 1st projection parameter, e.g., 30.0 degrees north lat.
   * @param p_bet The 2nd projection parameter, e.g., 60.0 degrees north lat.
   * @param p_gam The 3rd projection parameter, e.g., -90.0 degrees west lon.
   * @param xcent The center longitude that projects to 0, e.g., -90.0.
   * @param ycent The center latitude that projects to 0, e.g., 40.0.
   * @param xorig The lower-left corner x, e.g., -2320000.0.
   * @param yorig The lower-left corner x, e.g., -1720000.0.
   * @param xcell The cell width (e.g., in meters), 80000.
   * @param ycell The cell height (e.g., in meters), 80000.
   * @param nrows The number of grid rows.
   * @param ncols The number of grid columns.
   */
  static private boolean isValidM3IOProjection_( int gdtyp, double p_alp,
                                                 double p_bet, double p_gam,
                                                 double xcent, double ycent,
                                                 double xorig, double yorig,
                                                 double xcell, double ycell,
                                                 int nrows, int ncols ) {
    boolean result = false;

    switch ( gdtyp ) {
    case LATGRD3:
      result = inRange_( xorig, -180.0, 180.0 ) &&
               inRange_( yorig,  -90.0,  90.0 ) &&
               inRange_( xcell,    0.0, 360.0 ) &&
               inRange_( ycell,    0.0, 180.0 ) &&
               inRange_( xorig + ncols * xcell, -180.0, 540.0 ) &&
               inRange_( yorig + nrows * ycell,  -90.0,  90.0 );
      break;
    case LAMGRD3:
      result = inRange_( p_alp,  -90.0,  90.0 ) &&
               inRange_( p_bet,  p_alp,  p_alp > 0 ? 90.0 : 0.0 ) &&
               inRange_( p_gam, -180.0, 180.0 ) &&
               inRange_( xcent, -180.0, 180.0 ) &&
               inRange_( ycent,  -90.0,  90.0 );
      break;
    case MERGRD3:
      /* Fall thru */
    case STEGRD3:
      result = inRange_( p_alp,  -90.0,  90.0 ) &&
               inRange_( p_bet,  p_alp,  p_alp > 0 ? 90.0 : 0.0 ) &&
               inRange_( p_gam, -180.0, 180.0 );
      break;
    case UTMGRD3:
      result = inRange_( p_alp, 1.0, 60.0 );
      break;
    default:
      break;
    }

    return result;
  }


  /*
   * Does the given set of parameters constitute a valid M3IO vertical grid?
   * @param vgtyp The M3IO vertical grid type, e.g., VGPRES3.
   * @param vgtop The pressure, in pascals, at the top of the model.
   * @param vglvls The vertical grid levels that delimit the layers in units
   * indicated by vgtyp.
   * @param nlays The number of grid layers.
   * @pre vglvls != null
   */
  static private boolean isValidVG_(int vgtyp, double vgtop,double[] vglvls) {
    boolean result;

    switch ( vgtyp ) {
    case VGSGPH3: // Hydrostatic sigma pressures.
      /* Fall thru */
    case VGSGPN3: // Non-hydrostatic sigma pressures.
      /* Fall thru */
    case VGSIGZ3: // Sigma Z.
      result = orderedFromTo_( vglvls, 1.0, 0.0 );
      break;
    case VGPRES3: // Pressure (in pascals).
      result = decreasesToward_( vglvls, vgtop );
      break;
    case VGZVAL3: // Height in meters above mean sea level.
      result = orderedWithin_( vglvls, -200.0, 100000.0 );
      break;
    case VGHVAL3: // Height in meters above terrain.
      result = orderedWithin_( vglvls, 0.0, 100000.0 );
      break;
    default:
      result = vgtyp == IMISS3 && vglvls.length == 2;
      break;
    }

    return result;
  }


  /*
   * Convert from the VG grid scheme to meters above mean sea level.
   * @param vgtyp The M3IO vertical grid type, e.g., VGSGPH3.
   * @param vgtop The pressue, in pascals at the top of the model, ex. 10000.
   * @param vglvls The array of level values in the units of vgtyp.
   * @pre inRangeI_( vgtyp, VGSGPH3, VGHVAL3 ) || vgtyp == IMISS3
   * @pre vgtop > 0.0
   * @pre vglvls
   * @post return != null
   * @post return.length == vglvls.length
   */
  static private double[] convertVGLevels_( int vgtyp, double vgtop,
                                            double[] vglvls ) {
    final boolean computeZAtLevels = false; ///true;
    final int numberOfLevels = vglvls.length + (computeZAtLevels ? 0 : -1);
    double[] result = new double[ numberOfLevels ];

    for ( int level = 0; level < numberOfLevels; ++level ) {
      final double valueAtLevel =
        computeZAtLevels ? vglvls[ level ]
                         : ( vglvls[ level ] + vglvls[ level + 1 ] ) * 0.5;
      final double HEIGHT_OF_TERRAIN_IN_METERS = 0.0;
      double pressure;

      switch ( vgtyp ) {
      case VGSGPH3: // Hydrostatic sigma-P
        /* Fall thru */
      case VGSGPN3: // Non-h sigma-P
        pressure = pressureAtSigmaLevel_( valueAtLevel, vgtop * 0.01 );
        result[ level ] = heightAtPressure_( pressure );
        break;
      case VGSIGZ3: // Sigma-Z: vgtop is in meters and valueAtLevel increases:
        result[ level ] = HEIGHT_OF_TERRAIN_IN_METERS +
                          valueAtLevel * (vgtop -HEIGHT_OF_TERRAIN_IN_METERS);
        break;
      case VGPRES3: // Pressure (pascals)
        result[ level ] = heightAtPressure_( valueAtLevel * 0.01 );
        break;
      case VGZVAL3: // Z (m) (above sea lvl)
        result[ level ] = valueAtLevel;
        break;
      case VGHVAL3: // H (m) (above ground)
        result[ level ] = valueAtLevel + HEIGHT_OF_TERRAIN_IN_METERS;
        break;
      default:
        result[ level ] = level;
      }
    }

    return result;
  }


  /*
   * Pressure (in millibars) at a given sigma level.
   * Based on formula in the documentation for Vis5d by Bill Hibbard.
   * @param sigmaLevel Sigma level.
   * @param pressureAtTop  Pressure (in millibars) at top of the model.
   * @pre sigmaLevel >= 0.0
   * @pre presureAtTop > 0.0
   * @post return > 0.0
   */
  private static double pressureAtSigmaLevel_( double sigmaLevel,
                                               double pressureAtTop ) {
    return pressureAtTop + sigmaLevel * ( SURFACE_PRESSURE_IN_MB - pressureAtTop );
  }


  /*
   * Height (in meters) at a given pressure (in millibars).
   * Based on formula in the documentation for Vis5d by Bill Hibbard.
   * @param sigmaLevel Sigma level.
   * @param pressure  Pressure (in millibars).
   * @pre pressure > 0.0
   * @post return >= -500
   */
  private static double heightAtPressure_( double pressure ) {
    final double pressureToHeightScaleFactor = -7.2 * 1000.0;
    return pressureToHeightScaleFactor * Math.log(pressure/SURFACE_PRESSURE_IN_MB);
  }


  /*
   * Does a have values that range from first towards last inclusive?
   * @pre a != null
   */
  private static boolean orderedFromTo_( double[] a, double first,
                                         double last ) {
    final int count = a.length;
    final boolean increasing = first <= last;
    boolean result = increasing ? a[ 0 ] >= first && a[ count - 1 ] <= last
                                : a[ 0 ] >= last  && a[ count - 1 ] <= first;

    if ( increasing ) {

      for ( int index = 1; result && index < count; ++index ) {
        result = inRange_( a[ index ], a[ index - 1 ], last );
      }
    } else {

      for ( int index = 1; result && index < count; ++index ) {
        result = inRange_( a[ index ], last, a[ index - 1 ] );
      }
    }

    return result;
  }


  /*
   * Does a have values that range from first towards last exclusive?
   * @pre a != null
   */
  private static boolean orderedWithin_( double[] a, double first,
                                         double last ) {
    final int count = a.length;
    final boolean increasing = first < last;
    boolean result = increasing ? a[ 0 ] > first && a[ count - 1 ] < last
                                : a[ 0 ] > last  && a[ count - 1 ] < first;

    if ( increasing ) {

      for ( int index = 1; result && index < count; ++index ) {
        final double a_index = a[ index ];
        result = a_index > a[ index - 1 ] && a_index < last;
      }
    } else {

      for ( int index = 1; result && index < count; ++index ) {
        final double a_index = a[ index ];
        result = a_index > last && a_index < a[ index - 1 ];
      }
    }

    return result;
  }


  /*
   * Does a have values that decrease toward last?
   * @pre a != null
   */
  private static boolean decreasesToward_( double[] a, double last ) {
    final int count = a.length;
    boolean result = true;

    for ( int index = 1; result && index < count; ++index ) {
      result = inRange_( a[ index ], last, a[ index - 1 ] );
    }

    return result;
  }


  /*
   * Is x within [min, max]?
   */
  private static boolean inRangeI_( int x, int min, int max ) {
    return x >= min && x <= max;
  }


  /*
   * Is x within [min, max]?
   */
  private static boolean inRange_( double x, double min, double max ) {
    return x >= min && x <= max;
  }
}




