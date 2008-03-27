// $Id: SimpleTrajectoryObsDataset.java 63 2006-07-12 21:50:51Z edavis $
package ucar.nc2.dt.trajectory;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.TypedDatasetFactoryIF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.SimpleUnit;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Implements TrajectoryDataset for datasets with these characteristics:
 * <ul>
 *   <li> it has only one dimension, the dimension is UNLIMITED and is named "time"</li>
 *   <li> it has one coordinate variable, time(time), with units that
 *        are udunits time units</li>
 *   <li> has the variables latitude(time), longitude(time), and altitude(time) with
 *        units "deg", "deg", and "m", respectively.
 * </ul>
 *
 * @author edavis
 * @since Feb 22, 2005T5:37:14 PM
 */
public class SimpleTrajectoryObsDataset extends SingleTrajectoryObsDataset implements TypedDatasetFactoryIF
{
  private static String timeDimName = "time";
  private static String timeVarName = "time";
  private static String latVarName = "latitude";
  private static String lonVarName = "longitude";
  private static String elevVarName = "altitude";

  private static String trajId = "trajectory data";

  static public boolean isValidFile(NetcdfDataset ncd)
  {
    return ( buildConfig( ncd ) != null );
  }

  private static Config buildConfig( NetcdfDataset ncd )
  {
    // Check that only one dimension and that it is named "time".
    List list = ncd.getRootGroup().getDimensions();
    if ( list.size() != 1) return null;
    Dimension d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName)) return null;

    Config trajConfig = new Config();
    trajConfig.setTimeDim( d);

    // Check that have variable time(time) with units that are udunits time
    Variable var = ncd.getRootGroup().findVariable( timeVarName);
    if ( var == null) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName)) return null;
    String units = var.findAttribute( "units").getStringValue();
    Date date = DateUnit.getStandardDate( "0 " + units);
    if ( date == null) return null;

    trajConfig.setTimeVar( var);

    // Check for variable latitude(time) with units of "deg".
    var = ncd.getRootGroup().findVariable( latVarName);
    if ( var == null ) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName)) return null;
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "degrees_north")) return null;

    trajConfig.setLatVar( var);

    // Check for variable longitude(time) with units of "deg".
    var = ncd.getRootGroup().findVariable( lonVarName);
    if ( var == null ) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName)) return null;
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "degrees_east")) return null;

    trajConfig.setLonVar( var);

    // Check for variable altitude(time) with units of "m".
    var = ncd.getRootGroup().findVariable( elevVarName);
    if ( var == null) return null;
    list = var.getDimensions();
    if ( list.size() != 1) return null;
    d = (Dimension) list.get(0);
    if ( ! d.getName().equals( timeDimName)) return null;
    units = var.findAttribute( "units").getStringValue();
    if ( ! SimpleUnit.isCompatible( units, "meters")) return null;

    trajConfig.setElevVar( var);

    trajConfig.setTrajectoryId( trajId);
    return trajConfig;
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException {
    return new SimpleTrajectoryObsDataset( ncd);
  }
  public FeatureType getScientificDataType() { return FeatureType.TRAJECTORY; }

  public SimpleTrajectoryObsDataset() {}

  public SimpleTrajectoryObsDataset( NetcdfDataset ncd ) throws IOException
  {
    super( ncd);

    Config trajConfig = buildConfig( ncd);
    this.setTrajectoryInfo( trajConfig );
  }
}
