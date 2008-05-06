package thredds.tds.ethan;

import junit.framework.*;

import thredds.catalog.*;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 30, 2006 11:13:36 AM
 */
public class TestTdsIddPing extends TestCase
{

  private String host = "motherlode.ucar.edu:8080";
  private String targetTdsUrl;

  public TestTdsIddPing( String name )
  {
    super( name );
  }

  protected void setUp()
  {
    host = System.getProperty( "thredds.tds.test.server", host );

    targetTdsUrl = "http://" + host + "/thredds/";
  }

  public void testMainCatalog()
  {
    String catUrl = targetTdsUrl + "catalog.xml";
    System.out.println( "validate catalog: " + catUrl );

    StringBuilder msg = new StringBuilder();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString());
    }
  }

  public void testModelsCatalog()
  {
    String catUrl = targetTdsUrl + "idd/models.xml";
    System.out.println( "validate catalog: " + catUrl );

    StringBuilder msg = new StringBuilder();
    InvCatalogImpl catalog = TestAll.openAndValidateCatalog( catUrl, msg, false );
    if ( catalog == null )
    {
      fail( msg.toString() );
    }
  }

}
