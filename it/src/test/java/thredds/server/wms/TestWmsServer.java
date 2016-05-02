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

package thredds.server.wms;

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
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import thredds.TestWithLocalServer;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static com.eclipsesource.restfuse.Assert.assertOk;
import static org.junit.Assert.assertEquals;

@RunWith(HttpJUnitRunner.class)
@Category(NeedsCdmUnitTest.class)
public class TestWmsServer {

  @Rule
  public Destination destination = new Destination(TestWithLocalServer.server);

  @Context
  private Response response; // will be injected after every request

  private final Namespace NS_WMS = Namespace.getNamespace("wms", "http://www.opengis.net/wms");


  @HttpTest(method = Method.GET, path = "/wms/scanCdmUnitTests/conventions/coards/sst.mnmean.nc?service=WMS&version=1.3.0&request=GetCapabilities")
   public void testCapabilites() throws IOException, JDOMException {
     assertOk(response);
     String xml = response.getBody(String.class);
     Reader in = new StringReader(xml);
     SAXBuilder sb = new SAXBuilder();
     Document doc = sb.build(in);

     XPathExpression<Element> xpath = XPathFactory.instance().compile("//wms:Capability/wms:Layer/wms:Layer/wms:Layer", Filters.element(), null, NS_WMS);
     List<Element> elements = xpath.evaluate(doc);
     assertEquals(1, elements.size());

     XPathExpression<Element> xpath2 = XPathFactory.instance().compile("//wms:Capability/wms:Layer/wms:Layer/wms:Layer/wms:Name", Filters.element(), null, NS_WMS);
     Element emt = xpath2.evaluateFirst(doc);
     assertEquals("sst", emt.getTextTrim());
   }

  /* @HttpTest(method = Method.GET, path = "/wms/cdmUnitTest/conventions/coards/sst.mnmean.nc?request=DescribeCoverage&version=1.0.0&service=WMS&coverage=sst")
  public void testDescribeCoverage() throws IOException {
    assertOk(response);
  }

  @HttpTest(method = Method.GET, path = "/wms/cdmUnitTest/conventions/coards/sst.mnmean.nc?service=WMS&version=1.0.0&REQUEST=GetCoverage&COVERAGE=sst&CRS=EPSG%3a4326&BBOX=1,-79.5,359,89.5&TIME=2002-12-07T00:00:00Z&FORMAT=GeoTIFF&EXCEPTIONS=application/vnd.ogc.se_xml")
  public void testGetCoverage() throws IOException {
    assertOk(response);
  }  */


  /* @Test
  public void testNAM() throws IOException {
    String dataset = server+"testAll/namExtract/20060925_0600.nc";
    showGetCapabilities(dataset);
    getMap(dataset, "Pressure_surface", "C:/temp/wmsNAM.jpg");
  }

  // http://localhost:8081/thredds/wms/testWMS/cmor_pcmdi.nc


  //@Test
  public void testLatlon() throws IOException {
    String dataset = server+"testWMS/cmor_pcmdi.nc";
    showGetCapabilities(dataset);
    // getMap(dataset, "tos", "C:/temp/wmsLatlon.jpg");
  }

  //@Test
  public void testRot() throws IOException {
    String dataset = server+"testWMS/rotatedLatlon.grb";
    showGetCapabilities(dataset);
    getMap(dataset, "Geopotential_height", "C:/temp/wmsRot.jpg");
  }

  //@Test
  public void testGoogle() throws IOException {
    String g = "testWMS/rotatedlatlon.grb?VERSION=1.1.1&REQUEST=GetMap&SRS=EPSG:4326&WIDTH=512&HEIGHT=512&LAYERS=Geopotential_height&STYLES=BOXFILL/alg&TRANSPARENT=TRUE&FORMAT=image/gif&BBOX=-135,35.34046249175859,135,82.1914946416798";
    saveRead(server+g, "C:/temp/wmsGoogle.gif");
  }


  private void showGetCapabilities(String url) throws IOException {
    showRead(url+"?request=GetCapabilities&version=1.3.0&service=WMS");
  }

  private void getMap(String url, String grid, String filename) throws IOException {
    String opts = "&STYLES=BOXFILL/ncview&";
    opts += "CRS=CRS:84&";
    opts += "BBOX=0,-90,360,90&";
    opts += "WIDTH=1000&";
    opts += "HEIGHT=500&";
    opts += "FORMAT=image/png&";
    //opts += "time=2001-08-14T00:00:00Z&";

    saveRead(url+"?request=GetMap&version=1.3.0&service=WMS&Layers="+grid+opts, filename);
  }

  private void getMap3(String url, String grid, String filename) throws IOException {
    String styles = "&STYLES=BOXFILL/redblue&";
    String srs = "CRS=CRS:84&";
    String bb = "BBOX=-100,30,-40,40&";
    String width="WIDTH=600&";
    String height="HEIGHT=500&";
    String format="FORMAT=image/png";
    String opts=styles+srs+bb+width+height+format;

    saveRead(url+"?request=GetMap&version=1.3.0&service=WMS&Layers="+grid+opts, filename);
  }

  private void showRead(String url) throws IOException {
    System.out.println("****************\n");
    System.out.println(url+"\n");
    String contents = IO.readURLcontentsWithException( url);
    System.out.println(contents);
  }

  private void saveRead(String url, String filename) throws IOException {
    System.out.println("****************\n");
    System.out.println("Read "+url+"\n");
    File file = new File(filename);
    String result = IO.readURLtoFile(url, file);
    System.out.println("Save to "+filename+" result = "+result+"\n");

    System.out.println("****************\n");
    //showRead(url);
  } */




}
