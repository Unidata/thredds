/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grib2.table;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import thredds.featurecollection.TimeUnitConverter;
import ucar.nc2.grib.GribNumbers;
import ucar.nc2.grib.GribStatType;
import ucar.nc2.grib.GribTables;
import ucar.nc2.grib.collection.GribCollectionImmutable.VariableIndex;
import ucar.nc2.grib.coord.TimeCoordIntvDateValue;
import ucar.nc2.grib.coord.TimeCoordIntvValue;
import ucar.nc2.grib.coord.VertCoordType;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.Grib2SectionIdentification;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.TableType;
import ucar.nc2.grib.grib2.table.WmoCodeFlagTables.WmoTable;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.wmo.CommonCodeTable;

import javax.annotation.concurrent.Immutable;
import java.util.*;

/**
 * Grib 2 Tables - allows local overrides and augmentation of WMO tables.
 * This class serves the standard WMO tables, local tables are subclasses that override.
 * Methods are placed here because they may be overrided by local Tables.
 *
 * Tables include code, flag and parameter tables.
 *
 * @author caron
 * @since 4/3/11
 */
@Immutable
public class Grib2Tables implements ucar.nc2.grib.GribTables, TimeUnitConverter {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2Tables.class);
  private static Map<Grib2TablesId, Grib2Tables> tables = new HashMap<>();
  private static Grib2Tables wmoStandardTable = null;

  public static Grib2Tables factory(Grib2Record gr) {
    Grib2SectionIdentification ids = gr.getId();
    Grib2Pds pds = gr.getPDS();
    return factory(ids.getCenter_id(), ids.getSubcenter_id(), ids.getMaster_table_version(), ids.getLocal_table_version(), pds.getGenProcessId());
  }

  // Lazy instantiation.
  public static Grib2Tables factory(int center, int subCenter, int masterVersion, int localVersion, int genProcessId) {
    Grib2TablesId id = new Grib2TablesId(center, subCenter, masterVersion, localVersion, genProcessId);
    Grib2Tables cust = tables.get(id);
    if (cust != null) return cust;

    // note that we match on id, so same Grib2Customizer may be mapped to multiple id's (eg match on -1)
    Grib2TableConfig config = Grib2TableConfig.matchTable(id);
    cust = build(config);

    tables.put(id, cust);
    return cust;
  }

  private static Grib2Tables build(Grib2TableConfig config) {
    switch (config.getType()) {
      case cfsr: return new CfsrLocalTables(config);
      case eccodes: return new EccodesLocalTables(config);
      case gempak: return new GempakLocalTables(config); // LOOK: not used
      case gsd: return new FslHrrrLocalTables(config);
      case kma: return new KmaLocalTables(config);
      case ncep: return new NcepLocalTables(config);
      case ndfd: return new NdfdLocalTables(config);
      case mrms: return new MrmsLocalTables(config);
      case nwsDev: return new NwsMetDevTables(config);
      default:
        if (wmoStandardTable == null) wmoStandardTable = new Grib2Tables(config);
        return wmoStandardTable;
    }
  }

  public static int makeParamId(int discipline, int category, int number) {
    return (discipline << 16) + (category << 8) + number;
  }

  public static int[] unmakeParamId(int code) {
    int number = code & 255;
    code = code >> 8;
    int category = code & 255;
    int discipline = code >> 8;
    return new int[] {discipline, category, number};
  }

  public static String makeParamCode(int discipline, int category, int number) {
    return String.format("%d-%d-%d", discipline, category, number);
  }

  public static String makeParamCode(int code) {
    int number = code & 255;
    code = code >> 8;
    int category = code & 255;
    int discipline = code >> 8;
    return String.format("%d-%d-%d", discipline, category, number);
  }

  public static boolean isLocal(Parameter p) {
    return isLocal(p.getDiscipline(), p.getCategory(), p.getNumber());
  }

  public static boolean isLocal(int discipline, int category, int number) {
    return ((discipline <= 191) && (category <= 191) && (number <= 191));
  }

  public static boolean isLocal(int code) {
    int[] uncode = unmakeParamId(code);
    return isLocal(uncode[0], uncode[1], uncode[2]);
  }

  public static ImmutableList<Grib2Tables> getAllRegisteredTables() {
    ImmutableList.Builder<Grib2Tables> builder = ImmutableList.builder();
    for (Grib2TableConfig config : Grib2TableConfig.getTables()) {
      builder.add(build(config));
    }
    return builder.build();
  }

  ///////////////////////////////////////////////////////////////
  protected final Grib2TableConfig config;
  private boolean timeUnitWarnWasSent;

  protected Grib2Tables(Grib2TableConfig config) {
    this.config = config;
  }

  public String getName() {
    return config.getName();
  }

  public int getCenterId() {
    return config.getConfigId().center;
  }

  public String getPath() {
    return config.getPath();
  }

  public Grib2TablesId getConfigId() {
    return config.getConfigId();
  }

  public Grib2TablesId.Type getType() {
    return config.getType();
  }

  public String getVariableName(Grib2Record gr) {
    return getVariableName(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
  }

  /**
   * Make a IOSP Variable name, using the Parameter name is available, otherwise a synthezized name.
   */
  public String getVariableName(int discipline, int category, int parameter) {
    String s = WmoParamTable.getParameterName(discipline, category, parameter);
    if (s == null)
      s = "U" + discipline + "-" + category + "-" + parameter;
    return s;
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // Parameter interface (table 4.2.x)

  @Nullable
  public GribTables.Parameter getParameter(Grib2Record gr) {
    return getParameter(gr.getDiscipline(), gr.getPDS().getParameterCategory(), gr.getPDS().getParameterNumber());
  }

  @Nullable
  public GribTables.Parameter getParameter(VariableIndex vindex) {
    return getParameter(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
  }

  public GribTables.Parameter getParameter(int discipline, Grib2Pds pds) {
    return getParameter(discipline, pds.getParameterCategory(), pds.getParameterNumber());
  }

  @Nullable
  GribTables.Parameter getParameter(int discipline, int category, int number) {
    return WmoParamTable.getParameter(discipline, category, number);
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // Code interface (tables other than 4.2.x)

  @Nullable
  public String getCodeTableValue(String tableName, int code) {
    WmoCodeTable codeTable = WmoCodeFlagTables.getInstance().getCodeTable(tableName);
    Grib2CodeTableInterface.Entry entry = (codeTable == null) ? null : codeTable.getEntry(code);
    return (entry == null) ? null : entry.getName();
  }

  @Override
  @Nullable
  public String getSubCenterName(int center_id, int subcenter_id) {
    return CommonCodeTable.getSubCenterName(center_id, subcenter_id);
  }

  @Nullable
  public String getGeneratingProcessName(int genProcess) {
    return null;
  }

  @Nullable
  public String getGeneratingProcessTypeName(int genProcess) {
    return getCodeTableValue("4.3", genProcess);
  }

  @Nullable
  public String getCategory(int discipline, int category) {
    WmoCodeTable catTable = WmoCodeFlagTables.getInstance().getCodeTable("4.1." + discipline);
    Grib2CodeTableInterface.Entry entry = (catTable == null) ? null : catTable.getEntry(category);
    return (entry == null) ? null : entry.getName();
  }

  public String getStatisticName(int id) {
    String result = getCodeTableValue("4.10", id); // WMO
    if (result == null)
      result = getStatisticNameShort(id);
    return result;
  }

  public String getStatisticNameShort(int id) {
    GribStatType stat = GribStatType.getStatTypeFromGrib2(id);
    return (stat == null) ? "UnknownStatType-" + id : stat.toString();
  }

  @Override
  @Nullable
  public GribStatType getStatType(int grib2StatCode) {
    return GribStatType.getStatTypeFromGrib2(grib2StatCode);
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
        return "unweightedMean";
      case 1:
        return "weightedMean";
      case 2:
        return "stdDev";
      case 3:
        return "stdDevNormalized";
      case 4:
        return "spread";
      case 5:
        return "largeAnomalyIndex";
      case 6:
        return "unweightedMeanCluster";
      case 7:
        return "interquartileRange";
      case 8:
        return "minimumEnsemble";
      case 9:
        return "maximumEnsemble";
      default:
        return "UnknownProbType" + id;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  // Vertical Units

  /**
   * Unit of vertical coordinate.
   * from Grib2 code table 4.5.
   * Only levels with units get a dimension added
   *
   * @param code code from table 4.5
   * @return level unit, default is empty unit string
   */
  @Override
  public VertCoordType getVertUnit(int code) {
    //     VertCoordType(int code, String desc, String abbrev, String units, String datum, boolean isPositiveUp, boolean isLayer)
    switch (code) {

      case 11:
      case 12:
        return new VertCoordType(code, "m", null, true);

      case 20:
        return new VertCoordType(code, "K", null, false);

      case 100:
        return new VertCoordType(code, "Pa", null, false);

      case 102:
        return new VertCoordType(code, "m", "mean sea level", true);

      case 103:
        return new VertCoordType(code, "m", "ground", true);

      case 104:
      case 105:
        return new VertCoordType(code, "sigma", null, false); // positive?

      case 106:
        return new VertCoordType(code, "m", "land surface", false);

      case 107:
        return new VertCoordType(code, "K", null, true); // positive?

      case 108:
        return new VertCoordType(code, "Pa", "ground", true);

      case 109:
        return new VertCoordType(code, "K m2 kg-1 s-1", null, true); // positive?

      case 114:
        return new VertCoordType(code, "numeric", null, false);

      case 117:
        return new VertCoordType(code, "m", null, true);

      case 119:
        return new VertCoordType(code, "Pa", null, false); // ??

      case 160:
        return new VertCoordType(code, "m", "sea level", false);

      case 161:
        return new VertCoordType(code, "m", "water surface", false);

      // LOOK NCEP specific
      case 235:
        return new VertCoordType(code, "0.1 C", null, true);

      case 237:
        return new VertCoordType(code, "m", null, true);

      case 238:
        return new VertCoordType(code, "m", null, true);

      default:
        return new VertCoordType(code, null, null, true);
    }
  }

  public boolean isLevelUsed(int code) {
    VertCoordType vunit = getVertUnit(code);
    return vunit.isVerticalCoordinate();
  }

  @Nullable
  public String getLevelName(int id) {
    return getCodeTableValue("4.5", id);
  }

  public boolean isLayer(Grib2Pds pds) {
    return !(pds.getLevelType2() == 255 || pds.getLevelType2() == 0);
  }

  // Table 4.5
  @Override
  public String getLevelNameShort(int id) {

    switch (id) {
      case 1:
        return "surface";
      case 2:
        return "cloud_base";
      case 3:
        return "cloud_tops";
      case 4:
        return "zeroDegC_isotherm";
      case 5:
        return "adiabatic_condensation_lifted";
      case 6:
        return "maximum_wind";
      case 7:
        return "tropopause";
      case 8:
        return "atmosphere_top";
      case 9:
        return "sea_bottom";
      case 10:
        return "entire_atmosphere";
      case 11:
        return "cumulonimbus_base";
      case 12:
        return "cumulonimbus_top";
      case 20:
        return "isotherm";
      case 100:
        return "isobaric";
      case 101:
        return "msl";
      case 102:
        return "altitude_above_msl";
      case 103:
        return "height_above_ground";
      case 104:
        return "sigma";
      case 105:
        return "hybrid";
      case 106:
        return "depth_below_surface";
      case 107:
        return "isentrope";
      case 108:
        return "pressure_difference";
      case 109:
        return "potential_vorticity_surface";
      case 111:
        return "eta";
      case 113:
        return "log_hybrid";
      case 117:
        return "mixed_layer_depth";
      case 118:
        return "hybrid_height";
      case 119:
        return "hybrid_pressure";
      case 120:
        return "pressure_thickness";
      case 160:
        return "depth_below_sea";
      case GribNumbers.UNDEFINED:
        return "none";
      default:
        return "UnknownLevelType-" + id;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // Time utilities, generally Grib2 specific.

  private TimeUnitConverter timeUnitConverter;  // LOOK not really immutable

  public void setTimeUnitConverter(TimeUnitConverter timeUnitConverter) {
    if (this.timeUnitConverter != null) throw new RuntimeException("Cant modify timeUnitConverter once its been set");
    this.timeUnitConverter = timeUnitConverter;
  }

  @Override
  public int convertTimeUnit(int timeUnit) {
    if (timeUnitConverter == null) return timeUnit;
    return timeUnitConverter.convertTimeUnit(timeUnit);
  }

  @Nullable
  public CalendarDate getForecastDate(Grib2Record gr) {
    Grib2Pds pds = gr.getPDS();
    if (pds.isTimeInterval()) {
      TimeCoordIntvDateValue intv = getForecastTimeInterval(gr);
      return intv == null ? null : intv.getEnd();

    } else {
      int val = pds.getForecastTime();
      CalendarPeriod period = Grib2Utils.getCalendarPeriod(convertTimeUnit(pds.getTimeUnit()));
      if (period == null) return null;
      return gr.getReferenceDate().add(period.multiply(val));
    }
  }

  /*
  Code Table Code table 4.11 - Type of time intervals (4.11)
    0: Reserved
    1: Successive times processed have same forecast time, start time of forecast is incremented
    2: Successive times processed have same start time of forecast, forecast time is incremented
    3: Successive times processed have start time of forecast incremented and forecast time decremented so that valid time remains constant
    4: Successive times processed have start time of forecast decremented and forecast time incremented so that valid time remains constant
    5: Floating subinterval of time between forecast time and end of overall time interval

  public static class TimeInterval {
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
   *
   * @param gr Grib record, must have pds that is a  time interval.
   * @return time interval in units of gr.getPDS().getTimeUnit()
   */
  @Nullable
  public TimeCoordIntvDateValue getForecastTimeInterval(Grib2Record gr) {
    // note  from Arthur Taylor (degrib):
    /* If there was a range I used:

    End of interval (EI) = (bytes 36-42 show an "end of overall time interval")
    C1) End of Interval = EI;
    Begin of Interval = EI - range

    and if there was no interval then I used:
    C2) End of Interval = Begin of Interval = Ref + ForeT.
    */
    if (!gr.getPDS().isTimeInterval()) return null;
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) gr.getPDS();
    int timeUnitOrg = gr.getPDS().getTimeUnit();

    // calculate total "range"
    int range = 0;
    for (Grib2Pds.TimeInterval ti : pdsIntv.getTimeIntervals()) {
      if (ti.timeRangeUnit == 255)
        continue;
      if ((ti.timeRangeUnit != timeUnitOrg) || (ti.timeIncrementUnit != timeUnitOrg && ti.timeIncrementUnit != 255 && ti.timeIncrement != 0)) {
        if (!timeUnitWarnWasSent) {
          logger.warn("TimeInterval has different units timeUnit org=" + timeUnitOrg + " TimeInterval=" + ti.timeIncrementUnit);
          timeUnitWarnWasSent = true;
          // throw new RuntimeException("TimeInterval(2) has different units");
        }
      }

      range += ti.timeRangeLength;
      if (ti.timeIncrementUnit != 255) range += ti.timeIncrement;
    }

    CalendarPeriod unitPeriod = Grib2Utils.getCalendarPeriod(convertTimeUnit(timeUnitOrg));
    if (unitPeriod == null) return null;
    CalendarPeriod period = unitPeriod.multiply(range);

    // End of Interval as date
    CalendarDate EI = pdsIntv.getIntervalTimeEnd();
    if (EI == CalendarDate.UNKNOWN) {  // all values were set to zero   LOOK guessing!
      return new TimeCoordIntvDateValue(gr.getReferenceDate(), period);
    } else {
      return new TimeCoordIntvDateValue(period, EI);
    }
  }

  /**
   * Get interval size in units of hours.
   * Only use in GribVariable to decide on variable identity when intvMerge = false.
   * @param pds must be a Grib2Pds.PdsInterval
   * @return interval size in units of hours
   */
  public double getForecastTimeIntervalSizeInHours(Grib2Pds pds) {
    Grib2Pds.PdsInterval pdsIntv = (Grib2Pds.PdsInterval) pds;
    int timeUnitOrg = pds.getTimeUnit();

    // calculate total "range" in units of timeUnit
    int range = 0;
    for (Grib2Pds.TimeInterval ti : pdsIntv.getTimeIntervals()) {
      if (ti.timeRangeUnit == 255)
        continue;
      if ((ti.timeRangeUnit != timeUnitOrg) || (ti.timeIncrementUnit != timeUnitOrg && ti.timeIncrementUnit != 255 && ti.timeIncrement != 0)) {
        logger.warn("TimeInterval(2) has different units timeUnit org=" + timeUnitOrg + " TimeInterval=" + ti.timeIncrementUnit);
        throw new RuntimeException("TimeInterval(2) has different units");
      }

      range += ti.timeRangeLength;
      if (ti.timeIncrementUnit != 255) range += ti.timeIncrement;
    }

    // now convert that range to units of the requested period.
    CalendarPeriod timeUnitPeriod = Grib2Utils.getCalendarPeriod(convertTimeUnit(timeUnitOrg));
    if (timeUnitPeriod == null) return GribNumbers.UNDEFINEDD;
    if (timeUnitPeriod.equals(CalendarPeriod.Hour)) return range;

    double fac;
    if (timeUnitPeriod.getField() == CalendarPeriod.Field.Month) {
      fac = 30.0 * 24.0;  // nominal hours in a month
    } else if (timeUnitPeriod.getField() == CalendarPeriod.Field.Year) {
      fac = 365.0 * 24.0; // nominal hours in a year
    } else {
      fac = CalendarPeriod.Hour.getConvertFactor(timeUnitPeriod);
    }
    return fac * range;
  }

  /**
   * If this has a time interval coordinate, get time interval
   *
   * @param gr from this record
   * @return time interval in units of pds.getTimeUnit(), or null if not a time interval
   */
  @Nullable
  public int[] getForecastTimeIntervalOffset(Grib2Record gr) {
    TimeCoordIntvDateValue tinvd = getForecastTimeInterval(gr);
    if (tinvd == null) return null;

    Grib2Pds pds = gr.getPDS();
    int unit = convertTimeUnit(pds.getTimeUnit());
    TimeCoordIntvValue tinv = tinvd.convertReferenceDate(gr.getReferenceDate(), Grib2Utils.getCalendarPeriod(unit));
    if (tinv == null) return null;
    int[] result = new int[2];
    result[0] = tinv.getBounds1();
    result[1] = tinv.getBounds2();
    return result;
  }

  /////////////////////////////////////////////////////
  // debugging

  /**
   * Get the unprocessed parameter provided by this Grib2Table.
   */
  @Nullable
  public GribTables.Parameter getParameterRaw(int discipline, int category, int number) {
    return WmoParamTable.getParameter(discipline, category, number);
  }

  /**
   * Get the name of the parameter table that is being used for this parameter.
   */
  public String getParamTablePathUsedFor(int discipline, int category, int number) {
    return WmoCodeFlagTables.standard.getResourceName();
  }

  /**
   * Get the list of parameters provided by this Grib2Table.
   */
  public ImmutableList<GribTables.Parameter> getParameters() {
    ImmutableList.Builder<GribTables.Parameter> allParams = ImmutableList.builder();
    for (WmoTable wmoTable : WmoCodeFlagTables.getInstance().getWmoTables()) {
      if (wmoTable.getType() == TableType.param) {
        WmoParamTable paramTable = new WmoParamTable(wmoTable);
        allParams.addAll(paramTable.getParameters());
      }
    }
    return allParams.build();
  }

  public void lookForProblems(Formatter f) {
  }

  public void showDetails(Formatter f) {
  }

  public void showEntryDetails(Formatter f, List<GribTables.Parameter> params) {
  }

  public void showSpecialPdsInfo(Grib2Record pds, Formatter f) {
  }
}
