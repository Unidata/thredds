package thredds.server.services;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPath;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.TestWithLocalServer;
import thredds.util.ContentType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Consistency of calendar dates across services: ncss, wms, wcs
 */

@Category(NeedsCdmUnitTest.class)
public class ConsistentDatesTest {
  private static final boolean show = true;

  private final String[] expectedDateTime = {
           "0000-01-16T06:00:00Z",       // these are the actual dates from cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc
           "0000-02-15T16:29:06Z",       // this uses CF, so must use the non-standard calendar gregorian instead of proleptic_gregorian
           "0000-03-17T02:58:12Z",
           "0000-04-16T13:27:18Z",
           "0000-05-16T23:56:24Z",
           "0000-06-16T10:25:30Z",
           "0000-07-16T20:54:36Z",
           "0000-08-16T07:23:42Z",
           "0000-09-15T17:52:48Z",
           "0000-10-16T04:21:54Z",
           "0000-11-15T14:51:00Z",
           "0000-12-16T01:20:06Z"};

  private final List<String> expectedDatesAsDateTime = Collections.unmodifiableList(Arrays.asList(expectedDateTime));

  @Test
  @Ignore("WMS not working")
  public void checkWMSDates() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath("/wms/cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?service=WMS&version=1.3.0&request=GetCapabilities");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, ContentType.xml);
    Reader in = new StringReader( new String(result, CDM.utf8Charset));
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    if (show) {
      XMLOutputter fmt = new XMLOutputter(Format.getPrettyFormat());
      fmt.output(doc, System.out);
    }

    XPath xPath = XPath.newInstance("//wms:Dimension");
    xPath.addNamespace("wms", doc.getRootElement().getNamespaceURI());
    Element dimNode = (Element) xPath.selectSingleNode(doc);
    //List<String> content = Arrays.asList(dimNode.getText().trim().split(","));
    List<String> content = new ArrayList<>();
    for (String d : Arrays.asList(dimNode.getText().trim().split(","))) {
      // System.out.printf("Date= %s%n", d);
      CalendarDate cd = CalendarDate.parseISOformat(null, d);
      content.add(cd.toString());
    }

    assertEquals(expectedDatesAsDateTime, content);
  }

  @Test
  public void checkWCSDates() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath("/wcs/cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?service=WCS&version=1.0.0&request=DescribeCoverage&coverage=sst");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, ContentType.xml);
    Reader in = new StringReader( new String(result, CDM.utf8Charset));
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    Namespace wcs = Namespace.getNamespace("wcs", doc.getRootElement().getNamespaceURI());
    Namespace gml = Namespace.getNamespace("gml", "http://www.opengis.net/gml");
    XPathExpression<Element> xpath =
            XPathFactory.instance().compile("//wcs:temporalDomain/gml:timePosition", Filters.element(),
                    null, wcs, gml);
    List<Element> timePositionNodes = xpath.evaluate(doc);

    List<String> timePositionDateTime = new ArrayList<>();
    for (Element e : timePositionNodes) {
      System.out.printf("Date= %s%n", e.getText());
      CalendarDate cd = CalendarDate.parseISOformat(null, e.getText());
      timePositionDateTime.add(cd.toString());
    }

    assertEquals(expectedDatesAsDateTime, timePositionDateTime);
  }

  @Test
  public void checkNCSSDates() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath("/ncss/grid/cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?var=sst&latitude=45&longitude=-20&temporal=all&accept=xml");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, ContentType.xml);
    String results = new String(result, CDM.utf8Charset);
    if (show) System.out.printf("checkNCSSDates%n%s%n", results);
    Reader in = new StringReader( results );
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    XPathExpression<Element> xpath =
            XPathFactory.instance().compile("/stationFeatureCollection/stationFeature", Filters.element());
    List<Element> dataTimeNodes = xpath.evaluate(doc);

    List<String> timePositionDateTime = new ArrayList<>();
    for (Element e : dataTimeNodes) {
      CalendarDate cd = CalendarDate.parseISOformat(null, e.getAttributeValue("date"));
      System.out.printf(" extract date= %s%n", cd);
      timePositionDateTime.add(cd.toString());;
    }

    assertEquals(expectedDatesAsDateTime, timePositionDateTime);
  }

  // PF5_SST_Climatology:  :units = "hour since 0000-01-01 00:00:00";
  @Test
  public void checkNCSSDatesInNetcdf() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath("/ncss/grid/cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?var=sst&latitude=45&longitude=-20&temporal=all&accept=netcdf");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, ContentType.netcdf);
    NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", result);
    NetcdfDataset ds = new NetcdfDataset(nf);

    CoordinateAxis1D tAxis = (CoordinateAxis1D) ds.findCoordinateAxis("time");
    Attribute calendarAtt = tAxis.findAttribute(CF.CALENDAR);
    Calendar calendar;
    if (calendarAtt == null) {
      calendar = Calendar.getDefault();
    } else {
      calendar = Calendar.get(calendarAtt.getStringValue());
    }
    Attribute units = tAxis.findAttribute(CDM.UNITS);
    double[] values = tAxis.getCoordValues();

    System.out.printf("actual%n");
    List<String> ccdd = new ArrayList<>();
    CalendarDateUnit dateUnit = CalendarDateUnit.withCalendar(calendar, units.getStringValue());
    for (double value : values) {
      CalendarDate cd = dateUnit.makeCalendarDate(value);
      System.out.printf(" \"%s\",%n", cd);
      ccdd.add(cd.toString());
    }

    assertEquals(expectedDatesAsDateTime, ccdd);

    //FAIL!!! ???
    //CoordinateAxis1DTime tAxis2 = CoordinateAxis1DTime.factory(ds, ds.findCoordinateAxis("time") , null);
    //assertTrue(tAxis2.getCalendarDates().equals(expectedDatesAsDateTime));
  }


  /*  pr_HRM3_2038-2070.CO.nc:

    double time(time=95040);
      :units = "days since 2038-01-01 00:00:00";
      :standard_name = "time";
      :long_name = "time";
      :calendar = "360_day";
      :bounds = "time_bnds";
   */
  @Test
  public void checkNCSSDatesInNetcdfWithCalendars() throws JDOMException, IOException {
    //Dates for noleap calendar
    CalendarDate[] expectedCalendarDates = {
          CalendarDate.parseISOformat(Calendar.uniform30day.toString(), "2038-01-01T03:00:00Z"),
          CalendarDate.parseISOformat(Calendar.uniform30day.toString(), "2038-01-01T06:00:00Z"),
          CalendarDate.parseISOformat(Calendar.uniform30day.toString(), "2038-01-01T09:00:00Z"),
          CalendarDate.parseISOformat(Calendar.uniform30day.toString(), "2038-01-01T12:00:00Z"),
          CalendarDate.parseISOformat(Calendar.uniform30day.toString(), "2038-01-01T15:00:00Z"),
          CalendarDate.parseISOformat(Calendar.uniform30day.toString(), "2038-01-01T18:00:00Z"),
          CalendarDate.parseISOformat(Calendar.uniform30day.toString(), "2038-01-01T21:00:00Z"),
          CalendarDate.parseISOformat(Calendar.uniform30day.toString(), "2038-01-02T00:00:00Z"),
          CalendarDate.parseISOformat(Calendar.uniform30day.toString(), "2038-01-02T03:00:00Z")
    };
    List<CalendarDate> expectedCalendarDatesList = Arrays.asList(expectedCalendarDates);

    String endpoint = TestWithLocalServer.withPath("/ncss/grid/scanCdmUnitTests/ncss/test/pr_HRM3_2038-2070.CO.ncml?var=pr&latitude=44&longitude=18&time_start=2038-01-01T03%3A00%3A00Z&time_end=2038-01-02T03%3A00%3A00Z&accept=netcdf");
    byte[] result = TestWithLocalServer.getContent(endpoint, 200, ContentType.netcdf);

    ByteArrayInputStream is = new ByteArrayInputStream(result);
    File tmpFile = TestDir.getTempFile();
    System.out.printf("Write file to %s%n", tmpFile.getAbsolutePath());
    IO.appendToFile(is, tmpFile.getAbsolutePath());

    NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", result);
    NetcdfDataset ds = new NetcdfDataset(nf);

    CoordinateAxis1DTime tAxis = CoordinateAxis1DTime.factory(ds, ds.findCoordinateAxis("time"), null);
    List<CalendarDate> dates = tAxis.getCalendarDates();
    assert dates != null;
    Assert.assertEquals(expectedCalendarDatesList.size(), dates.size());

    int count = 0;
    for (CalendarDate cd : dates) {
      CalendarDate ecd = expectedCalendarDatesList.get(count++);
      Assert.assertEquals(ecd.getMillis(), cd.getMillis());
      Assert.assertEquals(ecd, cd);
    }
  }

}
