package ucar.nc2.dt.trajectory;

import ucar.nc2.dt.TypedDatasetFactoryIF;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dt.point.UnidataObsDatasetHelper;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.SimpleUnit;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.IndexIterator;

import java.io.IOException;
import java.util.*;

import thredds.catalog.DataType;

/**
 * Handle trajectory data files that follow the
 * Unidata Observation Dataset convention version 1.0.
 *
 * Documentation on this convention is available at
 * http://www.unidata.ucar.edu/software/netcdf-java/formats/UnidataObsConvention.html
 *
 * @author edavis
 * @since 2006-11-17T17:26:14-0700
 */
public class UnidataTrajectoryObsDataset extends SingleTrajectoryObsDataset  implements TypedDatasetFactoryIF
{
  private String timeDimName;
  private String timeVarName;
  private String latVarName;
  private String lonVarName;
  private String elevVarName;

  static public boolean isValidFile( NetcdfFile ds)
  {
    Attribute cdmDtAtt = ds.findGlobalAttributeIgnoreCase( "cdm_data_type");
    if ( cdmDtAtt == null )
      cdmDtAtt = ds.findGlobalAttributeIgnoreCase( "cdm_datatype");
    if ( cdmDtAtt == null ) return false;
    if ( ! cdmDtAtt.isString() ) return false;

    String cdmDtString = cdmDtAtt.getStringValue();
    if ( cdmDtString == null ) return false;
    if ( ! cdmDtString.equalsIgnoreCase( thredds.catalog.DataType.TRAJECTORY.toString() ))
      return false;

    Attribute conventionsAtt = ds.findGlobalAttributeIgnoreCase( "Conventions");
    if ( conventionsAtt == null) return( false);
    if ( ! conventionsAtt.isString()) return( false);
    String convString = conventionsAtt.getStringValue();

    StringTokenizer stoke = new StringTokenizer( convString, "," );
    while ( stoke.hasMoreTokens() )
    {
      String toke = stoke.nextToken().trim();
      if ( toke.equalsIgnoreCase( "Unidata Observation Dataset v1.0" ) )
        return true;
    }

    return false;
  }

    /////////////////////////////////////////////////
  // TypedDatasetFactoryIF
  public boolean isMine(NetcdfDataset ds) { return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException
  {
    return new UnidataTrajectoryObsDataset( ncd);
  }
  public DataType getScientificDataType() { return DataType.TRAJECTORY; }

  public UnidataTrajectoryObsDataset() {}

  public UnidataTrajectoryObsDataset( NetcdfDataset ncd) throws IOException
  {
    super( ncd );

    // coordinate variables
    latVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Lat );
    lonVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Lon );
    timeVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Time );
    elevVar = UnidataObsDatasetHelper.getCoordinate( ncd, AxisType.Height );

    if ( latVar == null )
      throw new IllegalStateException( "Missing latitude variable" );
    if ( lonVar == null )
      throw new IllegalStateException( "Missing longitude coordinate variable" );
    if ( timeVar == null )
      throw new IllegalStateException( "Missing time coordinate variable" );
    if ( elevVar == null )
      throw new IllegalStateException( "Missing height coordinate variable" );

    timeDimName = timeVar.getCoordinateDimension().getName();
    timeVarName = timeVar.getName();
    latVarName = latVar.getName();
    lonVarName = lonVar.getName();
    elevVarName = elevVar.getName();


    Config trajConfig = new Config( "1Hz data",
                                    ncd.getRootGroup().findDimension( timeDimName ),
                                    ncd.getRootGroup().findVariable( timeVarName ),
                                    ncd.getRootGroup().findVariable( latVarName ),
                                    ncd.getRootGroup().findVariable( lonVarName ),
                                    ncd.getRootGroup().findVariable( elevVarName ));
    this.setTrajectoryInfo( trajConfig );

  }
}
