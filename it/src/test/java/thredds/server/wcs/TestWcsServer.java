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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
import thredds.util.ContentType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.adapter.DtCoverageAdapter;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.IO;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Test WCS server */

@Category(NeedsCdmUnitTest.class)
public class TestWcsServer {
  static final boolean showContent = false;

  private final Namespace NS_WCS = Namespace.getNamespace("wcs", "http://www.opengis.net/wcs");

  //private String server = TestWithLocalServer.server+ "wcs/";
  //private String server2 = "http://thredds.ucar.edu/thredds/wcs/";

  //private String ncdcWcsServer = "http://eclipse.ncdc.noaa.gov:9090/thredds/wcs/";
  //private String ncdcWcsDataset = "http://eclipse.ncdc.noaa.gov:9090/thredds/wcs/gfsmon/largedomain.nc";
  //private String ncdcOpendapDataset = "http://eclipse.ncdc.noaa.gov:9090/thredds/dodsC/gfsmon/largedomain.nc";

  private String baloney = "?service=WCS&version=1.0.0&EXCEPTIONS=application/vnd.ogc.se_xml";
  private String dataset1 = "/wcs/cdmUnitTest/ncss/CONUS_80km_nc/GFS_CONUS_80km_20120419_0000.nc"+baloney;
  private String dataset2 = "/wcs/cdmUnitTest/conventions/coards/sst.mnmean.nc"+baloney;


  @Test
  public void testGetCapabilites() throws IOException, JDOMException {
    String endpoint = TestWithLocalServer.withPath(dataset1+"&request=GetCapabilities");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, ContentType.xml);

    Reader in = new StringReader( new String(result, CDM.utf8Charset));
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    //XPathExpression<Element> xpath = XPathFactory.instance().compile("ns:/WCS_Capabilities/ContentMetadata/CoverageOfferingBrief", Filters.element(), null, NS_WCS);
    XPathExpression<Element> xpath = XPathFactory.instance().compile("//wcs:CoverageOfferingBrief", Filters.element(), null, NS_WCS);
    List<Element> elements = xpath.evaluate(doc);
    for (Element emt : elements) {
        System.out.println("XPath has result: " + emt.getContent());
    }
    assertEquals(7, elements.size());

    XPathExpression<Element> xpath2 = XPathFactory.instance().compile("//wcs:CoverageOfferingBrief/wcs:name", Filters.element(), null, NS_WCS);
    List<String> names = new ArrayList<>();
    for (Element elem : xpath2.evaluate(doc)) {
      System.out.printf(" %s==%s%n", elem.getName(), elem.getValue());
      names.add(elem.getValue());
    }
    Assert.assertEquals(7, names.size());
    assert names.contains("Relative_humidity_height_above_ground");
    assert names.contains("Pressure_reduced_to_MSL");
  }

  @Test
  public void testDescribeCoverage() throws IOException {
    String endpoint = TestWithLocalServer.withPath(dataset2+"&request=DescribeCoverage&coverage=sst");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, ContentType.xml);
  }

  @Test
  public void testGetCoverageFuzzyTime() throws IOException { // no longer fails because we use closest time. LOOK is that ok?
    String endpoint = TestWithLocalServer.withPath(dataset2+"&request=GetCoverage&COVERAGE=sst&BBOX=10,0,300,80&TIME=2002-12-07T00:00:00Z&FORMAT=GeoTIFF");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, null);
  }

  @Test
  public void testGetCoverageFailBadCoverageName() throws IOException { // should fail because coverage name doesnt exist
    String endpoint = TestWithLocalServer.withPath(dataset2 + "&request=GetCoverage&COVERAGE=bad&BBOX=10,0,300,80&TIME=2002-12-07T00:00:00Z&FORMAT=GeoTIFF");
    byte[] result = TestWithLocalServer.getContent(endpoint, 400, null);
  }

  @Test
  public void testGetCoverage() throws IOException {
    String endpoint = TestWithLocalServer.withPath(dataset2+"&request=GetCoverage&COVERAGE=sst&BBOX=10,0,300,80&TIME=2002-12-01T00:00:00Z&FORMAT=GeoTIFF");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, null);
  }

  @Test
  public void testGetCoverageNetcdf() throws IOException {
    String endpoint = TestWithLocalServer.withPath(dataset2+"&request=GetCoverage&COVERAGE=sst&BBOX=10,0,300,80&TIME=2002-12-01T00:00:00Z&FORMAT=NetCDF3");
    byte[] content = TestWithLocalServer.getContent(endpoint, 200, null);

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
      System.out.printf("date = %s%n", date);
      CalendarDate expected = CalendarDate.parseISOformat(Calendar.gregorian.toString(), "2002-12-01T00:00:00Z"); // CF i guess
      Assert.assertEquals(expected.getMillis(), date.getMillis());
      Assert.assertEquals(expected.getCalendar(), date.getCalendar());
      Assert.assertEquals(expected, date);

      HorizCoordSys hcs = csys.getHorizCoordSys();
      CoverageCoordAxis1D xaxis = hcs.getXAxis();
      Assert.assertEquals(291, xaxis.getNcoords());
      Assert.assertEquals(10.5, xaxis.getCoordMidpoint(0), Misc.maxReletiveError);
      Assert.assertEquals(300.5, xaxis.getEndValue(), Misc.maxReletiveError); // LOOK is that ok? BB = 10-300: its just catching the edge
      Assert.assertEquals(1.0, xaxis.getResolution(), Misc.maxReletiveError);

      CoverageCoordAxis1D yaxis = hcs.getYAxis();
      Assert.assertEquals(81, yaxis.getNcoords());
      Assert.assertEquals(79.5, yaxis.getCoordMidpoint(0), Misc.maxReletiveError);
      Assert.assertEquals(-.5, yaxis.getEndValue(), Misc.maxReletiveError); // LOOK is that ok? BB = 0-80: its just catching the edge
      Assert.assertEquals(-1.0, yaxis.getResolution(), Misc.maxReletiveError);
    }
  }

  @Test
  public void saveGetCoverageNetcdf() throws IOException {
    String endpoint = TestWithLocalServer.withPath(dataset2 + "&request=GetCoverage&COVERAGE=sst&BBOX=10,0.01,299.99,80&TIME=2002-12-01T00:00:00Z&FORMAT=NetCDF3");

    File tempFile = File.createTempFile("tmp", ".nc", new File(TestDir.temporaryLocalDataDir));
    System.out.printf("write to %s%n", tempFile.getAbsolutePath());

    TestWithLocalServer.saveContentToFile(endpoint, 200, ContentType.netcdf, tempFile);
  }

  @Test
  public void saveGetCoverageNetcdf2() throws IOException {
    String endpoint = TestWithLocalServer.withPath(dataset1+"&request=GetCoverage&COVERAGE=Temperature&FORMAT=NetCDF3");

    File tempFile = File.createTempFile("tmp", ".nc", new File(TestDir.temporaryLocalDataDir));
    System.out.printf("write to %s%n", tempFile.getAbsolutePath());

    TestWithLocalServer.saveContentToFile(endpoint, 200, ContentType.netcdf, tempFile);
  }

  @Test
  public void multipleVertGeotiffFail() throws IOException {
    String endpoint = TestWithLocalServer.withPath(dataset1+"&request=GetCoverage&COVERAGE=Temperature&FORMAT=Geotiff");

    File tempFile = File.createTempFile("tmp", ".nc", new File(TestDir.temporaryLocalDataDir));
    System.out.printf("write to %s%n", tempFile.getAbsolutePath());

    TestWithLocalServer.getContent(endpoint, 400, null);
  }


  @Test
  public void testVert() throws IOException {
    String endpoint = TestWithLocalServer.withPath(dataset1+
            "&request=GetCoverage&COVERAGE=Temperature&TIME=2012-04-19T00:00:00Z&FORMAT=NetCDF3&vertical=800");
    byte[] content = TestWithLocalServer.getContent(endpoint, 200, null);

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
      System.out.printf("date = %s%n", date);
      Assert.assertEquals(date, CalendarDate.parseISOformat(null, "2012-04-19T00:00:00Z"));

      CoverageCoordAxis1D vert = (CoverageCoordAxis1D) csys.getZAxis();
      assertNotNull("vert", vert);
      Assert.assertEquals(1, vert.getNcoords());
      double vertCoord = vert.getCoordMidpoint(0);
      System.out.printf("date = %s%n", date);
      Assert.assertEquals(800.0, vertCoord, Misc.maxReletiveError);
    }
  }

  @org.junit.Test
  public void testFmrc() throws IOException {
    String endpoint = TestWithLocalServer.withPath("wcs/testNAMfmrc/NAM_FMRC_best.ncd");
    showGetCapabilities(endpoint);
    showDescribeCoverage(endpoint, "Precipitable_water");                   // lon,lat1,lon,lat2
    showGetCoverage(endpoint, "Precipitable_water", "2006-09-25T09:00:00Z",null,"-60,-20,0,50", "netCDF3", false);
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

    byte[] content = TestWithLocalServer.getContent(getURL, 200, null);

    // Open the binary response in memory
    if (isNetcdf) {
      try (NetcdfFile nf = NetcdfFile.openInMemory("WCS-return", content)) {
        assert nf != null;
        System.out.printf("%s%n", nf);
      }
    }
  }

  private void showRead(String url) throws IOException {
    System.out.println("****************\n");
    System.out.println(url+"\n");
    String contents = IO.readURLcontentsWithException( url);
    System.out.println(contents);
  }

}
