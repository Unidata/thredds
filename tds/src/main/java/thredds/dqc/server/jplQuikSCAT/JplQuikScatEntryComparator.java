// $Id$
package thredds.dqc.server.jplQuikSCAT;

import java.util.Comparator;
import java.util.Collection;
import java.io.IOException;

import ucar.nc2.dods.DODSStructure;
import ucar.ma2.Array;
import ucar.ma2.Index;

/**
 * A description
 *
 * User: edavis
 * Date: Jan 27, 2004
 * Time: 5:47:47 PM
 */
public class JplQuikScatEntryComparator implements Comparator
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( JplQuikScatEntryComparator.class );

  private JplQuikScatEntryComparatorType type = null;

  public JplQuikScatEntryComparator( JplQuikScatEntryComparatorType type)
  {
    this.type = type;
  }
  public int compare( Object obj1, Object obj2 )
  {
    if ( ! ( obj1 instanceof JplQuikScatEntry && obj2 instanceof JplQuikScatEntry) )
    {
      String tmp = "compare(): both objects should be JPL QuikSCAT entries <" + obj1.getClass().getName() +
              " - " + obj2.getClass().getName() + "> but are not.";
      log.debug( tmp);
      throw new ClassCastException( tmp);
    }
    JplQuikScatEntry entry1 = (JplQuikScatEntry) obj1;
    JplQuikScatEntry entry2 = (JplQuikScatEntry) obj2;

    // Sort by Date.
    if ( this.type == JplQuikScatEntryComparatorType.DATE)
    {
      return( entry1.getDate().compareTo( entry2.getDate()));
    }
    // Sort by Date, reverse order.
    if ( this.type == JplQuikScatEntryComparatorType.DATE_REVERSE)
    {
      return( entry2.getDate().compareTo( entry1.getDate()));
    }
    // Sort by longitude.
    else if ( this.type == JplQuikScatEntryComparatorType.LONGITUDE)
    {
      return( Float.compare( entry1.getLongitude(), entry2.getLongitude() ) );
    }
    // Sort by longitude, reverse order.
    else if ( this.type == JplQuikScatEntryComparatorType.LONGITUDE_REVERSE)
    {
      return( Float.compare( entry2.getLongitude(), entry1.getLongitude() ) );
    }
    // Natural ordering??? - sort by date.
    else
    {
      return( entry1.getDate().compareTo( entry2.getDate()));
    }
  }

  public boolean equals( Object obj)
  {
    if ( ! ( obj instanceof JplQuikScatEntryComparator ) )
    {
      return( false);
    }

    return( this.type.equals( ( (JplQuikScatEntryComparator) obj).type ) ? true : false );
  }

  /**
   * Type-safe enumeration for the JplQuikScatEntryComparator types.
   */
  public static final class JplQuikScatEntryComparatorType
  {
    private static java.util.HashMap hash = new java.util.HashMap(20);

    public final static JplQuikScatEntryComparatorType DATE = new JplQuikScatEntryComparatorType( "Date");
    public final static JplQuikScatEntryComparatorType DATE_REVERSE = new JplQuikScatEntryComparatorType( "DateReverse");
    public final static JplQuikScatEntryComparatorType LONGITUDE = new JplQuikScatEntryComparatorType( "Longitude");
    public final static JplQuikScatEntryComparatorType LONGITUDE_REVERSE = new JplQuikScatEntryComparatorType( "LongitudeReverse");

    private String name;
    private JplQuikScatEntryComparatorType( String name)
    {
      this.name = name;
      hash.put( this.name, this);
    }

    /**
     * Find the JplQuikScatEntryComparatorType that matches this name.
     * @param name
     * @return JplQuikScatEntryComparatorType or null if no match.
     */
    public static JplQuikScatEntryComparatorType getType( String name)
    {
      if ( name == null) return null;
      return ( (JplQuikScatEntryComparatorType) hash.get( name));
    }

    /**
     * Return the string name.
     */
    public String toString()
    {
      return this.name;
    }


  }
}

/*
 * $Log: JplQuikScatEntryComparator.java,v $
 * Revision 1.3  2006/01/20 20:42:03  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.2  2005/04/05 22:37:03  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.1  2004/03/05 06:32:03  edavis
 * Add DqcHandler and backend storage classes for the JPL QuikSCAT
 * DODS File Server.
 *
 */