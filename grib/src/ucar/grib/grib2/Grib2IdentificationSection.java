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

// $Id: Grib2IdentificationSection.java,v 1.15 2005/12/05 19:24:21 rkambic Exp $


package ucar.grib.grib2;


import ucar.grib.GribNumbers;

/**
 * Grib2IdentificationSection.java  1.0  07/25/2003
 * @author Robb Kambic
 *
 */

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import java.util.Calendar;
import java.util.Date;


/**
 * A class representing the IdentificationSection section 1 of a GRIB record.
 * Information about center, tables, and reference times.
 */

public final class Grib2IdentificationSection {

  /**
   * Length in bytes of this IdentificationSection.
   */
  private final int length;

  /**
   * Number of this section, should be 1.
   */
  private final int section;

  /**
   * Identification of center.
   */
  private final int center_id;

  /**
   * Identification of subcenter .
   */
  private final int subcenter_id;

  /**
   * Parameter Table Version number.
   */
  private final int master_table_version;

  /**
   * Local table version number
   */
  private final int local_table_version;

  /**
   * Model Run/Analysis/Reference time.
   */
  private final int significanceOfRT;

  /**
   * Reference time as a Date and long
   */
  private final Date baseTime;
  private final long refTime;

  /**
   * Product status. operational, test, research, etc.
   */
  private final int productStatus;

  /**
   * Product type. forecast, analysis, etc.
   */
  private final int productType;

  // *** constructors *******************************************************

  /**
   * Constructs a <tt>Grib2IdentificationSection</tt> object from a RandomAccessFile.
   *
   * @param raf RandomAccessFile with Section 1 content
   * @throws IOException if raf contains no valid GRIB file
   */
  public Grib2IdentificationSection(RandomAccessFile raf)
          throws IOException {
    long sectionEnd = raf.getFilePointer();

    // section 1 octet 1-4 (length of section)
    length = GribNumbers.int4(raf);
    sectionEnd += length;

    section = raf.read();

    // Center  octet 6-7
    center_id = GribNumbers.int2(raf);

    // Center  octet 8-9
    subcenter_id = GribNumbers.int2(raf);

    // Paramter master table octet 10
    master_table_version = raf.read();

    // Paramter local table octet 11
    local_table_version = raf.read();

    // significanceOfRT octet 12
    significanceOfRT = raf.read();

    // octets 13-19 (base time of forecast)
    {
      int year = GribNumbers.int2(raf);
      int month = raf.read() - 1;
      int day = raf.read();
      int hour = raf.read();
      int minute = raf.read();
      int second = raf.read();

      Calendar calendar = Calendar.getInstance();
      calendar.clear();
      calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
      calendar.set(Calendar.DST_OFFSET, 0);
      calendar.set(year, month, day, hour, minute, second);

      baseTime = calendar.getTime();
      refTime = calendar.getTimeInMillis();

    }

    productStatus = raf.read();
    //System.out.println( "productStatus=" + productStatus );

    productType = raf.read();
    //System.out.println( "productType=" + productType );

    raf.seek(sectionEnd);

  }  // end if Grib2IdentificationSection

  /**
   * Identification of center.
   *
   * @return center id as int
   */
  public final int getCenter_id() {
    return center_id;
  }

  /**
   * Name of Identification of center.
   *
   * @return center Identification Name
   */

  /*
  public final String getCenter_idName() {

    switch (center_id) {

      case 0:
        return "WMO Secretariat";

      case 1:
      case 2:
        return "Melbourne";

      case 4:
      case 5:
        return "Moscow";

      case 7:
        return "US National Weather Service (NCEP)";

      case 8:
        return "US National Weather Service (NWSTG)";

      case 9:
        return "US National Weather Service (other)";

      case 10:
        return "Cairo (RSMC/RAFC)";

      case 12:
        return "Dakar (RSMC/RAFC)";

      case 14:
        return "Nairobi (RSMC/RAFC)";

      case 18:
        return "Tunis Casablanca (RSMC)";

      case 20:
        return "Las Palmas (RAFC)";

      case 21:
        return "Algiers (RSMC)";

      case 24:
        return "Pretoria (RSMC)";

      case 25:
        return "La R?union (RSMC)";

      case 26:
        return "Khabarovsk (RSMC)";

      case 28:
        return "New Delhi (RSMC/RAFC)";

      case 30:
        return "Novosibirsk (RSMC)";

      case 32:
        return "Tashkent (RSMC)";

      case 33:
        return "eddah (RSMC)";

      case 34:
        return "Tokyo (RSMC), Japan Meteorological Agency";

      case 36:
        return "Bangkok";

      case 37:
        return "Ulan Bator";

      case 38:
        return "Beijing (RSMC)";

      case 40:
        return "Seoul";

      case 41:
        return "Buenos Aires (RSMC/RAFC)";

      case 43:
        return "Brasilia (RSMC/RAFC)";

      case 45:
        return "Santiago";

      case 46:
        return "Brazilian Space Agency ? INPE";

      case 51:
        return "Miami (RSMC/RAFC)";

      case 52:
        return "Miami RSMC, National Hurricane Center";

      case 53:
        return "Montreal (RSMC)";

      case 55:
        return "San Francisco";

      case 57:
        return "Air Force Weather Agency";

      case 58:
        return "Fleet Numerical Meteorology and Oceanography Center";

      case 59:
        return "The NOAA Forecast Systems Laboratory";

      case 60:
        return "United States National Centre for Atmospheric Research (NCAR)";

      case 64:
        return "Honolulu";

      case 65:
        return "Darwin (RSMC)";

      case 67:
        return "Melbourne (RSMC)";

      case 69:
        return "Wellington (RSMC/RAFC)";

      case 71:
        return "Nadi (RSMC)";

      case 74:
        return "UK Meteorological Office Bracknell (RSMC)";

      case 76:
        return "Moscow (RSMC/RAFC)";

      case 78:
        return "Offenbach (RSMC)";

      case 80:
        return "Rome (RSMC)";

      case 82:
        return "Norrk?ping";

      case 85:
        return "Toulouse (RSMC)";

      case 86:
        return "Helsinki";

      case 87:
        return "Belgrade";

      case 88:
        return "Oslo";

      case 89:
        return "Prague";

      case 90:
        return "Episkopi";

      case 91:
        return "Ankara";

      case 92:
        return "Frankfurt/Main (RAFC)";

      case 93:
        return "London (WAFC)";

      case 94:
        return "Copenhagen";

      case 95:
        return "Rota";

      case 96:
        return "Athens";

      case 97:
        return "European Space Agency (ESA)";

      case 98:
        return "ECMWF, RSMC";

      case 99:
        return "De Bilt";

      case 110:
        return "Hong-Kong";

      case 210:
        return "Frascati (ESA/ESRIN)";

      case 211:
        return "Lanion";

      case 212:
        return "Lisboa";

      case 213:
        return "Reykjavik";

      case 254:
        return "EUMETSAT Operation Centre";

      default:
        return "Unknown";
    }

  }
  */
  /**
   * Identification of subcenter.
   *
   * @return subcenter as int
   */

  public final int getSubcenter_id() {
    return subcenter_id;
  }
  
  /**
   * Parameter Table Version number.
   *
   * @return master_table_version as int
   */
  public final int getMaster_table_version() {
    return master_table_version;
  }

  /**
   * local table version number.
   *
   * @return local_table_version as int
   */
  public final int getLocal_table_version() {
    return local_table_version;
  }

  /**
   * Model Run/Analysis/Reference time.
   *
   * @return significanceOfRT as int
   */
  public final int getSignificanceOfRT() {
    return significanceOfRT;
  }

  /**
   * Model Run/Analysis/Reference time.
   *
   * @return significanceOfRT Name
   */
  public final String getSignificanceOfRTName() {
    switch (significanceOfRT) {

      case 0:
        return "Analysis";

      case 1:
        return "Start of forecast";

      case 2:
        return "Verifying time of forecast";

      case 3:
        return "Observation time";

      default:
        return "Unknown";
    }
  }

  /**
   * return reference time of product in milliseconds.
   *  @return referenceTime
   */
  public final long getRefTime() {
    return refTime;
  }

  /**
   * reference reference or base time as Dare.
   *
   * @return baseTime
   */
  public final Date getBaseTime() {
    return baseTime;
  }

  /**
   * productStatus
   * values are operational, test, research, etc.
   *
   * @return productStatus as int
   */
  public final int getProductStatus() {
    return productStatus;
  }

  /**
   * productStatusName.
   *
   * @return productStatus name
   */
  public final String getProductStatusName() {
    switch (productStatus) {

      case 0:
        return "Operational products";

      case 1:
        return "Operational test products";

      case 2:
        return "Research products";

      case 3:
        return "Re-analysis products";

      case 4:
        return "THORPEX Interactive Grand Global Ensemble (TIGGE)";

      case 5:
        return "THORPEX Interactive Grand Global Ensemble (TIGGE) test";

      default:
        return "Unknown";
    }
  }

  /**
   * Product type.
   *
   * @return productType as int
   */
  public final int getProductType() {
    return productType;
  }

  /**
   * Product type name.
   *
   * @return productType name
   */
  public final String getProductTypeName() {
    switch (productType) {

      case 0:
        return "Analysis products";

      case 1:
        return "Forecast products";

      case 2:
        return "Analysis and Forecast products";

      case 3:
        return "Control Forecast products";

      case 4:
        return "Perturbed Forecast products";

      case 5:
        return "Control and Perturbed Forecast products";

      case 6:
        return "Processed Satellite observations";

      case 7:
        return "Processed Radar observations";

      case 8:
        return "Event Probability";
      
      default:
        return "GRID data";
    }
  }
}

