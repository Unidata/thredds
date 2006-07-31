package thredds.catalog;

import junit.framework.*;
import java.util.*;
import java.io.IOException;


public class TestDatasetFmrc extends TestCase {

  public TestDatasetFmrc( String name) {
    super(name);
  }

  public void testRead() throws IOException {
    testRead( TestCatalogAll.open( "DatasetFmrc.xml", true));
  }

  public void testRead(InvCatalogImpl cat) throws IOException {
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( true);
    System.out.println("cat=\n"+ fac.writeXML(cat));
  }
}
