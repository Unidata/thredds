package ucar.nc2.time;

import org.joda.time.DateTimeZone;

import java.util.Set;
import java.util.TimeZone;

/**
 * Encapsolate TimeZone functionality.
 *
 * @author caron
 * @since 10/28/12
 */
public class CalendarTimeZone {

  public static Set<String> getAvailableIDs() {
    return DateTimeZone.getAvailableIDs();
  }

  private final DateTimeZone jodaTimeZone;

  /**
    * Gets a time zone instance for the specified time zone id.
    * <p>
    * The time zone id may be one of those returned by getAvailableIDs.
    * Short ids, as accepted by {@link java.util.TimeZone}, are not accepted.
    * All IDs must be specified in the long format.
    * The exception is UTC, which is an acceptable id.
    * <p>
    * Alternatively a locale independent, fixed offset, datetime zone can
    * be specified. The form <code>[+-]hh:mm</code> can be used.
    *
    * @param id  the ID of the datetime zone, null means default
    * @throws IllegalArgumentException if the ID is not recognised
    */
  public CalendarTimeZone(String id) {
    jodaTimeZone = DateTimeZone.forID(id);
  }

  public CalendarTimeZone(TimeZone zone) {
    jodaTimeZone = DateTimeZone.forTimeZone(zone);
  }

  // package private
  DateTimeZone getJodaTimeZone() {
    return jodaTimeZone;
  }
}
