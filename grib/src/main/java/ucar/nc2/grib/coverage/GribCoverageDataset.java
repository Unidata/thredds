package ucar.nc2.grib.coverage;

import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.coord.*;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.EnsCoord;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
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
 // private static final String RUNTIME_NAME = "runtime";
  //private static final String TIME_NAME = "time";

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
    coverageType = CoverageCoordSys.Type.Grid; // LOOK wrong

    isLatLon = this.group.getGdsHorizCoordSys().proj instanceof LatLonProjection;
    if (isLatLon) {
      yaxisName = CF.LATITUDE;
      xaxisName = CF.LONGITUDE;
    } else {
      yaxisName = "y";
      xaxisName = "x";
    }
  }

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

    // tricky business with coordinates.
    // same time, runtime and horiz coords for all
    List<CoverageCoordAxis> axes = new ArrayList<>();
    axes.addAll(makeHorizCoordinates());
    for (Coordinate axis : group.getCoordinates()) {
      if (axis.getType() == Coordinate.Type.runtime)
        axes.add(makeCoordAxis((CoordinateRuntime) axis));
      else if (axis.getType() == Coordinate.Type.time2D)
        axes.add(makeCoordAxis((CoordinateTime2D) axis));
      else if (axis.getType() == Coordinate.Type.time)
        axes.add(makeCoordAxis((CoordinateTime) axis));
      else if (axis.getType() == Coordinate.Type.timeIntv)
        axes.add(makeCoordAxis((CoordinateTimeIntv) axis));
      else if (axis.getType() == Coordinate.Type.vert)
        axes.add(makeCoordAxis((CoordinateVert) axis));
      else if (axis.getType() == Coordinate.Type.ens)
        axes.add(makeCoordAxis((CoordinateEns) axis));
    }

    // only the vert and ens coords generate seperate coordSys
    Map<String, CoverageCoordSys> coordSysSet = new HashMap<>();
    for (GribCollectionImmutable.VariableIndex v : group.getVariables()) {
      CoverageCoordSys sys = makeCoordSys(v, transforms);
      coordSysSet.put(sys.getName(), sys);
    }
    List<CoverageCoordSys> coordSys = new ArrayList<>(coordSysSet.values());

    // all vars are coverages
    List<Coverage> pgrids = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex v : group.getVariables()) {
      pgrids.add(makeCoverage(v));
    }

    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();
    return new CoverageDataset(name, coverageType, gatts,
            hcs.getLatLonBB(), hcs.getProjectionBB(), getCalendarDateRange(),
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
      List<Attribute> atts = new ArrayList<>(2);
      atts.add(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
      result.add(new CoverageCoordAxis1D(CF.LATITUDE, CDM.LAT_UNITS, null, DataType.FLOAT, AxisType.Lat, atts, CoverageCoordAxis.DependenceType.independent,
              new ArrayList<>(0), CoverageCoordAxis.Spacing.regular, hcs.ny, hcs.getStartY(), hcs.getEndY(), hcs.dy, null, this));

      atts = new ArrayList<>(2);
      atts.add(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      result.add(new CoverageCoordAxis1D(CF.LONGITUDE, CDM.LON_UNITS, null, DataType.FLOAT, AxisType.Lon, atts, CoverageCoordAxis.DependenceType.independent,
              new ArrayList<>(0), CoverageCoordAxis.Spacing.regular, hcs.nx, hcs.getStartX(), hcs.getEndX(), hcs.dx, null, this));

    } else {
      List<Attribute> atts = new ArrayList<>(2);
      atts.add(new Attribute(CDM.UNITS, "km"));
      result.add(new CoverageCoordAxis1D("y", "km", CF.PROJECTION_Y_COORDINATE, DataType.FLOAT, AxisType.GeoY, atts, CoverageCoordAxis.DependenceType.independent,
              new ArrayList<>(0), CoverageCoordAxis.Spacing.regular, hcs.ny, hcs.getStartY(), hcs.getEndY(), hcs.dy, null, this));

      atts = new ArrayList<>(2);
      atts.add(new Attribute(CDM.UNITS, "km"));
      result.add(new CoverageCoordAxis1D("x", "km", CF.PROJECTION_X_COORDINATE, DataType.FLOAT, AxisType.GeoX, atts, CoverageCoordAxis.DependenceType.independent,
              new ArrayList<>(0), CoverageCoordAxis.Spacing.regular, hcs.nx, hcs.getStartX(), hcs.getEndX(), hcs.dx, null, this));
    }

    return result;
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateRuntime runtime) {
    List<Double> offsets = runtime.getOffsetsInTimeUnits();
    int n = offsets.size();
    double start = offsets.get(0);
    double end = offsets.get(n - 1);
    double resol2 = (n > 1) ? (end - start) / (n - 1) : 0.0;
    Counters counters = runtime.calcDistributions();
    Comparable resolMode = counters.get("resol").getMode();
    double resol = (resolMode == null) ? 0.0 : ((Number) resolMode).doubleValue();

    // CoordinateRuntime master = gribCollection.getMasterRuntime();
    //int n = master.getSize();
    boolean isScalar = (n == 1);      // this is the case of runtime[1]
    CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;
    CoverageCoordAxis.Spacing spacing = Misc.closeEnough(resol, resol2) ? CoverageCoordAxis.Spacing.regular : CoverageCoordAxis.Spacing.irregularPoint;

    AttributeContainerHelper atts = new AttributeContainerHelper(runtime.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, runtime.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    // LOOK too many runtimes to enumerate ?
    return new CoverageCoordAxis1D(runtime.getName(), runtime.getUnit(), "GRIB reference time", DataType.DOUBLE, AxisType.RunTime, atts.getAttributes(),
            dependence, new ArrayList<>(0), spacing, n, start, end, resol, null, this);
  }

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
    boolean isScalar = (n == 1);      // this is the case of runtime[1]
    CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;
    CoverageCoordAxis.Spacing spacing = Misc.closeEnough(resol, resol2) ? CoverageCoordAxis.Spacing.regular : CoverageCoordAxis.Spacing.irregularPoint;

    AttributeContainerHelper atts = new AttributeContainerHelper(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, GribIosp.GRIB_VALID_TIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    // LOOK too many times to enumerate ?
    return new CoverageCoordAxis1D(time.getName(), time.getTimeUdUnit(), GribIosp.GRIB_VALID_TIME, DataType.DOUBLE, AxisType.Time, atts.getAttributes(),
            dependence, new ArrayList<>(0), spacing, n, start, end, resol, null, this);
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

    boolean isScalar = (n == 1);
    CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;

    AttributeContainerHelper atts = new AttributeContainerHelper(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, GribIosp.GRIB_VALID_TIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    // LOOK too many times to enumerate ?
    return new CoverageCoordAxis1D(time.getName(), time.getTimeUdUnit(), GribIosp.GRIB_VALID_TIME, DataType.DOUBLE, AxisType.Time, atts.getAttributes(),
            dependence, new ArrayList<>(0), spacing, n, start, end, resol, null, this);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateTime2D time2D) {
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
      for (int i=0; i<n-1;i++) {
        TimeCoord.Tinv tinv = (TimeCoord.Tinv) offsets.get(i);
        TimeCoord.Tinv tinv2 = (TimeCoord.Tinv) offsets.get(i+1);
        if (!Misc.closeEnough(tinv.getBounds2(), tinv2.getBounds1()))
          isContiguous = false;
      }

      spacing = isContiguous ? CoverageCoordAxis.Spacing.contiguousInterval : CoverageCoordAxis.Spacing.discontiguousInterval;

      if (isContiguous) {
        values = new double[n+1];
        int count = 0;
        for (Object offset : offsets)
          values[count++] = ((TimeCoord.Tinv) offset).getBounds1();
        values[count] = ((TimeCoord.Tinv) offsets.get(n-1)).getBounds2();

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

    boolean isScalar = (n == 1);  // LOOK
    CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;

    AttributeContainerHelper atts = new AttributeContainerHelper(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, GribIosp.GRIB_VALID_TIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    return new CoverageCoordAxis1D(time2D.getName(), time2D.getTimeUdUnit(), GribIosp.GRIB_VALID_TIME, DataType.DOUBLE, AxisType.TimeOffset, atts.getAttributes(),
            dependence, new ArrayList<>(0), spacing, n, start, end, resol, values, this);
  }

  /*

  private CoordinateTime timeUnion;
  private CoordinateTimeIntv timeIntvUnion;

  private List<CoverageCoordAxis> makeTimeCoordinates() {
    List<CoverageCoordAxis> result = new ArrayList<>(2);

    // make the union of all the time values
    CoordinateTime.Builder2 timeBuilder = null;
    CoordinateTimeIntv.Builder2 timeIntvBuilder = null;
    for (Coordinate axis : group.getCoordinates()) {
      if (axis.getType() == Coordinate.Type.time) {
        if (timeBuilder == null)
          timeBuilder = new CoordinateTime.Builder2((CoordinateTime) axis); // LOOK reference time correct ??
        timeBuilder.addAll(axis);

      } else if (axis.getType() == Coordinate.Type.timeIntv) {
        if (timeIntvBuilder == null)
          timeIntvBuilder = new CoordinateTimeIntv.Builder2((CoordinateTimeIntv) axis); // LOOK reference time correct ??
        timeIntvBuilder.addAll(axis);
      }
    }
    if (timeBuilder != null) timeUnion = (CoordinateTime) timeBuilder.finish();
    if (timeIntvBuilder != null) timeIntvUnion = (CoordinateTimeIntv) timeIntvBuilder.finish();

    if (timeUnion != null) {
      int n = timeUnion.getSize();
      boolean isScalar = (n == 1);  // LOOK
      CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;

      AttributeContainerHelper atts = new AttributeContainerHelper(TIME_NAME);
      atts.addAttribute(new Attribute(CDM.UNITS, timeUnion.getUnit()));
      atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
      atts.addAttribute(new Attribute(CDM.LONG_NAME, GribIosp.GRIB_VALID_TIME));
      atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

      Integer first = (Integer) timeUnion.getValue(0);
      Integer last = (Integer) timeUnion.getValue(n - 1);

      String units = timeUnion.getUnit() + " since " + timeUnion.getRefDate();

      // too many times to enumerate ?
      result.add(new CoverageCoordAxis1D(TIME_NAME, units, GribIosp.GRIB_VALID_TIME, DataType.LONG, AxisType.Time, atts.getAttributes(),
              dependence, new ArrayList<>(0), CoverageCoordAxis.Spacing.irregularPoint, n, first, last, 0.0, null, this));
    }

    if (timeIntvUnion != null) {
      int n = timeIntvUnion.getSize();
      boolean isScalar = (n == 1);      // LOOK
      CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;

      AttributeContainerHelper atts = new AttributeContainerHelper(TIME_NAME);
      atts.addAttribute(new Attribute(CDM.UNITS, timeIntvUnion.getUnit()));
      atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
      atts.addAttribute(new Attribute(CDM.LONG_NAME, GribIosp.GRIB_VALID_TIME));
      atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

      TimeCoord.Tinv first = (TimeCoord.Tinv) timeIntvUnion.getValue(0);
      TimeCoord.Tinv last = (TimeCoord.Tinv) timeIntvUnion.getValue(n - 1);

      String units = timeIntvUnion.getUnit() + " since " + timeIntvUnion.getRefDate();

      // too many times to enumerate ?
      result.add(new CoverageCoordAxis1D(TIME_NAME, units, GribIosp.GRIB_VALID_TIME, DataType.LONG, AxisType.Time, atts.getAttributes(),
              dependence, new ArrayList<>(0), CoverageCoordAxis.Spacing.discontiguousInterval, n, first.getBounds1(), last.getBounds2(), 0.0, null, this));
    }

    return result;
  }   */

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

    return new CoverageCoordAxis1D(vertCoord.getName(), vertCoord.getUnit(), null, DataType.DOUBLE, axisType, atts.getAttributes(),
            CoverageCoordAxis.DependenceType.independent, new ArrayList<>(0), spacing, n, values[0], values[values.length - 1], 0.0, values, this);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateEns ensCoord) {
    int n = ensCoord.getSize();
    // LOOK likely to be regular
    double[] values = new double[n];
    for (int i = 0; i < n; i++)
      values[i] = ((EnsCoord.Coord) ensCoord.getValue(i)).getEnsMember();

    return new CoverageCoordAxis1D(ensCoord.getName(), ensCoord.getUnit(), null, DataType.DOUBLE, AxisType.Ensemble, new ArrayList<>(0),
            CoverageCoordAxis.DependenceType.independent, new ArrayList<>(0), CoverageCoordAxis.Spacing.irregularPoint, ensCoord.getSize(),
            values[0], values[n - 1], 0.0, values, this);
  }

  private CoverageCoordSys makeCoordSys(GribCollectionImmutable.VariableIndex gribVar, List<CoverageTransform> transforms) {
    List<Coordinate> axes = new ArrayList<>();
    for (Coordinate axis : gribVar.getCoordinates())
      // if (axis.getType() == Coordinate.Type.vert || axis.getType() == Coordinate.Type.ens)
        axes.add(axis);
    Collections.sort(axes, (o1, o2) -> o1.getType().ordinal() - o2.getType().ordinal());

    Formatter fname = new Formatter();
    // fname.format("%s %s", RUNTIME_NAME, TIME_NAME);
    for (Coordinate axis : axes)
      fname.format(" %s", axis.getName());

    List<String> axisNames = new ArrayList<>();
    //axisNames.add(RUNTIME_NAME);
    //axisNames.add(TIME_NAME);
    for (Coordinate axis : axes)
      axisNames.add(axis.getName());
    axisNames.add(yaxisName);
    axisNames.add(xaxisName);

    List<String> transformNames = new ArrayList<>();
    for (CoverageTransform ct : transforms)
      transformNames.add(ct.getName());

    return new CoverageCoordSys(fname.toString(), axisNames, transformNames, coverageType);
  }

  private String makeCoordSysName(GribCollectionImmutable.VariableIndex gribVar) {
    List<Coordinate> axes = new ArrayList<>();
    for (Coordinate axis : gribVar.getCoordinates())
      // if (axis.getType() == Coordinate.Type.vert || axis.getType() == Coordinate.Type.ens)
        axes.add(axis);
    Collections.sort(axes, (o1, o2) -> o1.getType().ordinal() - o2.getType().ordinal());

    Formatter fname = new Formatter();
    // fname.format("%s %s", RUNTIME_NAME, TIME_NAME);
    for (Coordinate axis : axes)
      fname.format(" %s", axis.getName());

    return fname.toString();
  }

  private Coverage makeCoverage(GribCollectionImmutable.VariableIndex gribVar) {
    String coordSysName = makeCoordSysName(gribVar);
    // LOOK attributes not filled out
    return new Coverage(gribVar.makeVariableName(), DataType.FLOAT, new ArrayList<>(), coordSysName, gribVar.makeVariableUnits(), gribVar.makeVariableDescription(), this);
  }

  //////////////////////////////////////////////////////

  @Override
  public GeoReferencedArray readData(Coverage coverage, SubsetParams subset, boolean canonicalOrder) throws IOException, InvalidRangeException {
    return null;
  }

  @Override
  public void close() throws Exception {
    gribCollection.close();
  }

  @Override
  public double[] readValues(CoverageCoordAxis coordAxis) throws IOException {
    return new double[0];
  }
}
