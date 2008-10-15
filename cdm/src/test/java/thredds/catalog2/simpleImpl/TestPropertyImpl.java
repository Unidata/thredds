package thredds.catalog2.simpleImpl;

import junit.framework.*;
import thredds.catalog2.Property;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestPropertyImpl extends TestCase
{

//  private PropertyImpl me;

  public TestPropertyImpl( String name )
  {
    super( name );
  }

  public void testCtorNullArgs()
  {
    try
    { new PropertyImpl( null, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalArgumentException: " + e.getMessage() ); }
    fail( "No IllegalArgumentException." );
  }

  public void testCtorNullName()
  {
    String value = "a value";
    try
    { new PropertyImpl( null, value ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalArgumentException: " + e.getMessage() ); }
    fail( "No IllegalArgumentException." );
  }

  public void testCtorNullValue()
  {
    String name = "a name";
    try
    { new PropertyImpl( name, null ); }
    catch ( IllegalArgumentException e )
    { return; }
    catch ( Exception e )
    { fail( "Non-IllegalArgumentException: " + e.getMessage() ); }
    fail( "No IllegalArgumentException." );
  }

  public void testNormal()
  {
    String name = "a name";
    String value = "a value";
    Property p = null;
    try
    { p = new PropertyImpl( name, value ); }
    catch ( IllegalArgumentException e )
    { fail( "Unexpected IllegalArgumentException: " + e.getMessage() ); }
    catch ( Exception e )
    { fail( "Unexpected Non-IllegalArgumentException: " + e.getMessage()); }

    assertTrue( "Property name [" + p.getName() + "] not as expected [" + name + "].",
                p.getName().equals( name));
    assertTrue( "Property value [" + p.getValue() + "] not as expected [" + value + "].",
                p.getValue().equals( value));
  }
}
