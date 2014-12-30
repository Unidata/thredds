package ucar.nc2.grib.grib2;

import net.jcip.annotations.Immutable;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * The Identification section 1 for GRIB-2 files
 *
 * @author caron
 * @since 3/28/11
 */
@Immutable
public class Grib2SectionIdentification {

  private final int center_id;
  private final int subcenter_id;
  private final int master_table_version;
  private final int local_table_version;
  private final int significanceOfRT;
  private final int year, month, day, hour, minute, second;
  private final int productionStatus;
  private final int processedDataType;

  /**
   * Read Grib2SectionIndicator from raf.
   *
   * @param raf RandomAccessFile, with pointer at start of indicator section
   * @throws java.io.IOException on I/O error
   * @throws IllegalArgumentException if not a GRIB-2 record
   */
  public Grib2SectionIdentification(RandomAccessFile raf) throws IOException {
    long sectionEnd = raf.getFilePointer();

    // section 1 octet 1-4 (length of section)
    int length = GribNumbers.int4(raf);
    sectionEnd += length;

    int section = raf.read();
    if (section != 1)
      throw new IllegalArgumentException("Not a GRIB-2 Identification section");

    // Center  octet 6-7
    center_id = GribNumbers.int2(raf);

    // subCenter  octet 8-9
    subcenter_id = GribNumbers.int2(raf);

    // master table octet 10 (code table 1.0)
    master_table_version = raf.read();

    // local table octet 11  (code table 1.1)
    local_table_version = raf.read();

    // significanceOfRT octet 12 (code table 1.1)
    significanceOfRT = raf.read();

    // octets 13-19 (reference time of forecast)
    year = GribNumbers.int2(raf);
    month = raf.read();
    day = raf.read();
    hour = raf.read();
    minute = raf.read();
    second = raf.read();
    //refTime = CalendarDate.of(null, year, month, day, hour, minute, second);

    productionStatus = raf.read();
    processedDataType = raf.read();

    raf.seek(sectionEnd);
  }

  public Grib2SectionIdentification(int center_id, int subcenter_id, int master_table_version, int local_table_version,
                                    int significanceOfRT, int year, int month, int day, int hour, int minute, int second,
                                    int productionStatus, int processedDataType) {
    this.center_id = center_id;
    this.subcenter_id = subcenter_id;
    this.master_table_version = master_table_version;
    this.local_table_version = local_table_version;
    this.significanceOfRT = significanceOfRT;
    this.year = year;
    this.month = month;
    this.day = day;
    this.hour = hour;
    this.minute = minute;
    this.second = second;
    this.productionStatus = productionStatus;
    this.processedDataType = processedDataType;
  }

  /**
   * Identification of center (Common Code Table C-11)
   *
   * @return center id
   */
  public int getCenter_id() {
    return center_id;
  }

  /**
   * Identification of subcenter (allocated by center)
   *
   * @return subcenter
   */
  public int getSubcenter_id() {
    return subcenter_id;
  }

  /**
   * Parameter Table Version number (code table 1.0)
   *
   * @return master_table_version as int
   */
  public int getMaster_table_version() {
    return master_table_version;
  }

  /**
   * local table version number (code table 1.1)
   *
   * @return local_table_version as int
   */
  public int getLocal_table_version() {
    return local_table_version;
  }

  /**
   * Note 2 : if true, all entries come from the local table (!)
   * @return useLocalTablesOnly
   */
  public boolean useLocalTablesOnly() {
    return master_table_version == 255 && local_table_version != 0;
  }

  /**
   * Significance of Reference time (code table 1.2)
   *
   * @return significanceOfRT as int
   */
  public int getSignificanceOfRT() {
    return significanceOfRT;
  }

  /**
   * reference reference or base time as Dare.
   *
   * @return baseTime
   */
  public CalendarDate getReferenceDate() {
    return CalendarDate.of(null, year, month, day, hour, minute, second);
  }

  /**
   * production Status (code table 1.3)
   *
   * @return production Status
   */
  public int getProductionStatus() {
    return productionStatus;
  }

  /**
   * Get type Of Processed Data (code table 1.4)
   *
   * @return productType as int
   */
  public int getTypeOfProcessedData() {
    return processedDataType;
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public int getDay() {
    return day;
  }

  public int getHour() {
    return hour;
  }

  public int getMinute() {
    return minute;
  }

  public int getSecond() {
    return second;
  }

  @Override
  public String toString() {
    return "id {" +
            "center_id=" + center_id +
            ", subcenter_id=" + subcenter_id +
            ", master_table_version=" + master_table_version +
            ", local_table_version=" + local_table_version +
            '}';
  }
}
