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
import thredds.TestWithLocalServer;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.test.util.NeedsCdmUnitTest;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 9/15/2015.
 */
@Category(NeedsCdmUnitTest.class)
public class NcssGridaspointTest {
  String ds = "ncss/grid/gribCollection/GFS_CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1/GC";

  @Test
  public void checkGridAsPointCsv() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath(ds+"?var=Temperature_isobaric&latitude=40.01&longitude=-102.02&vertCoord=225");
    byte[] result = call( endpoint, 200);
    Assert.assertNotNull(result);
    String xml = new String( result);
    System.out.printf("%n%s%n", xml);
  }

  @Test
  public void checkGridAsPointXml() throws JDOMException, IOException {
    String endpoint = TestWithLocalServer.withPath(ds+"?var=Temperature_isobaric&latitude=40&longitude=-102&vertCoord=225&accept=xml");
    byte[] result = call( endpoint, 200);
    Assert.assertNotNull(result);
    String xml = new String( result);

    System.out.printf("xml=%s%n", xml);
    Reader in = new StringReader(xml);
    SAXBuilder sb = new SAXBuilder();
    Document doc = sb.build(in);

    XPathExpression<Element> xpath = XPathFactory.instance().compile("/stationFeatureCollection/stationFeature/data[@name='Temperature_isobaric']", Filters.element());
    List<Element> elements = xpath.evaluate(doc);
    Assert.assertEquals(1, elements.size());
  }

  private byte[] call(String endpoint, int expectCode) throws HTTPException {
    System.out.printf("req = '%s'%n", endpoint);
    try (HTTPSession session = new HTTPSession(endpoint)) {
      HTTPMethod method = HTTPFactory.Get(session);
      int statusCode = method.execute();
      if (statusCode != 200) {
        System.out.printf("statusCode = %d '%s'%n", statusCode, method.getResponseAsString());
        Assert.assertEquals(expectCode, statusCode);
        return null;
      }

      Assert.assertEquals(expectCode, statusCode);
      byte[] content = method.getResponseAsBytes();
      assert content.length > 0;
      return content;
    }
  }

}
