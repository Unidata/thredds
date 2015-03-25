/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.util.net;

import org.junit.Test;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.tools.CatalogXmlWriter;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.nc2.constants.CDM;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Describe
 *
 * @author caron
 * @since 3/5/14
 */
public class TestStream {

  @Test
  public void testStream1() throws URISyntaxException {
    String catalogName = "http://thredds.ucar.edu/thredds/catalog.xml";
    URI catalogURI = new URI(catalogName);

    try (HTTPSession client = HTTPFactory.newSession(catalogName)) {
      HTTPMethod m = HTTPFactory.Get(client);

      int statusCode = m.execute();
      System.out.printf("status = %d%n", statusCode);

      InputStream stream = m.getResponseBodyAsStream();
      CatalogBuilder builder = new CatalogBuilder();
      Catalog cat = builder.buildFromStream(stream, catalogURI);
      CatalogXmlWriter writer = new CatalogXmlWriter();
      writer.writeXML(cat, System.out, false);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testString() throws URISyntaxException {
    String catalogName = "http://thredds.ucar.edu/thredds/catalog.xml";
    URI catalogURI = new URI(catalogName);

    try (HTTPSession client = HTTPFactory.newSession(catalogName)) {
      HTTPMethod m = HTTPFactory.Get(client);

      int statusCode = m.execute();
      System.out.printf("status = %d%n", statusCode);

      String catAsString = m.getResponseAsString(CDM.UTF8);
      System.out.printf("cat = %s%n", catAsString);

      CatalogBuilder builder = new CatalogBuilder();
      Catalog cat = builder.buildFromString(catAsString, catalogURI);
      CatalogXmlWriter writer = new CatalogXmlWriter();
      writer.writeXML(cat, System.out, false);

    } catch (IOException e) {
      e.printStackTrace();

    }

  }



}
