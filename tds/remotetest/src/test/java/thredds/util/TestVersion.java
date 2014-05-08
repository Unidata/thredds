package thredds.util;

import junit.framework.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TestVersion extends TestCase
{

  public TestVersion( String name )
  {
    super( name );
  }

  public void testValidVersions()
  {
    validVersionExpected( "1.0" );
    validVersionExpected( "007" );
    validVersionExpected( "3.14159" );
    invalidVersionExpected( "1.0 " );
    invalidVersionExpected( " 1.0" );
    invalidVersionExpected( "1. 0.0" );
    invalidVersionExpected( "1.0a" );
    invalidVersionExpected( "-1.0" );
    invalidVersionExpected( "1.-8" );
    invalidVersionExpected( "." );
    invalidVersionExpected( ".." );
    invalidVersionExpected( "..." );
    invalidVersionExpected( ".1" );
    invalidVersionExpected( "1." );
  }

  /**
   * Test ...
   */
  public void testCompareTo()
  {
    Version v1, v1_alt, v1_0, v1_0_alt, v1_0_0, v1_0_0_alt;

    Version v1_1;


    try
    {
      v1 = new Version( "1" );
      v1_alt = new Version( "1" );
      v1_0 = new Version( "1.0" );
      v1_0_alt = new Version( "1.0" );
      v1_0_0 = new Version( "1.0.0" );
      v1_0_0_alt = new Version( "1.0.0" );

      v1_1 = new Version( "1.1" );

    }
    catch ( IllegalArgumentException e )
    {
      fail( "Unexpected IllegalArgumentException: " + e.getMessage() );
      return;
    }

    compareToEqualExpected( v1, v1_alt );
    compareToEqualExpected( v1, v1_0 );
    compareToEqualExpected( v1, v1_0_0 );
    compareToEqualExpected( v1_0, v1 );
    compareToEqualExpected( v1_0, v1_0_alt );
    compareToEqualExpected( v1_0, v1_0_0 );
    compareToEqualExpected( v1_0_0, v1 );
    compareToEqualExpected( v1_0_0, v1_0 );
    compareToEqualExpected( v1_0_0, v1_0_0_alt );

    compareToGreaterThanExpected( v1_1, v1 );
    compareToGreaterThanExpected( v1_1, v1_0 );
    compareToGreaterThanExpected( v1_1, v1_0_0 );

    compareToLessThanExpected( v1, v1_1 );
    compareToLessThanExpected( v1_0, v1_1 );
  }

  private void compareToEqualExpected( Version v1, Version v2 )
  {
    assertTrue( "Version(\"" + v1.getVersionString() + "\") NOT equal to Version(\"" + v2.getVersionString() + "\").",
                v1.compareTo( v2 ) == 0 );
  }

  private void compareToGreaterThanExpected( Version v1, Version v2 )
  {
    assertTrue( "Version(\"" + v1.getVersionString() + "\") NOT greater than to Version(\"" + v2.getVersionString() + "\").",
                v1.compareTo( v2 ) > 0 );
  }

  private void compareToLessThanExpected( Version v1, Version v2 )
  {
    assertTrue( "Version(\"" + v1.getVersionString() + "\") NOT less than to Version(\"" + v2.getVersionString() + "\").",
                v1.compareTo( v2 ) < 0 );
  }

  private void validVersionExpected( String verStr )
  {
    try
    {
      new Version( verStr );
    }
    catch ( IllegalArgumentException e )
    {
      fail( "Unexpected invalid version <" + verStr + ">: " + e.getMessage() );
    }
  }

  private void invalidVersionExpected( String verStr )
  {
    try
    {
      new Version( verStr );
    }
    catch ( IllegalArgumentException e )
    {
      return;
    }
    fail( "Unexpected valid version <" + verStr + ">: " );
  }

}
