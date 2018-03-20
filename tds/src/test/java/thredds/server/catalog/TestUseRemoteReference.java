/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.server.catalog.builder.ConfigCatalogBuilder;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.List;

/**
 * Test server catalog UseRemoteReference
 *
 * @author caron
 * @since 1/16/2015
 */
public class TestUseRemoteReference {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String testCatalog = "thredds/server/catalog/TestRemoteCatalogService.xml";
  private Catalog cat;

  @Before
  public void getCatalog() throws IOException {
    // get test catalog location
    ClassLoader cl = this.getClass().getClassLoader();
    URL url = cl.getResource(testCatalog);
    ConfigCatalogBuilder catFactory = new ConfigCatalogBuilder();
    cat = catFactory.buildFromLocation("file:" + url.getPath(), null);
    //cat = catFactory.buildFromURI(url);  // LOOK does this work ??
  }

  @Test
  public void testUseRemoteCatalogServiceIsSet() {
    List<Dataset> datasets = cat.getDatasets();
    assert datasets.size() == 1;

    Dataset ds = datasets.get(0);
    List<Dataset> childDatasets = ds.getDatasets();
    assert childDatasets.size() == 3;

    for (Dataset thisDs : childDatasets) {
      assert thisDs instanceof CatalogRef;
      String name = thisDs.getName();
      CatalogRef catref = (CatalogRef) thisDs;
      if (name.contains("default")) {
        // the default is null...this is detected and handled in HtmlWriter
        assert catref.useRemoteCatalogService() == null;

      } else if (name.contains("do not")) {
        // this catalogRef has useRemoteCatalogService set to false
        assert catref.useRemoteCatalogService() == false;

      } else if (name.contains("please")) {
        // this catalogRef has useRemoteCatalogService set to true
        assert catref.useRemoteCatalogService() == true;

      } else {
        assert false;

      }
    }
  }

}
