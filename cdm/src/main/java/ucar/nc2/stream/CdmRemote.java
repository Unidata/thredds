/*
 * Copyright 2009-2012 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.stream;

import ucar.httpservices.*;
import org.apache.http.Header;

import ucar.ma2.*;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.util.IO;

import java.io.*;
import java.net.URLEncoder;
import java.util.Formatter;

/**
 * A remote CDM dataset (extends NetcdfFile), using cdmremote protocol to communicate.
 * Similar to Opendap in that it is a remote access protocol.
 * Supports full CDM / netcdf-4 data model.
 *
 * @author caron
 * @since Feb 7, 2009
 */
public class CdmRemote extends ucar.nc2.NetcdfFile {
  static public final String PROTOCOL = "cdmremote";
  static public final String SCHEME = PROTOCOL+":";

  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CdmRemote.class);
  static private boolean showRequest = false;

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    showRequest = debugFlag.isSet("CdmRemote/showRequest");
  }

  /**
   * Create the canonical form of the URL.
   * If the urlName starts with "http:", change it to start with "cdmremote:", otherwise
   * leave it alone.
   *
   * @param urlName the url string
   * @return canonical form
   */
  public static String canonicalURL(String urlName) {
    if (urlName.startsWith("http:"))
      return SCHEME + urlName.substring(5);
    return urlName;
  }

  /* IGNORE
  static synchronized void initHttpClient() {
    if (httpClient != null) return;
    try {
      httpClient = HTTPFactory.newSession();
    } catch (HTTPException he) {
      httpClient = null;
    }
  }
  */

  //////////////////////////////////////////////////////

  private HTTPSession httpClient;

  private final String remoteURI;

  public CdmRemote(String _remoteURI) throws IOException {
    long start = System.currentTimeMillis();

    // get http URL
    String temp = _remoteURI;
    try {
      if (temp.startsWith(SCHEME))
        temp = temp.substring(SCHEME.length());
      if (!temp.startsWith("http:"))
        temp = "http:" + temp;
    } catch (Exception e) {
    }
    remoteURI = temp;

    httpClient = HTTPFactory.newSession(remoteURI);

    // get the header
    HTTPMethod method = null;
    try {
      String url = remoteURI + "?req=header";
      method = HTTPFactory.Get(httpClient, url);
      method.setFollowRedirects(true);
      if (showRequest) System.out.printf("CdmRemote request %s %n", url);
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(method.getURL() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getURL() + " " + method.getStatusLine());

      InputStream is = method.getResponseAsStream();
      NcStreamReader reader = new NcStreamReader();
      reader.readStream(is, this);
      this.location = SCHEME + remoteURI;
    } finally {
      if (method != null) method.close();
    }
    long took = System.currentTimeMillis() - start;
    if (showRequest) System.out.printf(" took %d msecs %n", took);
  }

  public CdmRemote(InputStream is, String location ) throws IOException {
    long start = System.currentTimeMillis();
    remoteURI = location;

    try {
      NcStreamReader reader = new NcStreamReader();
      reader.readStream(is, this);
      this.location = SCHEME + remoteURI;

    } finally {
    }
    long took = System.currentTimeMillis() - start;
    if (showRequest) System.out.printf(" took %d msecs %n", took);
  }

  @Override
  protected Array readData(ucar.nc2.Variable v, Section section) throws IOException, InvalidRangeException {
    //if (unlocked)
    //  throw new IllegalStateException("File is unlocked - cannot use");

    if (v.getDataType() == DataType.SEQUENCE) {
      Structure s = (Structure) v;
      StructureDataIterator siter = getStructureIterator(s, -1);
      return new ArraySequence(s.makeStructureMembers(), siter, -1);
    }

    Formatter f = new Formatter();
    f.format("%s?var=%s", remoteURI, v.getFullNameEscaped());
    if (section != null && v.getDataType() != DataType.SEQUENCE) {
      f.format("(%s)", section.toString());
    }
    // sbuff.append( URLEncoder.encode(f.toString(), "UTF-8")); // LOOK dont % escape query; entire thing varname and section

    if (showRequest)
      System.out.println(" CdmRemote data request for variable: " + v.getFullName() + " section= " + section + " url=" + f);

    HTTPMethod method = null;
    try {
      method = HTTPFactory.Get(httpClient, f.toString());
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(method.getPath() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getPath() + " " + method.getStatusLine());

      Header h = method.getResponseHeader("Content-Length");
      if (h != null) {
        String s = h.getValue();
        int readLen = Integer.parseInt(s);
        if (showRequest)
          System.out.printf(" content-length = %d%n", readLen);
        if (v.getDataType() != DataType.SEQUENCE) {
          int wantSize = (int) (v.getElementSize() * (section == null ? v.getSize() : section.computeSize()));
          if (readLen != wantSize)
            throw new IOException("content-length= " + readLen + " not equal expected Size= " + wantSize); // LOOK
        }
      }

      InputStream is = method.getResponseAsStream();
      NcStreamReader reader = new NcStreamReader();
      NcStreamReader.DataResult result = reader.readData(is, this);

      assert v.getFullNameEscaped().equals(result.varNameFullEsc);
      result.data.setUnsigned(v.isUnsigned());
      return result.data;

    } finally {
      if (method != null) method.close();
    }
  }

  protected StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {
    try {
      InputStream is = sendQuery(remoteURI, s.getFullNameEscaped());
      NcStreamReader reader = new NcStreamReader();
      return reader.getStructureIterator(is, this);

    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  public static InputStream sendQuery(String remoteURI, String query) throws IOException {
    long start = System.currentTimeMillis();

    HTTPSession session = null;
    HTTPMethod method = null;
    InputStream stream = null;
    int statusCode = 0;

    StringBuilder sbuff = new StringBuilder(remoteURI);
    sbuff.append("?");
    sbuff.append(query);

    if (showRequest)
      System.out.printf(" CdmRemote sendQuery= %s", sbuff);

    try {

      try {
        session = HTTPFactory.newSession(sbuff.toString());
        method = HTTPFactory.Get(session);
        statusCode = method.execute();
      } catch (HTTPException he) {
        throw new IOException(he);
      }

      if (statusCode == 404)
        throw new FileNotFoundException(method.getPath() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getPath() + " " + method.getStatusLine());

      stream = method.getResponseBodyAsStream();
      if (showRequest) System.out.printf(" took %d msecs %n", System.currentTimeMillis() - start);
      return stream;

    } catch (IOException ioe) {
      if (session != null) session.close();
      throw ioe;
    }
  }

  @Override
  public String getFileTypeId() {
    return "ncstreamRemote";
  }

  @Override
  public String getFileTypeDescription() {
    return "ncstreamRemote";
  }

  public void writeToFile(String filename) throws IOException {
    File file = new File(filename);
    try (FileOutputStream fos = new FileOutputStream(file)) {
      // WritableByteChannel wbc = fos.getChannel();

      long size = 4;
      fos.write(NcStream.MAGIC_START);

      // header
      HTTPMethod method = null;
      try {
        // get the header
        String url = remoteURI + "?req=header";
        method = HTTPFactory.Get(httpClient, url);
        if (showRequest) System.out.printf("CdmRemote request %s %n", url);
        int statusCode = method.execute();

        if (statusCode == 404)
          throw new FileNotFoundException(method.getURL() + " " + method.getStatusLine());

        if (statusCode >= 300)
          throw new IOException(method.getURL() + " " + method.getStatusLine());

        InputStream is = method.getResponseBodyAsStream();
        size += IO.copyB(is, fos, IO.default_socket_buffersize);

      } finally {
        if (method != null) method.close();
      }

      for (Variable v : getVariables()) {
        StringBuilder sbuff = new StringBuilder(remoteURI);
        sbuff.append("?var=");
        sbuff.append(URLEncoder.encode(v.getShortName(), "UTF-8"));

        if (showRequest)
          System.out.println(" CdmRemote data request for variable: " + v.getFullName() + " url=" + sbuff);

        try {
          method = HTTPFactory.Get(httpClient, sbuff.toString());
          int statusCode = method.execute();

          if (statusCode == 404)
            throw new FileNotFoundException(method.getPath() + " " + method.getStatusLine());

          if (statusCode >= 300)
            throw new IOException(method.getPath() + " " + method.getStatusLine());

          int wantSize = (int) (v.getSize());
          Header h = method.getResponseHeader("Content-Length");
          if (h != null) {
            String s = h.getValue();
            int readLen = Integer.parseInt(s);
            if (readLen != wantSize)
              throw new IOException("content-length= " + readLen + " not equal expected Size= " + wantSize);
          }

          InputStream is = method.getResponseBodyAsStream();
          size += IO.copyB(is, fos, IO.default_socket_buffersize);

        } finally {
          if (method != null) method.close();
        }
      }

      fos.flush();
    }
  }

  @Override
  public synchronized void close() throws java.io.IOException {
    if (httpClient != null) httpClient.close();
  }

}
