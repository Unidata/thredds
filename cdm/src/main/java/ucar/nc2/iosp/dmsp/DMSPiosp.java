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
// $Id: DMSPiosp.java 63 2006-07-12 21:50:51Z edavis $
package ucar.nc2.iosp.dmsp;

import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CancelTask;
import ucar.ma2.*;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.*;
import java.text.ParseException;

/**
 * This ucar.nc2.iosp.IOServiceProvider provides access to DMSP satellite data
 * in the NOAA/NGDC DMSP archive format. Currently only data from the OLS
 * instrument is supported, in particular only NOAA/NGDC DMSP OIS (OLS
 * Integrated Smooth) data files. The OIS data is visible and thermal
 * imagery at 2.7km resolution.
 *
 * The DMSP satellites are polar orbiting satellites crossing the equator,
 * depending on the satellite, at either dawn/dusk or noon/midnight.
 *
 * More information is available at http://dmsp.ngdc.noaa.gov/.
 *
 * @author Ethan Davis
 * @since 2004-08-13T13:21:19 MDT
 */
public class DMSPiosp extends AbstractIOServiceProvider {
  private NetcdfFile ncFile = null;
  private ucar.unidata.io.RandomAccessFile raf = null;

  DMSPHeader header = null;

  // Cached data arrays for time.
  private float[] calculatedTime = null; // seconds since "startDate" attribute
  private String startDateString = null;
  private Date startDate = null;
  private int[] cachedYear = null;
  private int[] cachedDayOfYear = null;
  private double[] cachedSecondsOfDay = null;

  // Cached data arrays for lat/lon.
  private float[] calculatedLatitude = null;
  private float[] calculatedLongitude = null;
  private float[] cachedSatEphemLatitude = null;
  private float[] cachedSatEphemLongitude = null;
  private float[] cachedSatEphemAltitude = null;
  private float[] cachedSatEphemHeading = null;
  private float[] cachedScannerOffset = null;
  private byte[] cachedScanDirection = null;

  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf )
  {
    DMSPHeader localHeader = new DMSPHeader();
    return( localHeader.isValidFile( raf ));
  }

  public String getFileTypeId() {
    return "DMSP";
  }

  public String getFileTypeDescription() {
    return "Defense Meteorological Satellite Program";
  }

  public void open( RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask )
          throws IOException
  {
    //this.raf = raf;
    this.ncFile = ncfile;
    this.raf = raf;

    this.raf.order( ucar.unidata.io.RandomAccessFile.BIG_ENDIAN ); // DMSP files are XDR

    this.header = new DMSPHeader();
    this.header.read( this.raf, this.ncFile );

    // Create dimension lists for adding to variables.
    List nonScanDimList = new ArrayList();
    nonScanDimList.add( this.header.getNumDataRecordsDim() );

    List scanDimList = new ArrayList();
    scanDimList.add( this.header.getNumDataRecordsDim() );
    scanDimList.add( this.header.getNumSamplesPerBandDim() );

    Iterator varInfoIt = VariableInfo.getAll().iterator();
    VariableInfo curVarInfo = null;
    Variable curVariable = null;
    while ( varInfoIt.hasNext() )
    {
      curVarInfo = (VariableInfo) varInfoIt.next();
      curVariable = new Variable( this.ncFile, this.ncFile.getRootGroup(), null, curVarInfo.getName());
      curVariable.setDataType( curVarInfo.getDataType());
      if ( curVarInfo.getNumElementsInRecord() == 1)
      {
        curVariable.setDimensions( nonScanDimList );
      }
      else
      {
        curVariable.setDimensions( scanDimList );
      }
      curVariable.addAttribute( new Attribute( "long_name", curVarInfo.getLongName()));
      curVariable.addAttribute( new Attribute( "units", curVarInfo.getUnits()));

      if ( curVariable.getName().equals( "latitude"))
      {
        curVariable.addAttribute( new Attribute( "calculatedVariable", "Using the geometry of the satellite scans and an ellipsoidal earth (a=6378.14km and e=0.0818191830)."));
        curVariable.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.Lat.toString()));
      }
      else if ( curVariable.getName().equals( "longitude"))
      {
        curVariable.addAttribute( new Attribute( "calculatedVariable", "Using the geometry of the satellite scans and an ellipsoidal earth (a=6378.14km and e=0.0818191830)."));
        curVariable.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.Lon.toString()));
      }
      else if ( curVariable.getName().equals( "time"))
      {
        curVariable.addAttribute( new Attribute( "calculatedVariable", "Using the satellite epoch for each scan."));
        this.startDateString = this.header.getStartDateAtt().getStringValue();
        try
        {
          this.startDate = DMSPHeader.DateFormatHandler.ISO_DATE_TIME.getDateFromDateTimeString( this.startDateString);
        }
        catch ( ParseException e )
        {
          throw new IOException( "Invalid DMSP file: \"startDate\" attribute value <" + this.startDateString +
                                 "> not parseable with format string <" + DMSPHeader.DateFormatHandler.ISO_DATE_TIME.getDateTimeFormatString() + ">.");
        }
        curVariable.addAttribute( new Attribute( "units", "seconds since " + this.startDateString));
        curVariable.addAttribute( new Attribute( _Coordinate.AxisType, AxisType.Time.toString()));
      }
      else if ( curVariable.getName().equals( "infraredImagery"))
      {
        curVariable.addAttribute( new Attribute( _Coordinate.Axes, "latitude longitude"));
        curVariable.addAttribute( new Attribute( "_Unsigned", "true"));
        curVariable.addAttribute( new Attribute( "scale_factor", new Float((310.0-190.0)/(256.0-1.0))));
        curVariable.addAttribute( new Attribute( "add_offset", new Float( 190.0)));
        curVariable.addAttribute( new Attribute( "description",
                                                 "Infrared pixel values correspond to a temperature range of 190 to 310 " +
                                                 "Kelvins in 256 equally spaced steps. Onboard calibration is performed " +
                                                 "during each scan. -- From http://dmsp.ngdc.noaa.gov/html/sensors/doc_ols.html"));
      }
      else if ( curVariable.getName().equals( "visibleImagery"))
      {
        curVariable.addAttribute( new Attribute( _Coordinate.Axes, "latitude longitude"));
        curVariable.addAttribute( new Attribute( "_Unsigned", "true"));
        curVariable.addAttribute( new Attribute( "description",
                                                 "Visible pixels are relative values ranging from 0 to 63 rather than " +
                                                 "absolute values in Watts per m^2. Instrumental gain levels are adjusted " +
                                                 "to maintain constant cloud reference values under varying conditions of " +
                                                 "solar and lunar illumination. Telescope pixel values are replaced by " +
                                                 "Photo Multiplier Tube (PMT) values at night. " +
                                                 "-- From http://dmsp.ngdc.noaa.gov/html/sensors/doc_ols.html"));
      }

      this.ncFile.addVariable( null, curVariable);
    }

    // Make sure the NetcdfFile is setup properly.
    this.ncFile.finish();

  }

  public Array readData( Variable v2, Section section ) throws IOException, InvalidRangeException
  {
    if ( v2 == null ) throw new IllegalArgumentException( "Variable must not be null.");
    if ( section == null ) throw new IllegalArgumentException( "Section must not be null.");

    Object data = null;
    Array dataArray = null;
    List<Range> ranges = section.getRanges();

    // Read in date/time variables for each scan (year, dayOfYear, and secondsOfDay).
    if ( v2.getName().equals( VariableInfo.YEAR.getName()))
    {
      if ( this.cachedYear == null )
      {
        this.cachedYear = (int[]) this.readIntArray1D( VariableInfo.YEAR.getByteOffsetInRecord());
      }
      data = this.cachedYear;
      dataArray = Array.factory( int.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges).copy());
    }
    else if ( v2.getName().equals( VariableInfo.DAY_OF_YEAR.getName()))
    {
      if ( this.cachedDayOfYear == null )
      {
        this.cachedDayOfYear = (int[]) this.readIntArray1D( VariableInfo.DAY_OF_YEAR.getByteOffsetInRecord());
      }
      data = this.cachedDayOfYear;
      dataArray = Array.factory( int.class, v2.getShape(), data);
      return ( dataArray.sectionNoReduce( ranges ).copy() );
    }
    else if ( v2.getName().equals( VariableInfo.SECONDS_OF_DAY.getName()))
    {
      if ( this.cachedSecondsOfDay == null )
      {
        this.cachedSecondsOfDay = (double[]) this.readDoubleArray1D( VariableInfo.SECONDS_OF_DAY.getByteOffsetInRecord());
      }
      data = this.cachedSecondsOfDay;
      dataArray = Array.factory( double.class, v2.getShape(), data);
      return ( dataArray.sectionNoReduce( ranges ).copy() );
    }
    else if ( v2.getName().equals( VariableInfo.TIME.getName()))
    {
      if ( this.calculatedTime == null )
      {
        this.calculatedTime = new float[ v2.getShape()[0] ];

        // Make sure the cached data for year, dayOfYear, and secondsOfDay
        // is available as it is used in the time calculation.
        // [Note: don't need Arrays returned.]
        // @todo Could seperate reading cache from Array production.
        Variable curVar = this.ncFile.findVariable( VariableInfo.YEAR.getName() );
        this.readData( curVar, curVar.getShapeAsSection());

        curVar = this.ncFile.findVariable( VariableInfo.DAY_OF_YEAR.getName() );
        this.readData( curVar, curVar.getShapeAsSection());

        curVar = this.ncFile.findVariable( VariableInfo.SECONDS_OF_DAY.getName() );
        this.readData( curVar, curVar.getShapeAsSection());

        Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ), Locale.US );

        double secOfDay, secOfHour, secOfMinute;
        double hours, mins, secs, millis;
        for( int i = 0; i < v2.getShape()[0]; i++ )
        {
          calendar.clear();
          calendar.set( Calendar.YEAR, this.cachedYear[i]);
          calendar.set( Calendar.DAY_OF_YEAR, this.cachedDayOfYear[i]);
          secOfDay = this.cachedSecondsOfDay[i];
          hours = Math.floor( secOfDay/3600);
          secOfHour = secOfDay % 3600.0;
          mins = Math.floor( secOfHour/60.0);
          secOfMinute = secOfHour % 60.0;
          secs = Math.floor( secOfMinute);
          millis = Math.floor(( secOfMinute - secs) * 1000.0);
          calendar.add( Calendar.HOUR, (int) hours);
          calendar.add( Calendar.MINUTE, (int) mins);
          calendar.add( Calendar.SECOND, (int) secs);
          calendar.add( Calendar.MILLISECOND, (int) millis );
          //calendar.add( Calendar.MILLISECOND, (int) ((this.cachedSecondsOfDay[i] - ((double) (int) this.cachedSecondsOfDay[i])) * 1000.0 ) );
          this.calculatedTime[i] = ( calendar.getTimeInMillis() - this.startDate.getTime()) / 1000.0F;
        }

        dataArray = Array.factory( float.class, v2.getShape(), this.calculatedTime);
        return ( dataArray.sectionNoReduce( ranges ).copy() );
      }
    }

    // Read in satellite ephemeris variables for each scan (satEphemLatitude,
    // satEphemLongitude, satEphemAltitude, satEphemHeading).
    else if ( v2.getName().equals( VariableInfo.SAT_EPHEM_LATITUDE.getName()))
    {
      if ( this.cachedSatEphemLatitude == null )
      {
        this.cachedSatEphemLatitude = (float[]) this.readFloatArray1D( VariableInfo.SAT_EPHEM_LATITUDE.getByteOffsetInRecord());
      }
      data = this.cachedSatEphemLatitude;
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return ( dataArray.sectionNoReduce( ranges ).copy() );
    }
    else if ( v2.getName().equals( VariableInfo.SAT_EPHEM_LONGITUDE.getName()))
    {
      if ( this.cachedSatEphemLongitude == null )
      {
        this.cachedSatEphemLongitude = (float[]) this.readFloatArray1D( VariableInfo.SAT_EPHEM_LONGITUDE.getByteOffsetInRecord());
      }
      data = this.cachedSatEphemLongitude;
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return ( dataArray.sectionNoReduce( ranges ).copy() );
    }
    else if ( v2.getName().equals( VariableInfo.SAT_EPHEM_ALTITUDE.getName()))
    {
      if ( this.cachedSatEphemAltitude == null )
      {
        this.cachedSatEphemAltitude = (float[]) this.readFloatArray1D( VariableInfo.SAT_EPHEM_ALTITUDE.getByteOffsetInRecord());
      }
      data = this.cachedSatEphemAltitude;
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return ( dataArray.sectionNoReduce( ranges ).copy() );
    }
    else if ( v2.getName().equals( VariableInfo.SAT_EPHEM_HEADING.getName()))
    {
      if ( this.cachedSatEphemHeading == null )
      {
        this.cachedSatEphemHeading = (float[]) this.readFloatArray1D( VariableInfo.SAT_EPHEM_HEADING.getByteOffsetInRecord());
      }
      data = this.cachedSatEphemHeading;
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return ( dataArray.sectionNoReduce( ranges ).copy() );
    }

    // Read in scan information variables for each scan (scannerOffset and scanDirection).
    else if ( v2.getName().equals( VariableInfo.SCANNER_OFFSET.getName()))
    {
      if ( this.cachedScannerOffset == null )
      {
        this.cachedScannerOffset = (float[]) this.readFloatArray1D( VariableInfo.SCANNER_OFFSET.getByteOffsetInRecord());
      }
      data = this.cachedScannerOffset;
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return ( dataArray.sectionNoReduce( ranges ).copy() );
    }
    else if ( v2.getName().equals( VariableInfo.SCAN_DIRECTION.getName()))
    {
      if ( this.cachedScanDirection == null )
      {
        this.cachedScanDirection = (byte[]) this.readUCharArray1D( VariableInfo.SCAN_DIRECTION.getByteOffsetInRecord());
      }
      data = this.cachedScanDirection;
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return ( dataArray.sectionNoReduce( ranges ).copy() );
    }

    // Read in sun and moon information variables for each scan (solarElevation,
    // solarAzimuth, lunarElevation, lunarAzimuth, and lunarPhase).
    else if ( v2.getName().equals( VariableInfo.SOLAR_ELEVATION.getName()))
    {
      data = this.readFloatArray1D( VariableInfo.SOLAR_ELEVATION.getByteOffsetInRecord());
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.SOLAR_AZIMUTH.getName()))
    {
      data = this.readFloatArray1D( VariableInfo.SOLAR_AZIMUTH.getByteOffsetInRecord());
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.LUNAR_ELEVATION.getName()))
    {
      data = this.readFloatArray1D( VariableInfo.LUNAR_ELEVATION.getByteOffsetInRecord());
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.LUNAR_AZIMUTH.getName()))
    {
      data = this.readFloatArray1D( VariableInfo.LUNAR_AZIMUTH.getByteOffsetInRecord());
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.LUNAR_PHASE.getName()))
    {
      data = this.readFloatArray1D( VariableInfo.LUNAR_PHASE.getByteOffsetInRecord());
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }

    // Read in gain information variables for each scan (gainCode, gainMode,
    // gainSubMode, and tChannelGain).
    else if ( v2.getName().equals( VariableInfo.GAIN_CODE.getName()))
    {
      data = this.readFloatArray1D( VariableInfo.GAIN_CODE.getByteOffsetInRecord());
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.GAIN_MODE.getName()))
    {
      data = this.readUCharArray1D( VariableInfo.GAIN_MODE.getByteOffsetInRecord());
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.GAIN_SUB_MODE.getName()))
    {
      data = this.readUCharArray1D( VariableInfo.GAIN_SUB_MODE.getByteOffsetInRecord());
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.T_CHANNEL_GAIN.getName()))
    {
      data = this.readFloatArray1D( VariableInfo.T_CHANNEL_GAIN.getByteOffsetInRecord());
      dataArray = Array.factory( float.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }

    // Read in calibration variables for each scan (hotTCalSegmentID,
    // coldTCalSegmentID, hotTCal, coldTCal, PMTCal).
    else if ( v2.getName().equals( VariableInfo.HOT_T_CAL_SEGMENT_ID.getName()))
    {
      data = this.readUCharArray1D( VariableInfo.HOT_T_CAL_SEGMENT_ID.getByteOffsetInRecord());
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.COLD_T_CAL_SEGMENT_ID.getName()))
    {
      data = this.readUCharArray1D( VariableInfo.COLD_T_CAL_SEGMENT_ID.getByteOffsetInRecord());
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.HOT_T_CAL.getName()))
    {
      data = this.readUCharArray1D( VariableInfo.HOT_T_CAL.getByteOffsetInRecord());
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.COLD_T_CAL.getName()))
    {
      data = this.readUCharArray1D( VariableInfo.COLD_T_CAL.getByteOffsetInRecord());
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.PMT_CAL.getName()))
    {
      data = this.readUCharArray1D( VariableInfo.PMT_CAL.getByteOffsetInRecord());
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }

    // Read in scan quality flag variables (visibleScanQualityFlag and thermalScanQualityFlag).
    else if ( v2.getName().equals( VariableInfo.VISIBLE_SCAN_QUALITY_FLAG.getName()))
    {
      data = this.readIntArray1D( VariableInfo.VISIBLE_SCAN_QUALITY_FLAG.getByteOffsetInRecord());
      dataArray = Array.factory( int.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.THERMAL_SCAN_QUALITY_FLAG.getName()))
    {
      data = this.readIntArray1D( VariableInfo.THERMAL_SCAN_QUALITY_FLAG.getByteOffsetInRecord());
      dataArray = Array.factory( int.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }

    // Read in scan variables (visibleImagery and infraredImagery).
    else if ( v2.getName().equals( VariableInfo.VISIBLE_SCAN.getName()))
    {
      // @todo Scan alternates direction, flip every other array (see ScanDirection variable) [Not with OIS data]
      data = this.readByteArray2D( VariableInfo.VISIBLE_SCAN.getByteOffsetInRecord(),
                                   VariableInfo.VISIBLE_SCAN.getNumElementsInRecord());
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }
    else if ( v2.getName().equals( VariableInfo.THERMAL_SCAN.getName()))
    {
      // @todo Scan alternates direction, flip every other array (see ScanDirection variable) [Not with OIS data]
      data = this.readByteArray2D( VariableInfo.THERMAL_SCAN.getByteOffsetInRecord(),
                                   VariableInfo.THERMAL_SCAN.getNumElementsInRecord());
      dataArray = Array.factory( byte.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges));
    }

    else if ( v2.getName().equals( VariableInfo.LATITUDE.getName()) ||
              v2.getName().equals( VariableInfo.LONGITUDE.getName()))
    {
      if ( this.calculatedLatitude == null && this.calculatedLongitude == null )
      {
        this.calculatedLatitude = new float[ header.getNumDataRecords() * this.header.getNumSamplesPerBandDim().getLength() ];
        this.calculatedLongitude = new float[ header.getNumDataRecords() * this.header.getNumSamplesPerBandDim().getLength() ];

        // Make sure the cached data for SCANNER_OFFSET, SAT_EPHEM_LATITUDE,
        // SAT_EPHEM_LONGITUDE, SAT_EPHEM_ALTITUDE, and SAT_EPHEM_HEADING
        // is available as it is used in the latitude and longitude calculation.
        Variable curVar = this.ncFile.findVariable( VariableInfo.SCANNER_OFFSET.getName() );
        this.readData( curVar, curVar.getShapeAsSection() );

        curVar = this.ncFile.findVariable( VariableInfo.SAT_EPHEM_LATITUDE.getName() );
        this.readData( curVar, curVar.getShapeAsSection() );

        curVar = this.ncFile.findVariable( VariableInfo.SAT_EPHEM_LONGITUDE.getName() );
        this.readData( curVar, curVar.getShapeAsSection() );

        curVar = this.ncFile.findVariable( VariableInfo.SAT_EPHEM_ALTITUDE.getName() );
        this.readData( curVar, curVar.getShapeAsSection() );

        curVar = this.ncFile.findVariable( VariableInfo.SAT_EPHEM_HEADING.getName() );
        this.readData( curVar, curVar.getShapeAsSection() );

        int satID = Integer.parseInt( this.ncFile.getRootGroup().findAttribute( "spacecraftId").getStringValue().substring( 1));
        GeolocateOLS.geolocateOLS( satID, 0, header.getNumDataRecords(),
                                   this.cachedScannerOffset,
                                   this.cachedSatEphemLatitude, this.cachedSatEphemLongitude,
                                   this.cachedSatEphemAltitude, this.cachedSatEphemHeading,
                                   this.calculatedLatitude, this.calculatedLongitude );
      }

      if ( v2.getName().equals( VariableInfo.LATITUDE.getName()) )
      {
        data = this.calculatedLatitude;
      }
      else
      {
        data = this.calculatedLongitude;
      }

      dataArray = Array.factory( float.class, v2.getShape(), data);
      return( dataArray.sectionNoReduce( ranges).copy());
    }

    else
    {
      // This shouldn't happen.
      throw( new IOException( "Requested variable <name=" + v2.getName() + "> not in DMSP file."));
    }

    return( null); // Should return from one of above if-else-if blocks.
  }

  public void close() throws IOException
  {
    if (this.raf != null)
      this.raf.close();
    this.header = null;
  }

  Object readUCharArray1D( int offsetInRecord ) throws IOException
  {
    // XDR u_char fills 4 bytes but only uses one byte.
    int elementSizeInBytes = 4;
    byte[] elementArray = new byte[ elementSizeInBytes ];

    byte[] array = new byte[ header.getNumDataRecords() ];

    this.raf.seek( header.getRecordSizeInBytes() * header.getNumHeaderRecords() + offsetInRecord);

    for ( int i = 0; i < header.getNumDataRecords(); i++ )
    {
      this.raf.read( elementArray );
      array[ i] = elementArray[ 3];
      this.raf.skipBytes( header.getRecordSizeInBytes() - elementSizeInBytes );
    }

    return( array);
  }

  Object readIntArray1D( int offsetInRecord ) throws IOException
  {
    int elementSizeInBytes = 4;

    int[] array = new int[ header.getNumDataRecords() ];

    this.raf.seek( header.getRecordSizeInBytes() * header.getNumHeaderRecords() + offsetInRecord);

    for ( int i = 0; i < header.getNumDataRecords(); i++ )
    {
      this.raf.readInt( array, i, 1);
      this.raf.skipBytes( header.getRecordSizeInBytes() - elementSizeInBytes );
    }

    return( array);
  }

  Object readFloatArray1D( int offsetInRecord ) throws IOException
  {
    int elementSizeInBytes = 4;

    float[] array = new float[ header.getNumDataRecords() ];

    this.raf.seek( header.getRecordSizeInBytes() * header.getNumHeaderRecords() + offsetInRecord);

    for ( int i = 0; i < header.getNumDataRecords(); i++ )
    {
      this.raf.readFloat( array, i, 1);
      this.raf.skipBytes( header.getRecordSizeInBytes() - elementSizeInBytes );
    }

    return( array);
  }

  Object readDoubleArray1D( int offsetInRecord ) throws IOException
  {
    int elementSizeInBytes = 8;

    double[] array = new double[ header.getNumDataRecords() ];

    this.raf.seek( header.getRecordSizeInBytes() * header.getNumHeaderRecords() + offsetInRecord);

    for ( int i = 0; i < header.getNumDataRecords(); i++ )
    {
      this.raf.readDouble( array, i, 1);
      this.raf.skipBytes( header.getRecordSizeInBytes() - elementSizeInBytes );
    }

    return( array);
  }

  Object readByteArray2D( int offsetInRecord, int numElementsInRecord ) throws IOException
  {
    byte[] array = new byte[ header.getNumDataRecords() * numElementsInRecord ];

    this.raf.seek( header.getRecordSizeInBytes() * header.getNumHeaderRecords() + offsetInRecord);

    for ( int i = 0; i < header.getNumDataRecords(); i++ )
    {
      this.raf.read( array, i * numElementsInRecord, numElementsInRecord);
      this.raf.skipBytes( header.getRecordSizeInBytes() - numElementsInRecord );
    }

    return( array);
  }

  private static class VariableInfo
  {
    // @todo Each of these items should BE a ucar.nc2.Variable with additional info.
    // That way it would be easier to add additional variable attributes to a particular variable.
    // Currently need to change some here and others in open().
    private static java.util.List list = new java.util.ArrayList( 30 );
    private static java.util.HashMap hash = new java.util.HashMap( 30 );

    public final static VariableInfo YEAR = new VariableInfo(
            "year", "year at time of scan", "year", DataType.INT, 0, 1 );
    public final static VariableInfo DAY_OF_YEAR = new VariableInfo(
            "dayOfYear", "day of year at time of scan", "day of year", DataType.INT, 4, 1 );
    public final static VariableInfo SECONDS_OF_DAY = new VariableInfo(
            "secondsOfDay", "seconds of day at time of scan", "seconds of day", DataType.DOUBLE, 8, 1 );
    public final static VariableInfo TIME = new VariableInfo(
            "time", "time of scan", "seconds since ??? (see above)", DataType.FLOAT, -1, 1 );

    public final static VariableInfo SAT_EPHEM_LATITUDE = new VariableInfo(
            "satEphemLatitude", "geodetic latitude of the satellite for this scan", "degrees_north", DataType.FLOAT, 16, 1);
    public final static VariableInfo SAT_EPHEM_LONGITUDE = new VariableInfo(
            "satEphemLongitude", "longitude of the satellite for this scan", "degrees_east", DataType.FLOAT, 20, 1);
    public final static VariableInfo SAT_EPHEM_ALTITUDE = new VariableInfo(
            "satEphemAltitude", "altitude of the satellite for this scan", "kilometers", DataType.FLOAT, 24, 1);
    public final static VariableInfo SAT_EPHEM_HEADING = new VariableInfo(
            "satEphemHeading", "heading of the satellite (degrees west of north) for this scan", "degrees", DataType.FLOAT, 28, 1);

    public final static VariableInfo SCANNER_OFFSET = new VariableInfo(
            "scannerOffset", "scanner offset", "radians", DataType.FLOAT, 32, 1);
    public final static VariableInfo SCAN_DIRECTION = new VariableInfo(
            "scanDirection", "scan direction", "", DataType.BYTE, 36, 1);

    public final static VariableInfo SOLAR_ELEVATION = new VariableInfo(
            "solarElevation", "solar elevation", "degrees", DataType.FLOAT, 40, 1);
    public final static VariableInfo SOLAR_AZIMUTH = new VariableInfo(
            "solarAzimuth", "solar azimuth", "degrees", DataType.FLOAT, 44, 1);
    public final static VariableInfo LUNAR_ELEVATION = new VariableInfo(
            "lunarElevation", "lunar elevation", "degrees", DataType.FLOAT, 48, 1);
    public final static VariableInfo LUNAR_AZIMUTH = new VariableInfo(
            "lunarAzimuth", "lunar azimuth", "degrees", DataType.FLOAT, 52, 1);
    public final static VariableInfo LUNAR_PHASE = new VariableInfo(
            "lunarPhase", "lunar phase", "degrees", DataType.FLOAT, 56, 1);

    public final static VariableInfo GAIN_CODE = new VariableInfo(
            "gainCode", "gain code", "decibels", DataType.FLOAT, 60, 1);
    public final static VariableInfo GAIN_MODE = new VariableInfo(
            "gainMode", "gain mode (0=linear, 1=logrithmic)", "", DataType.BYTE, 64, 1);
    public final static VariableInfo GAIN_SUB_MODE = new VariableInfo(
            "gainSubMode", "gain sub-mode", "", DataType.BYTE, 68, 1);

    public final static VariableInfo HOT_T_CAL_SEGMENT_ID = new VariableInfo(
            "hotTCalSegmentID", "Hot T cal seg ID (0 = right, 1 = left)", "", DataType.BYTE, 72, 1);
    public final static VariableInfo COLD_T_CAL_SEGMENT_ID = new VariableInfo(
            "coldTCalSegmentID", "Cold T cal seg ID (0 = right, 1 = left)", "", DataType.BYTE, 76, 1);
    public final static VariableInfo HOT_T_CAL = new VariableInfo(
            "hotTCal", "Hot T calibration", "", DataType.BYTE, 80, 1);
    public final static VariableInfo COLD_T_CAL = new VariableInfo(
            "coldTCal", "Cold T calibration", "", DataType.BYTE, 84, 1);
    public final static VariableInfo PMT_CAL = new VariableInfo(
            "pmtCal", "Photomultiplier tube calibration", "", DataType.BYTE, 88, 1);
    public final static VariableInfo T_CHANNEL_GAIN = new VariableInfo(
            "tChannelGain", "T channel gain", "decibels", DataType.FLOAT, 92, 1);

    public final static VariableInfo VISIBLE_SCAN_QUALITY_FLAG = new VariableInfo(
            "visibleScanQualityFlag", "quality flag for the visible scan", "", DataType.INT, 96, 1);
    public final static VariableInfo THERMAL_SCAN_QUALITY_FLAG = new VariableInfo(
            "thermalScanQualityFlag", "quality flag for the thermal scan", "", DataType.INT, 1568, 1);

    public final static VariableInfo LATITUDE = new VariableInfo(
            "latitude", "latitude of pixel", "degrees_north", DataType.FLOAT, -1, 1465);
    public final static VariableInfo LONGITUDE = new VariableInfo(
            "longitude", "longitude of pixel", "degrees_east", DataType.FLOAT, -1, 1465);

    public final static VariableInfo VISIBLE_SCAN = new VariableInfo(
            "visibleImagery", "visible imagery  (6-bit per pixel)", "", DataType.BYTE, 100, 1465);
    public final static VariableInfo THERMAL_SCAN = new VariableInfo(
            "infraredImagery", "infrared imagery (8-bit per pixel)", "kelvin", DataType.BYTE, 1572, 1465);

    // Infrared pixel values correspond to a temperature range of 190 to 310 Kelvins
    // in 256 equally spaced steps. Onboard calibration is performed during each scan.
    // Visible pixels are relative values ranging from 0 to 63 rather than absolute
    // values in Watts per m^2. Instrumental gain levels are adjusted to maintain
    // constant cloud reference values under varying conditions of solar and lunar
    // illumination. Telescope pixel values are replaced by Photo Multiplier Tube (PMT)
    // values at night. -- From http://dmsp.ngdc.noaa.gov/html/sensors/doc_ols.html

    private String name = null;
    private String longName = null;
    private String units = null;
    private DataType dataType = null;
    private int byteOffsetInRecord = -1;
    private int numElementsInRecord = 0;
    
    private VariableInfo( String name, String long_name, String units,
                          DataType dataType, int byteOffsetInRecord,
                          int numElementsInRecord)
    {
      this.name = name;
      this.longName = long_name;
      this.units = units;
      this.dataType = dataType;
      this.byteOffsetInRecord = byteOffsetInRecord;
      this.numElementsInRecord = numElementsInRecord;

      list.add( this );
      hash.put( this.name,  this);
    }

    public static VariableInfo findByName( String name )
    {
      if ( name == null )
      {
        return( null);
      }
      return (VariableInfo) hash.get( name );
    }

    public static List getAll() { return( list); }
    public static Set getAllNames() { return( hash.keySet()); }
    public String getName() { return( this.name); }
    public String getLongName() { return( this.longName); }
    public String getUnits() { return( this.units); }
    public DataType getDataType() { return( this.dataType); }
    public int getByteOffsetInRecord() { return( this.byteOffsetInRecord); }
    public int getNumElementsInRecord() { return( this.numElementsInRecord); }

    public String toString()
    {
      StringBuffer retVal = new StringBuffer();
      retVal.append( "Variable(").append( this.getName()).append(",")
              .append( this.getLongName()).append( ",")
              .append( this.getUnits()).append( ",")
              .append( this.getDataType()).append( ",")
              .append( this.getByteOffsetInRecord()).append( ",")
              .append( this.getNumElementsInRecord()).append( ")");

      return( retVal.toString() );
    }
  }

  /**
   * Geolocate OLS swath data, return geodetic latitude and longitude.
   *
   * Author:  Ethan R. Davis
   * Date:    30 November 1994
   *
   * Earth-centered coordinate system:
   *   x-axis goes through zero longitude at the equator.
   *   y-axis goes through 90 degrees longitude at the equator.
   *   z-axis goes through the north pole.
   *
   * The plane tangent to the earth surface at the subsatellite point
   * will be called the tangent plane.
   *
   * Method:
   *   - Calculate the subsatellite point.
   *   - Calculate the satellite point.
   *   - Calculate the parametric equation for the line from
   *     the satellite to the scan point.
   *     - Calculate north and west in the tangent plane.
   *     - Calculate heading vector in tangen plane.
   *     - Calculate scan line vector in tangent plane.
   *     - Calculate the scan angle.
   *     - Calculate the scan point in the tangent plane
   *       (i.e., the point where the line-of-sight and the
   *        tangent plane intersect).
   *   - Calculate the intersection of the line-of-sight and the
   *     earth surface (elliptical model).  This is done by
   *     solving the quadratic equation that is found when the
   *     equations for the line-of-sight are substituted into the
   *     equations for an ellipse.
   *
   */
  static class GeolocateOLS
  {
    static void geolocateOLS( int satID, int dataType,
                              int numScans, float[] scannerOffset,
                              float[] satEphemLatitude, float[] satEphemLongitude,
                              float[] satEphemAltitude, float[] satEphemHeading,
                              float[] latitude, float[] longitude)
    {
      if ( satID < OLSSensorModel.RANGE_SAT_GROUPS[0][0] || satID > OLSSensorModel.RANGE_SAT_GROUPS[1][1])
        throw new IllegalArgumentException( "Satellite ID <" + satID + "> outside supported range <min=" + OLSSensorModel.RANGE_SAT_GROUPS[0][0] + ",max=" + OLSSensorModel.RANGE_SAT_GROUPS[1][1] + ">.");
      if ( dataType < 0 || dataType > OLSSensorModel.NUM_DATA_TYPES )
        throw new IllegalArgumentException( "Data type <" + dataType + "> not in valid range <min=0,max=" + OLSSensorModel.NUM_DATA_TYPES + ">.");
      if ( scannerOffset.length != numScans ) throw new IllegalArgumentException( "Size of scannerOffset vector <" + scannerOffset.length + "> not as expected <" + numScans + ">." );
      if ( satEphemLatitude.length != numScans ) throw new IllegalArgumentException( "Size of satEphemLatitude vector <" + satEphemLatitude.length + "> not as expected <" + numScans + ">." );
      if ( satEphemLongitude.length != numScans ) throw new IllegalArgumentException( "Size of satEphemLongitude vector <" + satEphemLongitude.length + "> not as expected <" + numScans + ">." );
      if ( satEphemAltitude.length != numScans ) throw new IllegalArgumentException( "Size of satEphemAltitude vector <" + satEphemAltitude.length + "> not as expected <" + numScans + ">." );
      if ( satEphemHeading.length != numScans ) throw new IllegalArgumentException( "Size of satEphemHeading vector <" + satEphemHeading.length + "> not as expected <" + numScans + ">." );
      if ( latitude.length != ( OLSSensorModel.numSamplesPerScan[ dataType] * numScans ))
        throw new IllegalArgumentException( "Size of latitude vector <" + latitude.length +
                                            "> not as expected <" + OLSSensorModel.numSamplesPerScan[ dataType] + " * " + numScans + ">.");
      if ( longitude.length != ( OLSSensorModel.numSamplesPerScan[ dataType] * numScans ))
        throw new IllegalArgumentException( "Size of longitude vector <" + longitude.length +
                                            "> not as expected <" + OLSSensorModel.numSamplesPerScan[ dataType] + " * " + numScans + ">.");
      //-----
      // Geodetic latitude and longitude.
      //-----
      double gdLatitude;
      double gdLongitude;

      //-----
      // Geocentric latitude and longitude.
      //-----
      double gcLatitude;
      double gcLongitude;

      //-----
      // Sine and cosine of the desired latitude and longitude.
      //-----
      double cosLat, cosLon;
      double sinLat, sinLon;

      //-----
      // The subsatellite position.
      // The unit vector normal to the surface at the subsatellite point.
      // The altitude of the satellite.
      // The satellites position.
      //-----
      double subSatPoint[] = new double[3];
      double surfaceNormal[] = new double[3];
      double satAltitude;
      double satPoint[] = new double[3];

      //-----
      // Unit vectors in tangent plane pointing north and west.
      //-----
      double north[] = new double[3];
      double west[] = new double[3];
      double projectMag;

      //-----
      // Satellite heading angle (west of north), cosine and sine
      // of the heading, and the unit vector of the heading in the
      // tangent plane.
      //-----
      double satHeadingAngle;
      double cosHeading;
      double sinHeading;
      double satHeading[] = new double[3];

      //-----
      // Line in tangent plane perpendicular to the heading
      // (assume the scanner runs perpendicular to heading).
      // Point on scanLine that line-of-sight intersects.
      //-----
      double scanLine[] = new double[3];
      double scannerAngle;
      double scanPoint[] = new double[3];
      double scanPointMag;

      //-----
      // Parametric equation for the line-of-sight .
      //-----
      double lineOfSightSlope[] = new double[3];
      double lineParameter;

      //-----
      // Variables for determining where the line-of-sight and
      // the ellipsoid model of the earth intersect.
      //-----
      double earthMajorAxis;
      double earthMinorAxis;

      double earthMajorAxisSquared;
      double earthMinorAxisSquared;

      double quadraticEqnA;
      double quadraticEqnB;
      double quadraticEqnC;

      double quadraticSoln1;
      double quadraticSoln2;

      //-----
      // Parametric equation for the line-of-sight .
      //-----
      double geolocatedPoint[] = new double[3];
      double gcLat, gcLon;
      double earthRadius;

      double scratch;
      double scratchVec[] = new double[3];

      int swathIndex, sampleIndex, currentSampleIndex;

      // Step through the number of swaths in file.
      for ( swathIndex = 0; swathIndex < numScans; swathIndex++ )
      {
        gdLatitude = degreesToRadians( satEphemLatitude[ swathIndex]);
        gdLongitude = degreesToRadians( satEphemLongitude[ swathIndex]);

        satAltitude = satEphemAltitude[ swathIndex];
        satHeadingAngle = degreesToRadians( satEphemHeading[ swathIndex]);

        gcLatitude = EllipsoidalEarthModel.geodeticToGeocentric( gdLatitude);
        gcLongitude = gdLongitude;

        //-----
        // Calculate subsatellite point, vector subSatPoint[].
        //-----
        cosLat = Math.cos( gcLatitude);
        sinLat = Math.sin( gcLatitude);
        cosLon = Math.cos( gcLongitude);
        sinLon = Math.sin( gcLongitude);

        earthRadius = EllipsoidalEarthModel.earthRadiusKm( gcLatitude);

        scratchVec[0] = cosLat * cosLon;
        scratchVec[1] = cosLat * sinLon;
        scratchVec[2] = sinLat;

        subSatPoint = VectorMath.vectorScalarMultiplication( scratchVec, earthRadius );

        //-----
        // Calculate normal to surface at the subsatellite point,
        // unit vector surfaceNormal[].
        //-----
        cosLat = Math.cos( gdLatitude);
        sinLat = Math.sin( gdLatitude);
        cosLon = Math.cos( gdLongitude);
        sinLon = Math.sin( gdLongitude);

        surfaceNormal[0] = cosLat * cosLon;
        surfaceNormal[1] = cosLat * sinLon;
        surfaceNormal[2] = sinLat;

        //-----
        // Calculate satellite point, vector satPoint[].
        //-----
        satPoint[0] = subSatPoint[0] + surfaceNormal[0] * satAltitude;
        satPoint[1] = subSatPoint[1] + surfaceNormal[1] * satAltitude;
        satPoint[2] = subSatPoint[2] + surfaceNormal[2] * satAltitude;

        //-----
        // Calculate north on tangent to surface, unit vector north[].
        //-----
        north[0] = -subSatPoint[0];
        north[1] = -subSatPoint[1];
        north[2] = EllipsoidalEarthModel.earthRadiusKm( PI/2.0) - subSatPoint[2];

        projectMag = VectorMath.vectorDotProduct( north, surfaceNormal)
                     / Math.pow( VectorMath.vectorMagnitude( surfaceNormal), 2.0);

        north[0] = north[0] - projectMag * surfaceNormal[0];
        north[1] = north[1] - projectMag * surfaceNormal[1];
        north[2] = north[2] - projectMag * surfaceNormal[2];

        north = VectorMath.unitVector( north);

        //-----
        // Calculate west on tangent to surface, unit vector west[].
        //-----
        west = VectorMath.vectorCrossProduct( surfaceNormal, north);
        west = VectorMath.unitVector( west);

        //-----
        // Calculate the direction vector for the satellite heading
        // using the given heading angle west of north,
        // unit vector satHeading.
        //-----
        cosHeading = Math.cos( satHeadingAngle);
        sinHeading = Math.sin( satHeadingAngle);

        satHeading[0] = sinHeading * west[0] + cosHeading * north[0];
        satHeading[1] = sinHeading * west[1] + cosHeading * north[1];
        satHeading[2] = sinHeading * west[2] + cosHeading * north[2];

        //-----
        // Calculate the scan line in the tangent plane
        // (i.e., the line that the satellite-to-pixel
        // line-of-sight draws on the tangent plane),
        // unit vector scanLine[].
        //-----
        scanLine = VectorMath.vectorCrossProduct( surfaceNormal, satHeading);

        // Step through each sample/pixel in the current swath.
        for ( sampleIndex = 0; sampleIndex < OLSSensorModel.numSamplesPerScan[ dataType]; sampleIndex++)
        {
          //-----
          // Calculate the scan angle, scannerAngle.
          // A positive angle is to the port of the satellite.
          //-----
          scannerAngle = OLSSensorModel.scanAngleOLS( satID, dataType,
                                                      sampleIndex, scannerOffset[ swathIndex]);

          //-----
          // Calculate the point on the scan line through which the
          // line-of-sight passes, vector scanPoint[].
          //-----
          scanPointMag = satAltitude * Math.tan( scannerAngle);

          scanPoint[0] = subSatPoint[0] + scanPointMag * scanLine[0];
          scanPoint[1] = subSatPoint[1] + scanPointMag * scanLine[1];
          scanPoint[2] = subSatPoint[2] + scanPointMag * scanLine[2];

          //-----
          // Calculate the parametric equation for the line-of-sight line,
          // the line through the satellite (t=0) and the scan point (t=1).
          // x = lineOfSightSlope[0] * t + satPoint[0]
          // y = lineOfSightSlope[1] * t + satPoint[1]
          // z = lineOfSightSlope[2] * t + satPoint[2]
          //-----
          lineOfSightSlope[0] = scanPoint[0] - satPoint[0];
          lineOfSightSlope[1] = scanPoint[1] - satPoint[1];
          lineOfSightSlope[2] = scanPoint[2] - satPoint[2];

          //-----
          // Solve for the intersection of the line-of-sight
          // and the ellipsoid model of the earth.
          //
          // The quadratic equation is found by substituting the parametric
          // equations for the line-of-sight into the equation for the earth
          // ellipsoid, x^2/a^2 + y^2/a^2 + z^2/b^2 = 1, where a is the
          // earth major axis and b is the earth minor axis.
          //-----
          earthMajorAxis = EllipsoidalEarthModel.earthRadiusKm( 0.0);
          earthMinorAxis = EllipsoidalEarthModel.earthRadiusKm( PI/2.0);

          earthMajorAxisSquared = Math.pow( earthMajorAxis, 2.0);
          earthMinorAxisSquared = Math.pow( earthMinorAxis, 2.0);

          quadraticEqnA =
          earthMinorAxisSquared * Math.pow( lineOfSightSlope[0], 2.0)
          + earthMinorAxisSquared * Math.pow( lineOfSightSlope[1], 2.0)
          + earthMajorAxisSquared * Math.pow( lineOfSightSlope[2], 2.0);
          quadraticEqnB =
          2.0 * ( earthMinorAxisSquared * satPoint[0] * lineOfSightSlope[0]
                  + earthMinorAxisSquared * satPoint[1] * lineOfSightSlope[1]
                  + earthMajorAxisSquared * satPoint[2] * lineOfSightSlope[2]);
          quadraticEqnC =
          earthMinorAxisSquared * Math.pow( satPoint[0], 2.0)
          + earthMinorAxisSquared * Math.pow( satPoint[1], 2.0)
          + earthMajorAxisSquared * Math.pow( satPoint[2], 2.0)
          - earthMajorAxisSquared * earthMinorAxisSquared;

          scratch = Math.sqrt( Math.pow( quadraticEqnB, 2.0) -
                               4.0 * quadraticEqnA * quadraticEqnC);
          quadraticSoln1 = ( -quadraticEqnB + scratch)/( 2.0 * quadraticEqnA);
          quadraticSoln2 = ( -quadraticEqnB - scratch)/( 2.0 * quadraticEqnA);

          //-----
          // Select the point on the near side of the earth.
          //-----
          lineParameter = ( quadraticSoln1 < quadraticSoln2)
                          ? quadraticSoln1
                          : quadraticSoln2;

          //-----
          // Calculate the current position on the scanLine.
          //-----
          geolocatedPoint[0] = lineOfSightSlope[0] * lineParameter + satPoint[0];
          geolocatedPoint[1] = lineOfSightSlope[1] * lineParameter + satPoint[1];
          geolocatedPoint[2] = lineOfSightSlope[2] * lineParameter + satPoint[2];
          double [] unitVecGeolocatedPoint = VectorMath.unitVector( geolocatedPoint);

          //-----
          // Calculate the geocentric latitude and longitude at the calculated point.
          //-----
          gcLat = Math.asin( unitVecGeolocatedPoint[2]);
          gcLon = Math.acos( unitVecGeolocatedPoint[0]/Math.cos( gcLat));
          gcLon = ( unitVecGeolocatedPoint[1] < 0.0) ? -gcLon : gcLon;

          //-----
          // Return the geocentric latitude and longitude in degrees.
          //-----
          currentSampleIndex = swathIndex * OLSSensorModel.numSamplesPerScan[ dataType] + sampleIndex;
          latitude[ currentSampleIndex] = (float) radiansToDegrees( gcLat);
          longitude[currentSampleIndex] = (float) radiansToDegrees( gcLon);

//        //-----
//        // Return the geodetic latitude and longitude in degrees.
//        //-----
//        latitude[sampleIndex] = radiansToDegrees( EllipsoidalEarthModel.geocentricToGeodetic( gcLat));
//        longitude[sampleIndex] = radiansToDegrees( gcLon);
        }
      }
    }

    static final double PI = 3.141592653589793;
    static final double TWO_PI = PI * 2.0;
    static final double HALF_PI = PI / 2.0;
    static final double DEGREES_PER_RADIANS = 360.0 / TWO_PI;

    static double degreesToRadians( double angleInDegrees ) { return( angleInDegrees / DEGREES_PER_RADIANS ); }
    static double radiansToDegrees( double angleInRadians) { return( angleInRadians * DEGREES_PER_RADIANS); }
  }

  /**
   * Provides a method for calculating the scan angle for a given OLS scan sample.
   */
  static class OLSSensorModel
  {
    // Light Smooth (LS), Thermal Smooth (TS), Light Fine (LF), Thermal Fine (TF)
    static final int NUM_DATA_TYPES = 4;

    // Two satellite groups: first, satellites 11-15; second, satellites 16-20.
    static final int NUM_SAT_GROUPS = 2;
    static final int [][] RANGE_SAT_GROUPS = { {11, 16}, {15, 20}};

    static final double peakScanAngle = 1.00967;

    static final double [] nominalTotalSamplePeriod
            = {1464.436, 1464.436, 7322.179, 7322.179};

    static final int [] numSamplesPerScan = {1465, 1465, 7322, 7322};

    /** Constant M used in the calculation of the scan angle for a pixel. */
    static final double M = 2.66874;

    /** Constant B used in the calculation of the scan angle for a pixel.
     *  The value for TS data depends on the satellite group.
     */
    static final double [][] B =
    {
      { 0.23686, 0.23591, 0.23665, 0.23665},
      { 0.23686, 0.23686, 0.23665, 0.23665}
    };

    /**
     * Calculate the scan angle from nadir for the given sample. Positive scan
     * angles are to the left from the heading direction. Currently only supports
     * smooth data, samples 0 through 1464 (fine data, samples 0 through 7321).
     *
     * @param satID - the satellite number
     * @param dataType - light smooth, thermal smooth, light fine, or thermal fine.
     * @param sampleNumber - the sample number
     * @param scannerOffset - the scanner offset for this satellite and data type.
     * @return the scan angle from nadir for the given sample.
     */
    static double scanAngleOLS( int satID, int dataType, int sampleNumber, double scannerOffset)
    {
      if ( sampleNumber > 1464) throw new IllegalArgumentException( "Sample number <" + sampleNumber + "> not within smooth sample range <0-1464> (fine data not currently supported).");
      int satGroup = satID <=15 ? 0 : 1;

      return ( peakScanAngle * Math.cos( ( sampleNumber/nominalTotalSamplePeriod[dataType] * M )
                                         + B[satGroup][dataType] ) - scannerOffset );
    }
  }

  /**
   * Provide methods for 1) calculating the earth's radius at a given
   * geocentric latitude and longitude, using a ellipsoidal earth; and
   * 2) converting between geocentric and geodetic latitude.
   */
  static class EllipsoidalEarthModel
  {
    /*
    From http://maic.jmu.edu/sic/standards/datum.htm
    Table 1: Datums and their principle areas of use

    Datum            Area                         Origin                   Ellipsoid

    WGS 1984         Global                       Earth center of mass     WGS 84

    NAD 1983         North America, Caribbean     Earth center of mass     GRS 80

    NAD 1927         North America                Meades Ranch             Clarke 1866

    European 1950    Europe, Middle East,         Potsdam                  International
                     North Africa

    ------------------------------------

    From http://maic.jmu.edu/sic/standards/ellipsoid.htm

    Table 2: Reference Ellipsoids in current use (Maling, 1989).

    Ellipsoid and Year   Semi-major axis (meters)   1/Flattening

    Airy 1830            6,377,563                  299.33
    Everest 1830         6,377,276.3                300.80
    Bessel 1841          6,377,397.2                299.15
    Clarke 1866          6,378,206.4                294.98
    Clarke 1880          6,378,249.2                293.47
    International 1924   6,378,388                  297
    Krasovsky 1940       6,378,245                  298.3
    International        6,378,160                  298.25    (f=0.0033528919) (e=0.08182018)
      Astronomical
        Union 1968
    WGS 72 (1972)        6,378,135                  298.26    (f=0.0033527794) (e=0.08181881)
    GRS 80 (1980)        6,378,137                  298.26
    WGS 84 (1984)        6,378,137                  298.25722 (f=0.0033528107) (e=0.08181919)

    From http://williams.best.vwh.net/ellipsoid/node1.html

    f = 1 - b/a
    e^2 = 1 - b^2/a^2

    e^2 = f(2-f)
    */
    // These constants are from MI Cal/Val report.
    static final double EARTH_MEAN_EQUATORIAL_RADIUS_KM = 6378.14;
    static final double EARTHS_ECCENTRICITY = 0.0818191830;
    static final double E_ECC_SQUARED = Math.pow( EARTHS_ECCENTRICITY, 2.0);

    // static public String getEllipsoidName() {}
    // static public String getDatumName() {}
    // static public double getEccentricity() {}
    // static public double getFlattening() {}
    // static public double getMajor() {}
    // static public double getMinor() {}
    /**
     * Return the earth radius in units of EARTH_MEAN_EQUATORIAL_RADIUS_KM
     * at the given geocentric latitude.
     *
     * @param gcLatitude - the geocentric latitude
     * @return the earth radius at the given location in units of EARTH_MEAN_EQUATORIAL_RADIUS_KM
     */
    static double earthRadius( double gcLatitude)
    {
      return( Math.sqrt( 1.0 - E_ECC_SQUARED)
              / Math.sqrt( 1.0 - E_ECC_SQUARED * Math.cos( gcLatitude)) );
    }

    /**
     * Return the earth radius in kilometers at the given geocentric latitude.
     *
     * @param gcLatitude - the given geocentric latitude
     * @return the earth radius at the given location in kilometers
     */
    static double earthRadiusKm( double gcLatitude)
    {
      return( earthRadius( gcLatitude) * EARTH_MEAN_EQUATORIAL_RADIUS_KM );
    }

    /**
     * Return the geocentric latitude for the given geodetic latitude.
     *
     * @param geodeticLatitude - the given geodetic latitude
     * @return the geocentric latitude
     */
    static double geodeticToGeocentric( double geodeticLatitude)
    {
      return ( Math.atan( (1.0 - E_ECC_SQUARED) * Math.tan( geodeticLatitude)));
    }

    /**
     * Return the geodetic latitude for the given geocentric latitude.

     * @param geocentricLatitude - the given geocentric latitude
     * @return the geodetic latitude
     */
    static double geocentricToGeodetic( double geocentricLatitude)
    {
      return ( Math.atan( Math.tan( geocentricLatitude)/( 1.0 - E_ECC_SQUARED)));
    }
  }

  /**
   * A class to provide methods for manipulating 3-D vectors. E.g., calculating the
   * magnitude of a vector and converting a vector into a unit vector.
   */
  static class VectorMath
  {
    /**
     * Return the magnitude of a 3-D vector.
     * @param vector
     * @return the magnitude of the given 3-D vector.
     */
    static double vectorMagnitude( double[] vector)
    {
      if ( vector.length != 3) throw new IllegalArgumentException( "Argument not a 3-D vector <dim=" + vector.length + ">.");
      return ( Math.sqrt( Math.pow(vector[0], 2.0) + Math.pow( vector[1], 2.0)
                     + Math.pow( vector[2], 2.0)));
    }

    /**
     * Change a vector into a vector with magnitude of one.
     * @param vector
     */
    static double [] unitVector( double[] vector)
    {
      if ( vector.length != 3) throw new IllegalArgumentException( "Argument not a 3-D vector <dim=" + vector.length + ">.");
      double magnitude = vectorMagnitude( vector);

      double [] resultingVector = { vector[0]/magnitude, vector[1]/magnitude, vector[2]/magnitude };
      return( resultingVector );
    }

    static double [] vectorScalarMultiplication( double [] vector, double scalar)
    {
      if ( vector.length != 3) throw new IllegalArgumentException( "Argument not a 3-D vector <dim=" + vector.length + ">.");

      double [] resultingVector = { scalar * vector[0], scalar * vector[1], scalar * vector[2] };
      return( resultingVector );
    }

    static double vectorDotProduct( double [] vectorA, double [] vectorB )
    {
      if ( vectorA.length != 3) throw new IllegalArgumentException( "First argument not a 3-D vector <dim=" + vectorA.length + ">.");
      if ( vectorB.length != 3) throw new IllegalArgumentException( "Second argument not a 3-D vector <dim=" + vectorB.length + ">.");

      return( vectorA[0] * vectorB[0] + vectorA[1] * vectorB[1] + vectorA[2] * vectorB[2] );
    }

    static double [] vectorCrossProduct( double [] vectorA, double [] vectorB )
    {
      if ( vectorA.length != 3) throw new IllegalArgumentException( "First argument not a 3-D vector <dim=" + vectorA.length + ">.");
      if ( vectorB.length != 3) throw new IllegalArgumentException( "Second argument not a 3-D vector <dim=" + vectorB.length + ">.");

      double [] resultingVector = {
        vectorA[1] * vectorB[2] - vectorA[2] * vectorB[1],
        vectorA[2] * vectorB[0] - vectorA[0] * vectorB[2],
        vectorA[0] * vectorB[1] - vectorA[1] * vectorB[0]
      };

      return( resultingVector );
    }
  }
}
