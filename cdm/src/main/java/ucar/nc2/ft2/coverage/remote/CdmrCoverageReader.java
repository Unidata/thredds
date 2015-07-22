/* Copyright */
package ucar.nc2.ft2.coverage.remote;

import org.apache.http.Header;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.ma2.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.stream.NcStream;
import ucar.nc2.util.IO;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

/**
 * Client side cdmrFeature interface to GridCoverage
 *
 * @author caron
 * @since 5/5/2015
 */
public class CdmrCoverageReader implements CoverageReader, CoordAxisReader {

  String endpoint;
  HTTPSession httpClient;
  boolean showCompression = true;
  boolean showRequest = true;

  CdmrCoverageReader(String endpoint, HTTPSession httpClient) throws IOException {
    this.endpoint = endpoint;
    this.httpClient = httpClient;
  }

  @Override
  public void close() throws Exception {
    httpClient.close();
  }

  @Override
  public GeoReferencedArray readData(Coverage coverage, SubsetParams subset, boolean canonicalOrder) throws IOException {
    if (httpClient == null)
      httpClient = HTTPFactory.newSession(endpoint);

    Formatter f = new Formatter();
    f.format("%s?req=data&var=%s", endpoint, coverage.getName());  // LOOK full vs short name LOOK URL encoding

    for (Map.Entry<String,Object> entry : subset.getEntries()) {
      f.format("&%s=%s", entry.getKey(), entry.getValue());
    }

    if (showRequest)
      System.out.printf("CdmrFeature data request for gridCoverage: %s%n url=%s%n", coverage.getName(), f);

    try (HTTPMethod method = HTTPFactory.Get(httpClient, f.toString())) {
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(getErrorMessage(method));

      if (statusCode >= 300)
        throw new IOException(getErrorMessage(method));

      Header h = method.getResponseHeader("Content-Length");
      if (h != null) {
        String s = h.getValue();
        int readLen = Integer.parseInt(s);
      }

      CdmrfReader cdmrfReader = new CdmrfReader(endpoint);
      InputStream is = method.getResponseAsStream();
      if (!NcStream.readAndTest(is, NcStream.MAGIC_DATA))
        throw new IOException("Data transfer corrupted");

      // read Data message
      int psize = NcStream.readVInt(is);
      byte[] dp = new byte[psize];
      NcStream.readFully(is, dp);
      CdmrFeatureProto.DataResponse dproto = CdmrFeatureProto.DataResponse.parseFrom(dp);
      DataResponse dataResponse = cdmrfReader.decodeDataResponse(dproto);

      int narrays  = NcStream.readVInt(is);
      assert narrays == dataResponse.arrayResponse.size();
      List<GeoReferencedArray> geoArrays = new ArrayList<>();

      // read Data
      for (GeoArrayResponse arrayResponse : dataResponse.arrayResponse) {
        geoArrays.add(readData(dataResponse, arrayResponse, is));
      }

      assert geoArrays.size() == 1;
      return geoArrays.get(0);
    }

  }

  public GeoReferencedArray readData(DataResponse dataResponse, GeoArrayResponse arrayResponse, InputStream is) throws IOException {
    int sizeIn  = NcStream.readVInt(is);  // not used ?

    if (arrayResponse.deflate) {
      is = new InflaterInputStream(is);
      float ratio = (sizeIn == 0) ? 0.0f : ((float) arrayResponse.uncompressedSize) / sizeIn;
      if (showCompression) System.out.printf("  readData data message compress= %d decompress=%d compress=%f%n", sizeIn, arrayResponse.uncompressedSize, ratio);
    }

    byte[] datab = new byte[(int) arrayResponse.uncompressedSize];
    NcStream.readFully(is, datab);
    Array data = Array.factory(arrayResponse.dataType, arrayResponse.shape, ByteBuffer.wrap(datab));

    CoverageCoordSys csys = dataResponse.findCoordSys( arrayResponse.coordSysName);
    return new GeoReferencedArray(arrayResponse.coverageName, arrayResponse.dataType, data, csys);
  }


  private static String getErrorMessage(HTTPMethod method) {
    String path = method.getURL();
    String status = method.getStatusLine();
    String content = method.getResponseAsString();
    return (content == null) ? path+" "+status : path+" "+status +"\n "+content;
  }

  @Override
  public double[] readValues(CoverageCoordAxis coordAxis) throws IOException {
    return new double[0];
  }
}
