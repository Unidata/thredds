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
package thredds.util;

/**
 * Represent a version number formed by one or more non-negative integers
 * with a period separating each integer. E.g., "1.0" or "1.0.1" but not "1.0a".
 * <p>Augmented BNF:</p>
 * <pre>
 * nni = non-negative integer
 * version = nni *("." nni)
 * </pre>
 *
 * @author edavis
 * @since 4.0
 */
public class Version implements Comparable<Version>
{
  private final String versionString;
  private final int[] versionSegments;

  private final int hashCode;

  /**
   * Constructes a Version with the given version string.
   *
   * @param versionString the version string.
   * @throws IllegalArgumentException if version string is null or not a valid version number.
   */
  public Version( String versionString )
  {
    if ( versionString == null || versionString.equals( "" ))
      throw new IllegalArgumentException( "Version string must be non-empty and non-null.");

    if ( versionString.startsWith( "." ) || versionString.endsWith( "." ))
      throw new IllegalArgumentException( "Version string [] may not start or end with a period ('.').");

    // Split into segments.
    String[] tmpVerStringSegs = versionString.split( "\\." );

    if ( tmpVerStringSegs.length < 1 )
      throw new IllegalArgumentException( "Version string [" + versionString + "] must have at least one numerical segment.");

    // Parse segments making sure they are non-negative integers.
    int[] tmpVerSegs = new int[tmpVerStringSegs.length];
    for ( int i = 0; i < tmpVerStringSegs.length; i++ )
    {
      try
      {
        tmpVerSegs[i] = Integer.parseInt( tmpVerStringSegs[i] );
      }
      catch ( NumberFormatException e )
      {
        throw new IllegalArgumentException( "Version string [" + versionString + "] is not valid.", e);
      }
      if ( tmpVerSegs[i] < 0 )
        throw new IllegalArgumentException( "Segments of version string [" + versionString + "] must all be integers greater than zero." );
    }

    this.versionString = versionString;
    this.versionSegments = tmpVerSegs;

    // Compute hash code (this is an immutable object, so hash won't change)
    int tmpHashCode = 17;
    for ( int i : this.versionSegments )
      tmpHashCode = 37 * tmpHashCode + i;

    this.hashCode = tmpHashCode;
  }

  public String getVersionString()
  {
    return this.versionString;
  }

  public int compareTo( Version thatVersion )
  {
    if ( thatVersion == null )
      throw new IllegalArgumentException( "Version must be non-null.");

    // Loop through shortest number of segments and compare.
    int minLength = Math.min( this.versionSegments.length, thatVersion.versionSegments.length );
    for ( int i = 0; i < minLength; i++ )
    {
      if ( this.versionSegments[i] < thatVersion.versionSegments[i] )
        return -1; // this less than given
      else if ( this.versionSegments[i] > thatVersion.versionSegments[i] )
        return 1; // this greater than given
    }

    // If both versions have the same number of segments, they are equal.
    if ( this.versionSegments.length == thatVersion.versionSegments.length )
      return 0;

    // Determine which version has more segments.
    int[] tmpVerSegs;
    boolean thisNotThat;
    if ( this.versionSegments.length > minLength )
    {
      tmpVerSegs = this.versionSegments;
      thisNotThat = true;
    }
    else
    {
      tmpVerSegs = thatVersion.versionSegments;
      thisNotThat = false;
    }

    // If any of the extra segments are greater than zero, then
    // the version with more segments is greater.
    for ( int i = minLength; i < tmpVerSegs.length; i++ )
      if ( tmpVerSegs[i] > 0 )
        return thisNotThat ? 1 : -1;

    // If all extra segments are zero, versions are equal.
    return 0;

  }

  @Override
  public int hashCode()
  {
    return this.hashCode;
  }

  @Override
  public boolean equals( Object obj )
  {
    if ( obj == this )
      return true;
    if ( ! ( obj instanceof Version ))
      return false;

    Version v = (Version) obj;
    return this.compareTo( v ) == 0;
  }

  @Override
  public String toString()
  {
    return this.versionString;
  }
}
