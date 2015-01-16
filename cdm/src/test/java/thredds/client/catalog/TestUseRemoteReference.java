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
package thredds.client.catalog;

import org.junit.Before;
import org.junit.Test;
import thredds.client.catalog.builder.CatalogBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * move this to server client
 *
 * @author caron
 * @since 1/16/2015
 */
public class TestUseRemoteReference {
  private String testCatalog = "thredds/catalog/TestRemoteCatalogService.xml";
  private Catalog cat;

  @Before
  public void getCatalog() throws IOException {
      boolean validate = true;

      // get test catalog location
      ClassLoader cl = this.getClass().getClassLoader();
      URL url = cl.getResource(testCatalog);
      // read in catalog
      CatalogBuilder catFactory = new CatalogBuilder();
      cat = catFactory.buildFromLocation("file:" + url.getPath());
  }

  @Test
  public void testUseRemoteCatalogServiceIsSet() {

      List<Dataset> datasets = cat.getDatasets();
      assert datasets.size() == 1;

      Dataset ds = datasets.get(0);
      List<Dataset> childDatasets = ds.getDatasets();
      assert childDatasets.size() == 3;

      for (Dataset thisDs : childDatasets) {
          String name = thisDs.getName();
          if (name.contains("default")) {
              // the default is null...this is detected and handled in HtmlWriter
              assert ((CatalogRef) thisDs).useRemoteCatalogService() == false;
          } else if (name.contains("do not")) {
              // this catalogRef has useRemoteCatalogService set to false
              assert ((CatalogRef) thisDs).useRemoteCatalogService() == false;
          } else {
              // this catalogRef has useRemoteCatalogService set to true
              assert ((CatalogRef) thisDs).useRemoteCatalogService() == true;
          }
      }
  }
  
}
