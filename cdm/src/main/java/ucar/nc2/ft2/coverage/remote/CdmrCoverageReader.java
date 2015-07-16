/* Copyright */
package ucar.nc2.ft2.coverage.remote;

import org.apache.http.Header;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.ma2.*;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamProto;
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
  boolean debug = false;
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
  public GeoReferencedArray readData(Coverage coverage, SubsetParams subset) throws IOException {
    if (httpClient == null)
      httpClient = HTTPFactory.newSession(endpoint);

    Formatter f = new Formatter();
    f.format("%s?req=data&var=%s", endpoint, coverage.getName());  // LOOK full vs short name

    for (Map.Entry<String,Object> entry : subset.getEntries()) {
      f.format("&%s=%s", entry.getKey(), entry.getValue());
    }

    if (showRequest)
      System.out.printf(" CdmrFeature data request for gridCoverage: %s%n url=%s%n", coverage.getName(), f);

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
      if (debug) System.out.println("  readData data message len= " + psize);
      byte[] dp = new byte[psize];
      NcStream.readFully(is, dp);
      CdmrFeatureProto.DataResponse dproto = CdmrFeatureProto.DataResponse.parseFrom(dp);
      DataResponse dataResponse = cdmrfReader.decodeDataResponse(dproto);

      int narrays  = NcStream.readVInt(is);
      assert narrays == dataResponse.arrayResponse.size();
      List<GeoReferencedArray> geoArrays = new ArrayList<>();

      // read Data
      for (GeoArrayResponse response : dataResponse.arrayResponse) {
        geoArrays.add(readData(response, is));
      }

      assert geoArrays.size() == 1;
      return geoArrays.get(0);
    }

  }

  public GeoReferencedArray readData(GeoArrayResponse response, InputStream is) throws IOException {
    // is it compressed ?
    Array data;
    if (response.deflate) {
      //ByteArrayInputStream bin = new ByteArrayInputStream(datab);
      InflaterInputStream in = new InflaterInputStream(is);
      ByteArrayOutputStream bout = new ByteArrayOutputStream( (int) response.uncompressedSize);
      IO.copy(in, bout);  // decompress
      byte[] resultb = bout.toByteArray();  // another fucking copy - overrride ByteArrayOutputStream(byte[] myown);
      data = Array.factory(response.dataType, response.shape, ByteBuffer.wrap(resultb)); // another copy, not sure can do anything

    } else {
      byte[] datab = new byte[(int) response.uncompressedSize];
      is.read(datab);
      data = Array.factory(response.dataType, response.shape, ByteBuffer.wrap(datab));
    }

    // String coverageName, DataType dataType, Array data, CoverageCoordSys csSubset
    return new GeoReferencedArray(response.coverageName, response.dataType, data, null); // LOOK

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
