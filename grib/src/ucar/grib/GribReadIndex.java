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
/**
 * By:   Robb Kambic
 * Date: Jan 26, 2009
 * Time: 3:21:07 PM
 */

package ucar.grib;

import ucar.grid.GridIndex;
import ucar.grid.GridDefRecord;
import ucar.grib.grib2.Grib2PDSVariables;
import ucar.grib.grib2.Grib2GDSVariables;
import ucar.grib.grib2.Grib2Tables;
import ucar.grib.grib1.Grib1PDSVariables;
import ucar.grib.grib1.Grib1GDSVariables;
import ucar.grib.grib1.Grib1Tables;
import ucar.grib.grib1.Grib1Grid;

import java.net.URL;
import java.text.ParseException;
import java.io.*;
import java.util.Date;
import java.util.Calendar;

/**
 * Reads either a binary or text index and returns a GridIndex
 */

public class GribReadIndex {

  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribReadIndex.class);

  static private boolean debugTiming = false;
  static private boolean debugParse = false;

  private final java.text.SimpleDateFormat dateFormat;
  private final Calendar calendar;

  /**
   * Constructor for creating an Index from the Grib file.
   */
  public GribReadIndex() {
    dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));  //same as UTC

    calendar = Calendar.getInstance();
    calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  /**
   * open Grib Index file for scanning.
   *
   * @param location URL or local filename of Grib Index file
   * @return GridIndex
   * @throws java.io.IOException on read error
   */
  public final GridIndex open(String location) throws IOException {
    InputStream ios;
    if (location.startsWith("http:")) {
      URL url = new URL(location);
      ios = url.openStream();
    } else {
      ios = new FileInputStream(location);
    }
    if (ios == null)
      return null;
    return open(location, ios);
  }

  /**
   * open Grib Index file for scanning.
   *
   * @param location URL or local filename of Grib Index file
   * @param ios      input stream
   * @return false if does not match current version; you should regenerate it in that case.
   * @throws java.io.IOException on read error
   */
  public final GridIndex open(String location, InputStream ios) throws IOException {

    int tpasses = 5;
    for (int pass = 0; pass < tpasses; pass++) {
      if (pass != 0) // have to start with fresh ios
        ios = new FileInputStream(location);

      long start = System.currentTimeMillis();
      GridIndex gridIndex = null;
      DataInputStream dis = null;

      try {
        gridIndex = new GridIndex();
        dis = new DataInputStream(new BufferedInputStream(ios));
        // lastModified of raf, used for Index Extending
        long lastModified = dis.readLong();
        if (lastModified == 7597120008394602085L) {// this is a text index
          dis.close();  // close, has to be reopened differently
          dis = null;
          return new GribReadTextIndex().open(location);
        }

        // section 1 - global attributes
        Date baseTime = null;
        boolean grid_edition_1 = false;
        String index_version = "";
        int center = 0, sub_center = 0, table_version = 0;
        String line = dis.readUTF();
        if (debugParse)
          System.out.println(line);
        String[] split = line.split("\\s");
        for (int i = 0; i < split.length; i += 2) {
          gridIndex.addGlobalAttribute(split[i], split[i + 1]);
          if (split[i].equals("basetime")) {
            baseTime = dateFormat.parse(split[i + 1]);
          } else if (split[i].equals("grid_edition")) {
            grid_edition_1 = split[i + 1].equals("1");
          } else if (split[i].equals("index_version")) {
            index_version = split[i + 1];
            if (index_version.equals("7.1")) { // TODO: delete after 8.0 release
              File f = new File(location);
              f.delete();
              return null;
            }
          } else if (split[i].equals("center")) {
            center = Integer.parseInt(split[i + 1]);
          } else if (split[i].equals("sub_center")) {
            sub_center = Integer.parseInt(split[i + 1]);
          } else if (split[i].equals("table_version")) {
            table_version = Integer.parseInt(split[i + 1]);
          }
        }
        // number of grib records to read
        int number = dis.readInt();

        // section 2 -- grib records
        for (int i = 0; i < number; i++) {
          // Direct read into GribGridRecord
          GribGridRecord ggr = new GribGridRecord();
          if (index_version.equals("7.0")) {
            ggr.productTemplate = dis.readInt();
            ggr.discipline = dis.readInt();
            ggr.category = dis.readInt();
            ggr.paramNumber = dis.readInt();
            ggr.typeGenProcess = dis.readInt();
            ggr.levelType1 = dis.readInt();
            ggr.levelValue1 = dis.readFloat();
            ggr.levelType2 = dis.readInt();
            ggr.levelValue2 = dis.readFloat();
            long refTime = dis.readLong();
            calendar.setTimeInMillis(refTime);
            ggr.refTime = calendar.getTime();
            ggr.forecastTime = dis.readInt();
            // setValidTime
            calendar.add(Calendar.HOUR, ggr.forecastTime);
            ggr.setValidTime(calendar.getTime());

            ggr.gdsKey = dis.readInt();
            ggr.offset1 = dis.readLong();
            ggr.offset2 = dis.readLong();
            if (grid_edition_1) {
              ggr.decimalScale = dis.readInt();
              ggr.bmsExists = dis.readBoolean();
              ggr.center = dis.readInt();
              ggr.subCenter = dis.readInt();
              ggr.table = dis.readInt();
            }
          } else { // index version 8.0 or higher
            ggr.discipline = dis.readInt();
            long refTime = dis.readLong();
            calendar.setTimeInMillis(refTime);
            ggr.refTime = calendar.getTime();
            ggr.gdsKey = dis.readInt();
            ggr.offset1 = dis.readLong();
//            if ( pass == 0 )    // Testing partial index reads
//              throw new EOFException();
            ggr.offset2 = dis.readLong();
            // read PDS data
            int pdsSize = dis.readInt();
            byte[] pdsData = new byte[pdsSize];
            dis.readFully(pdsData);
            int tunit;

            if (grid_edition_1) {
              Grib1PDSVariables pdsv = new Grib1PDSVariables(pdsData);
              // read Grib1 vars
              ggr.productTemplate = pdsv.getProductDefinition();
              ggr.category = pdsv.getParameterCategory();
              ggr.paramNumber = pdsv.getParameterNumber();
              ggr.typeGenProcess = pdsv.getTypeGenProcess();
              ggr.levelType1 = pdsv.getTypeFirstFixedSurface();
              ggr.levelValue1 = pdsv.getValueFirstFixedSurface();
              ggr.levelType2 = pdsv.getTypeSecondFixedSurface();
              ggr.levelValue2 = pdsv.getValueSecondFixedSurface();
              // parameter with interval
              ggr.intervalStatType = pdsv.getIntervalStatType();
              if ( ggr.intervalStatType != -1 ) {
                int[] interval = pdsv.getForecastTimeInterval();
                ggr.startOfInterval = interval[ 0 ];
                ggr.forecastTime = interval[ 1 ];
                if( ggr.forecastTime - ggr.startOfInterval == 0 )
                  continue;
//                //System.out.println( "Total Precip Interval ="+ interval[0]
//                //+" "+ interval[1]);
              } else {
                ggr.forecastTime = pdsv.getForecastTime();
              }
              tunit = pdsv.getTimeRangeUnit();
              ggr.decimalScale = pdsv.getDecimalScale();
              ggr.bmsExists = pdsv.bmsExists();
              ggr.center = pdsv.getCenter();
              ggr.subCenter = pdsv.getSubCenter();
              ggr.table = pdsv.getTableVersion();
              if (pdsv.isEnsemble()) {
                //ggr.pdsVars = pdsv; To expensive to store.
                // ensemble, derived, or probability information
                ggr.isEnsemble = true;
                ggr.type = pdsv.getType();
                ggr.ensembleNumber = pdsv.getEnsembleNumber();
                ggr.numberForecasts = pdsv.getNumberForecasts();
                ggr.lowerLimit = pdsv.getValueLowerLimit();
                ggr.upperLimit = pdsv.getValueUpperLimit();
              }
            } else {  // Grib2
              Grib2PDSVariables pdsv = new Grib2PDSVariables(pdsData);
              ggr.productTemplate = pdsv.getProductDefinition();
              // These are accumulation variables.  
              if (ggr.productTemplate > 7 && ggr.productTemplate < 15 ||
                  ggr.productTemplate == 42 || ggr.productTemplate == 43) {
                int[] interval = pdsv.getForecastTimeInterval();
                ggr.startOfInterval = interval[0];
                ggr.forecastTime = interval[1];
                if( ggr.forecastTime - ggr.startOfInterval == 0 )
                  continue;
                //System.out.println( "Total Precip Interval ="+ interval[0]
                //+" "+ interval[1]);
              } else {
                ggr.forecastTime = pdsv.getForecastTime();
              }
              ggr.category = pdsv.getParameterCategory();
              ggr.paramNumber = pdsv.getParameterNumber();
              ggr.typeGenProcess = pdsv.getTypeGenProcess();
              //int typeForeProcess = pdsv.getAnalysisGenProcess();
              ggr.levelType1 = pdsv.getTypeFirstFixedSurface();
              ggr.levelValue1 = pdsv.getValueFirstFixedSurface();
              ggr.levelType2 = pdsv.getTypeSecondFixedSurface();
              ggr.levelValue2 = pdsv.getValueSecondFixedSurface();
              ggr.intervalStatType = pdsv.getIntervalStatType();

              tunit = pdsv.getTimeRangeUnit();
              ggr.center = center;
              ggr.subCenter = sub_center;
              ggr.table = table_version;
              if (pdsv.isEnsemble()) {
                //ggr.pdsVars = pdsv; To expensive to store.
                // ensemble, derived, or probability information
                ggr.isEnsemble = true;
                ggr.type = pdsv.getType();
                ggr.ensembleNumber = pdsv.getPerturbation();
                ggr.numberForecasts = pdsv.getNumberForecasts();
                ggr.lowerLimit = pdsv.getValueLowerLimit();
                ggr.upperLimit = pdsv.getValueUpperLimit();
              }
              if ( debugParse && ggr.productTemplate == 31 ) { // Satellite data
                int nb = pdsv.getNB();
                System.out.println( "NB ="+ pdsv.getNB() );
                int[] series = pdsv.getSatelliteSeries();
                int[] satellite = pdsv.getSatellite();
                int[] instrument = pdsv.getSatelliteInstrument();
                float[] wave = pdsv.getSatelliteWave();
                for( int n = 0; n < nb; n++) {
                  System.out.println( series[ n ] +" "+ satellite[ n ] +" "+
                      instrument[ n ] +" "+ wave[ n ]);
                }
              }
              
            }
            // setValidTime  hour, day, minute, month. second, year, decade, normal, century
            // only test data available for hour
            // hour
            if (tunit == 1 || tunit == 10 || tunit == 11 || tunit == 12) {
              calendar.add(Calendar.HOUR, ggr.forecastTime);
              // day, it's 24 hours so multiply forecast * 24
            } else if (tunit == 2) {
              calendar.add(Calendar.HOUR, ggr.forecastTime * 24);
              // minute
            } else if (tunit == 0) {
              calendar.add(Calendar.MINUTE, ggr.forecastTime);
              // month
            } else if (tunit == 3) {
              calendar.add(Calendar.MONTH, ggr.forecastTime);
              // second
            } else if (tunit == 13 || tunit == 254) {
              calendar.add(Calendar.SECOND, ggr.forecastTime * 3600);
              // year
            } else if (tunit == 4) {
              calendar.add(Calendar.YEAR, ggr.forecastTime);
              // decade
            } else if (tunit == 5) {
              calendar.add(Calendar.YEAR, ggr.forecastTime * 10);
              // normal
            } else if (tunit == 6) {
              calendar.add(Calendar.YEAR, ggr.forecastTime * 30);
              // century
            } else if (tunit == 7) {
              calendar.add(Calendar.YEAR, ggr.forecastTime * 100);
            }
            ggr.setValidTime(calendar.getTime());
            ggr.timeUnit = tunit;

            if (debugParse)
              System.out.println(ggr.productTemplate + " " + ggr.discipline + " " +
                  ggr.category + " " + ggr.paramNumber + " " +
                  ggr.typeGenProcess + " " + ggr.levelType1 + " " +
                  ggr.levelValue1 + " " + ggr.levelType2 + " " +
                  ggr.levelValue2 + " " + dateFormat.format(calendar.getTime()) + " " +
                  ggr.forecastTime + " " + ggr.gdsKey + " " + ggr.offset1 + " " + ggr.offset2 + " " +
                  ggr.decimalScale + " " + ggr.bmsExists + " " + ggr.center + " " + ggr.subCenter + " " + ggr.table + " " +
                  ggr.type + " " + ggr.numberForecasts + " " + ggr.lowerLimit + " " + ggr.upperLimit);
          }
          gridIndex.addGridRecord(ggr);
        }

        // section 3+ - GDS
        // old
        if (index_version.startsWith("7")) {
          while (true) {
            line = dis.readUTF();
            if (line.equals("End")) {
              break;
            }
            GribGridDefRecord gds = new GribGridDefRecord(line);
            gridIndex.addHorizCoordSys(gds);
          }
        } else {
          // new
          number = dis.readInt();
          for (int j = 0; j < number; j++) {
            int gdsSize = dis.readInt();
            if (gdsSize == 4) { // for Grib1 records with no GDS
              int gdskey = dis.readInt();
              GribGridDefRecord ggdr = new GribGridDefRecord();
              Grib1Grid.PopulateGDS(ggdr, gdskey);
              gridIndex.addHorizCoordSys(ggdr);
              continue;
            }
            byte[] gdsData = new byte[gdsSize];
            dis.readFully(gdsData);
            int gdskey;
            if (grid_edition_1) {
              Grib1GDSVariables gdsv = new Grib1GDSVariables(gdsData);
              GribGridDefRecord ggdr = new GribGridDefRecord(gdsv);
              if (index_version.startsWith("8.0")) {
                gdskey = gdsv.get80TypeGdsKey();
              } else {
                gdskey = gdsv.getGdsKey();
              }
              Grib1GDS( ggdr, gdsv, gdskey );
              gridIndex.addHorizCoordSys(ggdr);
              //System.out.println("GDS length =" + gdsv.getLength());
              //System.out.println("GDS GdsKey =" + gdsv.getOldTypeGdsKey());
            } else {
              Grib2GDSVariables gdsv = new Grib2GDSVariables(gdsData);
              GribGridDefRecord ggdr = new GribGridDefRecord(gdsv);
              if (index_version.startsWith("8.0")) {
                gdskey = gdsv.get80TypeGdsKey();
              } else {
                gdskey = gdsv.getGdsKey(); // version higher than 8.0
              }
              Grib2GDS(ggdr, gdsv, gdskey);
              gridIndex.addHorizCoordSys(ggdr);
              //System.out.println("GDS length =" + gdsv.getLength());
              //System.out.println("GDS GdsKey =" + gdsv.getGdsKey());
            }
            
          }
          //gridIndex.finish();
        }
        if (debugTiming) {
          long took = System.currentTimeMillis() - start;
          System.out.println(" Index read " + location + " count="
              + gridIndex.getGridCount() + " took=" + took + " msec ");
        }
        log.debug("Binary index read: " + location);
        log.debug("Number Records =" + gridIndex.getGridCount() + " at " +
            dateFormat.format(Calendar.getInstance().getTime()));
        return gridIndex;

      } catch (IOException e) {
        // there can be tpasses attempts to read the index, sometimes the 1st read
        // fails because of network or NFS errors
        if (pass == tpasses) {
          String message = "I/O error at record " + gridIndex.getGridCount() + " in index file";
          //log.warn("open(): " + message + "[" + location + "]");
          throw new IOException(message);
        }
        // retry
        log.info("open(): rereading index [" + location + "]");
        try {
          Thread.sleep(1000); // 1 secs to let file system catch up
        } catch (InterruptedException e1) {
        }

      } catch (ParseException e) {
        log.error("open(): ParseException reading index " + e.getMessage(), e);
        throw new RuntimeException(e);

      } finally {
        if (dis != null)
          dis.close();
      }

    } // for pass, code throws exception before getting here
    return null;

  }

  /**
   * Populates a GridDefRecord according to Projection.
   *
   * @param ggdr GridDefRecord
   * @param gdsv Grib2GDSVariables gdsv
   * @param gdskey  key for this gds
   */
  public void Grib2GDS(GribGridDefRecord ggdr, Grib2GDSVariables gdsv,
              int gdskey ) {

    int gdtn = gdsv.getGdtn();

    //ggdr.addParam(GridDefRecord.GDS_KEY, Integer.toString(gdsv.getGdsKey()));
    ggdr.addParam(GridDefRecord.GDS_KEY, Integer.toString(gdskey));
    ggdr.addParam(GridDefRecord.GRID_TYPE, gdtn);
    ggdr.addParam(GridDefRecord.GRID_NAME, Grib2Tables.codeTable3_1(gdtn));

    String winds = GribNumbers.isBitSet(gdsv.getResolution(), GribNumbers.BIT_5)
        ? "Relative"
        : "True";
    int component_flag = GribNumbers.isBitSet(gdsv.getResolution(), GribNumbers.BIT_5)
        ? 1 : 0;

    if (((gdtn < 50) || (gdtn > 53))
        && (gdtn != 100) && (gdtn != 120)
        && (gdtn != 1200)) {
      int shape = gdsv.getShape();

      ggdr.addParam(GridDefRecord.GRID_SHAPE_CODE, shape);
      ggdr.addParam(GridDefRecord.GRID_SHAPE, Grib2Tables.codeTable3_2(shape));
      if (shape < 2 || shape == 6 || shape == 8) {
        ggdr.addParam(GridDefRecord.RADIUS_SPHERICAL_EARTH, gdsv.getEarthRadius());
      } else if ((shape > 1) && (shape < 6) || shape == 7 ) {
        ggdr.addParam(GridDefRecord.MAJOR_AXIS_EARTH, gdsv.getMajorAxis());
        ggdr.addParam(GridDefRecord.MINOR_AXIS_EARTH, gdsv.getMinorAxis());
      }
    }

    if (gdsv.getOlon() == 0) {
      ggdr.addParam(GridDefRecord.QUASI, "false");
    } else {
      ggdr.addParam(GridDefRecord.QUASI, "true");
    }

    switch (gdtn) {                // Grid Definition Template Number

      case 0:
      case 1:
      case 2:
      case 3:     // Latitude/Longitude Grid
      case 32768: // Arakawa staggered E grid Latitude/Longitude Grid

        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LA2, gdsv.getLa2());
        ggdr.addParam(GridDefRecord.LO2, gdsv.getLo2());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

        if (gdtn == 1) {         //Rotated Latitude/longitude
          ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
          ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
          ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());

        } else if (gdtn == 2) {  //Stretched Latitude/longitude
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

        } else if (gdtn == 3) {  //Stretched and Rotated
          // Latitude/longitude
          ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
          ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
          ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());
        }
        break;

      case 10:                              // Mercator
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LAD, gdsv.getLaD());
        ggdr.addParam(GridDefRecord.LA2, gdsv.getLa2());
        ggdr.addParam(GridDefRecord.LO2, gdsv.getLo2());
        ggdr.addParam(GridDefRecord.BASICANGLE, gdsv.getAngle());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());
        break;

      case 20:  // Polar stereographic projection

        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LAD, gdsv.getLaD());
        ggdr.addParam(GridDefRecord.LOV, gdsv.getLoV());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.PROJ, gdsv.getProjectionFlag());
        String npproj = "false";
        if ((gdsv.getProjectionFlag() & 128) == 0)
          npproj = "true";
        ggdr.addParam(GridDefRecord.NPPROJ, npproj);
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

        break;

      case 30:                              // Lambert Conformal
      case 31:                              // Albers equal area

        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LAD, gdsv.getLaD());
        ggdr.addParam(GridDefRecord.LOV, gdsv.getLoV());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.PROJ, gdsv.getProjectionFlag());
        npproj = "false";
        if ((gdsv.getProjectionFlag() & 128) == 0)
          npproj = "true";
        ggdr.addParam(GridDefRecord.NPPROJ, npproj);
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());
        ggdr.addParam(GridDefRecord.LATIN1, gdsv.getLatin1());
        ggdr.addParam(GridDefRecord.LATIN2, gdsv.getLatin2());
        ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
        ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());

        break;

      case 40:
      case 41:
      case 42:
      case 43:  // Gaussian latitude/longitude
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LA2, gdsv.getLa2());
        ggdr.addParam(GridDefRecord.LO2, gdsv.getLo2());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());
        ggdr.addParam(GridDefRecord.NUMBERPARALLELS, gdsv.getNp());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

        if (gdtn == 41) {         //Rotated Gaussian Latitude/longitude
          ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
          ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
          ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());

        } else if (gdtn == 42) {  //Stretched Gaussian
          // Latitude/longitude
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

        } else if (gdtn == 43) {  //Stretched and Rotated Gaussian
          // Latitude/longitude
          ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
          ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
          ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

        }
        break;
        /*
        case 50:
        case 51:
        case 52:
        case 53:                       // Spherical harmonic coefficients
          ggdr.addParam(GridDefRecord.J\t" + gds.getJ());
          ggdr.addParam(GridDefRecord.K\t" + gds.getK());
          ggdr.addParam(GridDefRecord.M\t" + gds.getM());
          ggdr.addParam(GridDefRecord.MethodNorm\t" + gds.getMethod());
          ggdr.addParam(GridDefRecord.ModeOrder\t" + gds.getMode());
          ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
          if (gdtn == 51) {  //Rotated Spherical harmonic coefficients
            ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
            ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
            ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());

          } else if (gdtn == 52) {  //Stretched Spherical
            // harmonic coefficients
            ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
            ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
            ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

          } else if (gdtn == 53) {  //Stretched and Rotated
            // Spherical harmonic coefficients
            ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
            ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
            ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());
            ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
            ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
            ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

          }
          break;
        */

      case 90:  // Space view perspective or orthographic
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LAP, gdsv.getLap());
        ggdr.addParam(GridDefRecord.LOP, gdsv.getLop());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.XP, gdsv.getXp());
        ggdr.addParam(GridDefRecord.YP, gdsv.getYp());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode() );
        ggdr.addParam(GridDefRecord.ANGLE, gdsv.getAngle());
        ggdr.addParam(GridDefRecord.NR, gdsv.getNr());
        ggdr.addParam(GridDefRecord.XO, gdsv.getXo());
        ggdr.addParam(GridDefRecord.YO, gdsv.getYo());

        break;

        /*
        case 100:  // Triangular grid based on an icosahedron
          ggdr.addParam(GridDefRecord.Exponent2Intervals\t" + gds.getN2());
          ggdr.addParam(GridDefRecord.Exponent3Intervals\t" + gds.getN3());
          ggdr.addParam(GridDefRecord.NumberIntervals\t" + gds.getNi());
          ggdr.addParam(GridDefRecord.NumberDiamonds\t" + gds.getNd());
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.GridPointPosition\t" + gds.getPosition());
          ggdr.addParam(GridDefRecord.NumberOrderDiamonds\t" + gds.getOrder());
          ggdr.addParam(GridDefRecord.NumberParallels\t" + gds.getN());
          ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());
          ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());

          break;
        */
      case 110:  // Equatorial azimuthal equidistant projection
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        npproj = "false";
        if ((gdsv.getProjectionFlag() & 128) == 0)
          npproj = "true";
        ggdr.addParam(GridDefRecord.NPPROJ, npproj);
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.PROJ, gdsv.getProjectionFlag());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

        break;
        /*
        case 120:  // Azimuth-range Projection
          ggdr.addParam(GridDefRecord.NumberDataBins\t" + gds.getNb());
          ggdr.addParam(GridDefRecord.NumberRadials\t" + gds.getNr());
          ggdr.addParam(GridDefRecord.NumberPointsParallel\t" + gds.getNx());
          ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
          ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
          ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
          ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
          ggdr.addParam(GridDefRecord.OffsetFromOrigin\t" + gds.getDstart());
          //ggdr.addParam( "need code to get azi and adelta" );
          ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

          break;
        */
      case 204:  // Curvilinear orthographic
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GribGridDefRecord.GRID_UNITS, "degrees");

        break;

      default:
        log.warn("\tUnknown Grid Type\t" + gdtn);
    }             // end switch gdtn
  }  // end Grib2GDS

  /**
   * Populates a GridDefRecord according to Projection.
   *
   * @param ggdr GridDefRecord
   * @param gdsv Grib1GDSVariables gdsv
   */
  public void Grib1GDS(GribGridDefRecord ggdr, Grib1GDSVariables gdsv,
              int gdskey ) {
    int gdtn = gdsv.getGdtn();

    //ggdr.addParam(GridDefRecord.GDS_KEY, Integer.toString(gdsv.getGdsKey()));
    ggdr.addParam(GridDefRecord.GDS_KEY, Integer.toString(gdskey));
    ggdr.addParam(GridDefRecord.GRID_TYPE, gdtn);
    ggdr.addParam(GridDefRecord.GRID_NAME, Grib1Tables.getName(gdtn));

    String winds = GribNumbers.isBitSet(gdsv.getResolution(), GribNumbers.BIT_5)
        ? "Relative"
        : "True";

    int component_flag = GribNumbers.isBitSet(gdsv.getResolution(), GribNumbers.BIT_5)
        ? 1 : 0;

    if (((gdtn < 50) || (gdtn > 53))
        && (gdtn != 100) && (gdtn != 120)
        && (gdtn != 1200)) {
      int shape = gdsv.getShape();

      ggdr.addParam(GridDefRecord.GRID_SHAPE_CODE, shape);
      ggdr.addParam(GridDefRecord.GRID_SHAPE, Grib1Tables.getShapeName(shape));
      if ( shape == 0 ) {
        ggdr.addParam(GridDefRecord.RADIUS_SPHERICAL_EARTH, gdsv.getEarthRadius());
      } else if ( shape == 1 ) {
        ggdr.addParam(GridDefRecord.MAJOR_AXIS_EARTH, gdsv.getMajorAxis());
        ggdr.addParam(GridDefRecord.MINOR_AXIS_EARTH, gdsv.getMinorAxis());
      }
    }

    switch (gdtn) {                // Grid Definition Template Number

      case 0:
      case 10:
      case 20:
      case 30:
      case 201:
      case 202:
      case 203:
      case 205:
        // Latitude/Longitude Grid

        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LA2, gdsv.getLa2());
        ggdr.addParam(GridDefRecord.LO2, gdsv.getLo2());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

        if (gdtn == 10) {         //Rotated Latitude/longitude
          ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
          ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
          ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());

        } else if (gdtn == 20) {  //Stretched Latitude/longitude
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

        } else if (gdtn == 30) {  //Stretched and Rotated
          // Latitude/longitude
          ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
          ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
          ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());
        }
        break;

      case 1:
      case 6:  // Mercator
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LA2, gdsv.getLa2());
        ggdr.addParam(GridDefRecord.LO2, gdsv.getLo2());
        ggdr.addParam(GridDefRecord.LATIN, gdsv.getLatin1());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());
        break;

      case 5:  // Polar stereographic projection

        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LOV, gdsv.getLoV());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.PROJ, gdsv.getProjectionFlag());
        String npproj = "false";
        if ((gdsv.getProjectionFlag() & 128) == 0)
          npproj = "true";
        ggdr.addParam(GridDefRecord.NPPROJ, npproj);
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

        break;

      case 3:
      case 13:  // Lambert Conformal

        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LOV, gdsv.getLoV());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.PROJ, gdsv.getProjectionFlag());
        npproj = "false";
        if ((gdsv.getProjectionFlag() & 128) == 0)
          npproj = "true";
        ggdr.addParam(GridDefRecord.NPPROJ, npproj);
        ggdr.addParam(GridDefRecord.LATIN1, gdsv.getLatin1());
        ggdr.addParam(GridDefRecord.LATIN2, gdsv.getLatin2());
        ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
        ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());
        break;

      case 4:
      case 14:
      case 24:
      case 34:
        // Gaussian latitude/longitude
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.LA2, gdsv.getLa2());
        ggdr.addParam(GridDefRecord.LO2, gdsv.getLo2());
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.NUMBERPARALLELS, gdsv.getNp());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

        if (gdtn == 14) {         //Rotated Gaussian Latitude/longitude
          ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
          ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
          ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());

        } else if (gdtn == 24) {  //Stretched Gaussian
          // Latitude/longitude
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

        } else if (gdtn == 34) {  //Stretched and Rotated Gaussian
          // Latitude/longitude
          ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
          ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
          ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

        }
        break;
        /*
        case 50:
        case 60:
        case 70:
        case 80:                       // Spherical harmonic coefficients
          ggdr.addParam(GridDefRecord.J\t" + gds.getJ());
          ggdr.addParam(GridDefRecord.K\t" + gds.getK());
          ggdr.addParam(GridDefRecord.M\t" + gds.getM());
          ggdr.addParam(GridDefRecord.MethodNorm\t" + gds.getMethod());
          ggdr.addParam(GridDefRecord.ModeOrder\t" + gds.getMode());
          ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
          if (gdtn == 51) {  //Rotated Spherical harmonic coefficients
            ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
            ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
            ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());

          } else if (gdtn == 52) {  //Stretched Spherical
            // harmonic coefficients
            ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
            ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
            ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

          } else if (gdtn == 53) {  //Stretched and Rotated
            // Spherical harmonic coefficients
            ggdr.addParam(GridDefRecord.SPLAT, gdsv.getSpLat());
            ggdr.addParam(GridDefRecord.SPLON, gdsv.getSpLon());
            ggdr.addParam(GridDefRecord.ROTATIONANGLE, gdsv.getRotationAngle());
            ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
            ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
            ggdr.addParam(GridDefRecord.STRETCHINGFACTOR, gdsv.getStretchingFactor());

          }
          break;
        */

      case 90:  // Space view perspective or orthographic
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LAP, gdsv.getLap());
        ggdr.addParam(GridDefRecord.LOP, gdsv.getLop());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.XP, gdsv.getXp());
        ggdr.addParam(GridDefRecord.YP, gdsv.getYp());
        ggdr.addParam(GridDefRecord.ANGLE, gdsv.getAngle());
        ggdr.addParam(GridDefRecord.NR, gdsv.getNr());
        ggdr.addParam(GridDefRecord.XO, gdsv.getXo());
        ggdr.addParam(GridDefRecord.YO, gdsv.getYo());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

        break;

        /*
        case 100:  // Triangular grid based on an icosahedron
          ggdr.addParam(GridDefRecord.Exponent2Intervals\t" + gds.getN2());
          ggdr.addParam(GridDefRecord.Exponent3Intervals\t" + gds.getN3());
          ggdr.addParam(GridDefRecord.NumberIntervals\t" + gds.getNi());
          ggdr.addParam(GridDefRecord.NumberDiamonds\t" + gds.getNd());
          ggdr.addParam(GridDefRecord.PLAT, gdsv.getPoleLat());
          ggdr.addParam(GridDefRecord.PLON, gdsv.getPoleLon());
          ggdr.addParam(GridDefRecord.GridPointPosition\t" + gds.getPosition());
          ggdr.addParam(GridDefRecord.NumberOrderDiamonds\t" + gds.getOrder());
          ggdr.addParam(GridDefRecord.NumberParallels\t" + gds.getN());
          ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());

          break;
        */
        /*
      case 110:  // Equatorial azimuthal equidistant projection
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
        ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        npproj = "false";
        if ((gdsv.getProjectionFlag() & 128) == 0)
          npproj = "true";
        ggdr.addParam(GridDefRecord.NPPROJ, npproj);
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONET_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
        ggdr.addParam(GridDefRecord.DY, gdsv.getDy());
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.PROJ, gdsv.getProjectionFlag());

        break;
        */

        /*
        case 120:  // Azimuth-range Projection
          ggdr.addParam(GridDefRecord.NumberDataBins\t" + gds.getNb());
          ggdr.addParam(GridDefRecord.NumberRadials\t" + gds.getNr());
          ggdr.addParam(GridDefRecord.NumberPointsParallel\t" + gds.getNx());
          ggdr.addParam(GridDefRecord.LA1, gdsv.getLa1());
          ggdr.addParam(GridDefRecord.LO1, gdsv.getLo1());
          ggdr.addParam(GridDefRecord.DX, gdsv.getDx());
          ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
          ggdr.addParam(GridDefRecord.OffsetFromOrigin\t" + gds.getDstart());
          //ggdr.addParam( "need code to get azi and adelta" );

          break;
        */
      case 204:  // Curvilinear orthographic
        ggdr.addParam(GridDefRecord.NX, gdsv.getNx());
        ggdr.addParam(GridDefRecord.NY, gdsv.getNy());
        ggdr.addParam(GridDefRecord.RESOLUTION, gdsv.getResolution());
        ggdr.addParam(GridDefRecord.WIND_FLAG, winds);
        ggdr.addParam(GridDefRecord.VECTOR_COMPONENT_FLAG, component_flag);
        ggdr.addParam(GridDefRecord.GRID_UNITS, gdsv.getGridUnits());
        ggdr.addParam(GridDefRecord.SCANNING_MODE, gdsv.getScanMode());

        break;

      default:
        log.error("\tUnknown Grid Type\t" + gdtn);
    }             // end switch gdtn
  }  // end Grib1GDS

  public void setDebug(boolean flag) {
    debugTiming = flag;
  }

  public void setVerbose(boolean flag) {
    debugParse = flag;
  }

  /**
   * testing
   *
   * @param args index to read
   * @throws java.io.IOException on read error
   */
  static public void main(String[] args) throws IOException {
    // used to do timings on reading indexes
    if (false) {

      int tgbx = 0, tgbx8 = 0, count = 0;
      for (int i = 0; i < 10; i++) {

        String topDir = "/local/robb/data/grib/idd/binary";

        long start = System.currentTimeMillis();
        File dir = new File(topDir);
        if (dir.isDirectory()) {
          System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
          String[] children = dir.list();
          for (String child : children) {
            if (!child.endsWith("gbx8"))
              continue;
            GridIndex index = new GribReadIndex().open(topDir + "/" + child);

          }
          long time = System.currentTimeMillis() - start;
          System.out.println("Binary Took " + (time));
          tgbx8 += time;
        }
        // read text indexes
        topDir = "/local/robb/data/grib/idd/text";

        start = System.currentTimeMillis();
        dir = new File(topDir);
        if (dir.isDirectory()) {
          System.out.println("In directory " + dir.getParent() + "/" + dir.getName());
          String[] children = dir.list();
          for (String child : children) {
            if (!child.endsWith("gbx"))
              continue;
            GridIndex index = new GribReadIndex().open(topDir + "/" + child);
            if (i == 0)
              count++;
          }
          long time = System.currentTimeMillis() - start;
          System.out.println("Text Took " + (time));
          tgbx += time;
        }

      }
      System.out.println("Binary Avg took " + (tgbx8 / 10));
      System.out.println("Text Avg took " + (tgbx / 10));
      System.out.println("Number of files in directory " + count);
      return;
    }
      File gbx = new File(GribIndexName.get("C:/data/NDFD.grib2"));
      if (!gbx.exists()) { // work machine
        gbx = new File(GribIndexName.get("/local/robb/data/grib/idd/text/NDFD_CONUS_5km_20090221_1200.grib2"));
    }

    //debugTiming = true;
    debugParse = true;
    GridIndex index;
    if (args.length < 1) {
      index = new GribReadIndex().open(gbx.getPath());
    } else {
      index = new GribReadIndex().open(args[0]);
    }
    if (debugTiming)
      return;

    // test GDS conversions to int and double
//    List<GridDefRecord> gcs = index.getHorizCoordSys();
//    for( GridDefRecord gdr : gcs ) {
//      String wind = gdr.getParam( "Winds" );
//      int nx = gdr.getInt( "Nx");
//      System.out.println( "Nx ="+ nx );
//      nx = gdr.getInt( "Nx");
//      System.out.println( "Nx ="+ nx );
//      double la1 = gdr.getDouble( "La1");
//      System.out.println( "La1 ="+ la1 );
//      la1 = gdr.getDouble( "La1");
//      System.out.println( "La1 ="+ la1 );
//    }
  }
}
