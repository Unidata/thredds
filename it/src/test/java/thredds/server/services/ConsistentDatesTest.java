package thredds.server.services;

import com.eclipsesource.restfuse.Destination;
import com.eclipsesource.restfuse.HttpJUnitRunner;
import com.eclipsesource.restfuse.Method;
import com.eclipsesource.restfuse.Response;
import com.eclipsesource.restfuse.annotation.Context;
import com.eclipsesource.restfuse.annotation.HttpTest;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import thredds.TestWithLocalServer;
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
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.eclipsesource.restfuse.Assert.assertOk;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Consistency of calendar dates across services: ncss, wms, wcs
 */

@RunWith(HttpJUnitRunner.class)
@Category(NeedsCdmUnitTest.class)
public class ConsistentDatesTest {
  private static final boolean show = true;

  @Rule
  public Destination destination = new Destination(TestWithLocalServer.server);

  @Context
  private Response response; // will be injected after every request

  private final String[] expectedDateTime = {
           "0000-01-16T06:00:00Z",       // these are the actual dates from cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc
           "0000-02-15T16:29:06Z",       // this does not have a 360 calendar, so we need to find a dataset that does to test 360calendar
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
  //private final List<DateTime> expectedWMSDatesAsDateTime = Collections.unmodifiableList(Arrays.asList(expectedDateTime));

  @Before
  public void setUp() {
  }

  @HttpTest(method = Method.GET, path = "/wms/cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?service=WMS&version=1.3.0&request=GetCapabilities")
  public void checkWMSDates() throws JDOMException, IOException {

    assertOk(response);
    assertTrue(response.hasBody());
    String xml = response.getBody(String.class);
    Reader in = new StringReader(xml);
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

  @HttpTest(method = Method.GET, path = "/wcs/cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?service=WCS&version=1.0.0&request=DescribeCoverage&coverage=sst")
  public void checkWCSDates() throws JDOMException, IOException {
    assertOk(response);
    assertTrue(response.hasBody());
    String xml = response.getBody(String.class);
    Reader in = new StringReader(xml);
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    // old way - deprecated
    //XPath xPath = XPath.newInstance("//wcs:temporalDomain/gml:timePosition");
    //xPath.addNamespace("wcs", doc.getRootElement().getNamespaceURI());
    // List<Element> timePositionNodes = xPath.selectNodes(doc);

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

  @HttpTest(method = Method.GET, path = "/ncss/cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?var=sst&latitude=45&longitude=-20&temporal=all")
  public void checkNCSSDates() throws JDOMException, IOException {

    assertOk(response);
    assertTrue(response.hasBody());
    String xml = response.getBody(String.class);
    Reader in = new StringReader(xml);
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    // old way
    //XPath xPath = XPath.newInstance("/grid/point/data[@name='date']");
    //List<Element> dataTimeNodes = xPath.selectNodes(doc);

    XPathExpression<Element> xpath =
            XPathFactory.instance().compile("/grid/point/data[@name='date']", Filters.element());
    List<Element> dataTimeNodes = xpath.evaluate(doc);

    List<String> timePositionDateTime = new ArrayList<>();
    for (Element e : dataTimeNodes) {
      System.out.printf("Date= %s%n", e.getText());
      CalendarDate cd = CalendarDate.parseISOformat(null,  e.getText());
      timePositionDateTime.add(cd.toString());;
    }

    assertEquals(expectedDatesAsDateTime, timePositionDateTime);

  }

  // PF5_SST_Climatology:  :units = "hour since 0000-01-01 00:00:00";
  @HttpTest(method = Method.GET, path = "/ncss/cdmUnitTest/ncss/climatology/PF5_SST_Climatology_Monthly_1985_2001.nc?var=sst&latitude=45&longitude=-20&temporal=all&accept=netcdf")
  public void checkNCSSDatesInNetcdf() throws JDOMException, IOException {

    assertOk(response);
    assertTrue(response.hasBody());
    NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", response.getBody(byte[].class));
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
  @HttpTest(method = Method.GET, path = "/ncss/scanCdmUnitTests/ncss/test/pr_HRM3_2038-2070.CO.nc?var=pr&latitude=40.019&longitude=-105.293&time_start=2038-01-01T03%3A00%3A00Z&time_end=2038-01-02T03%3A00%3A00Z&accept=netcdf")
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
    List<CalendarDate> expectedDatesAsCalendarDate = Collections.unmodifiableList(Arrays.asList(expectedCalendarDates));

    assertOk(response);
    assertTrue(response.hasBody());
    NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", response.getBody(byte[].class));
    NetcdfDataset ds = new NetcdfDataset(nf);

    CoordinateAxis1DTime tAxis = CoordinateAxis1DTime.factory(ds, ds.findCoordinateAxis("time"), null);
    List<CalendarDate> dates = tAxis.getCalendarDates();
    assert dates != null;
    assertEquals(expectedDatesAsCalendarDate, dates);
  }

}
