package thredds.catalog;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class TestAllLite {
  public static String dataDir;

  public static junit.framework.Test suite ( ) {

    InvCatalogFactory myFactory = new InvCatalogFactory("nano", true);
    // myFactory.registerCatalogFactory("0.6", new thredds.catalog.parser.nano.Catalog6());
    //InvCatalogFactory.setDefaultFactory( myFactory);

    return TestCatalogAll.suite();
  }
}