package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog.ServiceType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestCatalogImpl extends TestCase
{
  private URI docBaseUri;

  private ServiceType type;
  private URI baseUri;

  public TestCatalogImpl( String name )
  {
    super( name );
  }

  protected void setUp()throws Exception
  {
    try
    { baseUri = new URI( "http://server/thredds/dodsC/" );
      docBaseUri = new URI( "http://server/thredds/aCat.xml" ); }
    catch ( URISyntaxException e )
    { fail("Bad URI syntax: " + e.getMessage()); }

    type = ServiceType.OPENDAP;
  }

  public void testConstructorNullDocBaseUri()
  {
    try
    { new CatalogImpl( "cat", null, "", null, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Unexpected exception: " + e.getMessage()); }
    fail( "No IllegalArgumentException.");
  }

  public void testConstructorNormal()
  {
    String name = "cat";
    String verString = "v1";
    Calendar cal = new GregorianCalendar();
    Date lastModTime = cal.getTime();
    cal.add( Calendar.YEAR, 1 );
    Date expiresTime = cal.getTime();
    CatalogBuilder cb = new CatalogImpl( name, docBaseUri, verString, expiresTime, lastModTime );
    assertTrue( "Name [" + cb.getName() + "] not as expected [" + name + "].",
                cb.getName().equals( name ) );
    assertTrue( "BaseUri [" + cb.getDocBaseUri() + "] not as expected [" + docBaseUri + "].",
                cb.getDocBaseUri().equals( docBaseUri ) );
    assertTrue( "Version [" + cb.getVersion() + "] not as expected [" + verString + "].",
                cb.getVersion().equals( verString ) );
    assertTrue( "Expires time [" + cb.getExpires() + "] not as expected [" + expiresTime + "].",
                cb.getExpires().equals( expiresTime ) );
    assertTrue( "Last modified time [" + cb.getLastModified() + "] not as expected [" + lastModTime + "].",
                cb.getLastModified().equals( lastModTime ) );

  }

  public void testContainerCatalogNonuniqueDatasetName()
  {
    CatalogBuilder cat = new CatalogImpl( "cat1", docBaseUri, "", null, null );
    cat.addService( "s1", type, baseUri );
    cat.addService( "s2", type, baseUri );
    try
    { cat.addService( "s1", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage() ); }
    fail( "No IllgalStateException." );
  }
  public void testContainerCatalogNonuniqueDatasetNameNested()
  {
    CatalogBuilder cat = new CatalogImpl( "cat1", docBaseUri, "", null, null );
    cat.addService( "s1", type, baseUri );
    ServiceBuilder s2 = cat.addService( "s2", type, baseUri );
    s2.addService( "s2.1", type, baseUri );
    try
    { s2.addService( "s1", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage() ); }
    fail( "No IllgalStateException." );
  }

  public void testContainerCatalogNonuniqueDatasetNameNestedTwoLevels()
  {
    CatalogBuilder cat = new CatalogImpl( "cat1", docBaseUri, "", null, null );
    cat.addService( "s1", type, baseUri );
    ServiceBuilder s2 = cat.addService( "s2", type, baseUri );
    s2.addService( "s2.1", type, baseUri );
    ServiceBuilder s2_2 = s2.addService( "s2.2", type, baseUri );
    s2_2.addService( "s2.2.1", type, baseUri );
    try
    { s2_2.addService( "s1", type, baseUri ); }
    catch ( IllegalStateException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalStateException: " + e.getMessage() ); }
    fail( "No IllgalStateException." );
  }

}
