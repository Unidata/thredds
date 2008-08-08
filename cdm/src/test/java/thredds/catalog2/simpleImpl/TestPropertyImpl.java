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

  public void testProperty()
  {
    String name = "a name";
    String value = "a value";
    boolean pass = false;
    try
    { new PropertyImpl( null, null ); }
    catch ( IllegalArgumentException e )
    { pass = true; }
    catch ( Exception e )
    { fail( "Non-IllegalArgumentException: " + e.getMessage()); }
    if ( ! pass ) fail( "No IllegalArgumentException.");

    pass = false;
    try
    { new PropertyImpl( name, null ); }
    catch ( IllegalArgumentException e )
    { pass = true; }
    catch ( Exception e )
    { fail( "Non-IllegalArgumentException: " + e.getMessage()); }
    if ( ! pass ) fail( "No IllegalArgumentException.");

    pass = false;
    try
    { new PropertyImpl( null, value ); }
    catch ( IllegalArgumentException e )
    { pass = true; }
    catch ( Exception e )
    { fail( "Non-IllegalArgumentException: " + e.getMessage()); }
    if ( ! pass ) fail( "No IllegalArgumentException.");

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
