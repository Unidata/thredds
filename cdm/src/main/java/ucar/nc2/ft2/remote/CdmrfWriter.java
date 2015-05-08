/* Copyright */
package ucar.nc2.ft2.remote;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.*;
import ucar.nc2.ft2.coverage.grid.*;
import ucar.nc2.stream.NcStream;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

/**
 * Server side for Cdmrf
 *
 * @author caron
 * @since 5/1/2015
 */
public class CdmrfWriter {
  static private final Logger logger = LoggerFactory.getLogger(CdmrfWriter.class);

  //  must start with this "CDFF"
  static public final byte[] MAGIC_START = new byte[]{0x43, 0x44, 0x46, 0x46};
  static public final int MAX_INLINE_NVALUES = 1000;
  private static final boolean show = false;

  GridCoverageDataset gridDataset;
  String location;

  public CdmrfWriter(GridCoverageDataset gridDataset, String location) throws IOException {
    this.gridDataset = gridDataset;
    this.location = location;
  }

  public long sendHeader(OutputStream out) throws IOException {
    long size = 0;

    CdmrFeatureProto.GridCoverageDataset.Builder headerBuilder = encodeHeader(gridDataset);
    CdmrFeatureProto.GridCoverageDataset header = headerBuilder.build();

    // header message
    size += NcStream.writeBytes(out, NcStream.MAGIC_HEADER);
    byte[] b = header.toByteArray();
    size += NcStream.writeVInt(out, b.length); // len
    if (show) System.out.println("Write Header len=" + b.length);

    // payload
    size += NcStream.writeBytes(out, b);
    if (show) System.out.println(" header size=" + size);

    return size;
  }

  /* message GridCoverageDataset {
      required string name = 1;
      repeated Attribute atts = 2;
      required Rectangle latlonRect = 3;
      optional Rectangle projRect = 4;
      required CalendarDateRange timeRange = 5;

      repeated CoordSys coordSys = 6;
      repeated CoordTransform coordTransforms = 7;
      repeated CoordAxis coordAxes = 8;
      repeated GridCoverage grids = 9;
    } */
  CdmrFeatureProto.GridCoverageDataset.Builder encodeHeader(GridCoverageDataset gridDataset) {
    CdmrFeatureProto.GridCoverageDataset.Builder builder = CdmrFeatureProto.GridCoverageDataset.newBuilder();
    builder.setName(location);
    builder.setDateRange(encodeDateRange(gridDataset.getCalendarDateRange()));
    builder.setLatlonRect(encodeRectangle(gridDataset.getLatLonBoundingBox()));
    if (gridDataset.getProjBoundingBox() != null)
      builder.setProjRect(encodeRectangle(gridDataset.getProjBoundingBox()));

    for (Attribute att : gridDataset.getGlobalAttributes())
      builder.addAtts(NcStream.encodeAtt(att));

    for (GridCoordSys gcs : gridDataset.getCoordSys())
      builder.addCoordSys(encodeCoordSys(gcs));

    for (GridCoordTransform gcs : gridDataset.getCoordTransforms())
      builder.addCoordTransforms(encodeCoordTransform(gcs));

    for (GridCoordAxis axis : gridDataset.getCoordAxes())
      builder.addCoordAxes(encodeCoordAxis(axis));

    for (GridCoverage grid : gridDataset.getGrids())
      builder.addGrids(encodeGrid(grid));

    return builder;
  }

  /* message Rectangle {
      required double startx = 1;
      required double starty = 2;
      required double incx = 3;
      required double incy = 4;
    } */
  CdmrFeatureProto.Rectangle.Builder encodeRectangle(LatLonRect rect) {
    CdmrFeatureProto.Rectangle.Builder builder = CdmrFeatureProto.Rectangle.newBuilder();
    //     this(r.getLowerLeftPoint(), r.getUpperRightPoint().getLatitude() - r.getLowerLeftPoint().getLatitude(), r.getWidth());
    LatLonPoint ll = rect.getLowerLeftPoint();
    LatLonPoint ur = rect.getUpperRightPoint();
    builder.setStartx(ll.getLongitude());
    builder.setStarty(ll.getLatitude());
    builder.setIncx(rect.getWidth());
    builder.setIncy(ur.getLatitude() - ll.getLatitude());
    return builder;
  }

  CdmrFeatureProto.Rectangle.Builder encodeRectangle(ProjectionRect rect) {
    CdmrFeatureProto.Rectangle.Builder builder = CdmrFeatureProto.Rectangle.newBuilder();
    //      this(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
    builder.setStartx(rect.getMinX());
    builder.setStarty(rect.getMaxX());
    builder.setIncx(rect.getWidth());
    builder.setIncy(rect.getHeight());
    return builder;
  }

  /* message CalendarDateRange {
        required int64 start = 1;
        required int64 end = 2;
        required int32 calendar = 3; // calendar ordinal
      } */
  CdmrFeatureProto.CalendarDateRange.Builder encodeDateRange(CalendarDateRange dateRange) {
    CdmrFeatureProto.CalendarDateRange.Builder builder = CdmrFeatureProto.CalendarDateRange.newBuilder();

    builder.setStart(dateRange.getStart().getMillis());
    builder.setEnd(dateRange.getEnd().getMillis());
    Calendar cal = dateRange.getStart().getCalendar();
    builder.setCalendar(cal.ordinal());
    return builder;
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
  CdmrFeatureProto.GridCoverage.Builder encodeGrid(GridCoverage grid) {
    CdmrFeatureProto.GridCoverage.Builder builder = CdmrFeatureProto.GridCoverage.newBuilder();
    builder.setName(grid.getName());
    builder.setDataType(NcStream.encodeDataType(grid.getDataType()));

    for (Attribute att : grid.getAttributes())
      builder.addAtts(NcStream.encodeAtt(att));

    builder.setUnits(grid.getUnits());
    builder.setDescription(grid.getDescription());
    builder.setCoordSys(grid.getCoordSysName());

    return builder;
  }

  /* message CoordSys {
      required string name = 1;
      repeated string axisNames = 2;
      repeated string transformNames = 3;
      repeated CoordSys components = 4;        // ??
    } */
  CdmrFeatureProto.CoordSys.Builder encodeCoordSys(GridCoordSys gcs) {
    CdmrFeatureProto.CoordSys.Builder builder = CdmrFeatureProto.CoordSys.newBuilder();
    builder.setName(gcs.getName());

    for (String axis : gcs.getAxisNames())
      builder.addAxisNames(axis);

    for (String gct : gcs.getTransformNames())
      builder.addTransformNames(gct);
    return builder;
  }


  /* message CoordTransform {
      required bool isHoriz = 1;
      required string name = 2;
      repeated Attribute params = 3;
    } */
  CdmrFeatureProto.CoordTransform.Builder encodeCoordTransform(GridCoordTransform gct) {
    CdmrFeatureProto.CoordTransform.Builder builder = CdmrFeatureProto.CoordTransform.newBuilder();
    builder.setIsHoriz(gct.isHoriz());
    builder.setName(gct.getName());
    for (Attribute att : gct.getAttributes())
      builder.addParams(NcStream.encodeAtt(att));
    return builder;
  }

  /* message CoordAxis {
      required string name = 1;
      required DataType dataType = 2;
      required int32 axisType = 3;    // ucar.nc2.constants.AxisType ordinal
      required int64 nvalues = 4;
      required string units = 5;
      optional bool isRegular = 6;
      required double min = 7;        // required ??
      required double max = 8;
      optional double resolution = 9;
    } */
  CdmrFeatureProto.CoordAxis.Builder encodeCoordAxis(GridCoordAxis axis) {
    CdmrFeatureProto.CoordAxis.Builder builder = CdmrFeatureProto.CoordAxis.newBuilder();
    builder.setName(axis.getName());
    builder.setDataType(NcStream.encodeDataType(axis.getDataType()));
    builder.setAxisType(axis.getAxisType().ordinal());
    builder.setNvalues(axis.getNvalues());
    if (axis.getUnits() != null) builder.setUnits(axis.getUnits());
    if (axis.getDescription() != null) builder.setDescription(axis.getDescription());

    builder.setSpacing(axis.getSpacing().ordinal());
    builder.setStartValue(axis.getStartValue());
    builder.setEndValue(axis.getEndValue());
    builder.setResolution(axis.getResolution());

    if (!axis.isRegular() && axis.getNvalues() < MAX_INLINE_NVALUES) {
      try {
        double[] values = axis.readValues();
        ByteBuffer bb = ByteBuffer.allocate(8 * values.length);
        DoubleBuffer db = bb.asDoubleBuffer();
        db.put(values);
        builder.setValues(ByteString.copyFrom(bb.array()));

      } catch (IOException e) {
        e.printStackTrace();
        logger.error("failed to read data", e);
      }
    }

    /* for (Attribute att : axis.getParameters())
      builder.addParams(NcStream.encodeAtt(att)); */
    return builder;
  }
}
