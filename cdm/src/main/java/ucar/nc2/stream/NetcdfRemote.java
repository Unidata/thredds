/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.stream;

import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import java.io.*;
import java.util.Arrays;
import java.nio.ByteBuffer;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * @author caron
 * @since Jun 24, 2008
 */
public class NetcdfRemote extends ucar.nc2.NetcdfFile {
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

  public NetcdfRemote(String _remoteURI, CancelTask cancel) throws IOException {
    initHttpClient(); // make sure the httpClient has been set

    // canonicalize name
    if (_remoteURI.startsWith(SCHEME)) {
      this.remoteURI = "http:" + _remoteURI.substring(SCHEME.length());
      this.location = _remoteURI; // canonical name uses SCHEME
    } else if (_remoteURI.startsWith("http:")) {
      this.location = SCHEME + _remoteURI.substring(5);
      this.remoteURI = _remoteURI;
    } else {
      throw new java.net.MalformedURLException(_remoteURI + " must start with dods: or http:");
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
      open(is, cancel);

    } finally {
      if (method != null) method.releaseConnection();
    }
  }

  // for testing file URIs - cant read any data
  NetcdfRemote(String fileURI) throws IOException {
    this.location = fileURI;
    this.remoteURI = fileURI;
    InputStream in = new FileInputStream(fileURI);
    open(in, null);
  }

  private void open(InputStream in, CancelTask cancel) throws IOException {
    long start = System.nanoTime();

    XMLInputFactory myFactory = XMLInputFactory.newInstance();
    myFactory.setProperty("javax.xml.stream.isCoalescing", Boolean.TRUE);
    XMLStreamReader parser = null;

    Group parentGroup = getRootGroup();
    Variable usingVariable = null;

    int tab = 0;
    try {
      parser = myFactory.createXMLStreamReader(in);
      while (true) {
        int event = parser.next();
        
        if (event == XMLStreamConstants.END_DOCUMENT) {
          parser.close();
          break;
        }

        if (event == XMLStreamConstants.START_ELEMENT) {
          /* tab++;
          indent(tab);
          System.out.println( parser.getLocalName());
          for (int a = 0; a < parser.getAttributeCount(); a++) {
            indent(tab);
            System.out.println("  "+parser.getAttributeName(a)+"="+parser.getAttributeValue(a));  
          } */

          if (parser.getLocalName().equals("attribute")) {
            Attribute att = readAttribute(parser);
            if (usingVariable != null)
              addVariableAttribute(usingVariable, att);
            else
              addAttribute(parentGroup, att);
          }

          if (parser.getLocalName().equals("dimension")) {
            Dimension dim = readDimension(parser);
            addDimension(parentGroup, dim);
          }

          if (parser.getLocalName().equals("group")) {
            Group group = readGroup(parser, parentGroup);
            addGroup(parentGroup, group);
            parentGroup = group;
          }

          if (parser.getLocalName().equals("netcdf")) {
            readNetcdf(parser);
          }

          if (parser.getLocalName().equals("values")) {
            if (usingVariable == null) {
              logger.warn("values element not inside a variable");
              continue;
            }
            Array data = readValues(parser, usingVariable.getDataType());
            if (data != null)
              usingVariable.setCachedData(data, false);
          }

          if (parser.getLocalName().equals("variable")) {
            Variable v = readVariable(parser);
            addVariable(parentGroup, v);
            usingVariable = v;
          }

        } // START_ELEMENT

        if (event == XMLStreamConstants.END_ELEMENT) {
          tab--;
          if (parser.getLocalName().equals("group")) {
            parentGroup = parentGroup.getParentGroup(); // pop the group
          }
          if (parser.getLocalName().equals("variable")) {
            usingVariable = null;
          }
        }

        if (cancel != null && cancel.isCancel()) break;
      }

    } catch (XMLStreamException e) {
      e.printStackTrace();
      String text = parser.hasText() ? parser.getText().trim() : "";
      System.out.println("BAD  text=(" + text + ")");
    }

    finish();

    double took = .001 * .001 * .001 * (System.nanoTime() - start);
    System.out.println(" that took = " + took + "sec; ");
    System.out.println("\n"+this);
  }

  void indent(int tab) {
    for (int i = 0; i < tab; i++)
      System.out.print("  ");
  }

  private Attribute readAttribute(XMLStreamReader parser) {
    String name = null;
    String value = null;
    String typeName = null;
    String seperate = null;
    for (int a = 0; a < parser.getAttributeCount(); a++) {
      if (parser.getAttributeLocalName(a).equals("name"))
        name = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("type"))
        typeName = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("value"))
        value = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("separator"))
        seperate = parser.getAttributeValue(a);
    }
    DataType dtype = (typeName == null) ? DataType.STRING : DataType.getType(typeName);
    if (dtype == DataType.STRING)
      return new Attribute(name, value);
    else
      return new Attribute(name, convertValues(dtype, value, seperate));
  }

  private Array convertValues(DataType dtype, String values, String seperate) {
    if (seperate == null) seperate = " ";
    String[] value = values.split(seperate);
    return Array.makeArray(dtype, Arrays.asList(value));
  }

  private Dimension readDimension(XMLStreamReader parser) {
    String name = null;
    String length = null;
    boolean isUnlimited = false, isVlen = false, isShared = true;

    for (int a = 0; a < parser.getAttributeCount(); a++) {
      if (parser.getAttributeLocalName(a).equals("name"))
        name = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("length"))
        length = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("isUnlimited"))
        isUnlimited = Boolean.parseBoolean( parser.getAttributeValue(a));
      else if (parser.getAttributeLocalName(a).equals("isVariableLength"))
        isVlen = Boolean.parseBoolean( parser.getAttributeValue(a));
      else if (parser.getAttributeLocalName(a).equals("isShared"))
        isShared = Boolean.parseBoolean( parser.getAttributeValue(a));
    }

    return new Dimension(name, Integer.parseInt(length), isShared, isUnlimited, isVlen);
  }

  private Group readGroup(XMLStreamReader parser, Group parent) {
    String name = null;

    for (int a = 0; a < parser.getAttributeCount(); a++) {
      if (parser.getAttributeLocalName(a).equals("name"))
        name = parser.getAttributeValue(a);
    }

    return new Group(this, parent, name);
  }

  private void readNetcdf(XMLStreamReader parser) {
    for (int a = 0; a < parser.getAttributeCount(); a++) {
      if (parser.getAttributeLocalName(a).equals("id"))
        setId(parser.getAttributeValue(a));
      else if (parser.getAttributeLocalName(a).equals("title"))
        setTitle(parser.getAttributeValue(a));
    }
  }

  private Variable readVariable(XMLStreamReader parser) {
    String name = null;
    String dims = null;
    String typeName = null;

    for (int a = 0; a < parser.getAttributeCount(); a++) {
      if (parser.getAttributeLocalName(a).equals("name"))
        name = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("shape"))
        dims = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("type"))
        typeName = parser.getAttributeValue(a);
    }
    DataType dtype = DataType.getType(typeName);
    return new Variable(this, null, null, name, dtype, dims);
  }

  private Array readValues(XMLStreamReader parser, DataType dtype) throws XMLStreamException {
    String startS = null;
    String incrementS = null;
    String nptsS = null;
    String separator = null;
    for (int a = 0; a < parser.getAttributeCount(); a++) {
      if (parser.getAttributeLocalName(a).equals("start"))
        startS = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("increment"))
        incrementS = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("npts"))
        nptsS = parser.getAttributeValue(a);
      else if (parser.getAttributeLocalName(a).equals("separator"))
        separator = parser.getAttributeValue(a);
    }

    if ((startS != null) && (incrementS != null) && (nptsS != null)) {
      int start = Integer.parseInt(startS);
      int increment = Integer.parseInt(incrementS);
      int npts = Integer.parseInt(nptsS);
      return Array.makeArray(dtype, start, increment, npts);
    }

    String values = parser.getElementText();
    if (values != null)
      return convertValues(dtype, values, separator);

    return null;
  }

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
    new NetcdfRemote("http://localhost:8080/thredds/netcdf/stream/test/testData.nc", null);
  }
}
