package ucar.nc2.time;

import org.joda.time.Chronology;
import org.joda.time.chrono.GregorianChronology;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.JulianChronology;
import uk.ac.rdg.resc.edal.time.AllLeapChronology;
import uk.ac.rdg.resc.edal.time.NoLeapChronology;
import uk.ac.rdg.resc.edal.time.ThreeSixtyDayChronology;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements CF calendar attribute.
 * Uses joda-time, may switch to JSP 310 at a later date.
 * So joda-time classes are not exposed.
 *
 * @author caron
 * @since 3/22/11
 */
public enum Calendar {
  gregorian, proleptic_gregorian, noleap, all_leap, uniform30day, julian, none;

  public static Calendar get(String s) {
    if (s == null) return null;
    if (s.equalsIgnoreCase("gregorian") || s.equalsIgnoreCase("standard")) return Calendar.gregorian;
    if (s.equalsIgnoreCase("proleptic_gregorian") || s.equalsIgnoreCase("ISO8601"))return Calendar.proleptic_gregorian;
    if (s.equalsIgnoreCase("noleap") || s.equalsIgnoreCase("365_day")) return Calendar.noleap;
    if (s.equalsIgnoreCase("all_leap") || s.equalsIgnoreCase("366_day")) return Calendar.all_leap;
    if (s.equalsIgnoreCase("uniform30day") || s.equalsIgnoreCase("360_day")) return Calendar.uniform30day;
    if (s.equalsIgnoreCase("julian")) return Calendar.julian;
    if (s.equalsIgnoreCase("none")) return Calendar.none;
    return null;
  }

  public static Calendar getDefault() {
    return proleptic_gregorian;
  }

  public static boolean isDefaultChronology(Calendar cal) {
    return cal == null || cal.equals(getDefault()) ||  cal.equals(Calendar.none) ;
  }

  /**
   * Map of CF identifiers for calendar systems to joda-time Chronologies
   */
  private static final Map<Calendar, Chronology> CHRONOLOGIES = new HashMap<Calendar, Chronology>();

  static {
    // Implements the Gregorian/Julian calendar system which is the calendar system used in most of the world. Wherever possible,
    // it is recommended to use the ISOChronology instead.
    // The Gregorian calendar replaced the Julian calendar, and the point in time when this chronology switches can be controlled
    // using the second parameter of the getInstance method. By default this cutover is set to the date the Gregorian calendar was first instituted, October 15, 1582.
    // Before this date, this chronology uses the proleptic Julian calendar (proleptic means extending indefinitely).
    // The Julian calendar has leap years every four years, whereas the Gregorian has special rules for 100 and 400 years.
    // A meaningful result will thus be obtained for all input values. However before 8 CE, Julian leap years were irregular,
    // and before 45 BCE there was no Julian calendar.
    // This chronology differs from GregorianCalendar in that years in BCE are returned correctly. Thus year 1 BCE is returned as -1
    // instead of 1. The yearOfEra field produces results compatible with GregorianCalendar.
    // The Julian calendar does not have a year zero, and so year -1 is followed by year 1. If the Gregorian cutover date is
    // specified at or before year -1 (Julian), year zero is defined. In other words, the proleptic Gregorian chronology used by this class has a year zero.
    CHRONOLOGIES.put(Calendar.gregorian, org.joda.time.chrono.GJChronology.getInstanceUTC());

    // Implements a pure proleptic Gregorian calendar system, which defines every fourth year as leap, unless the year
    // is divisible by 100 and not by 400. This improves upon the Julian calendar leap year rule.
    // Although the Gregorian calendar did not exist before 1582 CE, this chronology assumes it did, thus it is proleptic.
    // This implementation also fixes the start of the year at January 1, and defines the year zero.
    CHRONOLOGIES.put(Calendar.proleptic_gregorian, ISOChronology.getInstanceUTC());
    CHRONOLOGIES.put(Calendar.none, ISOChronology.getInstanceUTC());

    // Implements a pure proleptic Julian calendar system, which defines every fourth year as leap. This implementation follows
    // the leap year rule strictly, even for dates before 8 CE, where leap years were actually irregular. In the Julian calendar,
    // year zero does not exist: 1 BCE is followed by 1 CE.
    // Although the Julian calendar did not exist before 45 BCE, this chronology assumes it did, thus it is proleptic.
    // This implementation also fixes the start of the year at January 1.
    CHRONOLOGIES.put(Calendar.julian, JulianChronology.getInstanceUTC());

    CHRONOLOGIES.put(Calendar.all_leap, AllLeapChronology.getInstanceUTC());
    CHRONOLOGIES.put(Calendar.noleap, NoLeapChronology.getInstanceUTC());
    CHRONOLOGIES.put(Calendar.uniform30day, ThreeSixtyDayChronology.getInstanceUTC());
  }

  /**
   * Return joda Chronology corresponding to this Calendar, using UTC time zone.
   * @param cal want this Calendar, or null to use the default.
   * @return hronology corresponding to this Calendar
   */
  static Chronology getChronology(Calendar cal) {
    if (cal == null) cal = getDefault();
    return CHRONOLOGIES.get(cal);
  }

}

/*

http://www.unidata.ucar.edu/software/udunits/udunits-2/udunits2lib.html#Time

You should use a true calendar package rather than the UDUNITS-2 package to handle time. Having said that, many people use
the time-handling capabilities of the UDUNITS-2 package because it supports CDM.UNITS like "seconds since 1970-01-01".
You should be aware, however, that the hybrid Gregorian/Julian calendar used by the UDUNITS-2 package cannot be changed.
Dates on or after 1582-10-15 are assumed to be Gregorian dates; dates before that are assumed to be Julian dates.
In particular, the year 1 BCE is immediately followed by the year 1 CE.

 */

/*

CF

 4.4.1. Calendar
In order to calculate a new date and time given a base date, base time and a time increment one must know what calendar to use.
For this purpose we recommend that the calendar be specified by the attribute calendar which is assigned to the time coordinate variable.
The values currently defined for calendar are:

gregorian or standard
Mixed Gregorian/Julian calendar as defined by Udunits. This is the default.

proleptic_gregorian
A Gregorian calendar extended to dates before 1582-10-15. That is, a year is a leap year if either (i) it is divisible by 4 but not by 100 or (ii) it is divisible by 400.

noleap or 365_day
Gregorian calendar without leap years, i.e., all years are 365 days long.

all_leap or 366_day
Gregorian calendar with every year being a leap year, i.e., all years are 366 days long.

360_day
All years are 360 days divided into 30 day months.

julian
Julian calendar.

none
No calendar.

The calendar attribute may be set to none in climate experiments that simulate a fixed time of year.
The time of year is indicated by the date in the reference time of the units attribute.
The time coordinate that might apply in a perpetual July experiment are given in the following example.

Example 4.5. Perpetual time axis

variables:
 double time(time) ;
   time:long_name = "time" ;
   time:units = "days since 1-7-15 0:0:0" ;
   time:calendar = "none" ;
data:
 time = 0., 1., 2., ...;


Here, all days simulate the conditions of 15th July, so it does not make sense to give them different dates.
The time coordinates are interpreted as 0, 1, 2, etc. days since the start of the experiment.

If none of the calendars defined above applies (e.g., calendars appropriate to a different paleoclimate era),
a non-standard calendar can be defined. The lengths of each month are explicitly defined with the month_lengths attribute of the time axis:

month_lengths
A vector of size 12, specifying the number of days in the months from January to December (in a non-leap year).

If leap years are included, then two other attributes of the time axis should also be defined:

leap_year
An example of a leap year. It is assumed that all years that differ from this year by a multiple of four are also leap years.
If this attribute is absent, it is assumed there are no leap years.

leap_month
A value in the range 1-12, specifying which month is lengthened by a day in leap years (1=January). If this attribute
is not present, February (2) is assumed. This attribute is ignored if leap_year is not specified.

The calendar attribute is not required when a non-standard calendar is being used. It is sufficient to define the c
alendar using the month_lengths attribute, along with leap_year, and leap_month as appropriate. However, the calendar attribute is allowed to take non-standard values and in that case defining the non-standard calendar using the appropriate attributes is required.

Example 4.6. Paleoclimate time axis

double time(time) ;
 time:long_name = "time" ;
 time:units = "days since 1-1-1 0:0:0" ;
 time:calendar = "126 kyr B.P." ;
 time:month_lengths = 34, 31, 32, 30, 29, 27, 28, 28, 28, 32, 32, 34 ;


The mixed Gregorian/Julian calendar used by Udunits is explained in the following excerpt from the udunits(3) man page:

The udunits(3) package uses a mixed Gregorian/Julian  calen-
dar  system.   Dates  prior to 1582-10-15 are assumed to use
the Julian calendar, which was introduced by  Julius  Caesar
in 46 BCE and is based on a year that is exactly 365.25 days
long.  Dates on and after 1582-10-15 are assumed to use  the
Gregorian calendar, which was introduced on that date and is
based on a year that is exactly 365.2425 days long.  (A year
is  actually  approximately 365.242198781 days long.)  Seem-
ingly strange behavior of the udunits(3) package can  result
if  a user-given time interval includes the changeover date.
For example, utCalendar() and utInvCalendar() can be used to
show that 1582-10-15 *preceded* 1582-10-14 by 9 days.

Due to problems caused by the discontinuity in the default mixed Gregorian/Julian calendar, we strongly recommend that this calendar
should only be used when the time coordinate does not cross the discontinuity. For time coordinates that do cross the discontinuity
the proleptic_gregorian calendar should be used instead.
  */