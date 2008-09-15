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
