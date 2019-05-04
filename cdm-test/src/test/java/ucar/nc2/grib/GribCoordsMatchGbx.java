/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import javax.annotation.Nullable;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.collection.*;
import ucar.nc2.grib.coord.TimeCoordIntvDateValue;
import ucar.nc2.grib.grib1.*;
import ucar.nc2.grib.grib1.tables.Grib1Customizer;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.grib.grib2.Grib2Pds;
import ucar.nc2.grib.grib2.Grib2Record;
import ucar.nc2.grib.grib2.table.Grib2Tables;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Misc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

/**
 * Check that coordinate values match Grib Records.
 * Using GridDataset.
 * Using just the gbx
 */
public class GribCoordsMatchGbx {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static FeatureCollectionConfig config = new FeatureCollectionConfig(); // default values
  private static final String KIND_GRID = "grid";
  private static final String KIND_COVERAGE = "coverage";
  private static final int MAX_READS = -1;
  private static final boolean showMissing = false;

  // The maximum relative difference for floating-point comparisons.
  private static final double maxRelDiff = 1e-6;

  public static Counters getCounters() {
    Counters countersAll = new Counters();
    countersAll.add(KIND_GRID);
    countersAll.add(KIND_COVERAGE);
    return countersAll;
  }

  private String filename;
  ucar.nc2.util.Counters counters;

  private boolean isGrib1;
  private ArrayDouble.D3 timeCoord2DBoundsArray;

  public GribCoordsMatchGbx(String filename, ucar.nc2.util.Counters counters) {
    this.filename = filename;
    this.counters = counters;
  }

  private CalendarDate[] makeDateBounds(SubsetParams coords, CalendarDate runtime) {
    double[] time_bounds = coords.getTimeOffsetIntv();
    CalendarDateUnit dateUnit = (CalendarDateUnit) coords.get(SubsetParams.timeOffsetUnit);
    CalendarDate[] result = new CalendarDate[2];
    result[0] = runtime.add(CalendarPeriod.of((int) time_bounds[0], dateUnit.getCalendarField()));
    result[1] = runtime.add(CalendarPeriod.of((int) time_bounds[1], dateUnit.getCalendarField()));
    return result;
  }

  boolean nearlyEquals(CalendarDate date1, CalendarDate date2) {
    return Math.abs(date1.getMillis() - date2.getMillis()) < 5000; // 5 secs
  }

  ///////////////////////////////////////////////////////////
  // coverage
  String kind;

  public int readCoverageDataset() throws IOException {
    kind = KIND_COVERAGE;
    int countFailures = 0;

    try (FeatureDatasetCoverage fdc = CoverageDatasetFactory.open(filename)) {
      Attribute att = fdc.findGlobalAttributeIgnoreCase("file_format");
      isGrib1 = att.getStringValue().equalsIgnoreCase("GRIB-1");

      for (CoverageCollection cc : fdc.getCoverageCollections()) {
        for (Coverage cover : cc.getCoverages()) {
          if (readCoverage(cover))
            counters.count(kind, "success");
          else {
            counters.count(kind, "fail");
            countFailures++;
          }
        }
      }
    }
    return countFailures;
  }

  private boolean readCoverage(Coverage coverage) throws IOException {
    // if (!coverage.getName().startsWith("Total_pre")) return true;
    if (showMissing) logger.debug("coverage {}", coverage.getName());
    this.cover = coverage;
    countReadsForVariable = 0;

    CoverageCoordSys ccs = coverage.getCoordSys();
    List<CoverageCoordAxis> subsetAxes = new ArrayList<>();

    for (CoverageCoordAxis axis : ccs.getAxes()) {
      switch (axis.getAxisType()) { // LOOK what about 2D ??
        case RunTime:
        case Time:
        case TimeOffset:
        case GeoZ:
        case Height:
        case Pressure:
        case Ensemble:
          subsetAxes.add(axis);
      }
    }

    CoordsSet coordIter = CoordsSet.factory(ccs.isConstantForecast(), subsetAxes);
    try {
      Iterator<SubsetParams> iter = coordIter.iterator();
      while (iter.hasNext()) {
        SubsetParams coords = iter.next();
        readCoverageData(coverage, coords);
      }
    } catch (AssertionError e) {
      e.printStackTrace();
      return false;

    } catch (Throwable t) {
      t.printStackTrace();
      return false;
    }
    return true;
  }

  Coverage cover;
  private void readCoverageData(Coverage cover, SubsetParams coords) throws IOException, InvalidRangeException {
    countReadsForVariable++;
    if (MAX_READS > 0 && countReadsForVariable > MAX_READS) return;

    GribDataReader.currentDataRecord = null;
    GeoReferencedArray geoArray = cover.readData(coords);
    int[] shape = geoArray.getData().getShape();

    if (shape[1] != 1) {
      cover.readData(coords);
    }

    for (int i = 0; i < shape.length - 2; i++) {
      Assert.assertEquals(Misc.showInts(shape), 1, shape[i]);
    }

    if (isGrib1)
      readAndTestGrib1(cover.getName(), coords);
    else
      readAndTestGrib2(cover.getName(), coords);

    this.cover = null;
  }

  ////////////////////////////////////////////////////////////
  // GridDataset
  private GridCoordSystem gdc;
  private GridDatatype grid;
  private SubsetParams dtCoords;

  public int readGridDataset() throws IOException {
    kind = KIND_GRID;

    int countFailures = 0;
    try (GridDataset gds = GridDataset.open(filename)) {
      NetcdfFile ncfile = gds.getNetcdfFile();
      IOServiceProvider iosp = ncfile.getIosp();
      isGrib1 = iosp instanceof Grib1Iosp;

      for (GridDatatype gdt : gds.getGrids()) {
        if (read(gdt))
          counters.count(kind, "success");
        else {
          counters.count(kind, "fail");
          countFailures++;
        }
      }
    }
    return countFailures;
  }

  int countReadsForVariable;
  private boolean read(GridDatatype gdt) throws IOException {
    // if (!gdt.getName().startsWith("Total_pre")) return true;
    if (showMissing) logger.debug("grid {}", gdt.getName());
    countReadsForVariable = 0;
    gdc = gdt.getCoordinateSystem();
    grid = gdt;
    dtCoords = new SubsetParams();
    Dimension rtDim = gdt.getRunTimeDimension();
    Dimension tDim = gdt.getTimeDimension();
    Dimension zDim = gdt.getZDimension();

    try {
      // loop over runtime
      if (rtDim != null) {
        CoordinateAxis1DTime rtcoord = gdc.getRunTimeAxis();

        for (int rt = 0; rt < rtDim.getLength(); rt++) {
          dtCoords.setRunTime(rtcoord.getCalendarDate(rt));
          readTime(gdt, rt, tDim, zDim);
        }

      } else {
        readTime(gdt, -1, tDim, zDim);
      }
      timeCoord2DBoundsArray = null;

    } catch (AssertionError e) {
      e.printStackTrace();
      return false;

    } catch (Throwable t) {
      t.printStackTrace();
      return false;
    }

    return true;
  }

  private void readTime(GridDatatype gdt, int rtIndex, Dimension timeDim, Dimension zDim) throws IOException {
    boolean isTimeInterval;
    if (timeDim != null) {
      CoordinateAxis tcoord = gdc.getTimeAxis();
      isTimeInterval = tcoord.isInterval();

      if (rtIndex < 0) {
        CoordinateAxis1DTime tcoord1D = (CoordinateAxis1DTime) tcoord;

        for (int t = 0; t < timeDim.getLength(); t++) {
          if (isTimeInterval) {
            dtCoords.set("timeDateIntv", tcoord1D.getCoordBoundsDate(t));
            dtCoords.setTimeOffsetIntv(tcoord1D.getCoordBounds(t));
          } else {
            dtCoords.setTime(tcoord1D.getCalendarDate(t));

          }
          readVert(gdt, rtIndex, t, zDim);
        }
      } else {
        CoordinateAxis2D tcoord2D = (CoordinateAxis2D) tcoord;
        CoordinateAxisTimeHelper helper = tcoord2D.getCoordinateAxisTimeHelper();
        if (timeCoord2DBoundsArray == null)
          timeCoord2DBoundsArray = tcoord2D.getCoordBoundsArray();
        for (int t = 0; t < timeDim.getLength(); t++) {
          if (isTimeInterval) {
            double[] timeBounds = new double[2];
            timeBounds[0] = timeCoord2DBoundsArray.get(rtIndex, t, 0);
            timeBounds[1] = timeCoord2DBoundsArray.get(rtIndex, t, 1);
            CalendarDate[] timeBoundsDate = new CalendarDate[2];
            timeBoundsDate[0] = helper.makeCalendarDateFromOffset(timeBounds[0]);
            timeBoundsDate[1] = helper.makeCalendarDateFromOffset(timeBounds[1]);
            dtCoords.setTimeOffsetIntv(timeBounds);
            dtCoords.set("timeDateIntv", timeBoundsDate);
          } else {
            double timeCoord = tcoord2D.getCoordValue(rtIndex, t);
            CalendarDate timeCoordDate = helper.makeCalendarDateFromOffset(timeCoord);
            dtCoords.setTime(timeCoordDate);
          }
          readVert(gdt, rtIndex, t, zDim);
        }

      }

    } else {
      readVert(gdt, rtIndex, -1, zDim);
    }
  }

  private void readVert(GridDatatype gdt, int rtIndex, int tIndex, Dimension zDim) throws IOException {
    if (zDim != null) {
      CoordinateAxis1D zcoord = gdc.getVerticalAxis();
      boolean isLayer = zcoord.isInterval();
      for (int z = 0; z < zDim.getLength(); z++) {
        if (isLayer) {
          dtCoords.setVertCoordIntv(zcoord.getCoordBounds(z));
        } else {
          dtCoords.setVertCoord(zcoord.getCoordValue(z));
        }
        readAndTestGrib(gdt, rtIndex, tIndex, z);
      }
    } else {
      readAndTestGrib(gdt, rtIndex, tIndex, -1);
    }
  }

  private void readAndTestGrib(GridDatatype gdt, int rtIndex, int tIndex, int zIndex) throws IOException {
    countReadsForVariable++;
    if (MAX_READS > 0 && countReadsForVariable > MAX_READS) return;

    GribDataReader.currentDataRecord = null;
    gdt.readDataSlice(rtIndex, -1, tIndex, zIndex, -1, -1);
    dtCoords.set("rtIndex", rtIndex);
    dtCoords.set("tIndex", tIndex);
    dtCoords.set("zIndex", zIndex);

    if (isGrib1)
      readAndTestGrib1(gdt.getName(), dtCoords);
    else
      readAndTestGrib2(gdt.getName(), dtCoords);

    grid = null;
  }

  ////////////////////////////////////////////////////////
  // Grib1

  private Map<String, IdxHashGrib1> fileIndexMapGrib1 = new HashMap<>();

  private class IdxHashGrib1 {
    private Map<Long, Grib1Record> map = new HashMap<>();

    IdxHashGrib1(String idxFile) throws IOException {
      Grib1Index index = new Grib1Index();
      index.readIndex(idxFile, -1, CollectionUpdateType.never);
      for (Grib1Record gr : index.getRecords()) {
        Grib1SectionIndicator is = gr.getIs();
        map.put(is.getStartPos(), gr); // start of entire message
      }
    }

    Grib1Record get(long pos) {
      return map.get(pos);
    }
  }

  private void readAndTestGrib1(String name, SubsetParams coords) throws IOException {
    GribCollectionImmutable.Record dr = GribDataReader.currentDataRecord;
    if (dr == null) {
      if (showMissing) logger.debug("missing record= {}", coords);
      counters.count(kind, "missing1");
      return;
    }
    if (showMissing) logger.debug("found record= {}", coords);
    counters.count(kind, "found1");

    String filename = GribDataReader.currentDataRafFilename;
    String idxFile = filename.endsWith(".gbx9") ? filename : filename + ".gbx9";
    IdxHashGrib1 idxHash = fileIndexMapGrib1.get(idxFile);
    if (idxHash == null) {
      idxHash = new IdxHashGrib1(idxFile);
      fileIndexMapGrib1.put(idxFile, idxHash);
    }

    Grib1Record gr1 = idxHash.get(dr.pos);
    Grib1SectionProductDefinition pdss = gr1.getPDSsection();
    Grib1Customizer cust1 = Grib1Customizer.factory(gr1, null);

    // check runtime
    CalendarDate rt_val = coords.getRunTime();
    boolean runtimeOk = true;
    if (rt_val != null) {
      CalendarDate gribDate = pdss.getReferenceDate();
      runtimeOk &= rt_val.equals(gribDate);
      if (!runtimeOk) {
        tryAgain(coords);
        logger.debug("{} {} failed on runtime {} != gbx {}", kind, name, rt_val, gribDate);
      }
      Assert.assertEquals(gribDate, rt_val);
    }

    // check time
    CalendarDate time_val = coords.getTime();
    if (time_val == null)
      time_val = (CalendarDate) coords.get(SubsetParams.timeOffsetDate);
    Grib1ParamTime ptime = gr1.getParamTime(cust1);
    if (ptime.isInterval()) {
      CalendarDate[] gbxInv = getForecastInterval(pdss, ptime);
      CalendarDate[] date_bounds = (CalendarDate[]) coords.get("timeDateIntv");
      if (date_bounds == null) {
        date_bounds = makeDateBounds(coords, rt_val);
      }
      if (!date_bounds[0].equals(gbxInv[0]) || !date_bounds[1].equals(gbxInv[1])) {
        tryAgain(coords);
        logger.debug("{} {} failed on time intv: coord=[{},{}] gbx =[{},{}]", kind, name, date_bounds[0],
                date_bounds[1], gbxInv[0], gbxInv[1]);
      }
      Assert.assertArrayEquals(date_bounds, gbxInv);

    } else if (time_val != null) {
      CalendarDate gbxDate = getForecastDate(pdss, ptime);
      if (!time_val.equals(gbxDate)) {
        tryAgain(coords);
        logger.debug("{} {} failed on time: coord={} gbx = {}", kind, name, time_val, gbxDate);
      }
      Assert.assertEquals(time_val, gbxDate);
    }

    // check vert
    boolean vertOk = true;
    Double vert_val = coords.getVertCoord();
    Grib1ParamLevel plevel = cust1.getParamLevel(gr1.getPDSsection());
    if (cust1.isLayer(plevel.getLevelType())) {
      double[] edge = coords.getVertCoordIntv();
      if (edge != null) {
       // double low = Math.min(edge[0], edge[1]);
        //double hi = Math.max(edge[0], edge[1]);
        vertOk &= Misc.nearlyEquals(edge[0], plevel.getValue1(), maxRelDiff);
        vertOk &= Misc.nearlyEquals(edge[1], plevel.getValue2(), maxRelDiff);
        if (!vertOk) {
          tryAgain(coords);
          logger.debug("{} {} failed on vert [{},{}] != [{},{}]", kind, name, edge[0], edge[1],
                  plevel.getValue1(), plevel.getValue2());
        }
      }
    } else if (vert_val != null) {
      vertOk &= Misc.nearlyEquals(vert_val,  plevel.getValue1(), maxRelDiff);
      if (!vertOk) {
        tryAgain(coords);
        logger.debug("{} {} failed on vert {} != {}", kind, name, vert_val,  plevel.getValue1());
      }
    }
  }

  public CalendarDate getReferenceDate(Grib1SectionProductDefinition pds) {
    return pds.getReferenceDate();
  }

  public CalendarDate getForecastDate(Grib1SectionProductDefinition pds, Grib1ParamTime ptime) {
    CalendarPeriod period = GribUtils.getCalendarPeriod(pds.getTimeUnit());
    CalendarDateUnit unit = CalendarDateUnit.of(null, period.getField(), pds.getReferenceDate());
    int timeCoord = ptime.getForecastTime();
    return unit.makeCalendarDate(period.getValue() * timeCoord);
  }

  public CalendarDate[] getForecastInterval(Grib1SectionProductDefinition pds, Grib1ParamTime ptime) {
    CalendarPeriod period = GribUtils.getCalendarPeriod(pds.getTimeUnit());
    CalendarDateUnit unit = CalendarDateUnit.of(null, period.getField(), pds.getReferenceDate());
    int[] intv = ptime.getInterval();
    return new CalendarDate[]{
            unit.makeCalendarDate(period.getValue() * intv[0]),
            unit.makeCalendarDate(period.getValue() * intv[1])};
  }

  //////////////////////////////////////////////////////////////
  // Grib 2

  private Map<String, IdxHashGrib2> fileIndexMapGrib2 = new HashMap<>();

  private class IdxHashGrib2 {
    private Map<Long, Grib2Record> map = new HashMap<>();

    IdxHashGrib2(String idxFile) throws IOException {
      Grib2Index index = new Grib2Index();
      if (!index.readIndex(idxFile, -1, CollectionUpdateType.never)) {
        logger.debug("idxFile does not exist {}", idxFile);
        throw new FileNotFoundException();
      }
      for (Grib2Record gr : index.getRecords()) {
        long startPos = gr.getIs().getStartPos();
        map.put(startPos, gr); // start of entire message
      }
    }

    Grib2Record get(long pos) {
      return map.get(pos);
    }
  }

  private void readAndTestGrib2(String name, SubsetParams coords) throws IOException {
    GribCollectionImmutable.Record dr = GribDataReader.currentDataRecord;
    String filename = GribDataReader.currentDataRafFilename;
    if (dr == null) {
      counters.count(kind, "missing2");
      return;
    }
    counters.count(kind, "found2");

    String idxFile = filename.endsWith(".gbx9") ? filename : filename + ".gbx9";
    IdxHashGrib2 idxHash = fileIndexMapGrib2.get(idxFile);
    if (idxHash == null) {
      idxHash = new IdxHashGrib2(idxFile);
      fileIndexMapGrib2.put(idxFile, idxHash);
    }

    Grib2Record grib2 = idxHash.get(dr.pos);
    Grib2Tables cust = Grib2Tables.factory(grib2);

    Grib2RecordBean bean = new Grib2RecordBean(cust, grib2);
    boolean paramOk = true;

    //paramOk &= var_desc.equals(bean.getName());
    //paramOk &= var_param.equals(bean.getParamNo());
    //paramOk &= var_level_type.equals(bean.getLevelName());

    CalendarDate rt_val = coords.getRunTime();
    boolean runtimeOk = true;
    if (rt_val != null) {
      CalendarDate gribDate = bean.getRefDate();
      runtimeOk &= rt_val.equals(gribDate);
      if (!runtimeOk) {
        tryAgain(coords);
        logger.debug("{} {} failed on runtime {} != {}", kind, name, rt_val, gribDate);
      }
    }

    CalendarDate time_val = coords.getTime();
    if (time_val == null) {
      time_val = (CalendarDate) coords.get(SubsetParams.timeOffsetDate);
    }

    boolean timeOk = true;
    if (bean.isTimeInterval()) {
      TimeCoordIntvDateValue dateFromGribRecord = bean.getTimeIntervalDates();
      CalendarDate[] date_bounds = (CalendarDate[]) coords.get("timeDateIntv");
      if (date_bounds == null) {
        date_bounds = makeDateBounds(coords, rt_val);
      }
      if (date_bounds != null) {
        timeOk &= nearlyEquals(date_bounds[0], dateFromGribRecord.getStart());
        timeOk &= nearlyEquals(date_bounds[1], dateFromGribRecord.getEnd());
      }
      if (!timeOk) {
        tryAgain(coords);
        logger.debug("{} {} failed on timeIntv [{},{}] != {}", kind, name, date_bounds[0], date_bounds[1],
                bean.getTimeCoord());
      }

    } else if (time_val != null) {
      // timeOk &= timeCoord == bean.getTimeCoordValue();   // true if GC
      CalendarDate dateFromGribRecord = bean.getForecastDate();
      timeOk &= nearlyEquals(time_val, dateFromGribRecord);
      if (!timeOk) {
        tryAgain(coords);
        logger.debug("{} {} failed on time {} != {}", kind, name, time_val, bean.getTimeCoord());
      }
    }

    Double vert_val = coords.getVertCoord();
    double[] edge = coords.getVertCoordIntv();
    boolean vertOk = true;
    if (edge != null) {
      vertOk &= bean.isLayer();
      double low = Math.min(edge[0], edge[1]);
      double hi = Math.max(edge[0], edge[1]);
      vertOk &= Misc.nearlyEquals(low, bean.getLevelLowValue(), maxRelDiff);
      vertOk &= Misc.nearlyEquals(hi, bean.getLevelHighValue(), maxRelDiff);
      if (!vertOk) {
        tryAgain(coords);
        logger.debug("{} {} failed on vert [{},{}] != [{},{}]", kind, name, low, hi, bean.getLevelLowValue(),
                bean.getLevelHighValue());
      }
    } else if (vert_val != null) {
      vertOk &= Misc.nearlyEquals(vert_val, bean.getLevelValue1(), maxRelDiff);
      if (!vertOk) {
        tryAgain(coords);
        logger.debug("{} {} failed on vert {} != {}", kind, name, vert_val, bean.getLevelValue1());
      }
    }

    boolean ok = paramOk && runtimeOk && timeOk && vertOk;
    Assert.assertTrue(ok);
  }

  void tryAgain(SubsetParams coords) {
    try {
      if (cover != null)
        cover.readData(coords);

      if (grid != null) {
        int rtIndex = (Integer) dtCoords.get("rtIndex");
        int tIndex = (Integer) dtCoords.get("tIndex");
        int zIndex = (Integer) dtCoords.get("zIndex");
        grid.readDataSlice(rtIndex, -1, tIndex, zIndex, -1, -1);
      }

    } catch (InvalidRangeException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public class Grib2RecordBean {
    Grib2Tables cust;
    Grib2Record gr;
    Grib2Pds pds;
    int discipline;

    public Grib2RecordBean() {
    }

    public Grib2RecordBean(Grib2Tables cust, Grib2Record gr) throws IOException {
      this.cust = cust;
      this.gr = gr;
      this.pds = gr.getPDS();
      discipline = gr.getDiscipline();
    }

    public String getParamNo() {
      return discipline + "-" + pds.getParameterCategory() + "-" + pds.getParameterNumber();
    }

    public String getName() {
      return cust.getVariableName(gr);
    }

    public String getLevelName() {
      return cust.getLevelName(pds.getLevelType1());
    }

    public final CalendarDate getRefDate() {
      return gr.getReferenceDate();
    }

    public boolean isLayer() {
      return cust.isLayer(pds);
    }

    public final CalendarDate getForecastDate() {
      return cust.getForecastDate(gr);
    }

    public String getTimeCoord() {
      if (isTimeInterval())
        return getTimeIntervalDates().toString();
      else
        return getForecastDate().toString();
    }

    public final String getTimeUnit() {
      int unit = pds.getTimeUnit();
      return cust.getCodeTableValue("4.4", unit);
    }

    public final int getForecastTime() {
      return pds.getForecastTime();
    }

    public String getLevel() {
      int v1 = pds.getLevelType1();
      int v2 = pds.getLevelType2();
      if (v1 == 255) return "";
      if (v2 == 255) return "" + pds.getLevelValue1();
      if (v1 != v2) return pds.getLevelValue1() + "-" + pds.getLevelValue2() + " level2 type= " + v2;
      return pds.getLevelValue1() + "-" + pds.getLevelValue2();
    }

    public double getLevelValue1() {
      return pds.getLevelValue1();
    }

    public double getLevelLowValue() {
      return Math.min(pds.getLevelValue1(), pds.getLevelValue2());
    }

    public double getLevelHighValue() {
      return Math.max(pds.getLevelValue1(), pds.getLevelValue2());
    }

    /////////////////////////////////////////////////////////////
    /// time intervals

    public boolean isTimeInterval() {
      return pds instanceof Grib2Pds.PdsInterval;
    }

    @Nullable
    public TimeCoordIntvDateValue getTimeIntervalDates() {
      if (cust != null && isTimeInterval()) {
        return cust.getForecastTimeInterval(gr);
      }
      return null;
    }

    @Override
    public String toString() {
      final Formatter sb = new Formatter();
      sb.format("Record dataStart=%s%n", gr.getDataSection().getStartingPosition());
      sb.format(" %s (%s)%n", getName(), getParamNo());
      sb.format(" reftime=%s%n", getRefDate());
      sb.format(" time=%s%n", getTimeCoord());
      sb.format(" level=%s type=%s (%d)%n", getLevel(), getLevelName(), pds.getLevelType1());
      return sb.toString();
    }

    ///////////////////////////////
    // Ensembles
    public int getPertN() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getPerturbationNumber();
      if (v == GribNumbers.UNDEFINED) v = -1;
      return v;
    }

    public int getNForecastsInEns() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getNumberEnsembleForecasts();
      if (v == GribNumbers.UNDEFINED) v = -1;
      return v;
    }

    public int getPertType() {
      Grib2Pds.PdsEnsemble pdsi = (Grib2Pds.PdsEnsemble) pds;
      int v = pdsi.getPerturbationType();
      return (v == GribNumbers.UNDEFINED) ? -1 : v;
    }

    /////////////////////////////////
    // Probability

    public String getProbLimits() {
      Grib2Pds.PdsProbability pdsi = (Grib2Pds.PdsProbability) pds;
      double v = pdsi.getProbabilityLowerLimit();
      if (v == GribNumbers.UNDEFINEDD) return "";
      else return pdsi.getProbabilityLowerLimit() + "-" + pdsi.getProbabilityUpperLimit();
    }

  }

}
