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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

/**
 * Describe
 *
 * @author caron
 * @since 3/5/14
 */
@Category(NeedsExternalResource.class)
public class TestStream {
  @Test
  public void testStream1() throws URISyntaxException {
    String catalogName = "http://"+ TestDir.threddsTestServer+"/thredds/catalog.xml";
    URI catalogURI = new URI(catalogName);

    try {
      try (HTTPMethod m = HTTPFactory.Get(catalogName)) {

        int statusCode = m.execute();
        System.out.printf("status = %d%n", statusCode);

        InputStream stream = m.getResponseBodyAsStream();
        InvCatalogFactory reader = InvCatalogFactory.getDefaultFactory(true);

        InvCatalogImpl catalog = reader.readXML(stream, catalogURI);
        catalog.writeXML(System.out);
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testString() throws URISyntaxException {
    String catalogName = "http://"+TestDir.threddsTestServer+"/thredds/catalog.xml";
    URI catalogURI = new URI(catalogName);

    try {
      try (HTTPMethod m = HTTPFactory.Get(catalogName)) {

        int statusCode = m.execute();
        System.out.printf("status = %d%n", statusCode);

        String stream = m.getResponseAsString(CDM.UTF8);
        System.out.printf("cat = %s%n", stream);

        InvCatalogFactory reader = InvCatalogFactory.getDefaultFactory(true);

        InvCatalogImpl catalog = reader.readXML(stream, catalogURI);
        catalog.writeXML(System.out);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }



}
