/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.*;
import ucar.nc2.constants.FeatureType;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test datasetScan on files that are not datasets - should serve out like straight web server
 *
 * @author caron
 * @since 3/16/2016.
 */
public class TestScanNonDatasets {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testStandardServicesDatasetScan() throws IOException {
    String catalog = "/catalog/scanLocalHtml/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("scanLocalHtml/esfgTest.html");

    FeatureType ft = ds.getFeatureType();
    Assert.assertNull(ft);

    Service s = ds.getServiceDefault();
    Assert.assertNotNull(s);
    Assert.assertTrue(s.getType() == ServiceType.HTTPServer);

    String v = ds.findProperty(Dataset.NotAThreddsDataset);
    Assert.assertNotNull(v);
    Assert.assertEquals("true", v);
  }

}
