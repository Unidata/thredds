/* Copyright */
package ucar.nc2.ft2.remote;

import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.Attribute;
import ucar.nc2.ft2.coverage.grid.*;
import ucar.nc2.stream.NcStream;

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
    return (content == null) ? path+" "+status : path+" "+status +"\n "+content;
  }

  private GridCoverageDataset readHeader(InputStream is) throws IOException {
     byte[] b = new byte[4];
     NcStream.readFully(is, b);

     if (!NcStream.test(b, NcStream.MAGIC_HEADER))
       throw new IOException("Data corrupted on "+endpoint);

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

  /*
  message GridCoverageDataset {
    required string name = 1;
    repeated Attribute atts = 2;
    repeated CoordSys coordSys = 3;
    repeated CoordTransform coordTransforms = 4;
    repeated CoordAxis coordAxes = 5;
    repeated GridCoverage grids = 6;
  }
   */
  GridCoverageDataset decodeHeader(CdmrFeatureProto.GridCoverageDataset proto) {
    GridCoverageDataset result = new GridCoverageDataset();
    result.setName(endpoint);

    List<Attribute> gatts = new ArrayList<>();
    for (ucar.nc2.stream.NcStreamProto.Attribute patt : proto.getAttsList())
      gatts.add(NcStream.decodeAtt(patt));
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
    result.setGrids( grids);

    return result;
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
    GridCoordAxis result = new GridCoordAxis();
    result.setName(proto.getName());
    result.setDataType(NcStream.decodeDataType(proto.getDataType()));
    result.setAxisType(proto.getAxisType());
    result.setNvalues(proto.getNvalues());
    result.setUnits(proto.getUnits());
    result.setDescription(proto.getDescription());

    result.setSpacing(proto.getSpacing());
    result.setStartValue(proto.getStartValue());
    result.setEndValue(proto.getEndValue());
    result.setResolution(proto.getResolution());

    if (proto.hasValues()) {
      // LOOK may mess with ability to change var size later.
      ByteBuffer bb = ByteBuffer.wrap(proto.getValues().toByteArray());
      DoubleBuffer db = bb.asDoubleBuffer();
      int n = db.remaining();
      double[] data = new double[n];
      for (int i=0; i<n; i++) data[i] = db.get(i);
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
    result.setDataType(NcStream.decodeDataType(proto.getDataType()));

    List<Attribute> atts = new ArrayList<>();
    for (ucar.nc2.stream.NcStreamProto.Attribute patt : proto.getAttsList())
      atts.add(NcStream.decodeAtt(patt));
    result.setAtts(atts);

    result.setCoordSysName(proto.getCoordSys());
    result.setUnits(proto.getUnits());
    result.setDescription(proto.getDescription());

    return result;
  }

}
