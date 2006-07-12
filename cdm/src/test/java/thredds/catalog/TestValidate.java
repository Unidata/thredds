package thredds.catalog;

import junit.framework.*;

/** Test catalog read JUnit framework. */

public class TestValidate extends TestCase {
  private static boolean showValidation = false;

  public TestValidate( String name) {
    super(name);
  }

  public String open(String catalogName, boolean shouldValidate) {
    catalogName = "file:///"+TestCatalogAll.dataDir +"/"+ catalogName;

    StringBuffer buff = new StringBuffer();

    try {
      InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( showValidation);
      InvCatalog cat = catFactory.readXML(catalogName);
      boolean validate = ((InvCatalogImpl)cat).check( buff);
      if (showValidation) {
        if (validate)
          System.out.println("TestValidate validate OK on "+ catalogName+"\n----"+ buff.toString()+"\n");
        else
          System.out.println("TestValidate validate FAILED "+ catalogName+"\n----"+ buff.toString()+"\n");
      }
      if (validate != shouldValidate) {
        System.out.println("TestValidate ERROR validate "+ catalogName+" "+shouldValidate);
        assertTrue( false);
      }

    } catch (Exception e) {
      e.printStackTrace();
      assertTrue( false);
    }

    return buff.toString();
  }

  public void testValid() {
    open("InvCatalog.0.6.xml", true);
  }

  public void testInvalid() {
    open("ParseFails.xml", false);
    open("BadService.xml", false);
  }

}
