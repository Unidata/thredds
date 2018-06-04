/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
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
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
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
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

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
  public void checkGridAsPointCsv() {
    String endpoint = TestOnLocalServer.withHttpPath(ds + "?var=" + varName + query + "&accept=csv");
    byte[] result = TestOnLocalServer.getContent(endpoint, 200, ContentType.csv);
    Assert.assertNotNull(result);
    logger.debug("CSV\n{}", new String( result, CDM.utf8Charset));
  }

  @Test
  public void checkGridAsPointXml() throws JDOMException, IOException {
    String endpoint = TestOnLocalServer.withHttpPath(ds + "?var=" + varName + query + "&accept=xml");
    byte[] result = TestOnLocalServer.getContent(endpoint, 200, ContentType.xml);
    Assert.assertNotNull(result);
    String xml = new String( result);

    logger.debug("xml={}", xml);
    Reader in = new StringReader(xml);
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    String xpathq = String.format("/stationFeatureCollection/stationFeature");
    logger.debug("xpathq='{}'", xpathq);
    XPathExpression <Element> xpath = XPathFactory.instance().compile(xpathq, Filters.element());
    List<Element> elements = xpath.evaluate(doc);
    Assert.assertEquals((int) ntimes, elements.size());
    Element elem0 = elements.get(0);
    CalendarDate cd = CalendarDate.parseISOformat(null, elem0.getAttributeValue("date"));
    logger.debug(" xml date={}", cd);
    Assert.assertEquals(date0, cd);

    xpathq = String.format("/stationFeatureCollection/stationFeature/data[@name='%s']", varName);
    logger.debug("xpathq='{}'", xpathq);
    xpath = XPathFactory.instance().compile(xpathq, Filters.element());
    elements = xpath.evaluate(doc);
    Assert.assertEquals((int) ntimes, elements.size());
    elem0 = elements.get(0);
    double val = Double.parseDouble(elem0.getContent(0).getValue());
    Assert2.assertNearlyEquals(dataVal, val);
  }

  @Test
  public void writeGridAsPointNetcdf() throws JDOMException, IOException {
    String endpoint = TestOnLocalServer.withHttpPath(ds+"?var="+varName+query+"&accept=netcdf");
    File tempFile = tempFolder.newFile();
    logger.debug(" write {} to {}", endpoint, tempFile.getAbsolutePath());

    try (HTTPSession session = HTTPFactory.newSession(endpoint)) {
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();
      if (statusCode != 200) {
        logger.debug("statusCode = {} '{}'", statusCode, method.getResponseAsString());
        return;
      }

      IO.appendToFile(method.getResponseAsStream(), tempFile.getAbsolutePath());
    }
    logger.debug(" file length {} bytes exists={} {}", tempFile.length(), tempFile.exists(), tempFile.getAbsolutePath());

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
    String endpoint = TestOnLocalServer.withHttpPath(ds+"?var="+varName+query+"&accept=netcdf");
    byte[] content = TestOnLocalServer.getContent(endpoint, 200, ContentType.netcdf);
    Assert.assertNotNull(content);
    logger.debug("return size = {}", content.length);

    // Open the binary response in memory
    Formatter errlog = new Formatter();
    try (NetcdfFile nf = NetcdfFile.openInMemory("checkGridAsPointNetcdf.nc", content)) {
      FeatureDataset fd = FeatureDatasetFactoryManager.wrap(FeatureType.STATION, new NetcdfDataset(nf), null, errlog);
      assertNotNull(errlog.toString(), fd);
      VariableSimpleIF v = fd.getDataVariable(varName);
      assertNotNull(varName, v);
    }
  }
}
