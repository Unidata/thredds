/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.grib.coverage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.CollectionUpdateType;
import ucar.coord.Coordinate;
import ucar.coord.CoordinateEns;
import ucar.coord.CoordinateRuntime;
import ucar.coord.CoordinateTime;
import ucar.coord.CoordinateTime2D;
import ucar.coord.CoordinateTimeAbstract;
import ucar.coord.CoordinateTimeIntv;
import ucar.coord.CoordinateVert;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeIterator;
import ucar.ma2.SectionIterable;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.EnsCoord;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grib.TimeCoord;
import ucar.nc2.grib.VertCoord;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribDataReader;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.grib2.Grib2Utils;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.SimpleUnit;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Parameter;

/**
 * Create a FeatureDatasetCoverage from a GribCollection file.
 * Called from InvDatasetFcGrib and (by reflection) from CoverageDatasetFactory
 * LOOK might want to switch to Builder pattern
 *
 * @author John
 * @since 8/1/2015
 */
@Immutable
public class GribCoverageDataset implements CoverageReader, CoordAxisReader {
  static private final Logger logger = LoggerFactory.getLogger(GribCoverageDataset.class);

  static public Optional<FeatureDatasetCoverage> open(String endpoint) throws IOException {
    GribCollectionImmutable gc;

    if (endpoint.startsWith("file:"))
      endpoint = endpoint.substring("file:".length());

    // try to fail fast
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(endpoint, "r");
      gc = GribCdmIndex.openGribCollectionFromRaf(raf, new FeatureCollectionConfig(), CollectionUpdateType.nocheck, logger);

      if (gc == null) {
        raf.close();
        return Optional.empty(CoverageDatasetFactory.NOT_GRIB_FILE);
      }

    } catch (IOException ioe) {
      if (raf != null) raf.close();
      throw ioe;
    }

    try {
      List<CoverageCollection> datasets = new ArrayList<>();
      for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
        for (GribCollectionImmutable.GroupGC group : ds.getGroups()) {
          GribCoverageDataset gribCov = new GribCoverageDataset(gc, ds, group);
          datasets.add(gribCov.createCoverageCollection());
        }
      }
      FeatureDatasetCoverage result = new FeatureDatasetCoverage(endpoint, gc.getGlobalAttributes(), gc, datasets);
      return Optional.of(result);

    } catch (Throwable t) {
      logger.error("GribCoverageDataset.open failed", t);
      return Optional.empty(t.getMessage());
    }
  }

  //////////////////////////////////////////////////////////////////

  private final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.Dataset ds;
  private final GribCollectionImmutable.GroupGC group;
  private final FeatureType coverageType;
  private final boolean isGrib1, isLatLon, isCurvilinearOrthogonal;

  public GribCoverageDataset(GribCollectionImmutable gribCollection, GribCollectionImmutable.Dataset ds, GribCollectionImmutable.GroupGC group) {
    this.gribCollection = gribCollection;
    this.ds = (ds != null) ? ds : gribCollection.getDataset(0);
    this.group = (group != null) ? group : this.ds.getGroup(0);
    this.isGrib1 = gribCollection.isGrib1;

    GdsHorizCoordSys hcs = this.group.getGdsHorizCoordSys();
    this.isLatLon = hcs.isLatLon(); // isGrib1 ? hcs.isLatLon() : Grib2Utils.isLatLon(hcs.template, gribCollection.getCenter());
    this.isCurvilinearOrthogonal = !isGrib1 && Grib2Utils.isCurvilinearOrthogonal(hcs.template, gribCollection.getCenter());

    // figure out coverageType
    FeatureType ct;
    switch (this.ds.getType()) {
      case MRC:
        //case MRSTC:
        //case MRSTP:
      case TwoD:
        ct = FeatureType.FMRC;
        break;
      default:
        ct = FeatureType.GRID;
    }
    if (isCurvilinearOrthogonal)
      ct = FeatureType.CURVILINEAR;
    this.coverageType = ct;
  }

  @Override
  public void close() throws IOException {
    gribCollection.close();
  }

  @Override
  public String getLocation() {
    return gribCollection.getLocation() + "#" + group.getId(); // ??
  }

  public CoverageCollection createCoverageCollection() {
    String name = gribCollection.getName() + "#" + ds.getType();
    if (ds.getGroupsSize() > 1)
      name += "-" + group.getId();

    AttributeContainerHelper gatts = gribCollection.getGlobalAttributes();

    // make horiz transform if needed
    List<CoverageTransform> transforms = new ArrayList<>();
    if (!isLatLon) {
      AttributeContainerHelper projAtts = new AttributeContainerHelper(group.horizCoordSys.getId());
      for (Parameter p : group.getGdsHorizCoordSys().proj.getProjectionParameters())
        projAtts.addAttribute(new Attribute(p));
      CoverageTransform projTransform = new CoverageTransform(group.horizCoordSys.getId(), projAtts, true);
      transforms.add(projTransform);
    }

    // potential variables - need to remove any 2D LatLon
    List<GribCollectionImmutable.VariableIndex> vars = new ArrayList<>(group.getVariables());

    List<CoverageCoordAxis> axes = new ArrayList<>();
    if (isCurvilinearOrthogonal)
      axes.addAll(makeHorizCoordinates2D(vars));
    else
      axes.addAll(makeHorizCoordinates());

    /* runtime smooshing
    for (Coordinate axis : group.getCoordinates()) {
      switch (axis.getType()) {
        case runtime:
          runtimes.add(new RuntimeSmoosher((CoverageCoordAxis1D) axis));
          break;
      }
    } */

    Map<Coordinate, List<CoverageCoordAxis>> coord2axisMap = new HashMap<>(); // track which coverageAxis is usd by which GribCoord

    for (Coordinate axis : group.getCoordinates()) {
      switch (axis.getType()) {
        case runtime:
          // runtime coord added by time coord as needed
          break;

        case time2D:
          coord2axisMap.put(axis, makeTime2DCoordinates((CoordinateTime2D) axis));
          break;

        case time:
        case timeIntv:
          coord2axisMap.put(axis, makeTimeCoordinates((CoordinateTimeAbstract) axis));
          break;

        case vert:
          CoverageCoordAxis covAxisVert = makeCoordAxis((CoordinateVert) axis);
          coord2axisMap.put(axis, Lists.newArrayList(covAxisVert));
          break;

        case ens:
          CoverageCoordAxis covAxisEns = makeCoordAxis((CoordinateEns) axis);
          coord2axisMap.put(axis, Lists.newArrayList(covAxisEns));
          break;
      }
    }
    //makeRuntimeCoordAxes(axes);
    //makeTime2DCoordAxis(axes);
    for (Coordinate coord : coord2axisMap.keySet()) {
      for (CoverageCoordAxis covCoord : coord2axisMap.get(coord))
        if (!alreadyHave(axes, covCoord.getName()))
          axes.add(covCoord);
    }

    // make coord systems
    Map<String, CoverageCoordSys> coordSysSet = new HashMap<>();
    for (GribCollectionImmutable.VariableIndex v : vars) {
      CoverageCoordSys sys = makeCoordSys(v, transforms, coord2axisMap);
      coordSysSet.put(sys.getName(), sys);                    // duplicates get eliminated here
    }
    List<CoverageCoordSys> coordSys = new ArrayList<>(coordSysSet.values());

    // all vars that are left are coverages
    List<Coverage> pgrids = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex v : vars) {
      pgrids.add(makeCoverage(v, coord2axisMap));
    }
    // List<Coverage> pgrids = vars.stream().map(this::makeCoverage).collect(Collectors.toList());

    // put it together
    return new CoverageCollection(name, coverageType, gatts, null, null, // let cc calculate bbox
            getCalendarDateRange(), coordSys, transforms, axes, pgrids, this);
  }

  /////////////
  CalendarDateRange dateRange;

  private void trackDateRange(CalendarDateRange cdr) {
    if (dateRange == null) dateRange = cdr;
    else dateRange = dateRange.extend(cdr);
  }

  CalendarDateRange getCalendarDateRange() {
    return dateRange;
  }
  /////////////////

  private List<CoverageCoordAxis> makeHorizCoordinates() {
    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();

    List<CoverageCoordAxis> result = new ArrayList<>(2);
    if (isLatLon) {
      AttributeContainerHelper atts = new AttributeContainerHelper(CF.LATITUDE);
      atts.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));

      double[] values = null;
      CoverageCoordAxis.Spacing spacing = CoverageCoordAxis.Spacing.regularPoint;
      Array glats = hcs.getGaussianLats();
      if (glats != null) {
        spacing = CoverageCoordAxis.Spacing.irregularPoint;
        values = (double[]) glats.get1DJavaArray(DataType.DOUBLE);
        atts.addAttribute(new Attribute(CDM.GAUSSIAN, "true"));
      }

      result.add(new CoverageCoordAxis1D(new CoverageCoordAxisBuilder(CF.LATITUDE, CDM.LAT_UNITS, null, DataType.FLOAT, AxisType.Lat, atts,
              CoverageCoordAxis.DependenceType.independent, null, spacing,
              hcs.ny, hcs.getStartY(), hcs.getEndY(), hcs.dy, values, this)));

      atts = new AttributeContainerHelper(CF.LONGITUDE);
      atts.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
      result.add(new CoverageCoordAxis1D(new CoverageCoordAxisBuilder(CF.LONGITUDE, CDM.LON_UNITS, null, DataType.FLOAT, AxisType.Lon, atts, CoverageCoordAxis.DependenceType.independent,
              null, CoverageCoordAxis.Spacing.regularPoint, hcs.nx, hcs.getStartX(), hcs.getEndX(), hcs.dx, null, this)));

    } else {
      AttributeContainerHelper atts = new AttributeContainerHelper("y");
      atts.addAttribute(new Attribute(CDM.UNITS, "km"));
      result.add(new CoverageCoordAxis1D(new CoverageCoordAxisBuilder("y", "km", CF.PROJECTION_Y_COORDINATE, DataType.FLOAT, AxisType.GeoY, atts, CoverageCoordAxis.DependenceType.independent,
              null, CoverageCoordAxis.Spacing.regularPoint, hcs.ny, hcs.getStartY(), hcs.getEndY(), hcs.dy, null, this)));

      atts = new AttributeContainerHelper("x");
      atts.addAttribute(new Attribute(CDM.UNITS, "km"));
      result.add(new CoverageCoordAxis1D(new CoverageCoordAxisBuilder("x", "km", CF.PROJECTION_X_COORDINATE, DataType.FLOAT, AxisType.GeoX, atts, CoverageCoordAxis.DependenceType.independent,
              null, CoverageCoordAxis.Spacing.regularPoint, hcs.nx, hcs.getStartX(), hcs.getEndX(), hcs.dx, null, this)));
    }
    return result;
  }

  /**
   * identify any variables that are really 2D lat/lon
   *
   * @param vars check this list, but remove lat/lon coordinates from it
   * @return lat/lon coordinates
   */
  private List<CoverageCoordAxis> makeHorizCoordinates2D(List<GribCollectionImmutable.VariableIndex> vars) {
    GdsHorizCoordSys hcs = group.getGdsHorizCoordSys();

    List<GribCollectionImmutable.VariableIndex> remove = new ArrayList<>();
    List<CoverageCoordAxis> result = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex vindex : vars) {
      Grib2Utils.LatLon2DCoord ll2d = Grib2Utils.getLatLon2DcoordType(vindex.getDiscipline(), vindex.getCategory(), vindex.getParameter());
      if (ll2d == null) continue;

      AxisType axisType = ll2d.getAxisType();
      String name = ll2d.toString();
      AttributeContainerHelper atts = new AttributeContainerHelper(name);
      atts.addAttribute(new Attribute(_Coordinate.Stagger, CDM.CurvilinearOrthogonal));
      atts.addAttribute(new Attribute(CDM.StaggerType, ll2d.toString()));

      int[] shape = new int[]{hcs.ny, hcs.nx};
      int npts = hcs.ny * hcs.nx;

      // deffered read
      CoverageCoordAxisBuilder builder;
      if (axisType == AxisType.Lat) {
        atts.addAttribute(new Attribute(CDM.UNITS, CDM.LAT_UNITS));
        builder = new CoverageCoordAxisBuilder(name, CDM.LAT_UNITS, vindex.makeVariableDescription(), DataType.FLOAT, AxisType.Lat, atts,
                CoverageCoordAxis.DependenceType.twoD, null, CoverageCoordAxis.Spacing.irregularPoint, npts, 0, 0, 0, null, this);
      } else {

        atts.addAttribute(new Attribute(CDM.UNITS, CDM.LON_UNITS));
        builder = new CoverageCoordAxisBuilder(name, CDM.LON_UNITS, vindex.makeVariableDescription(), DataType.FLOAT, AxisType.Lon, atts,
                CoverageCoordAxis.DependenceType.twoD, null, CoverageCoordAxis.Spacing.irregularPoint, npts, 0, 0, 0, null, this);
      }

      builder.shape = shape;
      builder.userObject = vindex;
      result.add(new LatLonAxis2D(builder));
      remove.add(vindex);
    }

    // have to do this after the loop is done
    for (GribCollectionImmutable.VariableIndex vindex : remove) {
      vars.remove(vindex);
    }

    return result;
  }

  ////////////////////////////////

  /*

  private void addRuntimeCoordAxis(CoordinateRuntime runtime) {
    String units = runtime.getPeriodName() + " since " + gribCollection.getMasterFirstDate().toString();

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
    CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar :
            ds.getType().isTwoD() ? CoverageCoordAxis.DependenceType.independent : CoverageCoordAxis.DependenceType.dependent;
    CoverageCoordAxis.Spacing spacing = isRegular(counters.get("resol"), missingTolerence) ? CoverageCoordAxis.Spacing.regular : CoverageCoordAxis.Spacing.irregularPoint;

    double[] values = null;
    if (spacing == CoverageCoordAxis.Spacing.irregularPoint) {
      values = new double[n];
      int count = 0;
      for (Double offset : runtime.getOffsetsInTimeUnits())
        values[count++] = offset;
    }

    AttributeContainerHelper atts = new AttributeContainerHelper(runtime.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, units));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxis1D result = new CoverageCoordAxis1D(
            new CoverageCoordAxisBuilder(runtime.getName(), units, "GRIB reference time", DataType.DOUBLE, AxisType.RunTime, atts,
                    dependence, null, spacing, n, start, end, resol, values, this, false));

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
      // System.out.printf("runtime smooch %s into %s%n", runtime.getName(), combined.runtime.getName());
    }
  }

  private int alreadyHaveAtIndex(RuntimeSmoosher tester) {
    for (int i = 0; i < runtimes.size(); i++)
      if (runtimes.get(i).closeEnough(tester)) return i;
    return -1;
  }
  */



  /*
  private void makeRuntimeCoordAxes(List<CoverageCoordAxis> axes) {
    for (RuntimeSmoosher smoosh : runtimes) {
      if (!smoosh.combined) axes.add(smoosh.runtime);
      else {
        int n = (int) ((smoosh.end - smoosh.start) / smoosh.resol);

        CoverageCoordAxis1D combined = new CoverageCoordAxis1D(
                new CoverageCoordAxisBuilder(smoosh.runtime.getName(), smoosh.runtime.getUnits(), Grib.GRIB_RUNTIME, DataType.DOUBLE, AxisType.RunTime,
                        new AttributeContainerHelper(smoosh.runtime.getName(), smoosh.runtime.getAttributes()),
                        smoosh.runtime.getDependenceType(), null, CoverageCoordAxis.Spacing.regular, n, smoosh.start, smoosh.end, smoosh.resol, null, this, false));

        axes.add(combined);
      }
    }
  } */

  //////////////////////////////////////////////////////////

  /* private class Time2DSmoosher {
    CoordinateTime2D time2D;
    List<? extends Object> offsets;
    boolean isOrthogonal;

    public Time2DSmoosher(CoordinateTime2D time2D) {
      this.time2D = time2D;
      this.offsets = time2D.getOffsetsSorted();
      this.isOrthogonal = time2D.isOrthogonal();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Time2DSmoosher that = (Time2DSmoosher) o;
      if (isOrthogonal != that.isOrthogonal) return false;
      return offsets.equals(that.offsets);
    }

    @Override
    public int hashCode() {
      int result = offsets.hashCode();
      result = 31 * result + (isOrthogonal ? 1 : 0);
      return result;
    }
  }

  private Map<Time2DSmoosher, Time2DSmoosher> time2Dmap = new HashMap<>();

  private void addTime2DCoordAxis(CoordinateTime2D time2D) {
    trackDateRange(time2D.makeCalendarDateRange(ucar.nc2.time.Calendar.proleptic_gregorian));

    Time2DSmoosher tester = new Time2DSmoosher(time2D);
    Time2DSmoosher already = time2Dmap.get(tester);
    if (already == null)
      time2Dmap.put(tester, tester);
    else
      substCoords.put(time2D.getName(), already.time2D.getName());
  } */

  /*

  private void makeTime2DCoordAxis(List<CoverageCoordAxis> axes) {
    for (Time2DSmoosher smoosh : time2Dmap.keySet()) {
      if (ds.getType().isUniqueTime())
        axes.add(makeUniqueTimeAxis(smoosh.time2D));
      else if (smoosh.time2D.isOrthogonal())
        axes.add(makeTimeOffsetAxis(smoosh.time2D));
      else if (smoosh.time2D.isRegular())
        axes.add(makeFmrcRegTimeAxis(smoosh.time2D));
      else if (smoosh.time2D.getNtimes() == 1)  // LOOK same as MRSTC ?
        axes.add(makeFmrcOneTimeAxis(smoosh.time2D));
      else
        throw new IllegalStateException(); // LOOK
    }
  } */

  /*
            runtime     time
   SRC      scalar      independent
   unique   dependent   independent
   is2D
    orth    independent timeOffset(time)
    reg     independent time(runtime, time)
    general IllegalStateException
   */

  private List<CoverageCoordAxis> makeTime2DCoordinates(CoordinateTime2D time2D) {
    trackDateRange(time2D.makeCalendarDateRange(null)); // default calendar

    List<CoverageCoordAxis> result = new ArrayList<>();
    CoverageCoordAxis covTime;

    if (ds.getType() == GribCollectionImmutable.Type.SRC) {
      covTime = makeUniqueTimeAxis(time2D);
      CoordinateRuntime rt = time2D.getRuntimeCoordinate();
      //if (!alreadyHave(result, rt.getName()))
      result.add(makeRuntimeCoord(rt));

    } else if (ds.getType().isUniqueTime()) {
      covTime = makeUniqueTimeAxis(time2D);
      result.add(makeRuntimeAuxCoord(time2D, covTime.getNcoords()));

    } else if (time2D.isOrthogonal()) {
      covTime = makeTimeOffsetAxis(time2D);
      CoordinateRuntime rt = time2D.getRuntimeCoordinate();
      //if (!alreadyHave(result, rt.getName()))
      result.add(makeRuntimeCoord(rt));

    } else if (time2D.isRegular()) {
      covTime = makeFmrcRegTimeAxis(time2D);
      CoordinateRuntime rt = time2D.getRuntimeCoordinate();
      //if (!alreadyHave(result, rt.getName()))
      result.add(makeRuntimeCoord(rt)); // LOOK ?

    } else
      throw new IllegalStateException("Time2D with type= " + ds.getType());

    result.add(covTime);

    return result;
  }

  private boolean alreadyHave(List<CoverageCoordAxis> list, String name) {
    for (CoverageCoordAxis coord : list)
      if (coord.getName().equals(name)) return true;
    return false;
  }

  // make an independent time 1D coordinate from time2D, knowing values are unique
  private CoverageCoordAxis1D makeUniqueTimeAxis(CoordinateTime2D time2D) {
    int nruns = time2D.getNruns();

    int ntimes = 0;
    for (int run = 0; run < time2D.getNruns(); run++) {
      CoordinateTimeAbstract timeCoord = time2D.getTimeCoordinate(run);
      ntimes += timeCoord.getSize();
    }
    double[] values;

    if (time2D.isTimeInterval()) {
      values = new double[2 * ntimes];
      int count = 0;
      for (int runIdx = 0; runIdx < nruns; runIdx++) {
        CoordinateTimeIntv timeIntv = (CoordinateTimeIntv) time2D.getTimeCoordinate(runIdx);
        for (TimeCoord.Tinv tinv : timeIntv.getTimeIntervals()) {
          values[count++] = tinv.getBounds1() + time2D.getOffset(runIdx);
          values[count++] = tinv.getBounds2() + time2D.getOffset(runIdx);
        }
      }

    } else {
      values = new double[ntimes];
      int count = 0;
      for (int runIdx = 0; runIdx < nruns; runIdx++) {
        CoordinateTime coordTime = (CoordinateTime) time2D.getTimeCoordinate(runIdx);
        for (int val : coordTime.getOffsetSorted()) {
          double b1 = val + time2D.getOffset(runIdx);
          values[count++] = b1;
        }
      }
    }

    AttributeContainerHelper atts = new AttributeContainerHelper(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));
    atts.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(time2D.getName(), time2D.getTimeUdUnit(), CF.TIME, DataType.DOUBLE, AxisType.Time, atts,
            CoverageCoordAxis.DependenceType.independent, null, null, ntimes, 0.0, 0.0, 0.0, values, this);
    builder.setSpacingFromValues(time2D.isTimeInterval());

    return new CoverageCoordAxis1D(builder);
  }

  /* runtime (independent), time(runtime) (dependent)
  private CoverageCoordAxis1D makeFmrcOneTimeAxis(CoordinateTime2D time2D) {
    int n = time2D.getNruns();
    Counters counters = time2D.calcDistributions();
    Comparable resolMode = counters.get("resol").getMode();
    double resol = (resolMode == null) ? 0.0 : ((Number) resolMode).doubleValue();

    double start, end;
    double[] values;
    CoverageCoordAxis.Spacing spacing;

    if (time2D.isTimeInterval()) {
      values = new double[2 * n];
      int count = 0;
      for (int runIdx = 0; runIdx < n; runIdx++) {
        CoordinateTimeAbstract time = time2D.getTimeCoordinate(runIdx);
        assert time.getNCoords() == 1;
        TimeCoord.Tinv tinv = (TimeCoord.Tinv) time.getValue(0);
        values[count++] = tinv.getBounds1() + time2D.getOffset(runIdx);
        values[count++] = tinv.getBounds2() + time2D.getOffset(runIdx);
      }

      spacing = CoverageCoordAxis.Spacing.discontiguousInterval;

    } else {
      values = new double[n];
      for (int runIdx = 0; runIdx < n; runIdx++) {
        CoordinateTimeAbstract time = time2D.getTimeCoordinate(runIdx);
        assert time.getNCoords() == 1;
        Integer tinv = (Integer) time.getValue(0) + time2D.getOffset(runIdx);
        values[runIdx] = tinv;
      }

      spacing = CoverageCoordAxis.Spacing.irregularPoint; // detect regular?
    }

    start = values[0];
    end = values[n - 1];

    AttributeContainerHelper atts = new AttributeContainerHelper(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));
    atts.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.Time.toString()));

    return new CoverageCoordAxis1D (
            new CoverageCoordAxisBuilder(time2D.getName(), time2D.getTimeUdUnit(), CF.TIME, DataType.DOUBLE, AxisType.Time, atts,
                    CoverageCoordAxis.DependenceType.dependent, time2D.getRuntimeCoordinate().getName(),
                    spacing, n, start, end, resol, values, this, false));
  } */

  // time(runtime, time)
  private FmrcTimeAxis2D makeFmrcRegTimeAxis(CoordinateTime2D time2D) {
    CoordinateRuntime runtime = time2D.getRuntimeCoordinate();
    String dependsOn = runtime.getName();
    int nruns = time2D.getNruns();
    int ntimes = time2D.getNtimes();

    int nvalues = time2D.isTimeInterval() ? nruns * ntimes * 2 : nruns * ntimes;
    double[] values = new double[nvalues];

    for (int runIdx=0; runIdx<nruns; runIdx++) {
      int runOffset = time2D.getOffset(runIdx);

      CoordinateTimeAbstract time = time2D.getTimeCoordinate(runIdx);
      if (time2D.isTimeInterval()) {
        CoordinateTimeIntv coordIntv = (CoordinateTimeIntv) time;
        int n = coordIntv.getSize(); // may be different than ntimes
        for (int timeIdx=0; timeIdx<n; timeIdx++) {
          TimeCoord.Tinv tinv = (TimeCoord.Tinv) coordIntv.getValue(timeIdx);
          values[runIdx*ntimes+timeIdx] = tinv.getBounds1() + runOffset;
          values[runIdx*ntimes+timeIdx+1] = tinv.getBounds2() + runOffset;
        }
      } else {
        CoordinateTime coord = (CoordinateTime) time;
        int n = coord.getSize(); // may be different than ntimes
        for (int timeIdx=0; timeIdx<n; timeIdx++) {
          Integer offset = (Integer) coord.getValue(timeIdx);
          values[runIdx*ntimes+timeIdx] = offset + runOffset;
        }
      }
    }

    AttributeContainerHelper atts = new AttributeContainerHelper(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_OFFSET));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, CDM.TIME_OFFSET));
    atts.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));
    atts.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.TimeOffset.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(time2D.getName(), time2D.getUnit(), CDM.TIME_OFFSET, DataType.DOUBLE,
            AxisType.TimeOffset, atts,
            CoverageCoordAxis.DependenceType.fmrcReg, dependsOn, null, nruns * ntimes, 0.0, 0.0, 0.0, values, this);
    builder.setSpacingFromValues(time2D.isTimeInterval());

    return new FmrcTimeAxis2D(builder); // LOOK should be FmrcTimeAxisReg2D to take advantage of regular
  }

  // orthogonal runtime, offset; both independent
  private TimeOffsetAxis makeTimeOffsetAxis(CoordinateTime2D time2D) {
    List<? extends Object> offsets = time2D.getOffsetsSorted();
    int n = offsets.size();

    double[] values;

    if (time2D.isTimeInterval()) {
      values = new double[2 * n];
      int count = 0;
      for (Object offset : offsets) {
        TimeCoord.Tinv tinv = (TimeCoord.Tinv) offset;
        values[count++] = tinv.getBounds1();
        values[count++] = tinv.getBounds2();
      }

    } else {
      values = new double[n];
      int count = 0;
      for (Object val : offsets) {
        Integer off = (Integer) val;
        values[count++] = off;
      }
    }

    AttributeContainerHelper atts = new AttributeContainerHelper(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_OFFSET));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, CDM.TIME_OFFSET));
    atts.addAttribute(new Attribute(CDM.UDUNITS, time2D.getTimeUdUnit()));
    atts.addAttribute(new Attribute(_Coordinate.AxisType, AxisType.TimeOffset.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(time2D.getName(), time2D.getUnit(), CDM.TIME_OFFSET, DataType.DOUBLE, AxisType.TimeOffset, atts,
            CoverageCoordAxis.DependenceType.independent, null, null, n, 0.0, 0.0, 0.0, values, this);
    builder.setSpacingFromValues(time2D.isTimeInterval());

    return new TimeOffsetAxis(builder);
  }

  //////////////////////////////////////////////////////////

  private List<CoverageCoordAxis> makeTimeCoordinates(CoordinateTimeAbstract time) {
    List<CoverageCoordAxis> result = new ArrayList<>();

    if (time instanceof CoordinateTime)
      result.add(makeCoordAxis((CoordinateTime) time));
    else if (time instanceof CoordinateTimeIntv)
      result.add(makeCoordAxis((CoordinateTimeIntv) time));

    CoverageCoordAxis runAux = makeRuntimeAuxCoord(time);
    if (runAux != null)
      result.add(runAux);

    return result;
  }

  // create an independent runtime axis
  private CoverageCoordAxis1D makeRuntimeCoord(CoordinateRuntime runtime) {
    String units = runtime.getPeriodName() + " since " + gribCollection.getMasterFirstDate().toString();

    List<Double> offsets = runtime.getOffsetsInTimeUnits();
    int n = offsets.size();

    // CoordinateRuntime master = gribCollection.getMasterRuntime();
    boolean isScalar = (n == 1);      // this is the case of runtime[1]
    CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.independent;

    double[] values = new double[n];
    int count = 0;
    for (Double offset : runtime.getOffsetsInTimeUnits())
      values[count++] = offset;

    AttributeContainerHelper atts = new AttributeContainerHelper(runtime.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, units));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, "GRIB reference time"));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(runtime.getName(), units, "GRIB reference time", DataType.DOUBLE, AxisType.RunTime, atts,
            dependence, null, null, n, 0.0, 0.0, 0.0, values, this);

    builder.setSpacingFromValues(false);

    return new CoverageCoordAxis1D(builder);
  }

  // create a dependent runtime axis for this time2d, which has unique times
  private CoverageCoordAxis makeRuntimeAuxCoord(CoordinateTime2D time2D, int ntimes) {
    CoordinateRuntime runtimeU = time2D.getRuntimeCoordinate();
    List<Double> runOffsets = runtimeU.getOffsetsInTimeUnits();

    double[] values = new double[ntimes];
    int count = 0;
    for (int run = 0; run < time2D.getNruns(); run++) {
      CoordinateTimeAbstract timeCoord = time2D.getTimeCoordinate(run);
      for (int time = 0; time < timeCoord.getNCoords(); time++)
        values[count++] = runOffsets.get(run);
    }

    boolean isScalar = (time2D.getNruns() == 1);      // this is the case of runtime[1]
    CoverageCoordAxis.DependenceType dependence = isScalar ? CoverageCoordAxis.DependenceType.scalar : CoverageCoordAxis.DependenceType.dependent;
    String refName = "ref" + time2D.getName();

    AttributeContainerHelper atts = new AttributeContainerHelper(time2D.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time2D.getTimeUdUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(refName, time2D.getTimeUdUnit(), Grib.GRIB_RUNTIME, DataType.DOUBLE, AxisType.RunTime, atts,
            dependence, time2D.getName(), null, ntimes, 0, 0, 0, values, this);
    builder.setSpacingFromValues(false);

    return new CoverageCoordAxis1D(builder);
  }

  // create a dependent runtime axis for this time, using the index into the master runtimes array
  private CoverageCoordAxis makeRuntimeAuxCoord(CoordinateTimeAbstract time) {
    if (time.getTime2runtime() == null) return null;
    String refName = "ref" + time.getName();

    int length = time.getSize();
    double[] data = new double[length];
    for (int i = 0; i < length; i++) data[i] = Double.NaN;

    int count = 0;
    CoordinateRuntime master = gribCollection.getMasterRuntime();
    List<Double> masterOffsets = master.getOffsetsInTimeUnits();
    for (int masterIdx : time.getTime2runtime()) {
      data[count++] = masterOffsets.get(masterIdx - 1);
    }

    AttributeContainerHelper atts = new AttributeContainerHelper(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getTimeUdUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME_REFERENCE));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_RUNTIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(refName, master.getUnit(), Grib.GRIB_RUNTIME, DataType.DOUBLE, AxisType.RunTime, atts,
            CoverageCoordAxis.DependenceType.dependent, time.getName(), null, length, 0, 0, 0, data, this);
    builder.setSpacingFromValues(false);

    return new CoverageCoordAxis1D(builder);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateTime time) {
    trackDateRange(time.makeCalendarDateRange(null)); // default calendar

    List<Integer> offsets = time.getOffsetSorted();
    int n = offsets.size();
    double[] values = new double[n];

    int count = 0;
    for (int offset : offsets)
      values[count++] = offset;

    AttributeContainerHelper atts = new AttributeContainerHelper(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(time.getName(), time.getTimeUdUnit(), Grib.GRIB_VALID_TIME,
            DataType.DOUBLE, AxisType.Time, atts, CoverageCoordAxis.DependenceType.independent, null, null, n,
            0, 0, 0, values, this);
    builder.setSpacingFromValues(false);
    return new CoverageCoordAxis1D(builder);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateTimeIntv time) {
    trackDateRange(time.makeCalendarDateRange(null)); // default calendar

    List<TimeCoord.Tinv> offsets = time.getTimeIntervals();
    int n = offsets.size();
    double[] values = new double[2 * n];

    int count = 0;
    for (TimeCoord.Tinv offset : offsets) {
      values[count++] = offset.getBounds1();
      values[count++] = offset.getBounds2();
    }

    AttributeContainerHelper atts = new AttributeContainerHelper(time.getName());
    atts.addAttribute(new Attribute(CDM.UNITS, time.getUnit()));
    atts.addAttribute(new Attribute(CF.STANDARD_NAME, CF.TIME));
    atts.addAttribute(new Attribute(CDM.LONG_NAME, Grib.GRIB_VALID_TIME));
    atts.addAttribute(new Attribute(CF.CALENDAR, ucar.nc2.time.Calendar.proleptic_gregorian.toString()));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(time.getName(), time.getTimeUdUnit(), Grib.GRIB_VALID_TIME,
            DataType.DOUBLE, AxisType.Time, atts, CoverageCoordAxis.DependenceType.independent, null, null, n, 0, 0, 0, values, this);
    builder.setSpacingFromValues(true);
    return new CoverageCoordAxis1D(builder);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateVert vertCoord) {
    List<VertCoord.Level> levels = vertCoord.getLevelSorted();

    int n = vertCoord.getSize();
    double[] values;

    if (vertCoord.isLayer()) {
      int count = 0;
      values = new double[2 * n];
      for (int i = 0; i < n; i++) {
        values[count++] = levels.get(i).getValue1();
        values[count++] = levels.get(i).getValue2();
      }

    } else {
      values = new double[n];
      for (int i = 0; i < n; i++)
        values[i] = levels.get(i).getValue1();
    }

    AttributeContainerHelper atts = new AttributeContainerHelper(vertCoord.getName());
    String units = vertCoord.getUnit();
    atts.addAttribute(new Attribute(CDM.UNITS, units));
    AxisType axisType = AxisType.GeoZ;
    if (SimpleUnit.isCompatible("mbar", units)) axisType = AxisType.Pressure;
    else if (SimpleUnit.isCompatible("m", units)) axisType = AxisType.Height;

    String desc = vertCoord.getVertUnit().getDesc();
    if (desc != null) atts.addAttribute(new Attribute(CDM.LONG_NAME, desc));
    atts.addAttribute(new Attribute(CF.POSITIVE, vertCoord.isPositiveUp() ? CF.POSITIVE_UP : CF.POSITIVE_DOWN));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(vertCoord.getName(), vertCoord.getUnit(), null, DataType.DOUBLE, axisType, atts,
            CoverageCoordAxis.DependenceType.independent, null, null, n, 0.0, 0.0, 0.0, values, this);
    builder.setSpacingFromValues(vertCoord.isLayer());
    return new CoverageCoordAxis1D(builder);
  }

  private CoverageCoordAxis makeCoordAxis(CoordinateEns ensCoord) {
    int n = ensCoord.getSize();
    double[] values = new double[n];
    for (int i = 0; i < n; i++)
      values[i] = ((EnsCoord.Coord) ensCoord.getValue(i)).getEnsMember();

    AttributeContainerHelper atts = new AttributeContainerHelper(ensCoord.getName());
    String units = ensCoord.getUnit();
    atts.addAttribute(new Attribute(CDM.UNITS, units));

    CoverageCoordAxisBuilder builder = new CoverageCoordAxisBuilder(ensCoord.getName(), units, null, DataType.DOUBLE, AxisType.Ensemble, atts,
            CoverageCoordAxis.DependenceType.independent, null, null, ensCoord.getSize(),
            0, 0, 0, values, this);
    builder.setSpacingFromValues(false);
    return new CoverageCoordAxis1D(builder);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////

  private CoverageCoordSys makeCoordSys(GribCollectionImmutable.VariableIndex gribVar, List<CoverageTransform> transforms,
                                        Map<Coordinate, List<CoverageCoordAxis>> coord2axisMap) {
    List<String> axisNames = makeAxisNameList(gribVar, coord2axisMap);
    List<String> transformNames = transforms.stream().map(CoverageTransform::getName).collect(Collectors.toList());
    return new CoverageCoordSys(null, axisNames, transformNames, coverageType);
  }

  private class NameAndType {
    String name;
    AxisType type;

    public NameAndType(String name, AxisType type) {
      this.name = name;
      this.type = type;
    }
  }

  private List<String> makeAxisNameList(GribCollectionImmutable.VariableIndex gribVar, Map<Coordinate, List<CoverageCoordAxis>> coord2axisMap) {
    List<NameAndType> names = new ArrayList<>();
    for (Coordinate axis : gribVar.getCoordinates()) {
      List<CoverageCoordAxis> coordList = coord2axisMap.get(axis);
      if (coordList != null) {
        for (CoverageCoordAxis coord : coordList) names.add(new NameAndType(coord.getName(), coord.getAxisType()));
      }
      /* String subst = substCoords.get(axis.getName());
      if (subst != null)
        names.add(new NameAndType(subst, axis.getType()));
      else
      names.add(new NameAndType(axis.getName(), axis.getType()));
      if (axis.getType() == Coordinate.Type.time || axis.getType() == Coordinate.Type.timeIntv) {
        names.add(new NameAndType("ref" + axis.getName(), Coordinate.Type.runtime));  // LOOK do we have to test if it exists??
      } */
    }

    Collections.sort(names, (o1, o2) -> o1.type.axisOrder() - o2.type.axisOrder());
    List<String> axisNames = names.stream().map(o -> o.name).collect(Collectors.toList());
    if (isCurvilinearOrthogonal) {
      Grib2Utils.LatLonCoordType type = Grib2Utils.getLatLon2DcoordType(gribVar.makeVariableDescription());
      if (type != null) {
        switch (type) {
          case U:
            axisNames.add(Grib2Utils.LatLon2DCoord.U_Latitude.toString());
            axisNames.add(Grib2Utils.LatLon2DCoord.U_Longitude.toString());
            break;
          case V:
            axisNames.add(Grib2Utils.LatLon2DCoord.V_Latitude.toString());
            axisNames.add(Grib2Utils.LatLon2DCoord.V_Longitude.toString());
            break;
          case P:
            axisNames.add(Grib2Utils.LatLon2DCoord.P_Latitude.toString());
            axisNames.add(Grib2Utils.LatLon2DCoord.P_Longitude.toString());
            break;
        }
      }

    } else if (isLatLon) {
      axisNames.add(CF.LATITUDE);
      axisNames.add(CF.LONGITUDE);

    } else {
      axisNames.add("y");
      axisNames.add("x");
    }

    return axisNames;
  }

  private String makeCoordSysName(List<String> axes) {
    Formatter fname = new Formatter();
    for (String axis : axes)
      fname.format(" %s", axis);
    return fname.toString();
  }

  private Coverage makeCoverage(GribCollectionImmutable.VariableIndex gribVar, Map<Coordinate, List<CoverageCoordAxis>> coord2axisMap) {

    AttributeContainerHelper atts = new AttributeContainerHelper(gribVar.makeVariableName());
    atts.addAttribute(new Attribute(CDM.LONG_NAME, gribVar.makeVariableDescription()));
    atts.addAttribute(new Attribute(CDM.UNITS, gribVar.makeVariableUnits()));
    gribCollection.addVariableAttributes(atts, gribVar);

    /*
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
        atts.addAttribute(new Attribute(Grib.GRIB_STAT_TYPE, statType.toString()));
        CF.CellMethods cm = GribStatType.getCFCellMethod(statType);
        Coordinate timeCoord = gribVar.getCoordinate(Coordinate.Type.timeIntv);
        if (cm != null && timeCoord != null)
          atts.addAttribute(new Attribute(CF.CELL_METHODS, timeCoord.getName() + ": " + cm.toString()));
      } else {
        atts.addAttribute(new Attribute(Grib.GRIB_STAT_TYPE, gribVar.getIntvType()));
      }
    } */

    String coordSysName = makeCoordSysName(makeAxisNameList(gribVar, coord2axisMap));

    return new Coverage(gribVar.makeVariableName(), DataType.FLOAT, atts.getAttributes(), coordSysName, gribVar.makeVariableUnits(),
            gribVar.makeVariableDescription(), this, gribVar);
  }

  //////////////////////////////////////////////////////


  @Override
  public double[] readCoordValues(CoverageCoordAxis coordAxis) throws IOException {
    if (coordAxis instanceof LatLonAxis2D)
      return readLatLonAxis2DCoordValues((LatLonAxis2D) coordAxis);

    java.util.Optional<Coordinate> opt = group.findCoordinate(coordAxis.getName());
    if (!opt.isPresent()) throw new IllegalStateException();
    Coordinate coord = opt.get();

    if (coord instanceof CoordinateTime) {
      List<Integer> offsets = ((CoordinateTime) coord).getOffsetSorted();
      double[] values = new double[offsets.size()];
      int count = 0;
      for (int val : offsets) values[count++] = val;
      return values;

    } else if (coord instanceof CoordinateTimeIntv) {
      List<TimeCoord.Tinv> intv = ((CoordinateTimeIntv) coord).getTimeIntervals();
      double[] values;
      if (coordAxis.getSpacing() == CoverageCoordAxis.Spacing.discontiguousInterval) {
        values = new double[2 * intv.size()];
        int count = 0;
        for (TimeCoord.Tinv val : intv) {
          values[count++] = val.getBounds1();
          values[count++] = val.getBounds2();
        }
      } else {
        values = new double[intv.size() + 1];
        int count = 0;
        for (TimeCoord.Tinv val : intv) {
          values[count++] = val.getBounds1();
          values[count] = val.getBounds2(); // gets overritten except for the last
        }
      }
      return values;

    } else if (coord instanceof CoordinateRuntime) {
      List<Double> offsets = ((CoordinateRuntime) coord).getOffsetsInTimeUnits();
      double[] values = new double[offsets.size()];
      int count = 0;
      for (double val : offsets) {
        values[count++] = val;
      }
      return values;
    }

    throw new IllegalStateException();
  }

  public double[] readLatLonAxis2DCoordValues(LatLonAxis2D coordAxis) throws IOException {
    GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) coordAxis.getUserObject();
    int[] shape = coordAxis.getShape();
    List<RangeIterator> ranges = new ArrayList<>();
    List<Integer> fullShape = new ArrayList<>();
    for (Coordinate coord : vindex.getCoordinates()) {
      ranges.add(new Range(1));
      fullShape.add(coord.getNCoords());
    }
    ranges.add(new Range(shape[0]));
    fullShape.add(shape[0]);
    ranges.add(new Range(shape[1]));
    fullShape.add(shape[1]);
    SectionIterable siter = new SectionIterable(ranges, fullShape);

    GribDataReader dataReader = GribDataReader.factory(gribCollection, vindex);
    Array data;
    try {
      data = dataReader.readData(siter); // optimize pass in null ?? LOOK old way
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }

    return (double[]) data.get1DJavaArray(DataType.DOUBLE);  // LOOK lame conversion
  }


  //////////////////////////////////////////////////////

  @Override
  public GeoReferencedArray readData(Coverage coverage, SubsetParams params, boolean canonicalOrder) throws IOException, InvalidRangeException {
    GribCollectionImmutable.VariableIndex vindex = (GribCollectionImmutable.VariableIndex) coverage.getUserObject();
    CoverageCoordSys orgCoordSys = coverage.getCoordSys();
    ucar.nc2.util.Optional<CoverageCoordSys> opt = orgCoordSys.subset(params, false, true);
    if (!opt.isPresent())
      throw new InvalidRangeException(opt.getErrorMessage());

    CoverageCoordSys subsetCoordSys = opt.get();
    List<CoverageCoordAxis> coordsSetAxes = new ArrayList<>(); // for CoordsSet.factory()

    // this orders the coords based on the grib coords, which also orders the iterator in CoordsSet. could be different i think
    for (Coordinate gribCoord : vindex.getCoordinates()) {

      switch (gribCoord.getType()) {
        case runtime:
          CoverageCoordAxis runAxis = subsetCoordSys.getAxis(AxisType.RunTime);
          coordsSetAxes.addAll(axisAndDependents(runAxis, subsetCoordSys));
          break;

        case time2D:
          // also covers isConstantForecast
          if (ds.getType().isTwoD()) {
            CoverageCoordAxis toAxis = subsetCoordSys.getAxis(AxisType.TimeOffset);
            if (toAxis != null)
              coordsSetAxes.addAll(axisAndDependents(toAxis, subsetCoordSys));
          } else {
            CoverageCoordAxis toAxis = subsetCoordSys.getAxis(AxisType.Time);
            if (toAxis != null)
              coordsSetAxes.addAll(axisAndDependents(toAxis, subsetCoordSys));
          }

          // the OneTime case should be covered by axisAndDependents(runAxis)
          break;

        case vert:
          CoverageCoordAxis1D vertAxis = (CoverageCoordAxis1D) subsetCoordSys.getZAxis();
          coordsSetAxes.add(vertAxis);
          break;

        case time:
        case timeIntv:
        case ens:
          CoverageCoordAxis axis = subsetCoordSys.getAxis(gribCoord.getType().axisType);
          coordsSetAxes.addAll(axisAndDependents(axis, subsetCoordSys));
          break;
      }
    }

    /* debugging
    boolean hasruntime = false;
    for (CoverageCoordAxis axis : coordsSetAxes)
      if (axis.getAxisType() == AxisType.RunTime) hasruntime = true;
    if (!hasruntime)
      logger.warn("HEYA no runtime " + gribCollection.getName()); */

    List<CoverageCoordAxis> geoArrayAxes = new ArrayList<>(coordsSetAxes);  // for GeoReferencedArray
    geoArrayAxes.add(subsetCoordSys.getYAxis());
    geoArrayAxes.add(subsetCoordSys.getXAxis());
    List<RangeIterator> yxRange = subsetCoordSys.getHorizCoordSys().getRanges(); // may be 2D

    // iterator over all except x, y
    CoordsSet coordIter = CoordsSet.factory(subsetCoordSys.isConstantForecast(), coordsSetAxes);

    GribDataReader dataReader = GribDataReader.factory(gribCollection, vindex);
    Array data = dataReader.readData2(coordIter, yxRange.get(0), yxRange.get(1));

    return new GeoReferencedArray(coverage.getName(), coverage.getDataType(), data, subsetCoordSys);
  }

  // LOOK dependent axis could get added multiple times
  private List<CoverageCoordAxis> axisAndDependents(CoverageCoordAxis axis, CoverageCoordSys csys) {
    List<CoverageCoordAxis> result = new ArrayList<>();
    if (axis.getDependenceType() != CoverageCoordAxis.DependenceType.dependent)
      result.add(axis);
    for (CoverageCoordAxis dependent : csys.getDependentAxes(axis))
      result.add(dependent);
    return result;
  }
}

  /*
  RangeIterator subsetRuntime(CoordinateRuntime gribCoord, CoverageCoordAxis1D covAxisSubset) throws InvalidRangeException {
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
  // (runtime > 1, time = 1)  = constant forecast   (must cut down the runtime based on the time request - only those that have it.
  // (runtime > 1, time > 1).

  RangeIterator subsetTimeOffset(CoordinateTime2D gribCoord, CoverageCoordAxis1D runAxis, CoverageCoordAxis1D toAxis) throws InvalidRangeException {

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

  RangeIterator subset(Coordinate gribCoord, CoverageCoordAxis1D covAxisSubset) throws InvalidRangeException {
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
    }
  } */
