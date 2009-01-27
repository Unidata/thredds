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

import java.util.Arrays;

/**
 * Represents version numbers that have the form of one or more positive integers each seperated by a dot.
 * E.g., "1.0" or "1.0.1" but not "1.0a".
 *
 * @author edavis
 * @since Aug 23, 2007 4:56:06 PM
 */
public class Version
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( Version.class );

  private String versionString;
  private int[] versionSegments;

  /**
   * Constructes a Version with the given version string.
   *
   * @param versionString the version string.
   * @throws IllegalArgumentException if version string is null or not a valid version number.
   */
  public Version( String versionString )
  {
    if ( versionString == null ) throw new IllegalArgumentException( "Version string must not be null.");
    this.versionString = versionString;

    String[] tmpVerSegs = versionString.split( "\\.");
    this.versionSegments = new int[tmpVerSegs.length];
    for ( int i = 0; i < tmpVerSegs.length; i++ )
    {
      this.versionSegments[i] = Integer.parseInt( tmpVerSegs[i] );
      if ( this.versionSegments[i] < 0)
        throw new IllegalArgumentException( "Version segments must be integers greater than zero <" + versionString + ">.");
    }
  }

  public String getVersionString()
  {
    return versionString;
  }

  public int compareTo( Version v)
  {
    // Loop through shortest number of segments and compare.
    int minLength = Math.min( this.versionSegments.length, v.versionSegments.length );
    for ( int i = 0; i < minLength; i++)
    {
      if ( this.versionSegments[i] < v.versionSegments[i])
        return -1; // this less than given
      else if ( this.versionSegments[i] > v.versionSegments[i] )
        return 1; // this greater than given
    }

    // Deal with given Version having more segments than this Version
    if ( v.versionSegments.length > minLength )
    {
      for ( int i = minLength; i < v.versionSegments.length; i++)
      {
        if ( v.versionSegments[i] > 0)
          return -1; // this less than given
      }
      return 0; // this equal to given
    }

    // Deal with this Version having more segments than given Version
    else if ( this.versionSegments.length > minLength )
    {
      for ( int i = minLength; i < this.versionSegments.length; i++ )
      {
        if ( this.versionSegments[i] > 0 )
          return 1; // this greater than given
      }
      return 0; // this equal to given
    }

    // Same number of segments in this and given.
    else
      return 0; // this equal to given
  }

  public boolean greaterThan( Version v)
  {
    if ( compareTo( v) > 0)
      return true;
    return false;
  }
  public boolean lessThan( Version v)
  {
    if ( compareTo( v) < 0)
      return true;
    return false;
  }

  public boolean equals( Object o )
  {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    Version version = (Version) o;

    if ( !Arrays.equals( versionSegments, version.versionSegments ) ) return false;
    if ( versionString != null ? !versionString.equals( version.versionString ) : version.versionString != null )
    {
      return false;
    }

    return true;
  }

  public int hashCode()
  {
    int result;
    result = ( versionString != null ? versionString.hashCode() : 0 );
    result = 31 * result + ( versionSegments != null ? Arrays.hashCode( versionSegments ) : 0 );
    return result;
  }

}
