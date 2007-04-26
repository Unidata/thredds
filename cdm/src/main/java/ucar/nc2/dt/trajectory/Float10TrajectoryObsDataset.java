// $Id: Float10TrajectoryObsDataset.java 63 2006-07-12 21:50:51Z edavis $
package ucar.nc2.dt.trajectory;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.*;
import ucar.nc2.dt.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;

import java.util.*;
import java.io.IOException;

import thredds.catalog.DataType;

/*
netcdf U:/testdata/trajectory/buoy/testfloat10.nc {
 dimensions:
   DRIFTER100_110 = 11;   // (has coord.var)
   TIME1 = UNLIMITED;   // (1199 currently)   // (has coord.var)
 variables:
   double DRIFTER100_110(DRIFTER100_110);
     :point_spacing = "even";
     :AXIS = "X";
   double TIME1(TIME1);
     :units = "hour since 2001-07-10 12:00:00";
     :time_origin = "10-JUL-2001 12:00:00";
   float LONGITUDE(TIME1, DRIFTER100_110);
     :missing_value = -1.0E34; // float
     :_FillValue = -1.0E34; // float
     :long_name = "float longitude";
     :history = "From cgoa3_floats";
   float LATITUDE(TIME1, DRIFTER100_110);
     :missing_value = -1.0E34; // float
     :_FillValue = -1.0E34; // float
     :long_name = "float latitude";
     :history = "From cgoa3_floats";
   float DEPTH(TIME1, DRIFTER100_110);
     :missing_value = 1.0E35; // float
     :_FillValue = 1.0E35; // float
     :long_name = "depth of floats trajectories";
     :history = "From cgoa3_floats";
     :units = "meter";
   float TEMP(TIME1, DRIFTER100_110);
     :missing_value = 1.0E35; // float
     :_FillValue = 1.0E35; // float
     :long_name = "potential temperature";
     :history = "From cgoa3_floats";
     :units = "Celsius";
   float SALT(TIME1, DRIFTER100_110);
     :missing_value = 1.0E35; // float
     :_FillValue = 1.0E35; // float
     :long_name = "salinity";
     :history = "From cgoa3_floats";
     :units = "PSU";

 :history = "FERRET V5.51    3-Jan-05";
}
*/
/**
 * Implements TrajectoryDataset for datasets with these characteristics:
 * <ul>
 *   <li> it has two dimensions, an UNLIMITED time dimension and a trajectory dimension</li>
 *   <li> it has two coordinate variables: 1) a time variable has units that
 *        are udunits time units; and 2) a trajectory variable</li>
 *   <li> all other variables are on the (time, trajectory) coordinate system</li>
 *   <li> has latitude, longitude, and depth variables, only DEPTH has units
 *       (compatible with "m").</li>
 * </ul>
 *
 * @author edavis
 * @since Feb 22, 2005T5:37:14 PM
 */
public class Float10TrajectoryObsDataset extends MultiTrajectoryObsDataset implements TypedDatasetFactoryIF
{
  private static String trajDimNameDefault = "DRIFTER100_110";
  private static String trajVarNameDefault = "DRIFTER100_110";
  private static String timeDimNameDefault = "TIME1";
  private static String timeVarNameDefault = "TIME1";
  private static String latVarNameDefault = "LATITUDE";
  private static String lonVarNameDefault = "LONGITUDE";
  private static String elevVarNameDefault = "DEPTH";

  private String trajDimName;
  private String trajVarName;
  private String timeDimName;
  private String timeVarName;
  private String latVarName;
  private String lonVarName;
  private String elevVarName;


  static public boolean isValidFile(NetcdfDataset ds)
  {
    // Check that has a time dimension and a trajectory dimension.
    List list = ds.getRootGroup().getDimensions();
    if ( list.size() != 2) return( false);
    Dimension d;
    for ( int i = 0; i < 2; i++)
    {
      d = (Dimension) list.get(i);
      if ( ! d.getName().equals( timeDimNameDefault) &&
           ! d.getName().equals( trajDimNameDefault)) return( false);
    }

    // Check that has a trajectory coordinate variable.
    Variable var = ds.getRootGroup().findVariable( trajVarNameDefault);
    if ( var == null) return( false);
    list = var.getDimensions();
    if ( list.size() != 1) return( false);
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( trajDimNameDefault)) return( false);

    // Check that has a time coordinate variable with units that are udunits time
    var = ds.getRootGroup().findVariable( timeVarNameDefault);
    if ( var == null) return( false);
    list = var.getDimensions();
    if ( list.size() != 1) return( false);
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimNameDefault)) return( false);
    String units = var.findAttribute( "units").getStringValue();
    Date date = DateUnit.getStandardDate( "0 " + units);
    if ( date == null) return( false);

    // Check for variable latitude(time) with units of "deg".
    var = ds.getRootGroup().findVariable( latVarNameDefault);
    if ( var == null) return( false);
    list = var.getDimensions();
    if ( list.size() != 2) return( false);
    for ( int i = 0; i < 2; i++)
    {
      d = (Dimension) list.get(i);
      if ( ! d.getName().equals( timeDimNameDefault) &&
           ! d.getName().equals( trajDimNameDefault)) return( false);
    }
//    units = var.findAttribute( "units").getStringValue();
//    if ( ! SimpleUnit.isCompatible( units, "degrees_north")) return( false);

    // Check for variable longitude(time) with units of "deg".
    var = ds.getRootGroup().findVariable( lonVarNameDefault);
    if ( var == null) return( false);
    list = var.getDimensions();
    if ( list.size() != 2) return( false);
    for ( int i = 0; i < 2; i++)
    {
      d = (Dimension) list.get(i);
      if ( ! d.getName().equals( timeDimNameDefault) &&
           ! d.getName().equals( trajDimNameDefault)) return( false);
    }
//    units = var.findAttribute( "units").getStringValue();
//    if ( ! SimpleUnit.isCompatible( units, "degrees_east")) return( false);

    // Check for variable altitude(time) with units of "m".
    var = ds.getRootGroup().findVariable( elevVarNameDefault);
    if ( var == null) return( false);
    list = var.getDimensions();
    if ( list.size() != 2) return( false);
    for ( int i = 0; i < 2; i++)
    {
      d = (Dimension) list.get(i);
      if ( ! d.getName().equals( timeDimNameDefault) &&
           ! d.getName().equals( trajDimNameDefault)) return( false);
    }
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "m")) return( false);

    return( true);
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new Float10TrajectoryObsDataset( ncd);
  }
  public DataType getScientificDataType() { return DataType.TRAJECTORY; }

  public Float10TrajectoryObsDataset() {}

  public Float10TrajectoryObsDataset( NetcdfFile ncd ) throws IOException
  {
    super( ncd);

    // Get the names of the two coordinate variables and dimensions
    // and grab the variables and dimensions themselves.
    trajDimName = trajDimNameDefault;
    trajVarName = trajVarNameDefault;
    timeDimName = timeDimNameDefault;
    timeVarName = timeVarNameDefault;

    latVarName = latVarNameDefault;
    lonVarName = lonVarNameDefault;
    elevVarName = elevVarNameDefault;

    Variable latVar = ncd.getRootGroup().findVariable( latVarName );
    latVar.addAttribute( new Attribute( "units", "degrees_north" ) );

    Variable lonVar = ncd.getRootGroup().findVariable( lonVarName );
    lonVar.addAttribute( new Attribute( "units", "degrees_east" ) );

    this.setTrajectoryInfo( ncfile.getRootGroup().findDimension( trajDimName ),
                            ncfile.getRootGroup().findVariable( trajVarName ),
                            ncfile.getRootGroup().findDimension( timeDimName ),
                            ncfile.getRootGroup().findVariable( timeVarName ),
                            ncfile.getRootGroup().findVariable( latVarName ),
                            ncfile.getRootGroup().findVariable( lonVarName ),
                            ncfile.getRootGroup().findVariable( elevVarName ) );
  }
}
