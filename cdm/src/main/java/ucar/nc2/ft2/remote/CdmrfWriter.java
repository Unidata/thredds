/* Copyright */
package ucar.nc2.ft2.remote;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.*;
import ucar.nc2.ft2.coverage.grid.*;
import ucar.nc2.stream.NcStream;

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

  GridCoverageDatasetIF gridDataset;
  String location;

  public CdmrfWriter(GridCoverageDatasetIF gridDataset, String location) throws IOException {
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

  /*
  message GridCoverageDataset {
    required string name = 1;
    repeated Attribute atts = 2;
    repeated CoordSys coordSys = 3;
    repeated CoordTransform coordTransforms = 4;
    repeated CoordAxis axes = 5;
    repeated GridCoverage grids = 6;
  }
   */
  CdmrFeatureProto.GridCoverageDataset.Builder encodeHeader(GridCoverageDatasetIF gridDataset) {
    CdmrFeatureProto.GridCoverageDataset.Builder builder = CdmrFeatureProto.GridCoverageDataset.newBuilder();
    builder.setName(location);

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

    for (Attribute att : grid.getAtts())
      builder.addAtts(NcStream.encodeAtt(att));

    builder.setUnits(grid.getUnits());
    builder.setDescription(grid.getDescription());
    builder.setCoordSys(grid.getCoordSysName());

    return builder;
  }

  /*
  message CoordSys {
    required string name = 1;
    repeated string axisNames = 2;
    repeated string transformNames = 3;
    repeated CoordSys components = 4;        // ??
  }
  */
  CdmrFeatureProto.CoordSys.Builder encodeCoordSys(GridCoordSys gcs) {
    CdmrFeatureProto.CoordSys.Builder builder = CdmrFeatureProto.CoordSys.newBuilder();
    builder.setName(gcs.getName());

    for (String axis : gcs.getAxisNames())
      builder.addAxisNames(axis);

    for (String gct : gcs.getTransformNames())
      builder.addTransformNames(gct);
    return builder;
  }


  /*
  message CoordTransform {
    required bool isHoriz = 1;
    required string name = 2;
    repeated Attribute params = 3;
  }
  */
  CdmrFeatureProto.CoordTransform.Builder encodeCoordTransform(GridCoordTransform gct) {
    CdmrFeatureProto.CoordTransform.Builder builder = CdmrFeatureProto.CoordTransform.newBuilder();
    builder.setIsHoriz(gct.isHoriz());
    builder.setName(gct.getName());
    for (Attribute att : gct.getAttributes())
      builder.addParams(NcStream.encodeAtt(att));
    return builder;
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
        db.put( values);
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
