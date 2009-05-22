/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package thredds.catalog;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TimeLargeCatalogRead
{
  static final String catURL = "http://motherlode.ucar.edu:9080/thredds/radarServer/nexrad/level2/IDD?stn=KARX&time_start=2009-04-07T:00:00:00Z&time_end=2009-05-22T16:44:39Z";
  //static final String catURL = "http://motherlode.ucar.edu:9080/thredds/radarServer/nexrad/level2/IDD?stn=KARX&time_start=2009-04-07T:00:00:00Z&time_end=2009-05-22T16:44:39Z";

  public static void main( String[] args )
  {
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );

    InvCatalogImpl cat = null;
    long cum = 0;
    int numAttempts = 100;
    for ( int i = 0; i < numAttempts; i++ )
    {
      long start = System.currentTimeMillis();
      cat = fac.readXML( catURL );
      long done = System.currentTimeMillis();
      long elapsed = done - start;
      cum+=elapsed;
      System.out.println( "Read catalog ["+i+"]: " + elapsed + "\n" );
    }
    System.out.println( "Cum=" + cum );
    System.out.println( "Avg=" + cum/ numAttempts );

    StringBuilder sb = new StringBuilder();
    if ( cat.check( sb ) )
      System.out.println( "Failed check:\n" + sb );
    else
      System.out.println( "OK check:\n" + sb );

    System.out.println( "Done" );

  }
}
