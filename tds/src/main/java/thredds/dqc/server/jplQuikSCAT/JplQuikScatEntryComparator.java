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
// $Id: JplQuikScatEntryComparator.java 51 2006-07-12 17:13:13Z caron $
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