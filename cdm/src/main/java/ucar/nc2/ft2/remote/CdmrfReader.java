/* Copyright */
package ucar.nc2.ft2.remote;

import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerHelper;
import ucar.nc2.constants.AxisType;
import ucar.nc2.ft2.coverage.grid.*;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Client side for CdmrFeature Grid
 *
 * @author caron
 * @since 5/2/2015
 */
public class CdmrfReader {
  private static final boolean showRequest = true;
  String endpoint;
  GridCoverageDataset gridDataset;

  public CdmrfReader(String endpoint) {
    this.endpoint = endpoint;
  }

  public GridCoverageDataset open() throws IOException {
    long start = System.currentTimeMillis();

    HTTPSession httpClient = HTTPFactory.newSession(endpoint);
    String url = endpoint + "?req=header";

    // get the header
    try (HTTPMethod method = HTTPFactory.Get(httpClient, url)) {
      method.setFollowRedirects(true);
      if (showRequest) System.out.printf("CdmrFeature request %s %n", url);
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(getErrorMessage(method));

      if (statusCode >= 300)
        throw new IOException(getErrorMessage(method));

      InputStream is = method.getResponseAsStream();
      gridDataset = readHeader(is);
    }

    long took = System.currentTimeMillis() - start;
    if (showRequest) System.out.printf(" took %d msecs %n", took);
    return gridDataset;
  }

  private static String getErrorMessage(HTTPMethod method) {
    String path = method.getURL();
    String status = method.getStatusLine();
    String content = method.getResponseAsString();
    return (content == null) ? path + " " + status : path + " " + status + "\n " + content;
  }

  private GridCoverageDataset readHeader(InputStream is) throws IOException {
    byte[] b = new byte[4];
    NcStream.readFully(is, b);

    if (!NcStream.test(b, NcStream.MAGIC_HEADER))
      throw new IOException("Data corrupted on " + endpoint);

    // header
    int msize = NcStream.readVInt(is);
    byte[] m = new byte[msize];
    NcStream.readFully(is, m);

    CdmrFeatureProto.GridCoverageDataset proto = CdmrFeatureProto.GridCoverageDataset.parseFrom(m);
    GridCoverageDataset gridCoverage = decodeHeader(proto);
//     if (debug) System.out.printf("  proto= %s%n", proto);
//     if (debug) System.out.printf("  result= %s%n", gridCoverage);
    return gridCoverage;
  }

  /* message CalendarDateRange {
      required int64 start = 1;
      required int64 end = 2;
      required int32 calendar = 3; // ucar.nc2.time.Calendar ordinal
    }

    message GridCoverageDataset {
      required string name = 1;
      repeated Attribute atts = 2;
      required Rectangle latlonRect = 3;
      optional Rectangle projRect = 4;
      required CalendarDateRange dateRange = 5;

      repeated CoordSys coordSys = 6;
      repeated CoordTransform coordTransforms = 7;
      repeated CoordAxis coordAxes = 8;
      repeated GridCoverage grids = 9;
    } */
  GridCoverageDataset decodeHeader(CdmrFeatureProto.GridCoverageDataset proto) {
    GridCoverageDataset result = new GridCoverageDataset();
    result.setName(endpoint);
    result.setLatLonBoundingBox(decodeLatLonRectangle(proto.getLatlonRect()));
    result.setProjBoundingBox(decodeProjRectangle(proto.getProjRect()));
    if (proto.hasDateRange())
      result.setCalendarDateRange(decodeDateRange(proto.getDateRange()));

    AttributeContainerHelper gatts = new AttributeContainerHelper("global atts");
    for (ucar.nc2.stream.NcStreamProto.Attribute patt : proto.getAttsList())
      gatts.addAttribute(NcStream.decodeAtt(patt));
    result.setGlobalAttributes(gatts);

    List<GridCoordSys> csys = new ArrayList<>();
    for (CdmrFeatureProto.CoordSys pgrid : proto.getCoordSysList())
      csys.add(decodeCoordSys(pgrid));
    result.setCoordSys(csys);

    List<GridCoordTransform> transforms = new ArrayList<>();
    for (CdmrFeatureProto.CoordTransform ptransform : proto.getCoordTransformsList())
      transforms.add(decodeCoordTransform(ptransform));
    result.setCoordTransforms(transforms);

    List<GridCoordAxis> axes = new ArrayList<>();
    for (CdmrFeatureProto.CoordAxis paxes : proto.getCoordAxesList())
      axes.add(decodeCoordAxis(paxes));
    result.setCoordAxes(axes);

    List<GridCoverage> grids = new ArrayList<>();
    for (CdmrFeatureProto.GridCoverage pgrid : proto.getGridsList())
      grids.add(decodeGrid(pgrid));
    result.setGrids(grids);

    return result;
  }

  /* message Rectangle {
      required double startx = 1;
      required double starty = 2;
      required double incx = 3;
      required double incy = 4;
    } */
  LatLonRect decodeLatLonRectangle(CdmrFeatureProto.Rectangle proto) {
    LatLonPointImpl start = new LatLonPointImpl(proto.getStarty(), proto.getStartx());
    return new LatLonRect(start, proto.getIncy(), proto.getIncx());
  }

  ProjectionRect decodeProjRectangle(CdmrFeatureProto.Rectangle proto) {
    ProjectionPoint pt = new ProjectionPointImpl(proto.getStartx(), proto.getStarty());
    return new ProjectionRect(pt, proto.getIncy(), proto.getIncx());
  }

  CalendarDateRange decodeDateRange(CdmrFeatureProto.CalendarDateRange proto) {
    ucar.nc2.time.Calendar cal = convertCalendar(proto.getCalendar());
    CalendarDate start = CalendarDate.of(cal, proto.getStart());
    CalendarDate end = CalendarDate.of(cal, proto.getEnd());
    return CalendarDateRange.of(start, end);
  }


  /*
  message CoordSys {
    required string name = 1;
    repeated string axisNames = 2;
    repeated string transformNames = 3;
    repeated CoordSys components = 4;        // ??
  }
   */
  GridCoordSys decodeCoordSys(CdmrFeatureProto.CoordSys proto) {
    GridCoordSys result = new GridCoordSys();
    result.setName(proto.getName());
    result.setAxisNames(proto.getAxisNamesList());
    result.setTransformNames(proto.getTransformNamesList());

    return result;
  }

  /*
      message CoordTransform {
    required bool isHoriz = 1;
    required string name = 2;
    repeated Attribute params = 3;
  }
   */
  GridCoordTransform decodeCoordTransform(CdmrFeatureProto.CoordTransform proto) {
    GridCoordTransform result = new GridCoordTransform(proto.getName());
    result.setIsHoriz(proto.getIsHoriz());

    for (ucar.nc2.stream.NcStreamProto.Attribute patt : proto.getParamsList())
      result.addAttribute(NcStream.decodeAtt(patt));

    return result;
  }

  /*
    message CoordAxis {
    required string name = 1;
    required DataType dataType = 2;
    required int32 axisType = 3;    // ucar.nc2.constants.AxisType ordinal
    required int64 nvalues = 4;
    required string units = 5;
    optional bool isRegular = 6;
    required double min = 7;        // required ??
    required double max = 8;
    optional double resolution = 9;
  }
   */
  GridCoordAxis decodeCoordAxis(CdmrFeatureProto.CoordAxis proto) {
    AxisType type = convertAxisType(proto.getAxisType());
    GridCoordAxis result = new GridCoordAxis();
    result.setName(proto.getName());
    result.setDataType(NcStream.convertDataType(proto.getDataType()));
    result.setAxisType(type);
    result.setNvalues(proto.getNvalues());
    result.setMinIndex(0);
    result.setMaxIndex(proto.getNvalues()-1);
    result.setUnits(proto.getUnits());
    result.setDescription(proto.getDescription());
    result.setDependenceType(convertDependenceType(proto.getDepend()));
    if (proto.hasDependsOn())
      result.setDependsOn(proto.getDependsOn());

    AttributeContainerHelper atts = new AttributeContainerHelper("axis atts");
    for (ucar.nc2.stream.NcStreamProto.Attribute patt : proto.getAttsList())
      atts.addAttribute(NcStream.decodeAtt(patt));
    result.setAttributes(atts);

    result.setSpacing(convertSpacing(proto.getSpacing()));
    result.setStartValue(proto.getStartValue());
    result.setEndValue(proto.getEndValue());
    result.setResolution(proto.getResolution());

    if (proto.hasValues()) {
      // LOOK may mess with ability to change var size later.
      ByteBuffer bb = ByteBuffer.wrap(proto.getValues().toByteArray());
      DoubleBuffer db = bb.asDoubleBuffer();
      int n = db.remaining();
      double[] data = new double[n];
      for (int i = 0; i < n; i++) data[i] = db.get(i);
      result.setValues(data);
    }

    return result;
  }

  /*
message GridCoverage {
  required string name = 1; // short name
  required DataType dataType = 2;
  optional bool unsigned = 3 [default = false];
  repeated Attribute atts = 4;
  required string coordSys = 5;
}
 */
  GridCoverage decodeGrid(CdmrFeatureProto.GridCoverage proto) {
    GridCoverage result = new CdmrGridCoverage(endpoint);
    result.setName(proto.getName());
    result.setDataType(NcStream.convertDataType(proto.getDataType()));

    List<Attribute> atts = new ArrayList<>();
    for (ucar.nc2.stream.NcStreamProto.Attribute patt : proto.getAttsList())
      atts.add(NcStream.decodeAtt(patt));
    result.setAtts(atts);

    result.setCoordSysName(proto.getCoordSys());
    result.setUnits(proto.getUnits());
    result.setDescription(proto.getDescription());

    return result;
  }

  static public AxisType convertAxisType(CdmrFeatureProto.AxisType dtype) {
    switch (dtype) {
      case RunTime:
        return AxisType.RunTime;
      case Ensemble:
        return AxisType.Ensemble;
      case Time:
        return AxisType.Time;
      case GeoX:
        return AxisType.GeoX;
      case GeoY:
        return AxisType.GeoY;
      case GeoZ:
        return AxisType.GeoZ;
      case Lat:
        return AxisType.Lat;
      case Lon:
        return AxisType.Lon;
      case Height:
        return AxisType.Height;
      case Pressure:
        return AxisType.Pressure;
      case RadialAzimuth:
        return AxisType.RadialAzimuth;
      case RadialDistance:
        return AxisType.RadialDistance;
      case RadialElevation:
        return AxisType.RadialElevation;
      case Spectral:
        return AxisType.Spectral;
    }
    throw new IllegalStateException("illegal data type " + dtype);
  }

  static public Calendar convertCalendar(CdmrFeatureProto.Calendar type) {
    switch (type) {
      case gregorian:
        return Calendar.gregorian;
      case proleptic_gregorian:
        return Calendar.proleptic_gregorian;
      case noleap:
        return Calendar.noleap;
      case all_leap:
        return Calendar.all_leap;
      case uniform30day:
        return Calendar.uniform30day;
      case julian:
        return Calendar.julian;
      case none:
        return Calendar.none;
    }
    throw new IllegalStateException("illegal data type " + type);
  }

  static public GridCoordAxis.DependenceType convertDependenceType(CdmrFeatureProto.DependenceType type) {
    switch (type) {
      case independent:
        return GridCoordAxis.DependenceType.independent;
      case dependent:
        return GridCoordAxis.DependenceType.dependent;
      case twoD:
        return GridCoordAxis.DependenceType.twoD;
    }
    throw new IllegalStateException("illegal data type " + type);
  }


  static public GridCoordAxis.Spacing convertSpacing(CdmrFeatureProto.AxisSpacing type) {
    switch (type) {
      case regular:
        return GridCoordAxis.Spacing.regular;
      case irregularPoint:
        return GridCoordAxis.Spacing.irregularPoint;
      case contiguousInterval:
        return GridCoordAxis.Spacing.contiguousInterval;
      case discontiguousInterval:
        return GridCoordAxis.Spacing.discontiguousInterval;
    }
    throw new IllegalStateException("illegal data type " + type);
  }



}
