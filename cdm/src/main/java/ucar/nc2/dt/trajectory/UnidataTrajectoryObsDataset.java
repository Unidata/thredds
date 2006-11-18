package ucar.nc2.dt.trajectory;

import ucar.nc2.dt.TypedDatasetFactoryIF;
import ucar.nc2.dt.TypedDataset;
import ucar.nc2.dataset.NetcdfDataset;
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
  public boolean isMine(NetcdfDataset ds) { return false; } //return isValidFile(ds); }
  public TypedDataset open( NetcdfDataset ncd, ucar.nc2.util.CancelTask task, StringBuffer errlog) throws IOException
  {
    return new UnidataTrajectoryObsDataset( ncd);
  }
  public UnidataTrajectoryObsDataset() {}

  public UnidataTrajectoryObsDataset( NetcdfFile ncf) throws IOException
  {
    super( ncf );

    Attribute conventionsAtt = ncf.findGlobalAttributeIgnoreCase( "Conventions" );
    if ( ! conventionsAtt.getStringValue().equals( "NCAR-RAF/nimbus" ) )
      throw new IllegalArgumentException( "File <" + ncf.getId() + "> not a \"NCAR-RAF/nimbus\" convention file." );

    Attribute versionAtt = ncf.findGlobalAttributeIgnoreCase( "Version" );
    if ( versionAtt.getStringValue().equals( "1.2"))
    {
      timeDimName = "Time";
      timeVarName = "time_offset";

      latVarName = "LAT";
      lonVarName = "LON";
      elevVarName = "ALT";

      // Determine and set the units (base time) for the time variable.
      String baseTimeVarName = "base_time";
      Variable baseTimeVar = ncfile.findVariable( baseTimeVarName );
      int baseTime = baseTimeVar.readScalarInt();
      Date baseTimeDate;
      if ( baseTime != 0 )
      {
        String baseTimeString = baseTime + " seconds since 1970-01-01T00:00:00";
        baseTimeDate = DateUnit.getStandardDate( baseTimeString );
      }
      else
      {
        Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.US );

        // Read in start point date/time information and create java.util.Date
        Array yearArray, monthArray, dayArray, hourArray, minuteArray, secondArray, tmpTimeArray;
        try
        {
          yearArray = ncf.findVariable( "YEAR" ).read( "0" );
          monthArray = ncf.findVariable( "MONTH" ).read( "0" );
          dayArray = ncf.findVariable( "DAY" ).read( "0" );
          hourArray = ncf.findVariable( "HOUR" ).read( "0" );
          minuteArray = ncf.findVariable( "MINUTE" ).read( "0" );
          secondArray = ncf.findVariable( "SECOND" ).read( "0" );
          tmpTimeArray = ncf.findVariable( timeVarName).read( "0" );
        }
        catch ( InvalidRangeException e )
        {
          throw new IOException( "Failed while reading first value of YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, or time_offset: " + e.getMessage() );
        }

        calendar.clear();
        calendar.set( Calendar.YEAR, (int) yearArray.getFloat( yearArray.getIndex() ) );
        calendar.set( Calendar.MONTH, (int) monthArray.getFloat( monthArray.getIndex() ) );
        calendar.set( Calendar.DAY_OF_MONTH, (int) dayArray.getFloat( dayArray.getIndex() ) );
        calendar.set( Calendar.HOUR_OF_DAY, (int) hourArray.getFloat( hourArray.getIndex() ) );
        calendar.set( Calendar.MINUTE, (int) minuteArray.getFloat( minuteArray.getIndex() ) );
        calendar.set( Calendar.SECOND, (int) secondArray.getFloat( secondArray.getIndex() ) );
        calendar.set( Calendar.MILLISECOND, 0 );
        // Calculating base time so subtract seconds in time variable first value.
        calendar.add( Calendar.SECOND, - (int) tmpTimeArray.getFloat( tmpTimeArray.getIndex() ));
        baseTimeDate = calendar.getTime();
      }

      DateFormatter formatter = new DateFormatter();
      String timeUnitsString = "seconds since " + formatter.getStandardDateString( baseTimeDate );
      ncfile.findVariable( timeVarName ).addAttribute( new Attribute( "units", timeUnitsString ) );

      // Make sure alt units are "meters" convertible.
      String elevVarUnitsString = ncfile.findVariable( elevVarName ).findAttribute( "units").getStringValue();
      if ( ! SimpleUnit.isCompatible( elevVarUnitsString, "meters"))
      {
        if ( elevVarUnitsString.equals( "M"))
          ncfile.findVariable( elevVarName ).addAttribute( new Attribute( "units", "meters"));
      }
    }
    else if ( versionAtt.getStringValue().equals( "1.3" ) )
    {
      // Set default dimension and variable names.
      timeDimName = "Time";
      timeVarName = "Time";

      latVarName = "LAT";
      lonVarName = "LON";
      elevVarName = "ALT";

      // Set dimension and variable names as indicated by global attribute "coordinates".
      Attribute coordsAttrib = ncfile.findGlobalAttribute( "coordinates");
      if ( coordsAttrib != null )
      {
        String coordsAttribValue = coordsAttrib.getStringValue();
        if ( coordsAttribValue != null)
        {
          String[] varNames = coordsAttribValue.split( " ");
          latVarName = varNames[ 1];
          lonVarName = varNames[0];
          elevVarName = varNames[2];
          timeVarName = varNames[3];
          timeDimName = timeVarName;
        }
      }

      // If time variable is all zeros, set time variable to "time_offset".
      if ( timeVarAllZeros() )
      {
        timeVarName = "time_offset";
      }

      String varUnitsString = this.ncfile.findVariable( latVarName ).findAttributeIgnoreCase( "units" ).getStringValue();
      if ( !SimpleUnit.isCompatible( varUnitsString, "degrees_north" ) )
      {
        throw new IllegalStateException( "Latitude variable <" + latVarName + "> units not udunits compatible w/ \"degrees_north\"." );
      }
      varUnitsString = this.ncfile.findVariable( lonVarName ).findAttributeIgnoreCase( "units" ).getStringValue();
      if ( !SimpleUnit.isCompatible( varUnitsString, "degrees_east" ) )
      {
        throw new IllegalStateException( "Longitude variable <" + lonVarName + "> units not udunits compatible w/ \"degrees_east\"." );
      }
      varUnitsString = this.ncfile.findVariable( elevVarName ).findAttributeIgnoreCase( "units" ).getStringValue();
      if ( !SimpleUnit.isCompatible( varUnitsString, "meters" ) )
      {
        throw new IllegalStateException( "Elevation variable <" + elevVarName + "> units not udunits compatible w/ \"m\"." );
      }
      String timeUnitsString = this.ncfile.findVariable( timeVarName ).findAttributeIgnoreCase( "units" ).getStringValue();
      if ( !SimpleUnit.isCompatible( timeUnitsString, "seconds since 1970-01-01 00:00:00" ) )
      {
        throw new IllegalStateException( "Time variable units <" + timeUnitsString + "> not udunits compatible w/ \"seconds since 1970-01-01 00:00:00\"." );
      }
    }
    else
    {
      throw new IllegalArgumentException( "File <" + ncf.getId() + "> not a version 1.2 or 1.3 \"NCAR-RAF/nimbus\" convention file." );
    }

    Config trajConfig = new Config( "1Hz data",
                                    ncf.getRootGroup().findDimension( timeDimName ),
                                    ncf.getRootGroup().findVariable( timeVarName ),
                                    ncf.getRootGroup().findVariable( latVarName ),
                                    ncf.getRootGroup().findVariable( lonVarName ),
                                    ncf.getRootGroup().findVariable( elevVarName ));
    this.setTrajectoryInfo( trajConfig );

  }

  private boolean timeVarAllZeros() throws IOException
  {
    Variable curTimeVar = this.ncfile.getRootGroup().findVariable( timeVarName);
    List section = new ArrayList(1);
    Array a = null;
    try
    {
      section.add ( new Range( 0, 2));
      a = curTimeVar.read( section);
    }
    catch ( InvalidRangeException e )
    {
      throw new IOException( "Invalid range (0,2): " + e.getMessage());
    }
    IndexIterator it = a.getIndexIterator();
    for( ; it.hasNext(); )
    {
      if ( it.getDoubleNext() != 0.0 ) return ( false );
    }
    return( true);
  }
}
