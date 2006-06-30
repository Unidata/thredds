// $Id: SimpleTrajectoryObsDataset.java,v 1.11 2006/02/09 22:57:12 edavis Exp $
package ucar.nc2.dt.trajectory;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;
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
class SimpleTrajectoryObsDataset extends SingleTrajectoryObsDataset
{
  private static String timeDimName = "time";
  private static String timeVarName = "time";
  private static String latVarName = "latitude";
  private static String lonVarName = "longitude";
  private static String elevVarName = "altitude";

  private static String trajId = "trajectory data";

  static public boolean isMine(NetcdfDataset ncd)
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

  public SimpleTrajectoryObsDataset( NetcdfDataset ncd ) throws IOException
  {
    super( ncd);

    Config trajConfig = buildConfig( ncd);
    this.setTrajectoryInfo( trajConfig );
  }
}

/*
 * $Log: SimpleTrajectoryObsDataset.java,v $
 * Revision 1.11  2006/02/09 22:57:12  edavis
 * Add syncExtend() method to TrajectoryObsDataset.
 *
 * Revision 1.10  2005/11/28 23:15:36  edavis
 * Some minor clean up.
 *
 * Revision 1.9  2005/05/23 17:02:22  edavis
 * Deal with converting elevation data into "meters".
 *
 * Revision 1.8  2005/05/16 16:47:52  edavis
 * A few improvements to SingleTrajectoryObsDataset and start using
 * it in RafTrajectoryObsDataset. Add MultiTrajectoryObsDataset
 * (based on SingleTrajectoryObsDataset) and use in
 * Float10TrajectoryObsDataset.
 *
 * Revision 1.7  2005/05/06 22:19:13  edavis
 * Extract subclass SingleTrajectoryObsDataset.
 *
 * Revision 1.6  2005/05/05 16:08:13  edavis
 * Add TrajectoryObsDatatype.getDataVariables() methods.
 *
 * Revision 1.5  2005/05/04 17:18:45  caron
 * *** empty log message ***
 *
 * Revision 1.4  2005/05/01 19:16:03  caron
 * move station to point package
 * add implementations for common interfaces
 * refactor station adapters
 *
 * Revision 1.3  2005/03/18 00:29:07  edavis
 * Finish trajectory implementations with the new TrajectoryObsDatatype
 * and TrajectoryObsDataset interfaces and update tests.
 *
 * Revision 1.2  2005/03/15 23:20:53  caron
 * new radial dataset interface
 * change getElevation() to getAltitude()
 *
 * Revision 1.1  2005/03/10 21:34:17  edavis
 * Redo trajectory implementations with new TrajectoryObsDatatype and
 * TrajectoryObsDataset interfaces.
 *
 * Revision 1.1  2005/03/01 22:02:23  edavis
 * Two more implementations of the TrajectoryDataset interface.
 *
 */