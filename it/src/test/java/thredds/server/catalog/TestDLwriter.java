/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: TestDLwriter.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.catalog;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestDLwriter extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestDLwriter( String name) {
    super(name);
  }

  // This test sucks: a 404 response will yield success.
  public void testDLwriter() throws IOException {
    String url = "/DLwriter?type=ADN&catalog=/thredds/catalog/testEnhanced/catalog.xml";

    System.out.println("Response from "+ TestOnLocalServer.withHttpPath(url));
    String result = IO.readURLcontents(TestOnLocalServer.withHttpPath(url));
    assert result != null;
    System.out.println(result);
  }
}
