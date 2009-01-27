/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.server.wcs;

import junit.framework.*;
import thredds.server.wcs.Version;

/**
 * _more_
 *
 * @author edavis
 * @since Aug 24, 2007 1:37:02 PM
 */
public class TestVersion extends TestCase
{
  public TestVersion( String name )
  {
    super( name );
  }

  public void testValidVersions()
  {
    validVersionExpected( "1.0");
    validVersionExpected( "007");
    validVersionExpected( "3.14159");
    inValidVersionExpected( "1.0a");
    inValidVersionExpected( "-1.0");
    inValidVersionExpected( "1.-8");
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
      v1 = new Version( "1");
      v1_alt = new Version( "1");
      v1_0 = new Version( "1.0");
      v1_0_alt = new Version( "1.0");
      v1_0_0 = new Version( "1.0.0");
      v1_0_0_alt = new Version( "1.0.0");

      v1_1 = new Version( "1.1");

    }
    catch ( IllegalArgumentException e )
    {
      fail( "Unexpected IllegalArgumentException: " + e.getMessage());
      return;
    }

    compareToEqualExpected( v1, v1_alt);
    compareToEqualExpected( v1, v1_0);
    compareToEqualExpected( v1, v1_0_0);
    compareToEqualExpected( v1_0, v1);
    compareToEqualExpected( v1_0, v1_0_alt);
    compareToEqualExpected( v1_0, v1_0_0);
    compareToEqualExpected( v1_0_0, v1);
    compareToEqualExpected( v1_0_0, v1_0);
    compareToEqualExpected( v1_0_0, v1_0_0_alt);

    compareToGreaterThanExpected( v1_1, v1);
    compareToGreaterThanExpected( v1_1, v1_0);
    compareToGreaterThanExpected( v1_1, v1_0_0);

    compareToLessThanExpected( v1, v1_1);
    compareToLessThanExpected( v1_0, v1_1);
  }

  private void compareToEqualExpected( Version v1, Version v2)
  {
    assertTrue( "Version(\"" + v1.getVersionString() + "\") NOT equal to Version(\"" + v2.getVersionString() + "\").",
                v1.compareTo( v2 ) == 0 );
  }

  private void compareToGreaterThanExpected( Version v1, Version v2)
  {
    assertTrue( "Version(\"" + v1.getVersionString() + "\") NOT greater than to Version(\"" + v2.getVersionString() + "\").",
                v1.compareTo( v2 ) > 0 );
  }

  private void compareToLessThanExpected( Version v1, Version v2)
  {
    assertTrue( "Version(\"" + v1.getVersionString() + "\") NOT less than to Version(\"" + v2.getVersionString() + "\").",
                v1.compareTo( v2 ) < 0 );
  }

  private void validVersionExpected( String verStr )
  {
    try { new Version( verStr ); }
    catch ( IllegalArgumentException e )
    {
      fail( "Unexpected invalid version <" + verStr + ">: " + e.getMessage() );
    }
  }

  private void inValidVersionExpected( String verStr )
  {
    try { new Version( verStr ); }
    catch ( IllegalArgumentException e ) { return; }
    fail( "Unexpected valid version <" + verStr + ">: " );
  }

}
