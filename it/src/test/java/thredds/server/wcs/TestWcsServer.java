/*
 * (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 */
package thredds.server.wcs;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import thredds.TestOnLocalServer;
import thredds.util.ContentType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageCoordAxis1D;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.HorizCoordSys;
import ucar.nc2.ft2.coverage.adapter.DtCoverageAdapter;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Test WCS server */

@Category(NeedsCdmUnitTest.class)
public class TestWcsServer {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
  static final boolean showContent = false;

  private final Namespace NS_WCS = Namespace.getNamespace("wcs", "http://www.opengis.net/wcs");

  //private String server = TestOnLocalServer.server+ "wcs/";
  //private String server2 = "http://thredds.ucar.edu/thredds/wcs/";

  //private String ncdcWcsServer = "http://eclipse.ncdc.noaa.gov:9090/thredds/wcs/";
  //private String ncdcWcsDataset = "http://eclipse.ncdc.noaa.gov:9090/thredds/wcs/gfsmon/largedomain.nc";
  //private String ncdcOpendapDataset = "http://eclipse.ncdc.noaa.gov:9090/thredds/dodsC/gfsmon/largedomain.nc";

  private String baloney = "?service=WCS&version=1.0.0&EXCEPTIONS=application/vnd.ogc.se_xml";
  private String dataset1 = "/wcs/cdmUnitTest/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120419_0000.nc"+baloney;
  private String dataset2 = "/wcs/cdmUnitTest/conventions/coards/sst.mnmean.nc"+baloney;

  @Test
  public void testGetCapabilites() throws IOException, JDOMException {
    String endpoint = TestOnLocalServer.withHttpPath(dataset1+"&request=GetCapabilities");
    byte[] result = TestOnLocalServer.getContent(endpoint, 200, ContentType.xml);

    Reader in = new StringReader( new String(result, CDM.utf8Charset));
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    //XPathExpression<Element> xpath = XPathFactory.instance().compile("ns:/WCS_Capabilities/ContentMetadata/CoverageOfferingBrief", Filters.element(), null, NS_WCS);
    XPathExpression<Element> xpath = XPathFactory.instance().compile("//wcs:CoverageOfferingBrief", Filters.element(), null, NS_WCS);
    List<Element> elements = xpath.evaluate(doc);
    for (Element emt : elements) {
        logger.debug("XPath has result: {}", emt.getContent());
    }
    assertEquals(7, elements.size());

    XPathExpression<Element> xpath2 = XPathFactory.instance().compile("//wcs:CoverageOfferingBrief/wcs:name", Filters.element(), null, NS_WCS);
    List<String> names = new ArrayList<>();
    for (Element elem : xpath2.evaluate(doc)) {
      logger.debug(" {}=={}", elem.getName(), elem.getValue());
      names.add(elem.getValue());
    }
    Assert.assertEquals(7, names.size());
    assert names.contains("Relative_humidity_height_above_ground");
    assert names.contains("Pressure_reduced_to_MSL");
  }

  @Test
  public void testDescribeCoverage() throws IOException {
    String endpoint = TestOnLocalServer.withHttpPath(dataset2+"&request=DescribeCoverage&coverage=sst");
    byte[] result = TestOnLocalServer.getContent(endpoint, 200, ContentType.xml);
  }

  @Test
  public void testGetCoverageFuzzyTime() throws IOException { // no longer fails because we use closest time. LOOK is that ok?
    String endpoint = TestOnLocalServer.withHttpPath(dataset2+"&request=GetCoverage&COVERAGE=sst&BBOX=10,0,300,80&TIME=2002-12-07T00:00:00Z&FORMAT=GeoTIFF");
    byte[] result = TestOnLocalServer.getContent(endpoint, 200, null);
  }

  @Test
  public void testGetCoverageFailBadCoverageName() throws IOException { // should fail because coverage name doesnt exist
    String endpoint = TestOnLocalServer.withHttpPath(dataset2 + "&request=GetCoverage&COVERAGE=bad&BBOX=10,0,300,80&TIME=2002-12-07T00:00:00Z&FORMAT=GeoTIFF");
    byte[] result = TestOnLocalServer.getContent(endpoint, 400, null);
  }

  @Test
  public void testGetCoverage() throws IOException {
    String endpoint = TestOnLocalServer.withHttpPath(dataset2+"&request=GetCoverage&COVERAGE=sst&BBOX=10,0,300,80&TIME=2002-12-01T00:00:00Z&FORMAT=GeoTIFF");
    byte[] result = TestOnLocalServer.getContent(endpoint, 200, null);
  }

  @Test
  public void testGetCoverageNetcdf() throws IOException {
    String endpoint = TestOnLocalServer.withHttpPath(dataset2+"&request=GetCoverage&COVERAGE=sst&BBOX=10,0,300,80&TIME=2002-12-01T00:00:00Z&FORMAT=NetCDF3");
    byte[] content = TestOnLocalServer.getContent(endpoint, 200, null);

    // Open the binary response in memory
    try (NetcdfFile nf = NetcdfFile.openInMemory("test_data.nc", content)) {
      DtCoverageDataset dt = new DtCoverageDataset(new NetcdfDataset(nf), null);
      Formatter errlog = new Formatter();
      FeatureDatasetCoverage cdc = DtCoverageAdapter.factory(dt, errlog);
      Assert.assertNotNull(errlog.toString(), cdc);
      Assert.assertEquals(1, cdc.getCoverageCollections().size());
      CoverageCollection cd = cdc.getCoverageCollections().get(0);
      assertNotNull(cd);

      Coverage cov = cd.findCoverage("sst");
      assertNotNull("sst", cov);

      CoverageCoordSys csys = cov.getCoordSys();
      assertNotNull("csys", csys);

      CoverageCoordAxis1D time = (CoverageCoordAxis1D) csys.getAxis(AxisType.Time);
      assertNotNull("time", time);
      Assert.assertEquals(1, time.getNcoords());
      CalendarDate date = time.makeDate( time.getCoordMidpoint(0));
      logger.debug("date = {}", date);
      CalendarDate expected = CalendarDate.parseISOformat(Calendar.gregorian.toString(), "2002-12-01T00:00:00Z"); // CF i guess
      Assert.assertEquals(expected.getMillis(), date.getMillis());
      Assert.assertEquals(expected.getCalendar(), date.getCalendar());
      Assert.assertEquals(expected, date);

      HorizCoordSys hcs = csys.getHorizCoordSys();
      CoverageCoordAxis1D xaxis = hcs.getXAxis();
      Assert.assertEquals(291, xaxis.getNcoords());
      Assert2.assertNearlyEquals(10.5, xaxis.getCoordMidpoint(0));
      Assert2.assertNearlyEquals(300.5, xaxis.getEndValue()); // LOOK is that ok? BB = 10-300: its just catching the edge
      Assert2.assertNearlyEquals(1.0, xaxis.getResolution());

      CoverageCoordAxis1D yaxis = hcs.getYAxis();
      Assert.assertEquals(81, yaxis.getNcoords());
      Assert2.assertNearlyEquals(79.5, yaxis.getCoordMidpoint(0));
      Assert2.assertNearlyEquals(-.5, yaxis.getEndValue()); // LOOK is that ok? BB = 0-80: its just catching the edge
      Assert2.assertNearlyEquals(-1.0, yaxis.getResolution());
    }
  }

  @Test
  public void saveGetCoverageNetcdf() throws IOException {
    String endpoint = TestOnLocalServer.withHttpPath(dataset2 + "&request=GetCoverage&COVERAGE=sst&BBOX=10,0.01,299.99,80&TIME=2002-12-01T00:00:00Z&FORMAT=NetCDF3");

    File tempFile = tempFolder.newFile();
    logger.debug("write to {}", tempFile.getAbsolutePath());

    TestOnLocalServer.saveContentToFile(endpoint, 200, ContentType.netcdf, tempFile);
  }

  @Test
  public void saveGetCoverageNetcdf2() throws IOException {
    String endpoint = TestOnLocalServer.withHttpPath(dataset1+"&request=GetCoverage&COVERAGE=Temperature&FORMAT=NetCDF3");

    File tempFile = tempFolder.newFile();
    logger.debug("write to {}", tempFile.getAbsolutePath());

    TestOnLocalServer.saveContentToFile(endpoint, 200, ContentType.netcdf, tempFile);
  }

  @Test
  public void multipleVertGeotiffFail() throws IOException {
    String endpoint = TestOnLocalServer.withHttpPath(dataset1+"&request=GetCoverage&COVERAGE=Temperature&FORMAT=Geotiff");

    File tempFile = tempFolder.newFile();
    logger.debug("write to {}", tempFile.getAbsolutePath());

    TestOnLocalServer.getContent(endpoint, 400, null);
  }


  @Test
  public void testVert() throws IOException {
    String endpoint = TestOnLocalServer.withHttpPath(dataset1+
            "&request=GetCoverage&COVERAGE=Temperature&TIME=2012-04-19T00:00:00Z&FORMAT=NetCDF3&vertical=800");
    byte[] content = TestOnLocalServer.getContent(endpoint, 200, null);

    // Open the binary response in memory
    try (NetcdfFile nf = NetcdfFile.openInMemory("test_data.nc", content)) {
      DtCoverageDataset dt = new DtCoverageDataset(new NetcdfDataset(nf), null);
      Formatter errlog = new Formatter();
      FeatureDatasetCoverage cdc = DtCoverageAdapter.factory(dt, errlog);
      Assert.assertNotNull(errlog.toString(), cdc);
      Assert.assertEquals(1, cdc.getCoverageCollections().size());
      CoverageCollection cd = cdc.getCoverageCollections().get(0);
      assertNotNull(cd);

      Coverage cov = cd.findCoverage("Temperature");
      assertNotNull("Temperature", cov);

      CoverageCoordSys csys = cov.getCoordSys();
      assertNotNull("csys", csys);

      CoverageCoordAxis1D time = (CoverageCoordAxis1D) csys.getAxis(AxisType.Time);
      assertNotNull("time", time);
      Assert.assertEquals(1, time.getNcoords());
      CalendarDate date = time.makeDate(time.getCoordMidpoint(0));
      logger.debug("date = {}", date);
      Assert.assertEquals(date, CalendarDate.parseISOformat(null, "2012-04-19T00:00:00Z"));

      CoverageCoordAxis1D vert = (CoverageCoordAxis1D) csys.getZAxis();
      assertNotNull("vert", vert);
      Assert.assertEquals(1, vert.getNcoords());
      double vertCoord = vert.getCoordMidpoint(0);
      logger.debug("date = {}", date);
      Assert2.assertNearlyEquals(800.0, vertCoord);
    }
  }

  @org.junit.Test
  public void testFmrc() throws IOException {
    String endpoint = TestOnLocalServer.withHttpPath("wcs/testNAMfmrc/NAM_FMRC_best.ncd");
    showGetCapabilities(endpoint);
    showDescribeCoverage(endpoint, "Precipitable_water");                   // lon,lat1,lon,lat2
    showGetCoverage(endpoint, "Precipitable_water", "2006-09-25T09:00:00Z",null,"-60,-20,0,50", "netCDF3", false);
  }

  @Test
  public void testCatalogNcml() throws IOException, JDOMException {
    // test for opening NcML written in the catalog.
    // https://www.unidata.ucar.edu/mailing_lists/archives/thredds/2018/msg00003.html
    String endpoint = TestOnLocalServer.withHttpPath("wcs/ExampleNcML/Agg.nc");
    assert(isGetCoverageWcsDoc(endpoint));
  }

  ////////////////////////////////////////////////////////////////

  private void showGetCapabilities(String url) throws IOException {
    showRead(url+baloney+"&request=GetCapabilities");
  }

  private void showDescribeCoverage(String url, String grid) throws IOException {
    showRead(url+baloney+"&request=DescribeCoverage&coverage="+grid);
  }

  // bb = minx,miny,maxx,maxy
  private void showGetCoverage(String url, String grid, String time, String vert, String bb, String format, boolean showOnly) throws IOException {
    String getURL = url + baloney + "&request=GetCoverage&coverage=" + grid;
    boolean isNetcdf = format.equalsIgnoreCase("netCDF3");
    getURL = getURL + "&format=" + format;
    if (time != null)
      getURL = getURL + "&time=" + time;
    if (vert != null)
      getURL = getURL + "&vertical=" + vert;
    if (bb != null)
      getURL = getURL + "&bbox=" + bb;

    byte[] content = TestOnLocalServer.getContent(getURL, 200, null);

    // Open the binary response in memory
    if (isNetcdf) {
      try (NetcdfFile nf = NetcdfFile.openInMemory("WCS-return", content)) {
        assert nf != null;
        logger.debug("{}", nf);
      }
    }
  }

  private boolean isGetCoverageWcsDoc(String url) throws JDOMException, IOException {
    byte[] result = TestOnLocalServer.getContent(url+baloney+"&request=GetCapabilities", 200, ContentType.xml);
    Reader in = new StringReader( new String(result, CDM.utf8Charset));
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    boolean isName =  doc.getRootElement().getName().equals("WCS_Capabilities");
    boolean isNamespace = doc.getRootElement().getNamespaceURI().equals(NS_WCS.getURI());
    return (isName && isNamespace);
  }

  private void showRead(String url) throws IOException {
    logger.debug("****************");
    logger.debug(url);
    String contents = IO.readURLcontentsWithException( url);
    logger.debug(contents);
  }
}
