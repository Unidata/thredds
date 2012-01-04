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

package ucar.nc2.grib.grib2.table;

import net.jcip.annotations.Immutable;
import ucar.nc2.constants.CF;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.grib1.Grib1Record;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.iosp.grid.GridParameter;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Grib 2 Tables - allows local overrides and augmentation
 * This class implements the standard WMO tables.
 *
 * @author caron
 * @since 4/3/11
 */
@Immutable
public class Grib2Tables implements ucar.nc2.grib.GribTables {
  static private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Grib2Pds.class);
  static private Grib2Tables wmoTables, ncepTables, ndfdTables, kmaTables, dssTables;

  static public Grib2Tables factory(int center, int subCenter, int masterVersion, int localVersion) {
    /* if ((center == 7) && (masterVersion == 2)&& (localVersion == 1)) { // FAKE
      if (dssTables == null ) dssTables = new DssLocalTables(center, subCenter, masterVersion, localVersion);
      return dssTables;

    } else */
    if ((center == 7) || (center == 9) || (center == 54) || (center == 59)) { // canadian met, FSL
        if (ncepTables == null ) ncepTables = new NcepLocalTables(center, subCenter, masterVersion, localVersion);
        return ncepTables;

    } else if ((center == 8) && ((subCenter == 0) || (subCenter == -9999))){
      if (ndfdTables == null ) ndfdTables = new NdfdLocalTables(center, subCenter, masterVersion, localVersion);
      return ndfdTables;

    } else if (center == 40) {
      if (kmaTables == null ) kmaTables = new KmaLocalTables(center, subCenter, masterVersion, localVersion);
      return kmaTables;

    } else {
      if (wmoTables == null ) wmoTables = new Grib2Tables(center, subCenter, masterVersion, localVersion);
      return wmoTables;
    }
  }

  static public class GribTableId {
    public final String name;
    public final int center, subCenter, masterVersion, localVersion;

    GribTableId(String name, int center, int subCenter, int masterVersion, int localVersion) {
      this.name = name;
      this.center = center;
      this.subCenter = subCenter;
      this.masterVersion = masterVersion;
      this.localVersion = localVersion;
    }
  }

   public static class TableEntry implements Grib2Tables.Parameter, Comparable<Grib2Tables.TableEntry> {
    public int discipline, category, number;
    public String name, unit, abbrev;

    public TableEntry(int discipline, int category, int number, String name, String unit, String abbrev) {
      this.discipline = discipline;
      this.category = category;
      this.number = number;
      this.name = name.trim(); // StringUtil.toLowerCaseExceptFirstCharUpper(name.toLowerCase());
      this.abbrev = abbrev;
      this.unit = GridParameter.cleanupUnits(unit);
    }

    public String getId() {
      return discipline + "." + category + "." + number;
    }

    @Override
    public int compareTo(Grib2Tables.TableEntry o) {
      int c = discipline - o.discipline;
      if (c != 0) return c;
      c = category - o.category;
      if (c != 0) return c;
      return number - o.number;
    }

    @Override
    public int getDiscipline() {
      return discipline;
    }

    @Override
    public int getCategory() {
      return category;
    }

    @Override
    public int getNumber() {
      return number;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getUnit() {
      return unit;
    }

    public String getAbbrev() {
      return abbrev;
    }

    @Override
    public String toString() {
      return "TableEntry{" +
              "discipline=" + discipline +
              ", category=" + category +
              ", number=" + number +
              ", name='" + name + '\'' +
              ", unit='" + unit + '\'' +
              ", abbrev='" + abbrev + '\'' +
              '}';
    }
  }

  // debugging
  static public List<GribTableId> getLocalTableIds() {
    List<GribTableId> result = new ArrayList<GribTableId>();
    result.add(new GribTableId("NCEP",7,-1,-1,-1));
    result.add(new GribTableId("NDFD",8,0,-1,-1));
    result.add(new GribTableId("KMA",40,-1,-1,-1));
    result.add(new GribTableId("DSS",7,-1,2,1));
    return result;
  }


  static protected int makeHash(int discipline, int category, int number) {
    return (discipline << 16) + (category << 8) + number;
  }

  static public boolean isLocal(Parameter p) {
    return ((p.getCategory() > 191) || (p.getNumber() > 191));
  }

  ///////////////////////////////////////////////////////////////
  protected final int center, subCenter, masterVersion, localVersion;

  protected Grib2Tables(int center, int subCenter, int masterVersion, int localVersion) {
    this.center = center;
    this.subCenter = subCenter;
    this.masterVersion = masterVersion;
    this.localVersion = localVersion;
  }

  public String getVariableName(Grib2Record gr) {
    return getVariableName(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
  }

  public String getVariableName(int discipline, int category, int parameter) {
    String s = WmoCodeTable.getParameterName(discipline, category, parameter);
    if (s == null)
      s = "U" + discipline + "-" + category + "-" + parameter;
    return s;
  }

  public String getTableValue(String tableName, int code) {
    return WmoCodeTable.getTableValue(tableName, code);
  }

  public Grib2Tables.Parameter getParameter(int discipline, int category, int number) {
    return WmoCodeTable.getParameterEntry(discipline, category, number);
  }

  // debugging
  public List getParameters() {
    try {
      WmoCodeTable.WmoTables wmo = WmoCodeTable.getWmoStandard();
      WmoCodeTable params = wmo.map.get("4.2");
      return params.entries;
      //List<GribTables.Parameter> result = new ArrayList<GribTables.Parameter>();
      //for (WmoCodeTable.TableEntry entry : params.entries) result.add(entry); // covariant balony
      //return result;
    } catch (IOException e) {
      System.out.printf("Error reading wmo tables = %s%n", e.getMessage());
    }
    return null;
  }


  public CalendarDate getForecastDate(Grib2Record gr) {
    int val;
    Grib2Pds pds = gr.getPDS();
    if (pds.isInterval()) {
      int[] intv = getForecastTimeInterval(gr);
      val = intv[1];
    } else {
      val = pds.getForecastTime();
    }

    return gr.getReferenceDate().add( pds.getTimeDuration().multiply(val));
  }

  /*
  Code Table Code table 4.11 - Type of time intervals (4.11)
    0: Reserved
    1: Successive times processed have same forecast time, start time of forecast is incremented
    2: Successive times processed have same start time of forecast, forecast time is incremented
    3: Successive times processed have start time of forecast incremented and forecast time decremented so that valid time remains constant
    4: Successive times processed have start time of forecast decremented and forecast time incremented so that valid time remains constant
    5: Floating subinterval of time between forecast time and end of overall time interval

  static public class TimeInterval {
    public int statProcessType; // (code table 4.10) Statistical process used to calculate the processed field from the field at each time increment during the time range
    public int timeIncrementType;  // (code table 4.11) Type of time increment between successive fields used in the statistical processing
    public int timeRangeUnit;  // (code table 4.4) Indicator of unit of time for time range over which statistical processing is done
    public int timeRangeLength; // Length of the time range over which statistical processing is done, in units defined by the previous octet
    public int timeIncrementUnit; // (code table 4.4) Indicator of unit of time for the increment between the successive fields used
    public int timeIncrement; // Time increment between successive fields, in units defined by the previous octet

    from NDFD site:
    timeRangeUnit:    8-14 Day Outlooks = 2 (days); Monthly and Seasonal Outlooks = 3 (months)
    timeRangeLength:  8-14 Day Outlooks = 6 (days); Monthly Outlooks = 1 (month); Seasonal Outlooks = 3 (months)
   */

  /**
   * Get the time interval in units of gr.getPDS().getTimeUnit()
   * @param gr Grib record
   * @return time interval in units of gr.getPDS().getTimeUnit()
   */
  public int[] getForecastTimeInterval(Grib2Record gr) {
    // note  from Arthur Taylor (degrib):
    /* If there was a range I used:

    End of interval (EI) = (bytes 36-42 show an "end of overall time interval")
    C1) End of Interval = EI;
    Begin of Interval = EI - range

    and if there was no interval then I used:
    C2) End of Interval = Begin of Interval = Ref + ForeT.
    */
    if (!gr.getPDS().isInterval()) return null;
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) gr.getPDS();
    int timeUnit = gr.getPDS().getTimeUnit();

    // calculate total "range"
    int range = 0;
    for (Grib2Pds.TimeInterval ti : pdsIntv.getTimeIntervals()) {
      if ((ti.timeRangeUnit != timeUnit) || (ti.timeIncrementUnit != timeUnit && ti.timeIncrementUnit != 255)) {
        log.warn("TimeInterval has different units timeUnit= " + timeUnit + " TimeInterval=" + ti);
      }

      range += ti.timeRangeLength;
      if (ti.timeIncrementUnit != 255) range += ti.timeIncrement;
    }

    int[] result = new int[2];

    // End of Interval as date
    CalendarDate EI = pdsIntv.getIntervalTimeEnd();
    if (EI == null) {  // all values were set to zero   LOOK guessing!
      result[1] = range;
      result[0] = 0;

    } else {
      // End of Interval in units of getTimeUnit() since reference time
      CalendarPeriod period = Grib2Utils.getCalendarPeriod(timeUnit);
      int val = period.subtract(gr.getReferenceDate(), EI);

      result[1] = val;
      result[0] = result[1] - range;
    }

    return result;
  }

  /**
   * Get interval size in units of wantPeriod
   * @param gr must be an interval
   * @param wantPeriod in these units
   * @return  interval size in units of wantPeriod
   */
  public double getForecastTimeIntervalSize(Grib2Record gr, CalendarPeriod wantPeriod) {
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) gr.getPDS();
    int timeUnit = gr.getPDS().getTimeUnit();

    // calculate total "range" in units of timeUnit
    int range = 0;
    for (Grib2Pds.TimeInterval ti : pdsIntv.getTimeIntervals()) {
      if ((ti.timeRangeUnit != timeUnit) || (ti.timeIncrementUnit != timeUnit && ti.timeIncrementUnit != 255)) {
        log.warn("TimeInterval has different units timeUnit= " + timeUnit + " TimeInterval=" + ti);
      }

      range += ti.timeRangeLength;
      if (ti.timeIncrementUnit != 255) range += ti.timeIncrement;
    }

    // now convert that range to units of the requested period.
    CalendarPeriod timeUnitPeriod = Grib2Utils.getCalendarPeriod(timeUnit);
    if (timeUnitPeriod.equals(wantPeriod)) return range;
    double fac = wantPeriod.getConvertFactor(timeUnitPeriod);
    return fac * range;
  }

  public int[] getForecastTimeIntervalOld(Grib2Record gr) {
    // note  from Arthur Taylor (degrib):
    /* If there was a range I used:

    End of interval (EI) = (bytes 36-42 show an "end of overall time interval")
    C1) End of Interval = EI;      Begin of Interval = EI - range

    and if there was no interval then I used:
    C2) End of Interval = Begin of Interval = Ref + ForeT.
    */
    if (!gr.getPDS().isInterval()) return null;
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) gr.getPDS();
    int timeUnit = gr.getPDS().getTimeUnit();

    // calculate total "range"
    int range = 0;
    for (Grib2Pds.TimeInterval ti : pdsIntv.getTimeIntervals()) {
      if ((ti.timeRangeUnit != timeUnit) || (ti.timeIncrementUnit != timeUnit && ti.timeIncrementUnit != 255)) {
        log.warn("TimeInterval has different units timeUnit= " + timeUnit + " TimeInterval=" + ti);
      }

      range += ti.timeRangeLength;
      if (ti.timeIncrementUnit != 255) range += ti.timeIncrement;
    }

    int[] result = new int[2];

    // End of Interval as date
    CalendarDate EI = pdsIntv.getIntervalTimeEnd();
    if (EI == null) {  // all values were set to zero   LOOK guessing!
      //EI = gr.getReferenceDate();
      result[1] = range;
      result[0] = 0;

    } else {
      // End of Interval in units of getTimeUnit() since reference time
      long msecs = EI.getDifferenceInMsecs(gr.getReferenceDate());
      CalendarPeriod duration = Grib2Utils.getCalendarPeriod(timeUnit);
      int val = (int) Math.round(msecs / duration.getValueInMillisecs());

      result[1] = val;
      result[0] = result[1] - range;
    }

    return result;
  }

  // 4.5
  public String getLevelNameShort(int id) {

    switch (id) {
      case 1:
        return "Surface";
      case 2:
        return "Cloud_base";
      case 3:
        return "Cloud_tops";
      case 4:
        return "ZeroDegC_isotherm";
      case 5:
        return "Adiabatic_condensation_lifted";
      case 6:
        return "Maximum_wind";
      case 7:
        return "Tropopause";
      case 8:
        return "Atmosphere_top";
      case 9:
        return "Sea_bottom";
      case 10:
        return "Entire_atmosphere";
      case 11:
        return "Cumulonimbus_base";
      case 12:
        return "Cumulonimbus_top";
      case 20:
        return "Isotherm";
      case 100:
        return "Pressure";
      case 101:
        return "Msl";
      case 102:
        return "Altitude_above_msl";
      case 103:
        return "Height_above_ground";
      case 104:
        return "Sigma";
      case 105:
        return "Hybrid";
      case 106:
        return "Depth_below_surface";
      case 107:
        return "Isentrope";
      case 108:
        return "Pressure_difference";
      case 109:
        return "Potential_vorticity_surface";
      case 111:
        return "Eta";
      case 113:
        return "Log_hybrid";
      case 117:
        return "Mixed_layer_depth";
      case 118:
        return "Hybrid_height";
      case 119:
        return "Hybrid_pressure";
      case 120:
        return "Pressure_thickness";
      case 160:
        return "Depth_below_sea";
      case GribNumbers.UNDEFINED:
        return "none";
      default:
        return "UnknownLevelType-" + id;
    }
  }

  /*
Code Table Code table 4.7 - Derived forecast (4.7)
    0: Unweighted mean of all members
    1: Weighted mean of all members
    2: Standard deviation with respect to cluster mean
    3: Standard deviation with respect to cluster mean, normalized
    4: Spread of all members
    5: Large anomaly index of all members
    6: Unweighted mean of the cluster members
    7: Interquartile range (range between the 25th and 75th quantile)
    8: Minimum of all ensemble members
    9: Maximum of all ensemble members
   -1: Reserved
   -1: Reserved for local use
  255: Missing

  */
  public String getProbabilityNameShort(int id) {
    switch (id) {
      case 0:
        return "Unweighted_mean";
      case 1:
        return "Weighted_mean";
      case 2:
        return "Standard_deviation";
      case 3:
        return "Standard_deviation_normalized";
      case 4:
        return "Spread";
      case 5:
        return "Large_anomaly_index";
      case 6:
        return "Unweighted_mean_cluster";
      case 7:
        return "Interquartile_range";
      case 8:
        return "Minimum_ensemble";
      case 9:
        return "Maximum_ensemble";
      default:
        return "UnknownProbType" + id;
     }
  }

  // (code table 4.10) Statistical process used to calculate the processed field from the field at each time increment during the time range
  public String getIntervalNameShort(int id) {
    switch (id) {
      case 0:
        return "Average";
      case 1:
        return "Accumulation";
      case 2:
        return "Maximum";
      case 3:
        return "Minimum";
      case 4:
        return "Difference"; // (Value at the end of time range minus value at the beginning)";
      case 5:
        return "RootMeanSquare";
      case 6:
        return "StandardDeviation";
      case 7:
        return "Covariance"; // (Temporal variance)";
      case 8:
        return "Difference"; // (Value at the start of time range minus value at the end)";
      case 9:
        return "Ratio";
      default:
        return "UnknownIntervalType-" + id;
    }
  }

  public  CF.CellMethods convertTable4_10(int code) {
    switch (code) {
      case 0:
        return CF.CellMethods.mean; // "Average";
      case 1:
        return CF.CellMethods.sum; // "Accumulation";
      case 2:
        return CF.CellMethods.maximum; // "Maximum";
      case 3:
        return CF.CellMethods.minimum; // "Minimum";
      //case 4: return	"Difference"; // (Value at the end of time range minus value at the beginning)";
      //case 5: return	"RootMeanSquare";
      case 6:
        return CF.CellMethods.standard_deviation; // "StandardDeviation";
      case 7:
        return CF.CellMethods.variance; // "Covariance"; // (Temporal variance)";
      //case 8: return	"Difference"; // (Value at the start of time range minus value at the end)";
      //case 9: return	"Ratio";
      default:
        return null;
    }
  }

}
