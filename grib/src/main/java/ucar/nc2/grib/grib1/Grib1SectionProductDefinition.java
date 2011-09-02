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
import ucar.grib.GribNumbers;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;
import java.io.IOException;

/**
 * The Product Definition Section for GRIB-1 files
 *
 * @author caron
 */

@Immutable
public final class Grib1SectionProductDefinition {

  private byte[] rawData;

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
    raf.read(rawData);
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
   *
   * @return Grid Definition
   */
  public final int getGridTemplate() {
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
   * Level value (octet 11-12).
   *
   * @return level value
   */
  public final int getLevelValue() {
    return GribNumbers.int2(getOctet(11), getOctet(12));
  }

  /**
   * Reference Date (octet 13-17).
   * Reference time of data – date and time of start of averaging or accumulation period.
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
   * Time unit (octet 18) - code table 4.
   *
   * @return time unit
   */
  public final int getTimeUnit() {
    return getOctet(18);
  }

  /**
   * Time value 1 (octet 19).
   *  Period of time (number of time units) (0 for analyses or initialized analyses).
   *  Units of time given by octet 18
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
   public final int getTimeRange() {
     return getOctet(21);
   }

  /**
    * Number included in statistics (octet 22-23).
    *  Number included in calculation when octet 21 (Code table 5) refers to a statistical
   * process, such as average or accumulation; otherwise set to zero
    * @return Number included in statistics
    */
   public final int getNincluded() {
     return GribNumbers.int2(getOctet(22), getOctet(23));
   }

  /**
    * Number missing in statistics (octet 24).
    * Number missing from calculation in case of statistical process
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
   * @deprecated
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
  private final int getOctet(int index) {
    if (index > rawData.length) return GribNumbers.UNDEFINED;
    return rawData[index - 1] & 0xff;
  }

  /**
  * Get the time of the forecast.
  *
  * @return date and time
  * @deprecated
  */
 public final int[] getForecastTime() {
   return null;
 }

}


