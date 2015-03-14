/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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
 */

package ucar.nc2.grib.grib1;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribUtils;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib1.tables.Grib1ParamTableReader;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.wmo.CommonCodeTable;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.util.Formatter;
import java.util.zip.CRC32;

/**
 * The Product Definition Section for GRIB-1 files
 *
 * @author caron
 */

@Immutable
public final class Grib1SectionProductDefinition {

  private final byte[] rawData;

  /**
   * Read Product Definition section from raf.
   *
   * @param raf RandomAccessFile, with pointer at start of section
   * @throws java.io.IOException      on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
  public Grib1SectionProductDefinition(RandomAccessFile raf) throws IOException {
    int length = GribNumbers.uint3(raf);
    rawData = new byte[length];
    raf.skipBytes(-3);
    raf.readFully(rawData);
  }

  /**
   * Set PDS section from byte array.
   *
   * @param rawData the byte array
   */
  public Grib1SectionProductDefinition(byte[] rawData) {
    this.rawData = rawData;
  }

  /**
   * get the raw bytes of the PDS.
   *
   * @return PDS as byte[]
   */
  public byte[] getRawBytes() {
    return rawData;
  }

  public int getLength() {
    return GribNumbers.uint3(getOctet(1),getOctet(2),getOctet(3));
  }

  /**
   * gets the Table version (octet 4).
   *
   * @return table version
   */
  public final int getTableVersion() {
    return getOctet(4);
  }

  /**
   * Center (octet 5) common code C-1.
   *
   * @return center id
   */
  public final int getCenter() {
    return getOctet(5);
  }

  /**
   * Generating Process (octet 6).
   *
   * @return typeGenProcess
   */
  public final int getGenProcess() {
    return getOctet(6);
  }

  /**
   * Grid Definition (octet 7).
   * "Number of grid used – from catalogue defined by originating centre". So this is center dependent.
   * "Where octet 7 defines a catalogued grid, that grid should also be defined in Section 2, provided the flag in octet 8
   * indicates inclusion of Section 2.
   * Octet 7 must be set to 255 to indicate a non-catalogued grid, in which case the grid will be defined in Section 2."
   *
   * @return Grid Definition.
   */
  public final int getGridDefinition() {
    return getOctet(7);
  }

  /**
   * Flag (octet 8).
   *
   * @return Flag
   */
  public final int getFlag() {
    return getOctet(8);
  }

  /**
   * Parameter number (octet 9) - code table 2.
   *
   * @return index number of parameter in table
   */
  public final int getParameterNumber() {
    return getOctet(9);
  }

  /**
   * Level type (octet 10) - code table 3.
   *
   * @return level type
   */
  public final int getLevelType() {
    return getOctet(10);
  }

  /**
   * Level value1 (octet 11).
   *
   * @return level value1
   */
  public final int getLevelValue1() {
    return getOctet(11);
  }

  /**
   * Level value2 (octet 12).
   *
   * @return level value2
   */
  public final int getLevelValue2() {
    return getOctet(12);
  }

  /**
   * Reference Date (octet 13-17).
   * Reference time of data – date and time of start of averaging or accumulation period.
   *
   * @return Reference Date as CalendarDate.
   */
  public final CalendarDate getReferenceDate() {
    int century = getReferenceCentury() - 1;
    if (century == -1) century = 20;

    int year = getOctet(13);
    int month = getOctet(14);
    int day = getOctet(15);
    int hour = getOctet(16);
    int minute = getOctet(17);
    return CalendarDate.of(null, century * 100 + year, month, day, hour, minute, 0);
  }

  /**
   * Time unit (octet 18) - code table 4. Same as GRIB2 Table 4.4
   *
   * @return time unit
   */
  public final int getTimeUnit() {
    return getOctet(18);
  }

  /**
   * Time value 1 (octet 19).
   * Period of time (number of time units) (0 for analyses or initialized analyses).
   * Units of time given by octet 18
   *
   * @return time value 1
   */
  public final int getTimeValue1() {
    return getOctet(19);
  }

  /**
   * Time value 2 (octet 20).
   * Period of time (number of time units); or Time interval between successive analyses,
   * initialized analyses or forecasts, undergoing averaging or accumulation.
   * Units of time given by octet 18
   *
   * @return time value 2
   */
  public final int getTimeValue2() {
    return getOctet(20);
  }

  /**
   * Time range indicator (octet 21) - code table 5.
   *
   * @return Time range indicator
   */
  public final int getTimeRangeIndicator() {
    return getOctet(21);
  }

  /**
   * Number included in statistics (octet 22-23).
   * Number included in calculation when octet 21 (Code table 5) refers to a statistical
   * process, such as average or accumulation; otherwise set to zero
   *
   * @return Number included in statistics
   */
  public final int getNincluded() {
    return GribNumbers.int2(getOctet(22), getOctet(23));
  }

  /**
   * Number missing in statistics (octet 24).
   * Number missing from calculation in case of statistical process
   *
   * @return Number missing in statistics
   */
  public final int getNmissing() {
    return getOctet(24);
  }

  /**
   * Century of reference (octet 25).
   *
   * @return Century of reference
   */
  public final int getReferenceCentury() {
    return getOctet(25);
  }

  /**
   * Center (octet 26) common code C-12.
   *
   * @return subcenter id
   */
  public final int getSubCenter() {
    return getOctet(26);
  }


  /**
   * Units decimal scale factor (octet 27-28) .
   *
   * @return Units decimal scale factor
   */
  public final int getDecimalScale() {
    return GribNumbers.int2(getOctet(27), getOctet(28));
  }

  /**
   * Check if GDS exists from the flag.
   *
   * @return true, if GDS exists
   */
  public final boolean gdsExists() {
    return (getFlag() & 128) == 128;
  }

  /**
   * Check if BMS exists from the flag
   *
   * @return true, if BMS exists
   */
  public final boolean bmsExists() {
    return (getFlag() & 64) == 64;
  }

  /**
   * Get the indexth byte in the PDS as an integer.
   * THIS IS ONE BASED (not zero) to correspond with the manual
   *
   * @param index 1 based index
   * @return rawData[index-1] & 0xff
   */
  private int getOctet(int index) {
    if (index > rawData.length) return GribNumbers.UNDEFINED;
    return rawData[index - 1] & 0xff;
  }

  /////////////////////////////////////////////////////////////////////

  private String getCalendarPeriodAsString() {
    try {
      return GribUtils.getCalendarPeriod(getTimeUnit()).toString();
    } catch (UnsupportedOperationException e) {
      return "Unknown Time Unit";
    }
  }

  public void showPds(Grib1Customizer cust, Formatter f) {

    f.format("5           Originating Center : (%d) %s%n", getCenter(), CommonCodeTable.getCenterName(getCenter(), 1));
    f.format("26       Originating SubCenter : (%d) %s%n", getSubCenter(), cust.getSubCenterName( getSubCenter()));
    f.format("4                Table Version : %d%n", getTableVersion());

    Grib1Parameter parameter = cust.getParameter(getCenter(), getSubCenter(), getTableVersion(), getParameterNumber());
    if (parameter != null) {
      Grib1ParamTableReader ptable = parameter.getTable();
      f.format("               Parameter Table : (%d-%d-%d) %s%n", getCenter(), getSubCenter(), getTableVersion(), (ptable == null) ? "MISSING" : ptable.getPath());
      f.format("                Parameter Name : (%d) %s%n", getParameterNumber(), parameter.getName());
      f.format("                Parameter Desc : %s%n", parameter.getDescription());
      f.format("               Parameter Units : %s%n", parameter.getUnit());
    } else {
      f.format("               Parameter %d not found%n", getParameterNumber());
    }

    f.format("6      Generating Process Type : (%d) %s%n", getGenProcess(), cust.getGeneratingProcessName(getGenProcess()));
    f.format("7              Grid Definition : (%d) %n", getGridDefinition());
    f.format("8                         Flag : (%d) %n", getFlag());

    f.format("13-17           Reference Time : %s%n", getReferenceDate());
    f.format("18                  Time Units : (%d) %s%n", getTimeUnit(), getCalendarPeriodAsString());
    Grib1ParamTime ptime = cust.getParamTime(this);;
    f.format("19                 Time 1 (P1) : %d%n", getTimeValue1());
    f.format("20                 Time 2 (P2) : %d%n", getTimeValue2());
    f.format("21        Time Range Indicator : (%d) %s%n", getTimeRangeIndicator(), ptime.getTimeTypeName());
    f.format("22-23           N in statistic : (%d)%n", getNincluded());
    f.format("24                   N missing : (%d)%n", getNmissing());
    f.format("                   Time  coord : %s%n", ptime.getTimeCoord());
    Grib1ParamLevel plevel = cust.getParamLevel(this);
    f.format("10                  Level Type : (%d) %s%n", getLevelType(), plevel.getNameShort());
    f.format("             Level Description : %s%n", plevel.getDescription());
    f.format("                 Level Value 1 : %f%n", plevel.getValue1());
    f.format("                 Level Value 2 : %f%n", plevel.getValue2());
    f.format("27-28     Decimal Scale Factor : %d%n", getDecimalScale());

    f.format("                    GDS Exists : %s%n", gdsExists());
    f.format("                    BMS Exists : %s%n", bmsExists());
  }

 ////////////////////////////////////////////////////////
  // Ensembles

/* http://www.ecmwf.int/publications/manuals/d/gribapi/fm92/grib1/show/local/
   1)
   see " local definition # 1"
   from Ernst de Vreede via Jitka 01/31/2012
  ECMWF operational Ensemble Prediction System (EPS) have extra information in the PDS:
    Octet 41 = 1: ECMWF local GRIB extension: 1 = Mars labelling or ensemble forecast data&
    Octet 42 = 1: ECMWF GRIB class, 1=operational data
    Octet 43 = 10/11: ECMWF GRIB type: 10=Control forecast, 11=Perturbed forecast
    Octet 44,45 = 1035, ECMWF GRIB stream 1035 = Ensemble Prediction System
    Octet 50 = perturbationNumber (0 for control, 1-50 for perturbed forecasts)
    Octet 51 = numberOfForecastsInEnsemble (0 if not ensemble, 1-50 for perturbed forecasts)

  The really distinguishing octets are: octet 41 =1, octet 42=1, octet 43=10 or 11, octet 51>0 with octet 50 giving the perturbation number.
  The other parameters octets 42 (operational stream) and octet 44/45 might be too specific, might narrow down too much.

  2)
  http://www.ecmwf.int/publications/manuals/d/gribapi/fm92/grib1/detail/local/30/
*/

  /*
   * NCEP Appendix C Manual 388
   * http://www.nco.ncep.noaa.gov/pmb/docs/on388/appendixc.html
   * states that if the PDS is > 28 bytes and octet 41 == 1
   * then it's an ensemble an product.
   */

  public boolean isEnsemble() {
    switch (getCenter()) {
      case 7:
        return ((rawData.length >= 43) && (getOctet(41) == 1));

      case 98:
        return ((rawData.length >= 51) &&
            (getOctet(41) == 1 || getOctet(41) == 30) &&
            (getOctet(43) == 10 || getOctet(43) == 11));
    }
    return false;
  }

  public final int getPerturbationType() {
    if (!isEnsemble()) return GribNumbers.UNDEFINED;
    switch (getCenter()) {
      case 7: return getOctet(42);
      case 98: return getOctet(43);
    }
    return GribNumbers.UNDEFINED;
  }

  public final int getPerturbationNumber() {
    if (!isEnsemble()) return GribNumbers.UNDEFINED;

    switch (getCenter()) {
      case 7: {
        int type =  getOctet(42);
        int id =  getOctet(43);
        if (type == 1) return 0;
        if (type == 2) return id;
        if (type == 3) return 5 + id;
      }
      case 98: return getOctet(50);
    }
    return GribNumbers.UNDEFINED;
  }

  public long calcCRC() {
    CRC32 crc32 = new CRC32();
    crc32.update(rawData);
    return crc32.getValue();
  }

}


