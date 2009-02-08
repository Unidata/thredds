/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import ucar.nc2.util.CancelTask;

import ucar.nc2.*;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A remote NetcdfFile, using nc stream to communicate
 * @author caron
 * @since Feb 7, 2009
 */
public class NcStreamRemote extends ucar.nc2.NetcdfFile {
  static public final String SCHEME = "ncstream:";
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NetcdfRemote.class);
  static private HttpClient httpClient;

  /**
   * Set the HttpClient object - a single, shared instance is used within the application.
   * @param client the HttpClient object
   */
  static public void setHttpClient(HttpClient client) {
    httpClient = client;
  }

  private synchronized void initHttpClient() {
    if (httpClient != null) return;
    MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    httpClient = new HttpClient(connectionManager);
  }

  //////////////////////////////////////////////////////
  private final String remoteURI;

  public NcStreamRemote(String _remoteURI, CancelTask cancel) throws IOException {
    initHttpClient(); // make sure the httpClient has been set

    // canonicalize name
    if (_remoteURI.startsWith(SCHEME)) {
      this.remoteURI = "http:" + _remoteURI.substring(SCHEME.length());
      this.location = _remoteURI; // canonical name uses SCHEME
    } else if (_remoteURI.startsWith("http:")) {
      this.location = SCHEME + _remoteURI.substring(5);
      this.remoteURI = _remoteURI;
    } else {
      throw new java.net.MalformedURLException(_remoteURI + " must start with "+SCHEME+" or http:");
    }

    HttpMethod method = null;
    try {
      method = new GetMethod(remoteURI);
      method.setFollowRedirects(true);
      int statusCode = httpClient.executeMethod(method);

      if (statusCode == 404)
        throw new FileNotFoundException(method.getPath() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getPath() + " " + method.getStatusLine());

      InputStream is = method.getResponseBodyAsStream();
      NcStreamReader reader = new NcStreamReader();
      reader.readStream(is, this);

    } finally {
      if (method != null) method.releaseConnection();
    }
  }

  /* for testing file URIs - cant read any data
  NcStreamRemote(String fileURI) throws IOException {
    this.location = fileURI;
    this.remoteURI = fileURI;
    InputStream in = new FileInputStream(fileURI);
    readStream(in);
  }

  private void readStream(InputStream is) throws IOException {
    assert readAndTest(is, NcStream.MAGIC_HEADER);

    int msize = readVInt(is);
    System.out.println("READ header len= " + msize);

    byte[] m = new byte[msize];
    is.read(m);
    NcStreamProto.Stream proto = NcStreamProto.Stream.parseFrom(m);
    proto2nc(proto);

    // LOOK why doesnt this work ?
    //CodedInputStream cis = CodedInputStream.newInstance(is);
    //cis.setSizeLimit(msize);
    //NcStreamProto.Stream proto = NcStreamProto.Stream.parseFrom(cis);

    while (is.available() > 0) {
      assert readAndTest(is, NcStream.MAGIC_DATA);

      int psize = readVInt(is);
      System.out.println(" dproto len= " + psize);
      byte[] dp = new byte[psize];
      is.read(dp);
      NcStreamProto.Data dproto = NcStreamProto.Data.parseFrom(dp);
      System.out.println(" dproto = " + dproto);

      int dsize = readVInt(is);
      System.out.println(" data len= " + dsize);
      is.skip(dsize);
    }

  }

  private void proto2nc(NcStreamProto.Stream proto) throws InvalidProtocolBufferException {
    setLocation(proto.getName());

    NcStreamProto.Group root = proto.getRoot();

    for (NcStreamProto.Dimension dim : root.getDimsList()) {
      addDimension(null, NcStream.makeDim(dim));
    }

    for (NcStreamProto.Attribute att : root.getAttsList()) {
      addAttribute(null, NcStream.makeAtt(att));
    }

    for (NcStreamProto.Variable var : root.getVarsList()) {
      addVariable(null, NcStream.makeVar(this, var));
    }
  }

  private int readVInt(InputStream is) throws IOException {
    byte b = (byte) is.read();
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      b = (byte) is.read();
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  private boolean readAndTest(InputStream is, byte[] test) throws IOException {
    byte[] b = new byte[test.length];
    is.read(b);

    if (b.length != test.length) return false;
    for (int i = 0; i < b.length; i++)
      if (b[i] != test[i]) return false;
    return true;
  }   */

  @Override
  protected Array readData(ucar.nc2.Variable v, Section section) throws IOException, InvalidRangeException {

    StringBuilder sbuff = new StringBuilder(remoteURI);
    sbuff.append("?");
    sbuff.append(v.getShortName());
    sbuff.append("(");
    sbuff.append(section.toString());
    sbuff.append(")");

    if (showRequest)
      System.out.println("NetcdfRemote data request for variable: "+v.getName()+" section= "+section+ " url="+sbuff);

    HttpMethod method = null;
    try {
      method = new GetMethod(sbuff.toString());
      method.setFollowRedirects(true);
      int statusCode = httpClient.executeMethod(method);

      if (statusCode == 404)
        throw new FileNotFoundException(method.getPath() + " " + method.getStatusLine());

      if (statusCode >= 300)
        throw new IOException(method.getPath() + " " + method.getStatusLine());

      int wantSize = (int) (v.getElementSize() * section.computeSize());
      byte[] result = new byte[wantSize];

      Header h = method.getResponseHeader("Content-Length");
      if (h != null) {
        String s = h.getValue();
        int readLen = Integer.parseInt(s);
        if (readLen != wantSize)
          throw new IOException("content-length= "+readLen+" not equal expected Size= "+wantSize);
      }

      InputStream is = method.getResponseBodyAsStream();
      int actualRead = copy(is, result, 0, wantSize);
      if (actualRead != wantSize)
        throw new IOException("actualRead="+actualRead+" not equal expected Size= "+wantSize);

      return convert(result, v.getDataType(), section);

    } finally {
      if (method != null) method.releaseConnection();
    }
  }

  private int copy(InputStream in, byte[] buff, int offset, int want) throws IOException {
    int done = 0;
    while (want > 0) {
      int bytesRead = in.read(buff, offset + done, want);
      if (bytesRead == -1) break;
      done += bytesRead;
      want -= bytesRead;
    }
    return done;
  }

  // LOOK - should make Array that wraps a ByteBuffer, to avoid extra copy
  private Array convert(byte[] result, DataType dt, Section section) {
    int[] shape = section.getShape();
    if (dt == DataType.BYTE)
      return Array.factory(dt.getPrimitiveClassType(), shape, result);

    ByteBuffer bb = ByteBuffer.wrap(result);
    int n = (int) section.computeSize();

    if (dt == DataType.SHORT) {
      short[] pa = new short[n];
      for (int i=0; i<n; i++)
        pa[i] = bb.getShort();
      return Array.factory(dt.getPrimitiveClassType(), shape, pa);

    } else if (dt == DataType.INT) {
      int[] pa = new int[n];
      for (int i=0; i<n; i++)
        pa[i] = bb.getInt();
      return Array.factory(dt.getPrimitiveClassType(), shape, pa);

    } else if (dt == DataType.LONG) {
      long[] pa = new long[n];
      for (int i=0; i<n; i++)
        pa[i] = bb.getLong();
      return Array.factory(dt.getPrimitiveClassType(), shape, pa);

    } else if (dt == DataType.FLOAT) {
      float[] pa = new float[n];
      for (int i=0; i<n; i++)
        pa[i] = bb.getFloat();
      return Array.factory(dt.getPrimitiveClassType(), shape, pa);

    } else if (dt == DataType.DOUBLE) {
      double[] pa = new double[n];
      for (int i=0; i<n; i++)
        pa[i] = bb.getDouble();
      return Array.factory(dt.getPrimitiveClassType(), shape, pa);

    } else if (dt == DataType.CHAR) {
      char[] pa = new char[n];
      for (int i=0; i<n; i++)
        pa[i] = (char) bb.get();
      return Array.factory(dt.getPrimitiveClassType(), shape, pa);
    }

    throw new IllegalStateException("unimplmeneted datatype = "+dt);
  }

  static public void main( String args[]) throws IOException {
    new NcStreamRemote("http://localhost:8080/thredds/netcdf/stream/test/testData.nc", null);
  }
}
