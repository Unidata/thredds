/* Copyright */
package ucar.nc2.ft2.coverage.adapter;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.Parameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapt ucar.nc2.dt.GeoGridDataset to ucar.nc2.ft2.coverage.CoverageDataset
 *
 * @author caron
 * @since 5/1/2015
 */
public class DtCoverageAdapter implements CoverageReader, CoordAxisReader {

  public static CoverageDataset factory(DtCoverageDataset proxy) {
    AttributeContainerHelper atts = new AttributeContainerHelper(proxy.getName());
    atts.addAll(proxy.getGlobalAttributes());

    List<Coverage> pgrids = new ArrayList<>();
    for (DtCoverage dtGrid : proxy.getGrids())
      pgrids.add(makeCoverage(dtGrid));

    List<CoverageCoordSys> pcoordSys = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets())
      pcoordSys.add(makeCoordSys(gset.getGeoCoordSystem()));

    Set<String> transformNames = new HashSet<>();
    List<CoverageTransform> transforms = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateTransform ct : gcs.getCoordTransforms())
        if (!transformNames.contains(ct.getName())) {
          transforms.add(makeTransform(ct));
          transformNames.add(ct.getName());
        }
    }

    DtCoverageAdapter reader = new DtCoverageAdapter(proxy);

    Set<String> axisNames = new HashSet<>();
    List<CoverageCoordAxis> axes = new ArrayList<>();
    for (DtCoverageDataset.Gridset gset : proxy.getGridsets()) {
      DtCoverageCS gcs = gset.getGeoCoordSystem();
      for (ucar.nc2.dataset.CoordinateAxis axis : gcs.getCoordAxes())
        if (!axisNames.contains(axis.getFullName())) {
          axes.add(makeCoordAxis(axis, reader));
          axisNames.add(axis.getFullName());
        }
    }

    return new CoverageDataset(proxy.getName(), proxy.getCoverageType(), atts,
            proxy.getBoundingBox(), proxy.getProjBoundingBox(), proxy.getCalendarDateRange(),
            pcoordSys, transforms, axes, pgrids, reader);
  }

  private static Coverage makeCoverage(DtCoverage dt) {
    return new Coverage(dt.getName(), dt.getDataType(), dt.getAttributes(), dt.getCoordinateSystem().getName(), dt.getUnitsString(), dt.getDescription());
  }

  private static CoverageCoordSys makeCoordSys(DtCoverageCS dt) {
    List<String> transformNames = new ArrayList<>();
    for (CoordinateTransform ct : dt.getCoordTransforms())
      transformNames.add(ct.getName());
    List<String> axisNames = new ArrayList<>();
    for (CoordinateAxis axis : dt.getCoordAxes()) // LOOK should be just the grid axes ?
      axisNames.add(axis.getFullName());

    return new CoverageCoordSys(dt.getName(), axisNames, transformNames, dt.getCoverageType());
  }

  private static CoverageTransform makeTransform(ucar.nc2.dataset.CoordinateTransform dt) {
    AttributeContainerHelper atts = new AttributeContainerHelper(dt.getName());
    for (Parameter p : dt.getParameters())
      atts.addAttribute(new Attribute(p));
    //   public CoverageCoordTransform(String name, AttributeContainerHelper attributes, boolean isHoriz) {
    return new CoverageTransform(dt.getName(), atts, dt.getTransformType() == TransformType.Projection);
  }

  private static CoverageCoordAxis makeCoordAxis(ucar.nc2.dataset.CoordinateAxis dtCoordAxis, DtCoverageAdapter reader) {
    String name = dtCoordAxis.getFullName();
    DataType dataType = dtCoordAxis.getDataType();
    AxisType axisType = dtCoordAxis.getAxisType();
    String units = dtCoordAxis.getUnitsString();
    String description = dtCoordAxis.getDescription();

    CoverageCoordAxis.DependenceType dependenceType;
    String dependsOn = null;
    if (dtCoordAxis.isIndependentCoordinate())
      dependenceType = CoverageCoordAxis.DependenceType.independent;
    else if (dtCoordAxis.isScalar())
      dependenceType = CoverageCoordAxis.DependenceType.scalar;
    else {
      dependenceType = CoverageCoordAxis.DependenceType.dependent;
      dependsOn = dtCoordAxis.getDimension(0).toString();
    }

    int ncoords = (int) dtCoordAxis.getSize();
    CoverageCoordAxis.Spacing spacing = null;
    double startValue = 0.0;
    double endValue = 0.0;
    double resolution = 0.0;
    double[] values = null;

    // 1D case
    if (dtCoordAxis instanceof CoordinateAxis1D) {
      CoordinateAxis1D axis1D = (CoordinateAxis1D) dtCoordAxis;
      startValue = axis1D.getCoordValue(0);
      endValue = axis1D.getCoordValue((int) dtCoordAxis.getSize() - 1);

      if (axis1D.isRegular() || axis1D.isScalar()) {
        spacing = CoverageCoordAxis.Spacing.regular;

      } else if (!dtCoordAxis.isInterval()) {
        spacing = CoverageCoordAxis.Spacing.irregularPoint;
        values = axis1D.getCoordValues();

      } else if (dtCoordAxis.isContiguous()) {
        spacing = CoverageCoordAxis.Spacing.contiguousInterval;
        values = axis1D.getCoordEdges();

      } else {
        spacing = CoverageCoordAxis.Spacing.discontiguousInterval;
        values = new double[2 * ncoords];
        double[] bounds1 = axis1D.getBound1();
        double[] bounds2 = axis1D.getBound2();
        int count = 0;
        for (int i = 0; i < ncoords; i++) {
          values[count++] = bounds1[i];
          values[count++] = bounds2[i];
        }
      }

      return new CoverageCoordAxis1D(name, units, description, dataType, axisType, dtCoordAxis.getAttributes(), dependenceType, dependsOn, spacing,
              ncoords, startValue, endValue, resolution, values, reader);

    }

    // Fmrc Time
    if (dtCoordAxis instanceof CoordinateAxis2D && axisType == AxisType.Time) {

      spacing = CoverageCoordAxis.Spacing.regular;

      return new FmrcTimeAxis2D(name, units, description, dataType, axisType, dtCoordAxis.getAttributes(), dependenceType, dependsOn, spacing,
              ncoords, startValue, endValue, resolution, values, reader);
    }

    // 2D Lat Lon Time
    if (dtCoordAxis instanceof CoordinateAxis2D && (axisType == AxisType.Lat || axisType == AxisType.Lon)) {

      spacing = CoverageCoordAxis.Spacing.regular;

      return new LatLonAxis2D(name, units, description, dataType, axisType, dtCoordAxis.getAttributes(), dependenceType, dependsOn, spacing,
              ncoords, startValue, endValue, resolution, values, reader);
    }

    throw new IllegalStateException("Dont know what to do with axis " + dtCoordAxis.getFullName());
  }

  ////////////////////////
  private final DtCoverageDataset proxy;

  private DtCoverageAdapter(DtCoverageDataset proxy) {
    this.proxy = proxy;
  }


  //////////////////////////////////////////////////////////////////////////////////////
  // CoverageReader

  @Override
  public void close() throws IOException {
    proxy.close();
  }

  /*
    public static final String latlonBB = "latlonBB";     // value = LatLonRect
    public static final String projBB = "projBB";         // value = ProjRect
    public static final String horizStride = "horizStride";  // value = Integer
 ok   public static final String vertCoord = "vertCoord";   // value = double
 ok   public static final String vertIndex = "vertIndex";   // value = integer    LOOK BAD
 ok   public static final String timeRange = "timeRange";   // value = CalendarDateRange
 ok   public static final String time = "time";             // value = CalendarDate
    public static final String timeWindow = "timeWindow"; // value = TimeDuration
 ok   public static final String timeStride = "timeStride"; // value = Integer
  X  public static final String allTimes = "allTimes";     // value = Boolean
 ok   public static final String latestTime = "latestTime"; // value = Boolean
 ok   public static final String runtime = "runtime";       // value = CalendarDate
 ok   public static final String runtime = "runtimeRange";  // value = CalendarDateRange
 ok   public static final String ensCoord = "ensCoord";     // value = double ??
   */

  @Override
  public GeoReferencedArray readData(Coverage coverage, SubsetParams params, boolean canonicalOrder) throws IOException, InvalidRangeException {
    DtCoverage grid = proxy.findGridByName(coverage.getName());
    CoverageCoordSys orgCoordSys = coverage.getCoordSys();
    CoverageCoordSys subsetCoordSys = orgCoordSys.subset(params);

    List<Range> section = new ArrayList<>();
    for (CoverageCoordAxis axis : subsetCoordSys.getAxes()) {
      if (axis instanceof CoverageCoordAxis1D) {
        CoverageCoordAxis1D axis1D = (CoverageCoordAxis1D) axis;
        if (axis.isScalar()) continue;
        section.add(new Range(axis.getAxisType().toString(), axis1D.getMinIndex(), axis1D.getMaxIndex()));
      }
    }

    Array data = grid.readDataSection(section, canonicalOrder);
    return new GeoReferencedArray(coverage.getName(), coverage.getDataType(), data, subsetCoordSys);
  }


  //////////////////////////////////////////////////////////////////////////////////////
  // CoordAxisReader

  /*
 * regular: regularly spaced points or intervals (start, end, npts), edges halfway between coords
 * irregularPoint: irregular spaced points (values, npts), edges halfway between coords
 * contiguousInterval: irregular contiguous spaced intervals (values, npts), values are the edges, and there are npts+1, coord halfway between edges
 * discontinuousInterval: irregular discontiguous spaced intervals (values, npts), values are the edges, and there are 2*npts
 */
  @Override
  public double[] readValues(CoverageCoordAxis coordAxis) throws IOException {
    ucar.nc2.dataset.CoordinateAxis dtCoordAxis = proxy.getNetcdfDataset().findCoordinateAxis(coordAxis.getName());

    if (dtCoordAxis instanceof CoordinateAxis1D) {

      CoordinateAxis1D axis1D = (CoordinateAxis1D) dtCoordAxis;

      switch (coordAxis.getSpacing()) {
        case irregularPoint:
          return axis1D.getCoordValues();
        case contiguousInterval:
          return axis1D.getCoordEdges();
        case discontiguousInterval:
          int n = (int) dtCoordAxis.getSize();
          double[] result = new double[2 * n];
          double[] bounds1 = axis1D.getBound1();
          double[] bounds2 = axis1D.getBound2();
          int count = 0;
          for (int i = 0; i < n; i++) {
            result[count++] = bounds1[i];
            result[count++] = bounds2[i];
          }
          return result;
      }
    }

    // twoD case i guess
    Array data = dtCoordAxis.read();

    double[] result = new double[(int) data.getSize()];
    int count = 0;
    while (data.hasNext())
      result[count++] = data.nextDouble();

    return result;
  }

  /*



    //DtCoverageCS gcs = grid.getCoordinateSystem();
    CoverageCoordSys ccsys = coverage.getCoordSys();

    List<Range> section = new ArrayList<>();
    for (String key : params.getKeys()) {
      switch (key) {

        case SubsetParams.time: { // CalendarDate
          CalendarDate cdate = (CalendarDate) params.get(key);
          CoverageCoordAxis taxisOrg = ccsys.getTimeAxis();
          if (taxisOrg != null && !taxisOrg.isScalar()) {
            CoverageCoordAxis taxisWant = taxisOrg.subset(params);
            section.add(new Range(AxisType.Time.toString(), taxisWant.getMinIndex(), taxisWant.getMaxIndex()));

          if (taxis != null && !taxis.isScalar()) {
            if (taxis instanceof CoordinateAxis1DTime) {
              CoordinateAxis1DTime time1d = (CoordinateAxis1DTime) taxis;
              int wantIndex = time1d.findTimeIndexFromCalendarDate(cdate);               // LOOK change to closest
              section.add(new Range(AxisType.Time.toString(), wantIndex, wantIndex));

            } else if (taxis instanceof CoordinateAxis2D) {      // LOOK what to do ?
              // CoordinateAxis2D time2d = (CoordinateAxis2D) taxis;
              // time = time2d.findTimeIndexFromCalendarDate(cdate);
            }
          }
          break;
        }

        case SubsetParams.timeRange: { // CalendarDate
          CalendarDateRange dateRange = (CalendarDateRange) params.get(key);
          CoordinateAxis taxis = gcs.getTimeAxis();
          if (taxis != null && !taxis.isScalar()) {
            if (taxis instanceof CoordinateAxis1DTime) {
              CoordinateAxis1DTime time1d = (CoordinateAxis1DTime) taxis;
              int startIndex = time1d.findTimeIndexFromCalendarDate(dateRange.getStart());   // LOOK change to closest
              int endIndex = time1d.findTimeIndexFromCalendarDate(dateRange.getEnd());
              Integer stride = params.getInteger(SubsetParams.timeStride);
              if (stride == null)
                section.add(new Range(AxisType.Time.toString(), startIndex, endIndex));
              else
                section.add(new Range(AxisType.Time.toString(), startIndex, endIndex, stride));
            }
          }
          break;
        }

        case SubsetParams.latestTime: {
          CoordinateAxis taxis = gcs.getTimeAxis();
          if (taxis != null && !taxis.isScalar()) {
            int wantIndex = (int) taxis.getSize() - 1;
            section.add(new Range(AxisType.Time.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.runtime: { // CalendarDate
          CalendarDate cdate = (CalendarDate) params.get(key);
          CoordinateAxis1DTime rtaxis = gcs.getRunTimeAxis();
          if (rtaxis != null && !rtaxis.isScalar()) {
            int wantIndex = rtaxis.findTimeIndexFromCalendarDate(cdate);                   // LOOK change to closest
            section.add(new Range(AxisType.RunTime.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.runtimeRange: { // CalendarDateRange
          CalendarDateRange dateRange = (CalendarDateRange) params.get(key);
          CoordinateAxis1DTime rtaxis = gcs.getRunTimeAxis();
          if (rtaxis != null && !rtaxis.isScalar()) {
            int startIndex = rtaxis.findTimeIndexFromCalendarDate(dateRange.getStart());   // LOOK change to closest
            int endIndex = rtaxis.findTimeIndexFromCalendarDate(dateRange.getEnd());
            section.add(new Range(AxisType.RunTime.toString(), startIndex, endIndex));
          }
          break;
        }

        case SubsetParams.vertCoord: { // double
          CoordinateAxis1D zaxis = gcs.getVerticalAxis();
          if (zaxis != null && !zaxis.isScalar()) {
            double zval = params.getDouble(key);
            int wantIndex = zaxis.findCoordElement(zval);
            if (wantIndex < 0) {
              System.out.println("HEY");
              zaxis.findCoordElement(zval);
            }
            section.add(new Range(AxisType.GeoZ.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.vertIndex: {    // LOOK should we allow this ??
          CoordinateAxis1D zaxis = gcs.getVerticalAxis();
          if (zaxis != null && !zaxis.isScalar()) {
            int wantIndex = params.getInteger(key);
            section.add(new Range(AxisType.GeoZ.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.ensCoord: { // double
          CoordinateAxis1D eaxis = gcs.getEnsembleAxis();
          if (eaxis != null && !eaxis.isScalar()) {
            int wantIndex = eaxis.findCoordElement(params.getDouble(key));
            section.add(new Range(AxisType.Ensemble.toString(), wantIndex, wantIndex));
          }
          break;
        }

        case SubsetParams.projBB: { // ProjRect
          if (!ccsys.getHorizCoordSys().hasProjection) continue;
          ProjectionRect projRect = (ProjectionRect) params.get(key);
          ccsys.getHorizCoordSys().subset(params);
          break;
        }

      }
    }
   */

}
