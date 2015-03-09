package thredds.server.ncss;

import static com.eclipsesource.restfuse.Assert.assertOk;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Rule;
import org.junit.runner.RunWith;

import thredds.TestWithLocalServer;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import com.eclipsesource.restfuse.Destination;
import com.eclipsesource.restfuse.HttpJUnitRunner;
import com.eclipsesource.restfuse.Method;
import com.eclipsesource.restfuse.Response;
import com.eclipsesource.restfuse.annotation.Context;
import com.eclipsesource.restfuse.annotation.HttpTest;

/*
  These tests fail on jenkins, but work locally
 */

@RunWith(HttpJUnitRunner.class)
public class NcssIntegrationTest {

  @Rule
  public Destination destination = new Destination(TestWithLocalServer.server);

  @Context
  private Response response; // will be injected after every request

  @HttpTest(method = Method.GET, path = "ncss/gribCollection/gfs_conus80/20141024/GFS_CONUS_80km_20141024_1200.grib1/GC?var=Temperature_isobaric&latitude=40&longitude=-102&vertCoord=225")
  public void checkGoodRequest() throws JDOMException, IOException {
    assertOk(response);
    String xml = response.getBody(String.class);
    System.out.printf("xml=%s%n", xml);
    Reader in = new StringReader(xml);
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    XPathExpression<Element> xpath = XPathFactory.instance().compile("/grid/point/data[@name='Temperature_isobaric']", Filters.element());
    List<Element> elements = xpath.evaluate(doc);
    assertEquals(1, elements.size());
  }

  @HttpTest(method = Method.GET, path = "ncss/gribCollection/gfs_conus80/20141024/GFS_CONUS_80km_20141024_1200.grib1/GC?var=Temperature_isobaric")
  public void getSomeBinaryDataRequest() throws IOException {
    assertOk(response);
    assertTrue(response.hasBody());
    NetcdfFile nf = NetcdfFile.openInMemory("test_data.ncs", response.getBody(byte[].class));
    ucar.nc2.dt.grid.GridDataset gdsDataset = new ucar.nc2.dt.grid.GridDataset(new NetcdfDataset(nf));

    assertNotNull(gdsDataset.findGridByName("Temperature_isobaric"));
  }

}
