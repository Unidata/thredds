package thredds.catalog;

import junit.framework.*;

/* Test opening catalogs from web */

public class TestOpen extends TestCase {
  static private boolean showValidate = false;

  public TestOpen( String name) {
    super(name);
  }

  private InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);

  public String open(String catalogName, boolean shouldValidate) {

    StringBuilder buff = new StringBuilder();

    try {
      InvCatalog cat = catFactory.readXML(catalogName);
      boolean validate = cat.check( buff, false);
      if (!validate)
        System.out.println("TestOpen validate failed "+ catalogName+"\n"+ buff.toString()+"\n");

      if (validate != shouldValidate) {
        System.out.println("TestOpen ERROR validate "+ catalogName+" should be "+shouldValidate);
        System.out.println(" = <"+ buff.toString()+">");
        assert false;
      }

    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }

    return buff.toString();
  }

  public void testOpen() {
    // open( "http://www.unidata.ucar.edu/projects/THREDDS/xml/InvCatalog.0.6.xml", true);
    // open( "http://www.unidata.ucar.edu/projects/THREDDS/BARF/InvCatalog.0.6.xml", false);
    open( TestCatalogAll.makeFilepath("InvCatalog.0.6.xml"), true);
    open( TestCatalogAll.makeFilepath("InvCatalogBadDTD.xml"), true);
    open( TestCatalogAll.makeFilepath("InvCatalog.0.6d.xml"), false);
    open( TestCatalogAll.makeFilepath("ParseFails.xml"), false);
    open( TestCatalogAll.makeFilepath("TestAlias.xml"), true);
    open( TestCatalogAll.makeFilepath("BadService.xml"), true);
    open( TestCatalogAll.makeFilepath("enhancedCat.xml"), true); // */
    open( TestCatalogAll.makeFilepath("TestInherit.0.6.xml"), true);
  }

}
