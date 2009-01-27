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
package ucar.nc2.units;

import junit.framework.*;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * test that DateRange moves when start or end is "present".
 *
 * @author edavis
 * @since 4.0
 */
public class TestDateRange extends TestCase
{
  public TestDateRange( String name )
  {
    super( name );
  }

  /**
   * Check if start and end dates change over time for a DateRange
   * with start set to "present" and a duration set.
   */
  public void testStartPresentAndDuration()
  {
    DateRange drStartIsPresent;
    try
    {
      drStartIsPresent = new DateRange( new DateType( "present", null, null ), null, new TimeDuration( "P7D" ), null );
    }
    catch ( ParseException e )
    {
      fail( "Failed to parse \"present\" and/or \"P7D\": " + e.getMessage() );
      return;
    }
    checkValuesAfterDelay( drStartIsPresent );
  }

  /**
   * Check if start and end dates change over time for a DateRange
   * with end set to "present" and a duration set.
   */
  public void testEndPresentAndDuration()
  {
    DateRange drEndIsPresent;
    try
    {
      drEndIsPresent = new DateRange( null, new DateType( "present", null, null ), new TimeDuration( "P7D" ), null );
    }
    catch ( ParseException e )
    {
      fail( "Failed to parse \"present\" and/or \"P7D\": " + e.getMessage() );
      return;
    }
    checkValuesAfterDelay( drEndIsPresent );
  }

  private void checkValuesAfterDelay( DateRange dr )
  {
    long d = Calendar.getInstance().getTimeInMillis();
    Date startDate = dr.getStart().getDate();
    Date endDate = dr.getEnd().getDate();
    System.out.println( "Current : " + d );
    System.out.println( "Start   :  [" + startDate.getTime() + "]." );
    System.out.println( "End     :  [" + endDate.getTime() + "]." );

    try
    {
      synchronized ( this )
      {
        boolean cond = false;
        while ( !cond )
        {
          this.wait( 10000 );
          cond = true;
        }
      }
    }
    catch ( InterruptedException e )
    {
      fail( "Failed to wait: " + e.getMessage() );
      return;
    }

    long d2 = Calendar.getInstance().getTimeInMillis();
    Date startDate2 = dr.getStart().getDate();
    Date endDate2 = dr.getEnd().getDate();
    System.out.println( "\nCurrent : " + d2 );
    System.out.println( "Start   : [" + startDate2.getTime() + "]." );
    System.out.println( "End     : [" + endDate2.getTime() + "]." );

    assertTrue( "Start dates are equal ", !startDate.equals( startDate2 ) );
    assertTrue( "End dates are equal [" , !endDate.equals( endDate2 ) );
  }
}
