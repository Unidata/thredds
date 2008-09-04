package ucar.nc2.units;

import junit.framework.*;

import java.text.ParseException;
import java.util.Calendar;

/**
 * _more_
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
    DateType startDateType = dr.getStart();
    DateType endDateType = dr.getEnd();
    System.out.println( "Current : " + d );
    System.out.println( "Start   : " + startDateType.toDateTimeStringISO() + " [" + startDateType.getDate().getTime() + "]." );
    System.out.println( "End     : " + endDateType.toDateTimeStringISO() + " [" + endDateType.getDate().getTime() + "]." );
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
    DateType start2DateType = dr.getStart();
    DateType end2DateType = dr.getEnd();
    System.out.println( "\nCurrent : " + d2 );
    System.out.println( "Start   : " + start2DateType.toDateTimeStringISO() + " [" + start2DateType.getDate().getTime() + "]." );
    System.out.println( "End     : " + end2DateType.toDateTimeStringISO() + " [" + end2DateType.getDate().getTime() + "]." );

    assertTrue( "Start dates are equal [" +
                startDateType.toDateTimeStringISO() + " - " + startDateType.getDate().getTime() +
                "] [" + start2DateType.toDateTimeStringISO() + " - " + start2DateType.getDate().getTime() + "]",
                ! startDateType.equals( start2DateType ) );
    assertTrue( "End dates are equal [" +
                endDateType.toDateTimeStringISO() + " - " + endDateType.getDate().getTime() +
                "] [" + end2DateType.toDateTimeStringISO() + " - " + end2DateType.getDate().getTime() + "]",
                ! endDateType.equals( end2DateType ) );
  }
}
