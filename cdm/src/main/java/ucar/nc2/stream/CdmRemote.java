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


import opendap.dap.DAPHeader;
import opendap.dap.DAPMethod;
import opendap.dap.DAPSession;
import org.apache.commons.httpclient.Header;

import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 * A remote CDM dataset, using ncstream to communicate.
 *
 * @author caron
 * @since Feb 7, 2009
 */
public class CdmRemote extends ucar.nc2.NetcdfFile {
  static public final String SCHEME = "cdmremote:";

  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CdmRemote.class);
  static private boolean showRequest = true;

  private DAPSession session;

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

  //////////////////////////////////////////////////////
  private String remoteURI;

  public CdmRemote() throws IOException {
    session = new DAPSession();
  }

  public CdmRemote(String _remoteURI) throws IOException {

    // get http URL
    String temp = _remoteURI;

    if (temp.startsWith(SCHEME))
      temp = temp.substring(SCHEME.length());
    if (!temp.startsWith("http:"))
      temp = "http:" + temp;

    remoteURI = temp;

    session = new DAPSession();
    DAPMethod method = null;

    // get the header
    //HttpGet method = null;
    //HttpResponse response = null;
    try {
      String url = remoteURI + "?req=header";

      if (showRequest) System.out.printf(" ncstream request %s %n", url);

    method = session.newMethod("get",url);
    int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(method.getURI() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getURI() + " " + method.getStatusLine());

      InputStream is = method.getContentStream();
      NcStreamReader reader = new NcStreamReader();
      reader.readStream(is, this);
      this.location = SCHEME + remoteURI;

    } finally {
        method.close();
    }
  }

  @Override
  protected Array readData(ucar.nc2.Variable v, Section section) throws IOException, InvalidRangeException {
    if (unlocked)
      throw new IllegalStateException("File is unlocked - cannot use");

    StringBuilder sbuff = new StringBuilder(remoteURI);
    sbuff.append("?var=");
    sbuff.append(URLEncoder.encode(v.getShortName(), "UTF-8"));
    sbuff.append("(");
    sbuff.append(section.toString());
    sbuff.append(")");

    if (showRequest)
      System.out.println("CdmRemote data request for variable: " + v.getName() + " section= " + section + " url=" + sbuff);

    DAPMethod method = null;

    try {

      method = session.newMethod("get",(sbuff.toString()));
      int statusCode = method.execute();

      if (statusCode == 404)
        throw new FileNotFoundException(method.getURI() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getURI() + " " + method.getStatusLine());

      int wantSize = (int) (v.getElementSize() * section.computeSize());
      DAPHeader h = method.getResponseHeader("Content-Length");
      if (h != null) {
        String s = h.getValue();
        int readLen = Integer.parseInt(s);
        if (readLen != wantSize)
          throw new IOException("content-length= " + readLen + " not equal expected Size= " + wantSize);
      }

      InputStream is = method.getContentStream();
      NcStreamReader reader = new NcStreamReader();
      NcStreamReader.DataResult result = reader.readData(is, this);

      assert v.getName().equals(result.varName);
      result.data.setUnsigned(v.isUnsigned());
      return result.data;

    } finally {
      if (method != null) method.close();
    }
  }

  @Override
  public void close() throws IOException {
    if (session != null) session.close();
    super.close();
  }

  public InputStream sendQuery(String remoteURI, String query) throws IOException {
    //HttpSession session = new HttpSession();

    StringBuilder sbuff = new StringBuilder(remoteURI);
    sbuff.append("?");
    sbuff.append(query);

    if (showRequest)
      System.out.println("CdmRemote sendQuery=" + sbuff);

    DAPMethod method = null;
      try {
          method = session.newMethod("get",(sbuff.toString()));
    int statusCode = method.execute();

    if (statusCode == 404)
      throw new FileNotFoundException(method.getURI() + " " + method.getStatusLine());

    if (statusCode >= 300)
      throw new IOException(method.getURI() + " " + method.getStatusLine());
      } finally {
          if(method != null) method.close();
      }
    return method.getContentStream();
  }

  public String getFileTypeId() {
    return "ncstreamRemote";
  }

  public String getFileTypeDescription() {
    return "ncstreamRemote";
  }

}
