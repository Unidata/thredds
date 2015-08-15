package ucar.nc2.grib.coverage;

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.coord.*;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.*;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribDataReader;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.time.*;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.*;

/**
 * Create a CoverageDataset from a GribCollection file
 *
 * @author John
 * @since 8/1/2015
 */
@Immutable
public class GribCoverageDataset implements CoverageReader, CoordAxisReader {
  static private final Logger logger = LoggerFactory.getLogger(GribCoverageDataset.class);

  static public CoverageCollection open(String endpoint) throws IOException {
    GribCollectionImmutable gc;

    // try to fail fast
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(endpoint, "r");
      GribCdmIndex.GribCollectionType type = GribCdmIndex.getType(raf);
      boolean isIndexFile = (type != GribCdmIndex.GribCollectionType.none);
      if (isIndexFile) {
        gc = GribCdmIndex.openGribCollectionFromIndexFile(raf, new FeatureCollectionConfig(), logger);  // LOOK no config !
      } else {
        gc = GribCdmIndex.openGribCollectionFromRaf(raf, new FeatureCollectionConfig(), CollectionUpdateType.nocheck, logger);  // LOOK no config !
      }

      if (gc == null) {
        raf.close();
        return null;
      }

    } catch (IOException ioe) {
      if (raf != null) raf.close();
      return null;
    }

    List<CoverageDataset> datasets = new ArrayList<>();
    for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
      for (GribCollectionImmutable.GroupGC group : ds.getGroups()) {
        GribCoverageDataset gribCov = new GribCoverageDataset(gc, ds, group);
        datasets.add(gribCov.makeCoverageDataset());
      }
    }
    return new CoverageCollection(gc, datasets);
  }

  //////////////////////////////////////////////////////////////////

  private final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.Dataset ds;
  private final GribCollectionImmutable.GroupGC group;
  private final CoverageCoordSys.Type coverageType;
  private final boolean isLatLon;
  private final String xaxisName, yaxisName;

  public GribCoverageDataset(GribCollectionImmutable gribCollection, GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group) {
    this.gribCollection = gribCollection;
    this.ds = (ds != null) ? ds : gribCollection.getDataset(0);
    this.group = (group != null) ? group : this.ds.getGroup(0);

    // figure out coverageType
    coverageType = (this.ds.getType() == GribCollectionImmutable.Type.TwoD) ? CoverageCoordSys.Type.Fmrc : CoverageCoordSys.Type.Grid;

    isLatLon = this.group.getGdsHorizCoordSys().proj instanceof LatLonProjection;
    if (isLatLon) {
      yaxisName = CF.LATITUDE;
      xaxisName = CF.LONGITUDE;
    } else {
      yaxisName = "y";
      xaxisName = "x";
    }
  }

  /*
    SRC,               // GC: Single Runtime Collection                [ntimes]
    MRC,               // GC: Multiple Runtime Collection              [nruns, ntimes]
    MRSTC,             // GC: Multiple Runtime Single Time Collection  [nruns, 1]
    TP,                // PC: Multiple Runtime Single Time Partition   [nruns, 1]
    TwoD,              // PC: TwoD time partition                      [nruns, ntimes]
    Best,              // PC: Best time partition                      [ntimes]
      -- must generate aux runtime
   */

  public CoverageDataset makeCoverageDataset() {
    String name = gribCollection.getName() + "#" + ds.getType();
    if (ds.getGroupsSize() > 1)
      name += "-" + group.getId();

    AttributeContainerHelper gatts = new AttributeContainerHelper(name);
    gatts.addAll(gribCollection.getGlobalAttributes());

    // make horiz transform if needed
    List<CoverageTransform> transforms = new ArrayList<>();
    if (!isLatLon) {
      AttributeContainerHelper projAtts = new AttributeContainerHelper(group.horizCoordSys.getId());
      for (Parameter p : group.getGdsHorizCoordSys().proj.getProjectionParameters())
        projAtts.addAttribute(new Attribute(p));
      CoverageTransform projTransform = new CoverageTransform(group.horizCoordSys.getId(), projAtts, true);
      transforms.add(projTransform);
    }

    List<CoverageCoordAxis> axes = new ArrayList<>();
    axes.addAll(makeHorizCoordinates());

    for (Coordinate axis : group.getCoordinates()) {
      if (axis.getType() == Coordinate.Type.runtime)
        addRuntimeCoordAxis((CoordinateRuntime) axis);
      else if (axis.getType() == Coordinate.Type.time2D)
        addTime2DCoordAxis((CoordinateTime2D) axis);
      else if (axis.getType() == Coordinate.Type.time)
        axes.add(makeCoordAxis((CoordinateTime) axis));
      else if (axis.getType() == Coordinate.Type.timeIntv)
        axes.add(makeCoordAxis((CoordinateTimeIntv) axis));
      else if (axis.getType() == Coordinate.Type.vert)
        axes.add(makeCoordAxis((CoordinateVert) axis));
      else if (axis.getType() == Coordinate.Type.ens)
        axes.add(makeCoordAxis((CoordinateEns) axis));
    }
    makeRuntimeCoordAxes(axes);
    makeTime2DCoordAxis(axes);

    Map<String, CoverageCoordSys> coordSysSet = new HashMap<>();
    for (GribCollectionImmutable.VariableIndex v : group.getVariables()) {
      CoverageCoordSys sys = makeCoordSys(v, transforms);
      coordSysSet.put(sys.getName(), sys);                    // duplicates get eliminated here
    }
    List<CoverageCoordSys> coordSys = new ArrayList<>(coordSysSet.values());

    // all vars are coverages
    List<Coverage> pgrids = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex v : group.getVariables()) {
      pgrids.add(makeCoverage(v));
    }

    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();
    return new CoverageDataset(name, coverageType, gatts, hcs.getLatLonBB(), hcs.getProjectionBB(), getCalendarDateRange(),
            coordSys, transforms, axes, pgrids, this);
  }

  CalendarDateRange dateRange;
  private void trackDateRange(CalendarDateRange cdr) {
    if (dateRange == null) dateRange = cdr;
    else dateRange = dateRange.extend(cdr);
  }

  CalendarDateRange getCalendarDateRange() {
    return dateRange;
  }

  private List<CoverageCoordAxis> makeHorizCoordinates() {
    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();

    List<CoverageCoordAxis> result = new ArrayList<>(2);
    if (isLatLon) {
      AttributeContainerHelper atts = new AttributeContainerHelper(CF.LATITUDE);
      atts.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
      result.add(new CoverageCoordAxis1D(CF.LATITUDE, CDM.LAT_UNITS, null, DataType.FLOAT, AxisType.Lat, atts, CoverageCoordAxis.DependenceType.independent,
              new ArrayList<>(0), CoverageCoordAxis.Spacing.regular, hcs.ny, hcs.getStartY(), hcs.getEndY(), hcs.dy, null, this, false));

      atts = new AttributeContainerHelper(CF.LONGITUDE);
      atts.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      result.add(new CoverageCoordAxis1D(CF.LONGITUDE, CDM.LON_UNITS, null, DataType.FLOAT, AxisType.Lon, atts, CoverageCoordAxis.DependenceType.independent,
              new ArrayList<>(0), CoverageCoordAxis.Spacing.regular, hcs.nx, hcs.getStartX(), hcs.getEndX(), hcs.dx, null, this, false));

    } else {
      AttributeContainerHelper atts = new AttributeContainerHelper("y");
      atts.addAttribute(new Attribute(CDM.UNITS, "km"));
      result.add(new CoverageCoordAxis1D("y", "km", CF.PROJECTION_Y_COORDINATE, DataType.FLOAT, AxisType.GeoY, atts, CoverageCoordAxis.DependenceType.independent,
              new ArrayList<>(0), CoverageCoordAxis.Spacing.regular, hcs.ny, hcs.getStartY(), hcs.getEndY(), hcs.dy, null, this, false));

      atts = new AttributeContainerHelper("x");
      atts.addAttribute(new Attribute(CDM.UNITS, "km"));
      result.add(new CoverageCoordAxis1D("x", "km", CF.PROJECTION_X_COORDINATE, DataType.FLOAT, AxisType.GeoX, atts, CoverageCoordAxis.DependenceType.independent,
              new ArrayList<>(0), CoverageCoordAxis.Spacing.regular, hcs.nx, hcs.getStartX(), hcs.getEndX(), hcs.dx, null, this, false));
    }

    return result;
  }

  ////////////////////////////////

  private void addRuntimeCoordAxis(CoordinateRuntime runtime) {
    String units = runtime.getPeriodName()+" since "+gribCollection.getMasterFirstDate().toString();

    double offsetFromMaster = runtime.getOffsetInTimeUnits(gribCollection.getMasterFirstDate());
    List<Double> offsets = runtime.getOffsetsInTimeUnits();
    int n = offsets.size();
    double start = offsets.get(0) + offsetFromMaster;
    double end = offsets.get(n - 1) + offsetFromMaster;
    double resol2 = (n > 1) ? (end - start) / (n - 1) : 0.0;
    Counters counters = runtime.calcDistributions();
    Comparable resolMode = counters.get("resol").getMode();
    double resol = (resolMode == null) ? 0.0 : ((Number) resolMode).doubleValue();

    // CoordinateRuntime master = gribCollection.getMasterRuntime();
    //int n = master.getSize();
    boolean isScalar = (n == 1);      // this is the case of runtime[1]
    CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;
    CoverageCoordAxis.Spacing spacing = Misc.closeEnough(resol, resol2, percentTolerence) ? CoverageCoordAxis.Spacing.regular : CoverageCoordAxis.Spacing.irregularPoint;

    AttributeContainerHelper atts = new AttributeContainerHelper(runtime.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, units));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    // LOOK too many runtimes to enumerate ?
    CoverageCoordAxis1D result = new CoverageCoordAxis1D(runtime.getName(), units, "GRIB reference time", DataType.DOUBLE, AxisType.RunTime, atts,
            dependence, new ArrayList<>(0), spacing, n, start, end, resol, null, this, false);

    // smoosh runtimes with similar start, end, resol
    RuntimeSmoosher tester = new RuntimeSmoosher(result);
    int index = alreadyHaveAtIndex(tester);
    if (index < 0) {
      runtimes.add(tester);
    } else {
      RuntimeSmoosher already = runtimes.get(index);
      RuntimeSmoosher combined = already.combine(result);
      runtimes.set(index, combined);        // replace with combined one
      substCoords.put(runtime.getName(), combined.runtime.getName()); // track substitutes
      System.out.printf("runtime smooch %s into %s%n", runtime.getName(), combined.runtime.getName());
    }
  }

  private int alreadyHaveAtIndex( RuntimeSmoosher tester) {
    for (int i=0; i<runtimes.size(); i++)
      if (runtimes.get(i).closeEnough(tester)) return i;
    return -1;
  }

  private static final double percentTolerence = .10;
  private Map<String, String> substCoords = new HashMap<>();
  private List<RuntimeSmoosher> runtimes = new ArrayList<>();
  private class RuntimeSmoosher {
    final CoverageCoordAxis1D runtime;
    final double start, end, resol;
    final int npts;
    final boolean combined;

    RuntimeSmoosher(CoverageCoordAxis1D runtime, double start, double end, double resol, int npts) {
      this.runtime = runtime;
      this.start = start;
      this.end = end;
      this.resol = resol;
      this.npts = npts;
      this.combined = true;
    }

    RuntimeSmoosher(CoverageCoordAxis1D runtime) {
      this.runtime = runtime;
      this.start = runtime.getStartValue();
      this.end = runtime.getEndValue();
      this.resol = runtime.getResolution();
      this.npts = runtime.getNcoords();
      this.combined = false;
    }

    RuntimeSmoosher combine(CoverageCoordAxis1D runtime) {
      double start = Math.min(this.start, runtime.getStartValue());
      double end = Math.max(this.end, runtime.getEndValue());
      int npts = Math.max(this.npts, runtime.getNcoords());
      return new RuntimeSmoosher(this.runtime, start, end, this.resol, npts);
    }

    public boolean closeEnough(RuntimeSmoosher that) {
      double total = (end - start);
      double totalOther = (that.end - that.start);
      if (!Misc.closeEnough(totalOther, total,  percentTolerence)) return false;

      double startP = Math.abs(start-that.start)/total;
      if (startP > percentTolerence) return false;

      double endP = Math.abs(end-that.end)/total;
      if (endP > percentTolerence) return false;

      double nptsP = Math.abs(npts-that.npts)/(double)npts;
      if (nptsP > percentTolerence) return false;

      return (Double.compare(that.resol, resol) == 0);
    }
  }

  private void makeRuntimeCoordAxes( List<CoverageCoordAxis> axes) {
    for (RuntimeSmoosher smoosh : runtimes)  {
      if (!smoosh.combined) axes.add(smoosh.runtime);
      else {
        int n = (int) ((smoosh.end - smoosh.start) / smoosh.resol);
        CoverageCoordAxis1D combined = new CoverageCoordAxis1D(smoosh.runtime.getName(), smoosh.runtime.getUnits(), "GRIB reference time", DataType.DOUBLE, AxisType.RunTime,
                new AttributeContainerHelper(smoosh.runtime.getName(), smoosh.runtime.getAttributes()),
                smoosh.runtime.getDependenceType(), new ArrayList<>(0), CoverageCoordAxis.Spacing.regular, n, smoosh.start, smoosh.end, smoosh.resol, null, this, false);
        axes.add(combined);
      }
    }
  }

  //////////////////////////////////////////////////////////

  private class Time2DSmoosher {
    CoordinateTime2D time2D;
    List<? extends Object> offsets;

    public Time2DSmoosher(CoordinateTime2D time2D) {
      this.time2D = time2D;
      this.offsets = time2D.getOffsetsSorted();
      // double offsetFromMaster = time2D.getOffsetInTimeUnits(gribCollection.getMasterFirstDate());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Time2DSmoosher that = (Time2DSmoosher) o;
      return offsets.equals(that.offsets);
    }

    @Override
    public int hashCode() {
      return offsets.hashCode();
    }
  }

  private Map<Time2DSmoosher, Time2DSmoosher> time2Dmap = new HashMap<>();
  private void addTime2DCoordAxis(CoordinateTime2D time2D) {
    Time2DSmoosher tester = new Time2DSmoosher( time2D);
    Time2DSmoosher already = time2Dmap.get(tester);
    if (already == null)
      time2Dmap.put(tester, tester);
    else
      substCoords.put(time2D.getName(), already.time2D.getName());
  }

  private void makeTime2DCoordAxis(List<CoverageCoordAxis> axes) {
    for (Time2DSmoosher smoosh : time2Dmap.keySet()) {
      CoordinateTime2D time2D = smoosh.time2D;

      List<? extends Object> offsets = time2D.getOffsetsSorted();
      int n = offsets.size();
      Counters counters = time2D.calcDistributions();
      Comparable resolMode = counters.get("resol").getMode();
      double resol = (resolMode == null) ? 0.0 : ((Number) resolMode).doubleValue();

      double start, end, resol2;
      double[] values = null;
      CoverageCoordAxis.Spacing spacing;

      if (time2D.isTimeInterval()) {
        start = ((TimeCoord.Tinv) offsets.get(0)).getBounds1();
        end = ((TimeCoord.Tinv) offsets.get(n - 1)).getBounds2();

        boolean isContiguous = true;
        for (int i = 0; i < n - 1; i++) {
          TimeCoord.Tinv tinv = (TimeCoord.Tinv) offsets.get(i);
          TimeCoord.Tinv tinv2 = (TimeCoord.Tinv) offsets.get(i + 1);
          if (!Misc.closeEnough(tinv.getBounds2(), tinv2.getBounds1()))
            isContiguous = false;
        }

        spacing = isContiguous ? CoverageCoordAxis.Spacing.contiguousInterval : CoverageCoordAxis.Spacing.discontiguousInterval;

        if (isContiguous) {
          values = new double[n + 1];
          int count = 0;
          for (Object offset : offsets)
            values[count++] = ((TimeCoord.Tinv) offset).getBounds1();
          values[count] = ((TimeCoord.Tinv) offsets.get(n - 1)).getBounds2();

        } else {
          values = new double[2 * n];
          int count = 0;
          for (Object offset : offsets) {
            TimeCoord.Tinv tinv = (TimeCoord.Tinv) offset;
            values[count++] = tinv.getBounds1();
            values[count++] = tinv.getBounds2();
          }
        }

      } else {
        start = (Integer) offsets.get(0);
        end = (Integer) offsets.get(n - 1);
        resol2 = (n > 1) ? (end - start) / (n - 1) : 0.0;

        spacing = (n == 1) || Misc.closeEnough(resol, resol2) ? CoverageCoordAxis.Spacing.regular : CoverageCoordAxis.Spacing.irregularPoint;

        if (spacing == CoverageCoordAxis.Spacing.irregularPoint) {
          values = new double[n];
          int count = 0;
          for (Object offset : offsets) {
            Integer tinv = (Integer) offset;
            values[count++] = tinv;
          }
        }
      }

      //boolean isScalar = (n == 1);  // LOOK
      //CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;

      AttributeContainerHelper atts = new AttributeContainerHelper(time2D.getName());
      atts.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
      atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
      atts.addAttribute(new Attribute(CDM.LONG_NAME, GribIosp.GRIB_VALID_TIME));
      atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));
      atts.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));

      String reftimeName = time2D.getRuntimeCoordinate().getName();
      String subst = substCoords.get(reftimeName);
      if (subst != null) reftimeName = subst;

      axes.add(new TimeOffsetAxis(time2D.getName(), time2D.getUnit(), GribIosp.GRIB_VALID_TIME, DataType.DOUBLE, AxisType.TimeOffset, atts,
              CoverageCoordAxis.DependenceType.independent, new ArrayList<>(0), spacing, n, start, end, resol, values, this, false, reftimeName));
    }
  }

  //////////////////////////////////////////////////////////

  private CoverageCoordAxis makeCoordAxis(CoordinateTime time) {
    trackDateRange(time.makeCalendarDateRange(ucar.nc2.time.Calendar.proleptic_gregorian));
    List<Integer> offsets = time.getOffsetSorted();
    int n = offsets.size();
    double start = offsets.get(0);
    double end = offsets.get(n - 1);
    double resol2 = (n > 1) ? (end - start) / (n - 1) : 0.0;
    Counters counters = time.calcDistributions();
    Comparable resolMode = counters.get("resol").getMode();
    double resol = (resolMode == null) ? 0.0 : ((Number) resolMode).doubleValue();

    // CoordinateRuntime master = gribCollection.getMasterRuntime();
    //int n = master.getSize();
    //boolean isScalar = (n == 1);      // this is the case of runtime[1]
    //CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;
    CoverageCoordAxis.Spacing spacing = Misc.closeEnough(resol, resol2) ? CoverageCoordAxis.Spacing.regular : CoverageCoordAxis.Spacing.irregularPoint;

    AttributeContainerHelper atts = new AttributeContainerHelper(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, GribIosp.GRIB_VALID_TIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    // LOOK too many times to enumerate ?
    return new CoverageCoordAxis1D(time.getName(), time.getTimeUdUnit(), GribIosp.GRIB_VALID_TIME, DataType.DOUBLE, AxisType.Time, atts,
            CoverageCoordAxis.DependenceType.independent, new ArrayList<>(0), spacing, n, start, end, resol, null, this, false);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateTimeIntv time) {
    trackDateRange(time.makeCalendarDateRange(ucar.nc2.time.Calendar.proleptic_gregorian));
    List<TimeCoord.Tinv> offsets = time.getTimeIntervals();
    int n = offsets.size();
    double start = offsets.get(0).getBounds1();
    double end = offsets.get(n - 1).getBounds2();
    Counters counters = time.calcDistributions();
    Comparable resolMode = counters.get("resol").getMode();
    double resol = (resolMode == null) ? 0.0 : ((Number) resolMode).doubleValue();

    boolean isContiguous = true;
    for (int i=0; i<n-1;i++) {
      if (!Misc.closeEnough(offsets.get(1).getBounds2(), offsets.get(i+1).getBounds1()))
        isContiguous = false;
    }
    CoverageCoordAxis.Spacing spacing = isContiguous ? CoverageCoordAxis.Spacing.contiguousInterval : CoverageCoordAxis.Spacing.discontiguousInterval;

    //boolean isScalar = (n == 1);
    //CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;

    AttributeContainerHelper atts = new AttributeContainerHelper(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, GribIosp.GRIB_VALID_TIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    // LOOK too many times to enumerate ?
    return new CoverageCoordAxis1D(time.getName(), time.getTimeUdUnit(), GribIosp.GRIB_VALID_TIME, DataType.DOUBLE, AxisType.Time, atts,
            CoverageCoordAxis.DependenceType.independent, new ArrayList<>(0), spacing, n, start, end, resol, null, this, false);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateVert vertCoord) {
    List<VertCoord.Level> levels = vertCoord.getLevelSorted();

    int n = vertCoord.getSize();
    double[] values;
    CoverageCoordAxis.Spacing spacing;

    if (vertCoord.isLayer()) {

      boolean isContiguous = true;
      for (int i=0; i<n-1;i++) {
        if (!Misc.closeEnough(levels.get(i).getValue2(), levels.get(i + 1).getValue1()))
          isContiguous = false;
      }

      spacing = isContiguous ? CoverageCoordAxis.Spacing.contiguousInterval : CoverageCoordAxis.Spacing.discontiguousInterval;

      if (isContiguous) {
        values = new double[n+1];
        int count = 0;
        for (int i = 0; i < n; i++)
          values[count++] = levels.get(i).getValue1();
        values[count] = levels.get(n-1).getValue2();

      } else {
        int count = 0;
        values = new double[2 * n];
        for (int i = 0; i < n; i++) {
          values[count++] = levels.get(i).getValue1();
          values[count++] = levels.get(i).getValue2();
        }
      }

    } else {

      values = new double[n];
      for (int i = 0; i < n; i++)
        values[i] = levels.get(i).getValue1();
      spacing = (n == 1) ? CoverageCoordAxis.Spacing.regular : CoverageCoordAxis.Spacing.irregularPoint;
    }

    AttributeContainerHelper atts = new AttributeContainerHelper(vertCoord.getName());
    String units = vertCoord.getUnit();
    atts.addAttribute(new Attribute(CDM.UNITS, units));
    AxisType axisType = AxisType.GeoZ;
    if (SimpleUnit.isCompatible("mbar", units))
      axisType = AxisType.Pressure;
    else if (SimpleUnit.isCompatible("m", units))
      axisType = AxisType.Height;

    String desc = vertCoord.getVertUnit().getDesc();
    if (desc != null) atts.addAttribute(new Attribute(CDM.LONG_NAME, desc));
    atts.addAttribute(new Attribute(CF.POSITIVE, vertCoord.isPositiveUp() ? CF.POSITIVE_UP : CF.POSITIVE_DOWN));

    return new CoverageCoordAxis1D(vertCoord.getName(), vertCoord.getUnit(), null, DataType.DOUBLE, axisType, atts,
            CoverageCoordAxis.DependenceType.independent, new ArrayList<>(0), spacing, n, values[0], values[values.length - 1], 0.0, values, this, false);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateEns ensCoord) {
    int n = ensCoord.getSize();
    // LOOK likely to be regular
    double[] values = new double[n];
    for (int i = 0; i < n; i++)
      values[i] = ((EnsCoord.Coord) ensCoord.getValue(i)).getEnsMember();

    AttributeContainerHelper atts = new AttributeContainerHelper(ensCoord.getName());
    String units = ensCoord.getUnit();
    atts.addAttribute(new Attribute(CDM.UNITS, units));

    return new CoverageCoordAxis1D(ensCoord.getName(), units, null, DataType.DOUBLE, AxisType.Ensemble, atts,
            CoverageCoordAxis.DependenceType.independent, new ArrayList<>(0), CoverageCoordAxis.Spacing.irregularPoint, ensCoord.getSize(),
            values[0], values[n - 1], 0.0, values, this, false);
  }

  private CoverageCoordSys makeCoordSys(GribCollectionImmutable.VariableIndex gribVar, List<CoverageTransform> transforms) {
    List<Coordinate> axes = makeCoordSysAxes(gribVar);

    List<String> axisNames = new ArrayList<>();
    for (Coordinate axis : axes)
      axisNames.add(axis.getName());
    axisNames.add(yaxisName);
    axisNames.add(xaxisName);

    List<String> transformNames = new ArrayList<>();
    for (CoverageTransform ct : transforms)
      transformNames.add(ct.getName());

    return new CoverageCoordSys(makeCoordSysName(axes), axisNames, transformNames, coverageType);
  }

  private List<Coordinate> makeCoordSysAxes(GribCollectionImmutable.VariableIndex gribVar) {
    List<Coordinate> axes = new ArrayList<>();
    for (Coordinate axis : gribVar.getCoordinates()) {
      String subst = substCoords.get(axis.getName());
      if (subst != null)
        axis = gribVar.findCoordinate(subst);
      axes.add(axis);
    }
    Collections.sort(axes, (o1, o2) -> o1.getType().order - o2.getType().order);
    return axes;
  }

  private String makeCoordSysName(List<Coordinate> axes) {
    Formatter fname = new Formatter();
    for (Coordinate axis : axes)
      fname.format(" %s", axis.getName());
    return fname.toString();
  }

  private Coverage makeCoverage(GribCollectionImmutable.VariableIndex gribVar) {
    String coordSysName = makeCoordSysName(makeCoordSysAxes(gribVar));

    AttributeContainerHelper atts = new AttributeContainerHelper(gribVar.makeVariableName());
    atts.addAttribute(new Attribute(CDM.LONG_NAME, gribVar.makeVariableDescription()));
    atts.addAttribute(new Attribute(CDM.UNITS, gribVar.makeVariableUnits()));

    GribTables.Parameter gp = gribVar.getGribParameter();
    if (gp != null) {
      if (gp.getDescription() != null)
        atts.addAttribute(new Attribute(CDM.DESCRIPTION, gp.getDescription()));
      if (gp.getAbbrev() != null)
        atts.addAttribute(new Attribute(CDM.ABBREV, gp.getAbbrev()));
      atts.addAttribute(new Attribute(CDM.MISSING_VALUE, gp.getMissing()));
      if (gp.getFill() != null)
        atts.addAttribute(new Attribute(CDM.FILL_VALUE, gp.getFill()));
    } else {
      atts.addAttribute(new Attribute(CDM.MISSING_VALUE, Float.NaN));
    }

    // statistical interval type
    if (gribVar.getIntvType() >= 0) {
      GribStatType statType = gribVar.getStatType();
      if (statType != null) {
        atts.addAttribute(new Attribute("Grib_Statistical_Interval_Type", statType.toString()));
        CF.CellMethods cm = GribStatType.getCFCellMethod(statType);
        Coordinate timeCoord = gribVar.getCoordinate(Coordinate.Type.timeIntv);
        if (cm != null && timeCoord != null)
          atts.addAttribute(new Attribute(CF.CELL_METHODS, timeCoord.getName() + ": " + cm.toString()));
      } else {
        atts.addAttribute(new Attribute("Grib_Statistical_Interval_Type", gribVar.getIntvType()));
      }
    }

    return new Coverage(gribVar.makeVariableName(), DataType.FLOAT, atts.getAttributes(), coordSysName, gribVar.makeVariableUnits(),
            gribVar.makeVariableDescription(), this, gribVar);
  }

  //////////////////////////////////////////////////////

  @Override
  public GeoReferencedArray readData(Coverage coverage, SubsetParams params, boolean canonicalOrder) throws IOException, InvalidRangeException {
    GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) coverage.getUserObject();
    CoverageCoordSys orgCoordSys = coverage.getCoordSys();
    CoverageCoordSys subsetCoordSys = orgCoordSys.subset(params); // LOOK, can this be done without knowing more from the Grib coords? eg missing ??

    List<Coordinate> gribCoords = vindex.getCoordinates();
    int[] gribFullShape = new int[gribCoords.size()]; // the full shape of the sparseArray

    List<CoverageCoordAxis> covCoordSubset = new ArrayList<>(gribCoords.size()+2);
    List<Range> gribSubset = new ArrayList<>(gribCoords.size()+2);

    int count = 0;
    for (Coordinate gribCoord : gribCoords) {  // must be in the order of the grib coordinates
      gribFullShape[count++] = gribCoord.getNCoords();

      switch (gribCoord.getType()) {
        case runtime:
          covCoordSubset.add( subsetCoordSys.getAxis(AxisType.RunTime));
          gribSubset.add( subsetRuntime( (CoordinateRuntime) gribCoord, (CoverageCoordAxis1D) subsetCoordSys.getAxis(AxisType.RunTime)));
          break;

        case time2D:
          CoverageCoordAxis1D runAxis = (CoverageCoordAxis1D) subsetCoordSys.getAxis(AxisType.RunTime);
          CoverageCoordAxis1D toAxis = (CoverageCoordAxis1D) subsetCoordSys.getAxis(AxisType.TimeOffset);
          covCoordSubset.add( toAxis);
          gribSubset.add( subsetTimeOffset((CoordinateTime2D) gribCoord, runAxis, toAxis));
          break;

        case vert:
          CoverageCoordAxis1D vertAxis = (CoverageCoordAxis1D) subsetCoordSys.getZAxis();
          covCoordSubset.add(vertAxis);
          gribSubset.add( subset(gribCoord, vertAxis));
          break;

        case time:
        case timeIntv:
        case ens:
          CoverageCoordAxis1D covAxis = (CoverageCoordAxis1D) subsetCoordSys.getAxis(gribCoord.getType().axisType);
          covCoordSubset.add(covAxis);
          gribSubset.add( subset(gribCoord, covAxis));
          break;
      }
    }

    covCoordSubset.add( subsetCoordSys.getYAxis());
    gribSubset.add( subsetCoordSys.getYAxis().getRange());

    covCoordSubset.add( subsetCoordSys.getXAxis());
    gribSubset.add( subsetCoordSys.getXAxis().getRange());

    Section gribSection = new Section(gribSubset);
    System.out.printf("GribCoverageDataset.readData section=%n%s%n", gribSection.show());

    GribDataReader dataReader = GribDataReader.factory(gribCollection, vindex);
    Array data =  dataReader.readData(gribSection, gribFullShape);

    return new GeoReferencedArray(coverage.getName(), coverage.getDataType(), data, covCoordSubset);
  }

  Range subsetRuntime(CoordinateRuntime gribCoord, CoverageCoordAxis1D covAxisSubset) throws InvalidRangeException {
    if (!covAxisSubset.isSubset())
      return new Range(gribCoord.getSize()); // get all of it

    List<Integer> idxIntoSA = new ArrayList<>(covAxisSubset.getNcoords());
    for (int i=0; i<covAxisSubset.getNcoords(); i++) {
      double val = covAxisSubset.getCoord(i);
      CalendarDate cdval = covAxisSubset.makeDate(val);
      int idx =  gribCoord.getIndex(cdval.getMillis());  // must be exact
      idxIntoSA.add(idx);
    }

    if (idxIntoSA.size() == 1) {
      int wantIdx = idxIntoSA.get(0);
      return new Range(gribCoord.getName(), wantIdx, wantIdx);
    } else {
      int[] vals = new int[idxIntoSA.size()];
      for (int i=0; i<idxIntoSA.size(); i++) vals[i] = idxIntoSA.get(i);
      return new RangeScatter(gribCoord.getName(), vals);
    }
  }

  // this only allows for the case of rectangular (runtime, offset) or (runtime=1, time) queries.
  // (runtime, offset=1) = constant offset
  // (runtime=1, time) = constant runtime

  // CANT DO YET (HOW - switch timeOffset to CoverageCoordAxis1D time)  (NO: dataReader.readData() only does rectangle. need new API)
  // (runtime > 1, time=1)  = constant forecast   (must cut down the runtime based on the time request - only those that have it.
  // (runtime > 1, time > 1).

  Range subsetTimeOffset(CoordinateTime2D gribCoord, CoverageCoordAxis1D runAxis, CoverageCoordAxis1D toAxis) throws InvalidRangeException {

    double rundateVal = runAxis.getCoord(0);
    CalendarDate rundate = runAxis.makeDate(rundateVal);
    CoordinateRuntime gribRuntime = gribCoord.getRuntimeCoordinate();
    int runIdx =  gribRuntime.getIndex(rundate.getMillis());  // must be exact

    List<Integer> idxIntoSA = new ArrayList<>(toAxis.getNcoords());
    for (int i=0; i<toAxis.getNcoords(); i++) {
      double val = toAxis.getCoord(i);
      int idx = gribCoord.findIndexContaining(runIdx, val, rundate);
      idxIntoSA.add(idx);
    }

    if (idxIntoSA.size() == 1) {
      int wantIdx = idxIntoSA.get(0);
      return new Range(gribCoord.getName(), wantIdx, wantIdx);
    } else {
      int[] vals = new int[idxIntoSA.size()];
      for (int i=0; i<idxIntoSA.size(); i++) vals[i] = idxIntoSA.get(i);
      return new RangeScatter(gribCoord.getName(), vals);
    }
  }

  Range subset(Coordinate gribCoord, CoverageCoordAxis1D covAxisSubset) throws InvalidRangeException {
    if (!covAxisSubset.isSubset())
      return new Range(gribCoord.getSize()); // get all of it

    List<Integer> idxIntoSA = new ArrayList<>(covAxisSubset.getNcoords());
    for (int i=0; i<covAxisSubset.getNcoords(); i++) {
      double need = covAxisSubset.getCoord(i);
      int idx =  gribCoord.findIndexContaining(need);
      idxIntoSA.add(idx);
    }

    if (idxIntoSA.size() == 1) {
      int wantIdx = idxIntoSA.get(0);
      return new Range(gribCoord.getName(), wantIdx, wantIdx);
    } else {
      int[] vals = new int[idxIntoSA.size()];
      for (int i=0; i<idxIntoSA.size(); i++) vals[i] = idxIntoSA.get(i);
      return new RangeScatter(gribCoord.getName(), vals);
    }  }

  @Override
  public void close() throws Exception {
    gribCollection.close();
  }

  @Override
  public double[] readValues(CoverageCoordAxis coordAxis) throws IOException {
    return new double[0];
  }
}
