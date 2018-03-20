/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.TestOnLocalServer;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.builder.CatalogBuilder;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * Test catalog utilities
 */
@Category(NeedsCdmUnitTest.class)
public class TdsLocalCatalog {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  public static boolean showValidationMessages = false;

  public static Catalog openFromURI(URI uri) throws IOException {
    String catPath = uri.toString();
    CatalogBuilder builder = new CatalogBuilder();
    Catalog cat = builder.buildFromLocation(catPath, null);
    if (builder.hasFatalError()) {
      System.out.println("Validate failed "+ catPath+" = \n<"+ builder.getErrorMessage()+">");
      assert false : builder.getErrorMessage();
    } else if (showValidationMessages)
      System.out.println("Validate ok "+ catPath+" = \n<"+ builder.getErrorMessage()+">");

    return cat;
  }


  public static Catalog open(String catalogName) throws IOException {
    if (catalogName == null) catalogName = "/catalog.xml";
    String catalogPath = TestOnLocalServer.withHttpPath(catalogName);
    System.out.println("\n open= "+catalogPath);

    CatalogBuilder builder = new CatalogBuilder();
    Catalog cat = builder.buildFromLocation(catalogPath, null);
    if (builder.hasFatalError()) {
      System.out.println("Validate failed "+ catalogName+" = \n<"+ builder.getErrorMessage()+">");
      assert false : builder.getErrorMessage();
    } else if (showValidationMessages)
      System.out.println("Validate ok "+ catalogName+" = \n<"+ builder.getErrorMessage()+">");

    return cat;
  }

  @Test
  public void readCatalog() {
    Catalog mainCat;
    try {
      mainCat = open(null);
      assert mainCat != null;
    } catch (IOException e) {
      assert false;
    }
  }

}
