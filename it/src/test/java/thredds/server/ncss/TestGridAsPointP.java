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
package thredds.server.ncss;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import thredds.TestWithLocalServer;
import thredds.util.ContentType;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.NetcdfFile;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.IO;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Ncss TestGridAsPointMisc queries
 *
 * @author caron
 * @since 9/15/2015.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridAsPointP {
  static String ds1 = "ncss/grid/gribCollection/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
  static String varName1 = "Vertical_velocity_pressure_isobaric";
  static String query1 = "&latitude=37.86&longitude=-122.2&vertCoord=850"; // this particular variable has only 500, 700, 850 after forecast 120

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[]{ ds1, varName1, query1, 1, .0046999454, "2012-03-08T00:00:00Z"});
    result.add(new Object[]{ ds1, varName1, query1+"&time=all", 35, 0.08099997, "2012-02-27T06:00:00Z"});

    return result;
  }

  String ds;
  String varName;
  String query;
  Integer ntimes;
  Double dataVal;
  CalendarDate date0;

  public TestGridAsPointP(String ds, String varName, String query, Integer ntimes, Double dataVal, String date0) {
    this.ds = ds;
    this.varName = varName;
    this.query = query;
    this.ntimes = ntimes;
    this.dataVal = dataVal;
    this.date0 = CalendarDate.parseISOformat(null, date0);
  }

  @Test
  public void checkGridAsPointCsv() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath(ds + "?var=" + varName + query + "&accept=csv");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, ContentType.csv);
    Assert.assertNotNull(result);
    System.out.printf("CSV%n%s%n", new String( result, CDM.utf8Charset));
  }

  @Test
  public void checkGridAsPointXml() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath(ds + "?var=" + varName + query + "&accept=xml");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, ContentType.xml);
    Assert.assertNotNull(result);
    String xml = new String( result);

    System.out.printf("xml=%s%n", xml);
    Reader in = new StringReader(xml);
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    String xpathq = String.format("/stationFeatureCollection/stationFeature");
    System.out.printf("xpathq='%s'%n", xpathq);
    XPathExpression <Element> xpath = XPathFactory.instance().compile(xpathq, Filters.element());
    List<Element> elements = xpath.evaluate(doc);
    Assert.assertEquals((int) ntimes, elements.size());
    Element elem0 = elements.get(0);
    CalendarDate cd = CalendarDate.parseISOformat(null, elem0.getAttributeValue("date"));
    System.out.printf(" xml date=%s%n", cd);
    Assert.assertEquals(date0, cd);

    xpathq = String.format("/stationFeatureCollection/stationFeature/data[@name='%s']", varName);
    System.out.printf("xpathq='%s'%n", xpathq);
    xpath = XPathFactory.instance().compile(xpathq, Filters.element());
    elements = xpath.evaluate(doc);
    Assert.assertEquals((int) ntimes, elements.size());
    elem0 = elements.get(0);
    double val = Double.parseDouble(elem0.getContent(0).getValue());
    Assert.assertEquals(dataVal, val, Misc.maxReletiveError * dataVal);
  }

  @Test
  public void writeGridAsPointNetcdf() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath(ds+"?var="+varName+query+"&accept=netcdf");
    File tempFile = TestDir.getTempFile();
    System.out.printf(" write %sto %n  %s%n", endpoint, tempFile.getAbsolutePath());

    try (HTTPSession session = HTTPFactory.newSession(endpoint)) {
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();
      if (statusCode != 200) {
        System.out.printf("statusCode = %d '%s'%n", statusCode, method.getResponseAsString());
        return;
      }

      IO.appendToFile(method.getResponseAsStream(), tempFile.getAbsolutePath());
    }
    System.out.printf(" file length %d bytes exists=%s %n  %s%n", tempFile.length(), tempFile.exists(), tempFile.getAbsolutePath());

    // Open the result file as Station feature dataset
    Formatter errlog = new Formatter();
    try (FeatureDataset fd = FeatureDatasetFactoryManager.open(FeatureType.STATION, tempFile.getAbsolutePath(), null, errlog)) {
      assertNotNull(errlog.toString(), fd);
      VariableSimpleIF v = fd.getDataVariable(varName);
      assertNotNull(varName, v);
    }
  }

  @Test
  public void checkGridAsPointNetcdf() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath(ds+"?var="+varName+query+"&accept=netcdf");
    byte[] content = TestWithLocalServer.getContent(endpoint, 200, ContentType.netcdf);
    Assert.assertNotNull(content);
    System.out.printf("return size = %s%n", content.length);

    // Open the binary response in memory
    Formatter errlog = new Formatter();
    try (NetcdfFile nf = NetcdfFile.openInMemory("checkGridAsPointNetcdf.nc", content)) {
      // System.out.printf("%s%n", nf);
      FeatureDataset fd = FeatureDatasetFactoryManager.wrap(FeatureType.STATION, new NetcdfDataset(nf), null, errlog);
      assertNotNull(errlog.toString(), fd);
      VariableSimpleIF v = fd.getDataVariable(varName);
      assertNotNull(varName, v);
    }
  }


}
