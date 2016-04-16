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
 *
 */
package ucar.nc2.ft2.coverage.remote;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import org.apache.http.Header;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.ma2.Array;
import ucar.nc2.ft2.coverage.CoordAxisReader;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCoordAxis;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageReader;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamReader;

/**
 * Client side cdmrFeature interface to GridCoverage.
 * This class handles reading the data, or the coordinates when the values are deferred.
 *
 * @author caron
 * @since 5/5/2015
 */
public class CdmrCoverageReader implements CoverageReader, CoordAxisReader {
  private Escaper urlParamEscaper = UrlEscapers.urlFormParameterEscaper();

  String endpoint;
  HTTPSession httpClient;
  boolean showCompression = true;
  boolean showRequest = true;

  CdmrCoverageReader(String endpoint, HTTPSession httpClient) throws IOException {
    this.endpoint = endpoint;
    this.httpClient = httpClient;
  }

  @Override
  public void close() throws IOException {
    httpClient.close();
  }

  @Override
  public String getLocation() {
    return endpoint;
  }

  @Override
  public GeoReferencedArray readData(Coverage coverage, SubsetParams subset, boolean canonicalOrder) throws IOException {
    if (httpClient == null)
      httpClient = HTTPFactory.newSession(endpoint); // LOOK is this ok? no authentication...

    Formatter f = new Formatter();
    f.format("%s?", endpoint);
    subset.encodeForCdmrfDataRequest(f, coverage.getName());

    if (showRequest)
      System.out.printf("CdmrFeature data request for gridCoverage: %s%n url=%s", coverage.getName(), f);

    long start = System.currentTimeMillis();
    try (HTTPMethod method = HTTPFactory.Get(httpClient, f.toString())) {
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(getErrorMessage(method));

      if (statusCode >= 300)
        throw new IOException(getErrorMessage(method));

      int readLen = 0;
      Header h = method.getResponseHeader("Content-Length");
      if (h != null) {
        String s = h.getValue();
        readLen = Integer.parseInt(s);
      }

      CdmrfReader cdmrfReader = new CdmrfReader(endpoint);
      InputStream is = method.getResponseAsStream();
      if (!NcStream.readAndTest(is, NcStream.MAGIC_DATACOV))
        throw new IOException("Data transfer corrupted");

      // read Data message
      int psize = NcStream.readVInt(is);
      if (psize < 0 || (psize > readLen && readLen > 0))
        throw new IOException("Data transfer corrupted");

      byte[] dp = new byte[psize];
      NcStream.readFully(is, dp);
      CdmrFeatureProto.CoverageDataResponse dproto = CdmrFeatureProto.CoverageDataResponse.parseFrom(dp);
      CoverageDataResponse dataResponse = cdmrfReader.decodeDataResponse(dproto);

      List<GeoReferencedArray> geoArrays = dataResponse.arrayResponse;
      assert geoArrays.size() == 1; // LOOK maybe need readData(List<names>) returns List<GeoArray> ?

      if (showRequest)
        System.out.printf(" took %d msecs%n", System.currentTimeMillis()-start);

      return geoArrays.get(0);
    }
  }

  public GeoReferencedArray readData(CoverageDataResponse dataResponse, GeoArrayResponse arrayResponse, InputStream is) throws IOException {
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
    if (csys == null) throw new IOException("Misformed response - no coordsys");
    return new GeoReferencedArray(arrayResponse.coverageName, arrayResponse.dataType, data, csys);
  }

  private static String getErrorMessage(HTTPMethod method) {
    String path = method.getURI().toString();
    String status = method.getStatusLine();
    String content = method.getResponseAsString();
    return (content == null) ? path+" "+status : path+" "+status +"\n "+content;
  }

  @Override
  public double[] readCoordValues(CoverageCoordAxis coordAxis) throws IOException {
    if (httpClient == null)
      httpClient = HTTPFactory.newSession(endpoint); // LOOK is this ok? no authentication...

    Formatter f = new Formatter();
    f.format("%s?req=coord&var=%s", endpoint, urlParamEscaper.escape(coordAxis.getName()));

    if (showRequest)
      System.out.printf("CdmrFeature data request for gridCoverage: %s%n url=%s", coordAxis.getName(), f);

    long start = System.currentTimeMillis();
    try (HTTPMethod method = HTTPFactory.Get(httpClient, f.toString())) {
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(getErrorMessage(method));

      if (statusCode >= 300)
        throw new IOException(getErrorMessage(method));

      InputStream is = method.getResponseAsStream();

      NcStreamReader reader = new NcStreamReader();
      NcStreamReader.DataResult result = reader.readData(is, null, endpoint);

      if (showRequest)
        System.out.printf(" took %d msecs%n", System.currentTimeMillis()-start);

      return (double []) result.data.getStorage();
    }
  }
}
