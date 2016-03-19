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
// $Id: DMSPHeader.java 63 2006-07-12 21:50:51Z edavis $
package ucar.nc2.iosp.dmsp;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;

import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * DMSP header parser
 * <p/>
 * User: edavis
 * Date: Aug 6, 2004
 * Time: 3:38:11 PM
 */
public class DMSPHeader
{
  private String[] header = null;
  private Map<String, String> headerInfo = new HashMap<>();
  private int headerSizeInBytes = 0;
  private int headerSizeInBytesGuess = 5000;

  private ucar.unidata.io.RandomAccessFile raFile;
  private ucar.nc2.NetcdfFile ncFile;
  private long actualSize;

  // File information
  private String fileIdAttName = "fileId";
  private Attribute fileIdAtt = null;

  private String datasetIdAttName = "datasetId";
  private Attribute datasetIdAtt = null;

  private int recordSizeInBytes = 0;

  private int numHeaderRecords = 0;
  private int numDataRecords = 0;

  private String numDataRecordsDimName = "numScans";
  private Dimension numDataRecordsDim = null;

  private int numArtificialDataRecords = 0;
  private int numRecords = 0;

  // Processing information
  private String suborbitHistoryAttName = "suborbitHistory";
  private Attribute suborbitHistoryAtt = null;

  private String processingSystemAttName = "processingSystem";
  private Attribute processingSystemAtt = null;

  private Date processingDate = null;

  private String processingDateAttName = "processingDate";
  private Attribute processingDateAtt = null;

  // Satellite information
  private String spacecraftIdAttName = "spacecraftId";
  private Attribute spacecraftIdAtt = null;

  private String noradIdAttName = "noradId";
  private Attribute noradIdAtt = null;

  // Orbit information
  private String startDateAttName = "startDate";
  private Attribute startDateAtt = null;
  private Date startDate = null;

  private String endDateAttName = "endDate";
  private Attribute endDateAtt = null;
  private Date endDate = null;

  private String startDateLocalAttName = "startDateLocal";
  private Attribute startDateLocalAtt = null;

  private String startTimeLocalAttName = "startTimeLocal";
  private Attribute startTimeLocalAtt = null;

  private String startLatitudeAttName = "startLatitude";
  private Attribute startLatitudeAtt = null;

  private String startLongitudeAttName = "startLongitude";
  private Attribute startLongitudeAtt = null;

  private String endLatitudeAttName = "endLatitude";
  private Attribute endLatitudeAtt = null;

  private String endLongitudeAttName = "endLongitude";
  private Attribute endLongitudeAtt = null;

  private String startSubsolarCoordsAttName = "startSubsolarCoords";
  private Attribute startSubsolarCoordsAtt = null;

  private String endSubsolarCoordsAttName = "endSubsolarCoords";
  private Attribute endSubsolarCoordsAtt = null;

  private String startLunarCoordsAttName = "startLunarCoords";
  private Attribute startLunarCoordsAtt = null;

  private String endLunarCoordsAttName = "endLunarCoords";
  private Attribute endLunarCoordsAtt = null;

  private String ascendingNodeAttName = "ascendingNode";
  private Attribute ascendingNodeAtt = null;

  private String nodeHeadingAttName = "nodeHeading";
  private Attribute nodeHeadingAtt = null;

  // Sensor information
  private int numSamplesPerBand = 0;
  private String numSamplesPerBandDimName = "numSamplesPerScan";
  private Dimension numSamplesPerBandDim = null;

  private String nominalResolutionAttName = "nominalResolution";
  private Attribute nominalResolutionAtt = null;

  private String bandsPerScanlineAttName = "bandsPerScanline";
  private Attribute bandsPerScanlineAtt = null;

  private String bytesPerSampleAttName = "bytesPerSample";
  private Attribute bytesPerSampleAtt = null;

  private String byteOffsetBand1AttName = "byteOffsetBand1";
  private Attribute byteOffsetBand1Att = null;

  private String byteOffsetBand2AttName = "byteOffsetBand2";
  private Attribute byteOffsetBand2Att = null;

  private String band1AttName = "band1";
  private Attribute band1Att = null;

  private String band2AttName = "band2";
  private Attribute band2Att = null;

  private String bandOrganizationAttName = "bandOrganization";
  private Attribute bandOrganizationAtt = null;

  private String thermalOffsetAttName = "thermalOffset";
  private Attribute thermalOffsetAtt = null;

  private String thermalScaleAttName = "thermalScale";
  private Attribute thermalScaleAtt = null;

  private String percentDaylightAttName = "percentDaylight";
  private Attribute percentDaylightAtt = null;

  private String percentFullMoonAttName = "percentFullMoon";
  private Attribute percentFullMoonAtt = null;

  private String percentTerminatorEvidentAttName = "percentTerminatorEvident";
  private Attribute percentTerminatorEvidentAtt = null;

  // QC information
  private String qcFlagsAttName = "qcFlags";
  private Attribute qcFlagsAtt = null;


  public Attribute getFileIdAtt() { return( this.fileIdAtt ); }
  public Attribute getDatasetIdAtt() { return( this.datasetIdAtt ); }
  public int getNumHeaderRecords() { return( this.numHeaderRecords ); }
  public int getNumDataRecords() { return( this.numDataRecords ); }
  public Dimension getNumDataRecordsDim() { return( this.numDataRecordsDim ); }
  public Dimension getNumSamplesPerBandDim() { return( this.numSamplesPerBandDim ); }
  public int getRecordSizeInBytes() { return( this.recordSizeInBytes ); }

  public Attribute getSuborbitHistoryAtt() { return( this.suborbitHistoryAtt ); }
  public Attribute getProcessingSystemAtt() { return( this.processingSystemAtt ); }
  public Attribute getProcessingDateAtt() { return( this.processingDateAtt ); }
  public Attribute getStartDateAtt() { return( this.startDateAtt ); }


  /** Check basic DMSP file validity of given random access file. */
  boolean isValidFile( ucar.unidata.io.RandomAccessFile raFile )
  {
    // @todo This method should not be called if read() has or will be called on this instance.
    this.raFile = raFile;
    try
    {
      this.actualSize = raFile.length();
    }
    catch ( IOException e )
    {
      return( false );
    }
    try
    {
      this.readHeaderFromFile( raFile );

      this.handleFileInformation();
      this.handleProcessingInformation();
      this.handleSatelliteInformation();
      this.handleSensorInformation();
    }
    catch ( IOException e )
    {
      return( false );
    }
    return( true );
  }

  void read( ucar.unidata.io.RandomAccessFile raFile, ucar.nc2.NetcdfFile ncFile )
          throws IOException
  {
    this.raFile = raFile;
    this.ncFile = ncFile;
    actualSize = this.raFile.length();

    // Read the header from the file
    this.readHeaderFromFile( raFile );

    // Deal with file/record information.
    this.handleFileInformation();

    this.ncFile.addAttribute( null, fileIdAtt );
    this.ncFile.addAttribute( null, datasetIdAtt );
    this.ncFile.addDimension( null, numDataRecordsDim );

    // Deal with processing/history information.
    this.handleProcessingInformation();

    this.ncFile.addAttribute( null, suborbitHistoryAtt);
    this.ncFile.addAttribute( null, processingSystemAtt);
    this.ncFile.addAttribute( null, processingDateAtt);

    // Deal with satellite information
    this.handleSatelliteInformation();

    this.ncFile.addAttribute( null, spacecraftIdAtt );
    this.ncFile.addAttribute( null, noradIdAtt );

    // Deal with orbit information
    this.handleOrbitInformation();

    this.ncFile.addAttribute( null, startDateAtt);
    this.ncFile.addAttribute( null, endDateAtt);
    this.ncFile.addAttribute( null, startDateLocalAtt);
    this.ncFile.addAttribute( null, startTimeLocalAtt);
    this.ncFile.addAttribute( null, startLatitudeAtt);
    this.ncFile.addAttribute( null, startLongitudeAtt);
    this.ncFile.addAttribute( null, endLatitudeAtt);
    this.ncFile.addAttribute( null, endLongitudeAtt);
    this.ncFile.addAttribute( null, startSubsolarCoordsAtt);
    this.ncFile.addAttribute( null, endSubsolarCoordsAtt);
    this.ncFile.addAttribute( null, startLunarCoordsAtt);
    this.ncFile.addAttribute( null, endLunarCoordsAtt);
    this.ncFile.addAttribute( null, ascendingNodeAtt);
    this.ncFile.addAttribute( null, nodeHeadingAtt);

    // Deal with sensor information
    this.handleSensorInformation();

    this.ncFile.addDimension( null, numSamplesPerBandDim );
    this.ncFile.addAttribute( null, nominalResolutionAtt );
    this.ncFile.addAttribute( null, bandsPerScanlineAtt );
    this.ncFile.addAttribute( null, bytesPerSampleAtt );
    this.ncFile.addAttribute( null, byteOffsetBand1Att );
    this.ncFile.addAttribute( null, byteOffsetBand2Att );
    this.ncFile.addAttribute( null, band1Att );
    this.ncFile.addAttribute( null, band2Att );
    this.ncFile.addAttribute( null, bandOrganizationAtt );
    this.ncFile.addAttribute( null, thermalOffsetAtt );
    this.ncFile.addAttribute( null, thermalScaleAtt );
    this.ncFile.addAttribute( null, percentDaylightAtt );
    this.ncFile.addAttribute( null, percentFullMoonAtt );
    this.ncFile.addAttribute( null, percentTerminatorEvidentAtt );

    // Deal with QC information
    this.handleQCInformation();

    this.ncFile.addAttribute( null, qcFlagsAtt );

    // Add some general metadata in global attributes.
    this.ncFile.addAttribute( null, new Attribute( "title",
                                                   new StringBuilder("NGDC archived ")
                                                   .append( datasetIdAtt.getStringValue())
                                                   .append( " data with start time ")
                                                   .append( startDateAtt.getStringValue())
                                                   .toString()));
    this.ncFile.addAttribute( null, new Attribute( "Convention", _Coordinate.Convention));

    // Add some THREDDS specific metadata in global attributes.
    this.ncFile.addAttribute( null, new Attribute( "thredds_creator", "DOD/USAF/SMC > Space and Missile Systems Center (SMC), U.S. Air Force, U.S. Department of Defense"));
    this.ncFile.addAttribute( null, new Attribute( "thredds_contributor", "DOC/NOAA/NESDIS/NGDC > National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce"));
    this.ncFile.addAttribute( null, new Attribute( "thredds_contributor_role", "archive"));
    this.ncFile.addAttribute( null, new Attribute( "thredds_publisher", "DOC/NOAA/NESDIS/NGDC > National Geophysical Data Center, NESDIS, NOAA, U.S. Department of Commerce"));
    this.ncFile.addAttribute( null, new Attribute( "thredds_publisher_url", "http://dmsp.ngdc.noaa.gov/"));
    this.ncFile.addAttribute( null, new Attribute( "thredds_publisher_email", "ngdc.dmsp@noaa.gov"));
    this.ncFile.addAttribute( null, new Attribute( "thredds_summary",
                                                   new StringBuilder("This dataset contains data from the DMSP ").append( spacecraftIdAtt.getStringValue())
                                                   .append( " satellite OLS instrument and includes both visible smooth and thermal smooth imagery with 2.7km resolution.")
                                                   .append( " The start time for this data is ").append( startDateAtt.getStringValue())
                                                   .append( " and the northerly equatorial crossing longitude is ").append( startLongitudeAtt.getNumericValue())
                                                   .append( ".  The DMSP satellite is a polar-orbiting satellite crossing the equator, depending on the satellite, at either dawn/dusk or noon/midnight.")
                                                   .append( " This data is in the NOAA/NGDC DMSP archive format.")
                                                   .toString()));
    this.ncFile.addAttribute( null, new Attribute( "thredds_history", ""));
    this.ncFile.addAttribute( null, new Attribute( "thredds_timeCoverage_start", startDateAtt.getStringValue()));
    this.ncFile.addAttribute( null, new Attribute( "thredds_timeCoverage_end", endDateAtt.getStringValue()));
    this.ncFile.addAttribute( null, new Attribute( "thredds_geospatialCoverage",
                                                   new StringBuilder("Polar orbit with northerly equatorial crossing at longitude ")
                                                   .append( ascendingNodeAtt.getNumericValue()).append( ".")
                                                   .toString()));


    // Set position of random access file to just after header record(s).
    this.raFile.seek( this.headerSizeInBytes );
  }

  /**
   * Read the header information from the file into name/value pairs.
   *
   * @param raFile - the file to be read.
   * @throws IOException if any problems reading the file (or validating the file).
   */
  private void readHeaderFromFile( ucar.unidata.io.RandomAccessFile raFile )
          throws IOException
  {
    long pos = 0;
    raFile.seek( pos );

    // Read in first record.
    this.headerSizeInBytes = raFile.length() > this.headerSizeInBytesGuess ?
                             this.headerSizeInBytesGuess : (int) raFile.length();
    byte[] b = new byte[this.headerSizeInBytes];
    if ( raFile.read(b) != this.headerSizeInBytes ) throw new IOException( "Invalid DMSP file: could not read first " + this.headerSizeInBytes + " bytes.");
    String fullHeader = new String( b, CDM.utf8Charset);
    // Make sure header starts with the proper item.
    if ( ! fullHeader.startsWith( HeaderInfoTitle.FILE_ID.toString() ) )
    {
      throw new IOException( "Invalid DMSP file: header does not start with \"" + HeaderInfoTitle.FILE_ID.toString() + "\"." );
    }
    // Make sure header contains end-of-header marker.
    int endOfHeaderIndex = fullHeader.indexOf( HeaderInfoTitle.END_HEADER.toString() );
    if ( endOfHeaderIndex == -1 )
    {
      throw new IOException( "Invalid DMSP file: header does not end with \"" + HeaderInfoTitle.END_HEADER.toString() + "\"." );
    }

    // Drop the end-of-header marker and the line feed ('\n') proceeding it.
    header = fullHeader.substring( 0, endOfHeaderIndex - 1 ).split( "\n" );

    int lineSeperatorIndex = 0;
    String curHeaderLine = null;
    String curHeaderTitle = null;
    String curHeaderValue = null;
    for (String aHeader : this.header) {
      curHeaderLine = aHeader.trim();
      lineSeperatorIndex = curHeaderLine.indexOf(':');
      if (lineSeperatorIndex == -1)
        throw new IOException("Invalid DMSP file: header line <" + curHeaderLine + "> contains no seperator <:>.");
      if (lineSeperatorIndex == 0)
        throw new IOException("Invalid DMSP file: header line <" + curHeaderLine + "> contains no title.");
      if (lineSeperatorIndex == curHeaderLine.length() - 1)
        throw new IOException("Invalid DMSP file: header line <" + curHeaderLine + "> contains no value.");

      curHeaderTitle = curHeaderLine.substring(0, lineSeperatorIndex).trim();
      curHeaderValue = curHeaderLine.substring(lineSeperatorIndex + 1).trim();
      if (curHeaderValue.equals(""))
        throw new IOException("Invalid DMSP file: header line <" + curHeaderLine + "> contains no value.");

      headerInfo.put(curHeaderTitle, curHeaderValue);
    }
  }

  /**
   * Parse the file header information about the file (e.g., file ID, dataset ID,
   * record size, number of records) and create netCDF attributes and dimensions
   * where appropriate.
   *
   * @throws IOException if any problems reading the file (or validating the file).
   */
  private void handleFileInformation()
          throws IOException
  {
    fileIdAtt = new Attribute( this.fileIdAttName, headerInfo.get(HeaderInfoTitle.FILE_ID.toString() ) );
    datasetIdAtt = new Attribute( this.datasetIdAttName, headerInfo.get(HeaderInfoTitle.DATA_SET_ID.toString() ) );
    recordSizeInBytes = Integer.parseInt( headerInfo.get( HeaderInfoTitle.RECORD_BYTES.toString() ) );
    numRecords = Integer.parseInt( headerInfo.get( HeaderInfoTitle.NUM_RECORDS.toString() ) );
    numHeaderRecords = Integer.parseInt( headerInfo.get( HeaderInfoTitle.NUM_HEADER_RECORDS.toString() ) );
    numDataRecords = Integer.parseInt( headerInfo.get( HeaderInfoTitle.NUM_DATA_RECORDS.toString() ) );
    numDataRecordsDim = new Dimension( this.numDataRecordsDimName, numDataRecords, true, true, false);
    numArtificialDataRecords = Integer.parseInt( headerInfo.get( HeaderInfoTitle.NUM_ARTIFICIAL_DATA_RECORDS.toString() ) );
    this.headerSizeInBytes = this.numHeaderRecords * this.recordSizeInBytes;
    if ( numRecords * ((long) this.recordSizeInBytes) != this.actualSize )
    {
      throw new IOException( "Invalid DMSP file: the number of records <" + this.numRecords + "> times the record size <" + this.recordSizeInBytes + "> does not equal the size of the file <" + this.actualSize + ">." );
    }
  }

  /**
   * Parse the processing/history information from the header.
   *
   * @throws IOException if any problems reading the file (or validating the file).
   */
  private void handleProcessingInformation()
          throws IOException
  {
    suborbitHistoryAtt = new Attribute(this.suborbitHistoryAttName,
            headerInfo.get(HeaderInfoTitle.SUBORBIT_HISTORY.toString()));
    processingSystemAtt = new Attribute(this.processingSystemAttName,
            headerInfo.get(HeaderInfoTitle.PROCESSING_SYSTEM.toString()));
    String processingDateString = headerInfo.get(HeaderInfoTitle.PROCESSING_DATE.toString());
    try
    {
      processingDate = DateFormatHandler.ALT_DATE_TIME.getDateFromDateTimeString( processingDateString );
    }
    catch ( ParseException e )
    {
      throw new IOException( "Invalid DMSP file: processing date string <" + processingDateString + "> not parseable: " + e.getMessage() );
    }
    processingDateAtt = new Attribute(
            this.processingDateAttName,
            DateFormatHandler.ISO_DATE_TIME.getDateTimeStringFromDate( processingDate ) );
  }

  /**
   * Parse the satellite information from the header.
   */
  private void handleSatelliteInformation()
  {
    spacecraftIdAtt = new Attribute(
            this.spacecraftIdAttName,
            headerInfo.get(HeaderInfoTitle.SPACECRAFT_ID.toString()));
    noradIdAtt = new Attribute(
            this.noradIdAttName,
            headerInfo.get(HeaderInfoTitle.NORAD_ID.toString()));
  }

  private void handleOrbitInformation() throws IOException
  {
    // Read the start date UTC information.
    String time = headerInfo.get(  HeaderInfoTitle.START_TIME_UTC.toString());
    String startDateTimeUTC = headerInfo.get(  HeaderInfoTitle.START_DATE_UTC.toString())
                              + "T" + time.substring( 0, time.indexOf( '.')+4) + "Z";
    try {
      this.startDate = DateFormatHandler.ISO_DATE_TIME.getDateFromDateTimeString( startDateTimeUTC);
    }
    catch ( ParseException e )
    {
      throw new IOException( "Invalid DMSP file: start date/time string <" + startDateTimeUTC + "> not parseable: " + e.getMessage() );
    }
    this.startDateAtt = new Attribute( this.startDateAttName, DateFormatHandler.ISO_DATE_TIME.getDateTimeStringFromDate( this.startDate));

    // Read the end date UTC information.
    time = headerInfo.get(  HeaderInfoTitle.END_TIME_UTC.toString());
    String endDateTimeUTC = headerInfo.get(  HeaderInfoTitle.END_DATE_UTC.toString())
                            + "T" + time.substring( 0, time.indexOf( '.')+4) + "Z";
    try {
      this.endDate = DateFormatHandler.ISO_DATE_TIME.getDateFromDateTimeString( endDateTimeUTC);
    }
    catch ( ParseException e )
    {
      throw new IOException( "Invalid DMSP file: end date/time string <" + endDateTimeUTC + "> not parseable: " + e.getMessage() );
    }
    this.endDateAtt = new Attribute( this.endDateAttName,
                                       DateFormatHandler.ISO_DATE_TIME.getDateTimeStringFromDate( this.endDate));

    // Read the local start/end date/time
    this.startDateLocalAtt = new Attribute( this.startDateLocalAttName, HeaderInfoTitle.START_DATE_LOCAL.toString());
    this.startTimeLocalAtt = new Attribute( this.startTimeLocalAttName, HeaderInfoTitle.START_TIME_LOCAL.toString());

    // Read the start latitude/longitude information.
    String startLatLon = headerInfo.get(  HeaderInfoTitle.START_LAT_LON.toString());
    String[] latLon = startLatLon.split( " ");
    Double lat, lon;
    if ( latLon.length != 2) throw new IOException( "Invalid DMSP file: start lat/lon <" + startLatLon + "> invalid.");
    try
    {
      lat = Double.valueOf( latLon[0]);
      lon = Double.valueOf( latLon[1]);
    }
    catch ( NumberFormatException e )
    {
      throw new IOException( "Invalid DMSP file: start lat/lon string <" + startLatLon + "> not parseable: " + e.getMessage() );
    }
    this.startLatitudeAtt = new Attribute( this.startLatitudeAttName, lat);
    this.startLongitudeAtt = new Attribute( this.startLongitudeAttName, lon);

    // Read the end latitude/longitude information.
    String endLatLon = headerInfo.get(  HeaderInfoTitle.END_LAT_LON.toString());
    latLon = endLatLon.split( " ");
    if ( latLon.length != 2) throw new IOException( "Invalid DMSP file: end lat/lon <" + endLatLon + "> invalid.");
    try
    {
      lat = Double.valueOf( latLon[0]);
      lon = Double.valueOf( latLon[1]);
    }
    catch ( NumberFormatException e )
    {
      throw new IOException( "Invalid DMSP file: end lat/lon string <" + endLatLon + "> not parseable: " + e.getMessage() );
    }
    this.endLatitudeAtt = new Attribute( this.endLatitudeAttName, lat);
    this.endLongitudeAtt = new Attribute( this.endLongitudeAttName, lon);

    // Read the start sub-solar coordinates.
    this.startSubsolarCoordsAtt = new Attribute( this.startSubsolarCoordsAttName, headerInfo.get(HeaderInfoTitle.START_SUBSOLAR_COORD.toString()));

    // Read the end sub-solar coordinates.
    this.endSubsolarCoordsAtt = new Attribute( this.endSubsolarCoordsAttName, headerInfo.get(HeaderInfoTitle.END_SUBSOLAR_COORD.toString()));

    // Read the start lunar coordinates.
    this.startLunarCoordsAtt = new Attribute( this.startLunarCoordsAttName, headerInfo.get(HeaderInfoTitle.START_LUNAR_COORD.toString()));

    // Read the end lunar coordinates.
    this.endLunarCoordsAtt = new Attribute( this.endLunarCoordsAttName, headerInfo.get(HeaderInfoTitle.END_LUNAR_COORD.toString()));

    // Read the ascending node.
    Double ascendingNode = Double.valueOf( headerInfo.get( HeaderInfoTitle.ASCENDING_NODE.toString()) );
    this.ascendingNodeAtt = new Attribute( this.ascendingNodeAttName, ascendingNode);

    Double nodeHeading = Double.valueOf( headerInfo.get( HeaderInfoTitle.NODE_HEADING.toString()) );
    this.nodeHeadingAtt = new Attribute( this.nodeHeadingAttName, nodeHeading);
  }

  /**
   * Parse the sensor information from the header.
   */
  private void handleSensorInformation()
  {
    numSamplesPerBand = Integer.parseInt( headerInfo.get( HeaderInfoTitle.SAMPLES_PER_BAND.toString()) );
    numSamplesPerBandDim = new Dimension(
            this.numSamplesPerBandDimName,
            numSamplesPerBand);

    // Read nominal resolution information
    nominalResolutionAtt = new Attribute( nominalResolutionAttName, headerInfo.get(HeaderInfoTitle.NOMINAL_RESOLUTION.toString()));

    // Read bands per scanlin information.
    bandsPerScanlineAtt = new Attribute( bandsPerScanlineAttName, Integer.valueOf(headerInfo.get(HeaderInfoTitle.BANDS_PER_SCANLINE.toString())));

    // Read bytes per smaple information
    bytesPerSampleAtt = new Attribute( bytesPerSampleAttName, Integer.valueOf(headerInfo.get(HeaderInfoTitle.BYTES_PER_SAMPLE.toString())));

    // Read byte offset for band 1 information.
    byteOffsetBand1Att = new Attribute( byteOffsetBand1AttName, Integer.valueOf(headerInfo.get(HeaderInfoTitle.BYTE_OFFSET_BAND_1.toString())));

    // Read byte offset for band 2 information.
    byteOffsetBand2Att = new Attribute( byteOffsetBand2AttName, Integer.valueOf(headerInfo.get(HeaderInfoTitle.BYTE_OFFSET_BAND_2.toString())));

    // Band 1 description
    band1Att = new Attribute( band1AttName, headerInfo.get( HeaderInfoTitle.BAND_1.toString()));

    // Band 2 description
    band2Att = new Attribute( band2AttName, headerInfo.get( HeaderInfoTitle.BAND_2.toString()));

    // Band organization
    bandOrganizationAtt = new Attribute( bandOrganizationAttName, headerInfo.get(HeaderInfoTitle.ORGANIZATION.toString()));

    // thermal offset
    thermalOffsetAtt = new Attribute( thermalOffsetAttName, headerInfo.get(HeaderInfoTitle.THERMAL_OFFSET.toString()));

    // thermal scale
    thermalScaleAtt = new Attribute( thermalScaleAttName, headerInfo.get(HeaderInfoTitle.THERMAL_SCALE.toString()));

    // percent daylight
    percentDaylightAtt = new Attribute( percentDaylightAttName, Double.valueOf(headerInfo.get(HeaderInfoTitle.PERCENT_DAYLIGHT.toString())));

    // percent full moon
    percentFullMoonAtt = new Attribute( percentFullMoonAttName, Double.valueOf(headerInfo.get(HeaderInfoTitle.PERCENT_FULL_MOON.toString())));

    // percent terminator evident
    percentTerminatorEvidentAtt = new Attribute( percentTerminatorEvidentAttName, Double.valueOf(headerInfo.get(HeaderInfoTitle.PERCENT_TERMINATOR_EVIDENT.toString())));
  }

  /**
   * Parse the sensor information from the header.
   */
  private void handleQCInformation()
  {
    // QC flags
    qcFlagsAtt = new Attribute( qcFlagsAttName, headerInfo.get(HeaderInfoTitle.QC_FLAGS.toString()));
  }

  /**
   * Return a string containing the header name/value pairs.
   *
   * @return a string of the file header name/value pairs.
   */
  protected String headerInfoDump()
  {
    StringBuilder retVal = new StringBuilder( );
    for ( String curHeaderTitle : this.headerInfo.keySet() ) {
      String curHeaderValue = this.headerInfo.get( curHeaderTitle );
      retVal.append( curHeaderTitle );
      retVal.append( ":::::" );
      retVal.append( curHeaderValue );
      retVal.append( ":::::\n" );
    }

    return( retVal.toString() );
  }

  /**
   * Return the header information for this file as a String.
   *
   * @return the string form (NGDC DMSP archive format) of the header information.
   */
  public String toString()
  {
    StringBuilder retVal = new StringBuilder();

    retVal.append( HeaderInfoTitle.FILE_ID.toString() );
    retVal.append( ": ");
    retVal.append( this.fileIdAtt.getStringValue());
    retVal.append( "\n");

    retVal.append( HeaderInfoTitle.DATA_SET_ID.toString() );
    retVal.append( ": " );
    retVal.append( this.datasetIdAtt.getStringValue() );
    retVal.append( "\n" );

    retVal.append( HeaderInfoTitle.RECORD_BYTES.toString() );
    retVal.append( ": " );
    retVal.append( this.recordSizeInBytes );
    retVal.append( "\n" );

    retVal.append( HeaderInfoTitle.NUM_HEADER_RECORDS.toString() );
    retVal.append( ": " );
    retVal.append( this.numHeaderRecords );
    retVal.append( "\n" );

    retVal.append( HeaderInfoTitle.NUM_RECORDS.toString() );
    retVal.append( ": " );
    retVal.append( this.numRecords );
    retVal.append( "\n" );

    retVal.append( HeaderInfoTitle.SUBORBIT_HISTORY.toString() );
    retVal.append( ": " );
    retVal.append( this.suborbitHistoryAtt.getStringValue() );
    retVal.append( "\n" );

    retVal.append( HeaderInfoTitle.PROCESSING_SYSTEM.toString() );
    retVal.append( ": " );
    retVal.append( this.processingSystemAtt.getStringValue() );
    retVal.append( "\n" );

    retVal.append( HeaderInfoTitle.PROCESSING_DATE.toString() );
    retVal.append( ": " );
    retVal.append( DateFormatHandler.ALT_DATE_TIME.getDateTimeStringFromDate( this.processingDate ) );
    retVal.append( "\n" );

    retVal.append( HeaderInfoTitle.SPACECRAFT_ID.toString() );
    retVal.append( ": " );
    retVal.append( this.spacecraftIdAtt.getStringValue() );
    retVal.append( "\n" );

    retVal.append( HeaderInfoTitle.NORAD_ID.toString() );
    retVal.append( ": " );
    retVal.append( this.noradIdAtt.getStringValue() );
    retVal.append( "\n" );

    return ( retVal.toString() );
  }

  /**
   * Class for dealing with date/time formats (from the header and for nc file).
   */
  static class DateFormatHandler
  {
    // Available date format handlers.
    public final static DateFormatHandler ISO_DATE = new DateFormatHandler( "yyyy-MM-dd");
    public final static DateFormatHandler ISO_TIME = new DateFormatHandler( "HH:mm:ss.SSSz" );
    public final static DateFormatHandler ISO_DATE_TIME = new DateFormatHandler( "yyyy-MM-dd\'T\'HH:mm:ss.SSS'Z'" );
    public final static DateFormatHandler ALT_DATE_TIME = new DateFormatHandler( "EEE MMM dd HH:mm:ss yyyy" );

    private String dateTimeFormatString = null;

    private DateFormatHandler( String dateTimeFormatString )
    {
      this.dateTimeFormatString = dateTimeFormatString;
    }

    public String getDateTimeFormatString() { return( this.dateTimeFormatString ); }

    /**
     * Return a java.util.Date given a date string using the date/time format string.
     *
     * @param dateTimeString - date/time string to be used to set java.util.Date.
     * @return The java.util.Date set by the given date/time string.
     */
    public Date getDateFromDateTimeString( String dateTimeString )
            throws ParseException
    {
      Date theDate = null;

      SimpleDateFormat dateFormat = new SimpleDateFormat( this.dateTimeFormatString, Locale.US );
      dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

      theDate = dateFormat.parse( dateTimeString );

      return ( theDate );
    }

    /**
     * Return the date/time string that represents the given a java.util.Date
     * in the format of this DataFormatHandler.
     *
     * @param date - the Date to be formatted into a date/time string.
     * @return The date/time string formatted from the given Date.
     */
    public String getDateTimeStringFromDate( Date date )
    {
      SimpleDateFormat dateFormat = new SimpleDateFormat( this.dateTimeFormatString, Locale.US );
      dateFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

      String dateString = dateFormat.format( date );

      return ( dateString );
    }
  }

  /**
   * Contains information about the NGDC DMSP archive file format's header information.
   */
  static class HeaderInfoTitle
  {
    private static java.util.Map<String, HeaderInfoTitle> hash = new java.util.HashMap<>( 20 );

    public final static HeaderInfoTitle FILE_ID = new HeaderInfoTitle( "file ID");                  // /dmsp/moby-1-3/subscriptions/IBAMA/1353226646955.tmp
    public final static HeaderInfoTitle DATA_SET_ID = new HeaderInfoTitle( "data set ID" );              // DMSP F14 OLS LS & TS
    public final static HeaderInfoTitle RECORD_BYTES = new HeaderInfoTitle( "record bytes" );             // 3040
    public final static HeaderInfoTitle NUM_HEADER_RECORDS = new HeaderInfoTitle( "number of header records" ); // 1
    public final static HeaderInfoTitle NUM_RECORDS = new HeaderInfoTitle( "number of records" );        // 692
    public final static HeaderInfoTitle SUBORBIT_HISTORY = new HeaderInfoTitle( "suborbit history" );         // F14200307192230.OIS (1,691)
    public final static HeaderInfoTitle PROCESSING_SYSTEM = new HeaderInfoTitle( "processing system" );        // v2.1b
    public final static HeaderInfoTitle PROCESSING_DATE = new HeaderInfoTitle( "processing date" );          // Sat Jul 19 19:33:23 2003
    public final static HeaderInfoTitle SPACECRAFT_ID = new HeaderInfoTitle( "spacecraft ID" );            // F14
    public final static HeaderInfoTitle NORAD_ID = new HeaderInfoTitle( "NORAD ID" );                 // 24753
    public final static HeaderInfoTitle START_DATE_UTC = new HeaderInfoTitle( "start date UTC" );           // 2003-07-19
    public final static HeaderInfoTitle START_TIME_UTC = new HeaderInfoTitle( "start time UTC" );           // 22:30:31.37112
    public final static HeaderInfoTitle END_DATE_UTC = new HeaderInfoTitle( "end date UTC" );             // 2003-07-19
    public final static HeaderInfoTitle END_TIME_UTC = new HeaderInfoTitle( "end time UTC" );             // 22:35:21.83694
    public final static HeaderInfoTitle START_DATE_LOCAL = new HeaderInfoTitle( "start date local" );         // 2003-07-19
    public final static HeaderInfoTitle START_TIME_LOCAL = new HeaderInfoTitle( "start time local" );         // 19:52:42.03518
    public final static HeaderInfoTitle START_LAT_LON = new HeaderInfoTitle( "start lat,lon" );            // 0.00 320.54
    public final static HeaderInfoTitle END_LAT_LON = new HeaderInfoTitle( "end lat,lon" );              // 16.99 316.69
    public final static HeaderInfoTitle START_SUBSOLAR_COORD = new HeaderInfoTitle( "start sub-solar coord" );    // 20.87 202.37
    public final static HeaderInfoTitle END_SUBSOLAR_COORD = new HeaderInfoTitle( "end sub-solar coord" );      // 20.87 201.16
    public final static HeaderInfoTitle START_LUNAR_COORD = new HeaderInfoTitle( "start lunar coord" );        // UNKNOWN
    public final static HeaderInfoTitle END_LUNAR_COORD = new HeaderInfoTitle( "end lunar coord" );          // UNKNOWN
    public final static HeaderInfoTitle START_DIRECTION = new HeaderInfoTitle("start direction");
    public final static HeaderInfoTitle QA_1_8 = new HeaderInfoTitle("QA 1/8");
    public final static HeaderInfoTitle QA_2_8 = new HeaderInfoTitle("QA 2/8");
    public final static HeaderInfoTitle QA_3_8 = new HeaderInfoTitle("QA 3/8");
    public final static HeaderInfoTitle QA_4_8 = new HeaderInfoTitle("QA 4/8");
    public final static HeaderInfoTitle QA_5_8 = new HeaderInfoTitle("QA 5/8");
    public final static HeaderInfoTitle QA_6_8 = new HeaderInfoTitle("QA 6/8");
    public final static HeaderInfoTitle QA_7_8 = new HeaderInfoTitle("QA 7/8");
    public final static HeaderInfoTitle QA_8_8 = new HeaderInfoTitle("QA 8/8");
    public final static HeaderInfoTitle DELTA_T = new HeaderInfoTitle("delta-t");
    public final static HeaderInfoTitle ASCENDING_NODE = new HeaderInfoTitle( "ascending node" );           // 320.55
    public final static HeaderInfoTitle NODE_HEADING = new HeaderInfoTitle( "node heading" );             // 8.64
    public final static HeaderInfoTitle EPHEMERIS_SOURCE = new HeaderInfoTitle( "ephemeris source" );         // NORAD
    public final static HeaderInfoTitle NUM_DATA_RECORDS = new HeaderInfoTitle( "number of data records" );   // 691
    public final static HeaderInfoTitle NUM_ARTIFICIAL_DATA_RECORDS = new HeaderInfoTitle( "number of artificial data records" ); // 0
    public final static HeaderInfoTitle NOMINAL_RESOLUTION = new HeaderInfoTitle( "nominal resolution" );                // 2.7 km
    public final static HeaderInfoTitle BANDS_PER_SCANLINE = new HeaderInfoTitle( "bands per scanline" );                // 2
    public final static HeaderInfoTitle SAMPLES_PER_BAND = new HeaderInfoTitle( "samples per band" );                  // 1465
    public final static HeaderInfoTitle BYTES_PER_SAMPLE = new HeaderInfoTitle( "bytes per sample" );                  // 1
    public final static HeaderInfoTitle BYTE_OFFSET_BAND_1 = new HeaderInfoTitle( "byte offset band 1" );                // 96
    public final static HeaderInfoTitle BYTE_OFFSET_BAND_2 = new HeaderInfoTitle( "byte offset band 2" );                // 1568
    public final static HeaderInfoTitle BAND_1 = new HeaderInfoTitle( "band 1" );                            // OLS Visible .4-1.1um
    public final static HeaderInfoTitle BAND_2 = new HeaderInfoTitle( "band 2" );                            // OLS Thermal 10.5-12.6um
    public final static HeaderInfoTitle ORGANIZATION = new HeaderInfoTitle( "organization" );                      // band interleaved by line
    public final static HeaderInfoTitle THERMAL_OFFSET = new HeaderInfoTitle( "thermal offset" );                    // 190.00 K
    public final static HeaderInfoTitle THERMAL_SCALE = new HeaderInfoTitle( "thermal scale" );                     // 0.47
    public final static HeaderInfoTitle QC_FLAGS = new HeaderInfoTitle( "QC flags" );                          // 0=not QC'ed  1=artificial  2=bad vis
    public final static HeaderInfoTitle PERCENT_DAYLIGHT = new HeaderInfoTitle( "% daylight" );                        // 0.0
    public final static HeaderInfoTitle PERCENT_FULL_MOON = new HeaderInfoTitle( "% full moon" );                       // 57.8
    public final static HeaderInfoTitle PERCENT_TERMINATOR_EVIDENT = new HeaderInfoTitle( "% terminator evident" );              // 0.0
    public final static HeaderInfoTitle END_HEADER = new HeaderInfoTitle( "end header" );


    private String HeaderInfoTitle;

    private HeaderInfoTitle( String title )
    {
      this.HeaderInfoTitle = title;
      hash.put( title, this );
    }

    /**
     * Find the HeaderInfoTitle that matches this title.
     *
     * @param title
     * @return HeaderInfoTitle or null if no match.
     */
    public static HeaderInfoTitle getTitle( String title )
    {
      if ( title == null )
      {
        return null;
      }
      return hash.get( title );
    }

    /**
     * Return the string title.
     */
    public String toString()
    {
      return HeaderInfoTitle;
    }

  }
}
