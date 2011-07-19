/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

import ucar.nc2.util.net.HTTPException;
import ucar.nc2.util.net.HTTPMethod;
import ucar.nc2.util.net.HTTPMethodStream;
import ucar.nc2.util.net.HTTPSession;
import org.apache.commons.httpclient.Header;

import ucar.ma2.*;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.util.IO;

import java.io.*;
import java.net.URLEncoder;
import java.nio.channels.WritableByteChannel;
import java.util.Formatter;

/**
 * A remote CDM dataset, using ncstream to communicate.
 * Supports full CDM / netcdf-4 data model.
 *
 * @author caron
 * @since Feb 7, 2009
 */
public class CdmRemote extends ucar.nc2.NetcdfFile {
  static public final String SCHEME = "cdmremote:";

  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CdmRemote.class);
  static private boolean showRequest = false;

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    showRequest = debugFlag.isSet("CdmRemote/showRequest");
  }

  /**
   * Create the canonical form of the URL.
   * If the urlName starts with "http:", change it to start with "dods:", otherwise
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
      httpClient = new HTTPSession();
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

    httpClient = new HTTPSession();

    // get the header
    HTTPMethod method = null;
    try {
      String url = remoteURI + "?req=header";
      method = httpClient.newMethodGet(url);
      method.setFollowRedirects(true);
      if (showRequest) System.out.printf("CdmRemote request %s %n", url);
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(method.getURI() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getURI() + " " + method.getStatusLine());

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

  @Override
  protected Array readData(ucar.nc2.Variable v, Section section) throws IOException, InvalidRangeException {
    if (unlocked)
      throw new IllegalStateException("File is unlocked - cannot use");

    if (v.getDataType() == DataType.SEQUENCE) {
      Structure s = (Structure) v;
      StructureDataIterator siter = getStructureIterator(s, -1);
      return new ArraySequence(s.makeStructureMembers(), siter, -1);
    }

    StringBuilder sbuff = new StringBuilder(remoteURI);
    sbuff.append("?var=");
    Formatter f = new Formatter();
    f.format("%s", v.getFullNameEscaped()); // full name
    if (section != null && v.getDataType() != DataType.SEQUENCE) {
      f.format("(%s)", section.toString());
    }
    sbuff.append( URLEncoder.encode(f.toString(), "UTF-8")); // % escape entire thing varname and section

    if (showRequest)
      System.out.println(" CdmRemote data request for variable: " + v.getFullName() + " section= " + section + " url=" + sbuff);

    HTTPMethod method = null;
    try {
      method = httpClient.newMethodGet(sbuff.toString());
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

    HTTPSession session = null;
    HTTPMethod method = null;
    HTTPMethodStream hmstream = null;
    InputStream stream = null;
    int statusCode = 0;

    StringBuilder sbuff = new StringBuilder(remoteURI);
    sbuff.append("?");
    sbuff.append(query);

    if (showRequest)
      System.out.println(" CdmRemote sendQuery=" + sbuff);

    try {

      try {
        session = new HTTPSession();
        method = session.newMethodGet(sbuff.toString());
        statusCode = method.execute();
      } catch (HTTPException he) {
        throw new IOException(he);
      }

      if (statusCode == 404)
        throw new FileNotFoundException(method.getPath() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getPath() + " " + method.getStatusLine());

      stream = method.getResponseBodyAsStream();
      hmstream = new HTTPMethodStream(session, method, stream);
      return hmstream;

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
    FileOutputStream fos = new FileOutputStream(file);
    WritableByteChannel wbc = fos.getChannel();

    long size = 4;
    fos.write(NcStream.MAGIC_START);

    // header
    HTTPMethod method = null;
    try {
      // get the header
      String url = remoteURI + "?req=header";
      method = httpClient.newMethodGet(url);
      if (showRequest) System.out.printf("CdmRemote request %s %n", url);
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(method.getURI() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getURI() + " " + method.getStatusLine());

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
        method = httpClient.newMethodGet(sbuff.toString());
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
    fos.close();
  }

  @Override
  public synchronized void close() throws java.io.IOException {
    if (httpClient != null) httpClient.close();
  }

}
