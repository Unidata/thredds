/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.catalog;

import org.junit.Test;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.writer.CatalogXmlWriter;

import java.io.IOException;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 1/21/2015
 */
public class TestDatasetScan {

  @Test
  public void testReadCatalog() throws IOException {
    String filePath = "C:/dev/github/thredds46/tds/content/thredds/catalog.xml";
    ConfigCatalog cat = TestServerCatalogs.open("file:" + filePath);
    CatalogXmlWriter writer = new CatalogXmlWriter();
    System.out.printf("%s%n",  writer.writeXML( cat ));

    List<DatasetRoot> roots = cat.getRoots();
    for (DatasetRoot root : roots)
      System.out.printf("DatasetRoot %s -> %s%n", root.path, root.location);
    assert roots.size() == 1;

    Dataset ds = cat.findDatasetByID("testDatasetScan");
    assert ds != null;
    assert (ds instanceof DatasetScan);
    DatasetScan dss = (DatasetScan) ds;
    String serviceName = dss.getServiceNameDefault();
    assert serviceName.equals("all");

    DatasetScanConfig config = dss.getConfig();
    System.out.printf("%s%n", config);

    Catalog scanCat = dss.makeCatalogForDirectory("testAll", cat.getBaseURI());
    System.out.printf("%n%s%n",  writer.writeXML( scanCat ));

    scanCat = dss.makeCatalogForDirectory("testAll/testdata", cat.getBaseURI());
    System.out.printf("%s%n",  writer.writeXML( scanCat ));

  }

}
