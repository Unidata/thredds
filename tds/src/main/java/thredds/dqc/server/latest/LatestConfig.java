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
// $Id: LatestConfig.java 51 2006-07-12 17:13:13Z caron $
package thredds.dqc.server.latest;

import java.util.*;

/**
 * Configuration for LatestDqcHandler.
 *
 * The configuration for LatestDqcHandler is a collection of "latest" items.
 * Each item can be be retreived by using the item's ID.
 *
 * @author edavis
 * @since Sep 20, 2005 3:25:18 PM
 */
class LatestConfig
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( LatestConfig.class );

  private Map items;

  /** Constructor */
  public LatestConfig()
  {
    items = new HashMap();
  }

  /** Return true if config contains no items. */
  public boolean isEmpty()
  {
    return items.isEmpty();
  }

  /**
   * Add an item to the config. Return true if the item is added successfully.
   * Neither the ID or the item can be null. TheID also cannot be an empty
   * string (""). Duplicate IDs are not allowed.
   *
   * @param id the ID of the item to be added.
   * @param item the item being added.
   * @return true if the item is added successfully, false otherwise.
   */
  public boolean addItem( String id, Item item )
  {
    if ( id == null ) throw new IllegalArgumentException( "Null item ID not allowed." );
    if ( id.equals( "") ) throw new IllegalArgumentException( "Empty string for item ID not allowed." );
    if ( item == null ) throw new IllegalArgumentException( "Null item not allowed." );
    if ( items.keySet().contains( id ) )
    {
      log.info( "addItem(): Config already contains Item with this id <" + id + ">.");
      return false;
    }
    //  throw new IllegalArgumentException( "Item with given ID <" + id + "> already exists." );
    if ( null != items.put( id, item ) )
    {
      return false;
    }
    return true;
  }

  /**
   * Remove the item with the given ID.
   *
   * @param id the ID of the item to remove.
   * @return true if there is a matching item and it is removed successfully.
   */
  public boolean removeItem( String id )
  {
    if ( null == items.remove( id) )
    {
      return( false );
    }
    return true;
  }

  /**
   * Return the set of IDs of the items contained in this config.
   *
   * @return the set of IDs for the items contained in this config.
   */
  public Set getIds()
  {
    return items.keySet();
  }

  /**
   * Return the Item from this config with the given ID.
   *
   * @param id the ID of the Item to be retreived.
   * @return the Item with the given ID, or null if there is no Item with the given ID.
   */
  public Item getItem( String id )
  {
    return (Item) items.get( id );
  }

  public String toString()
  {
    StringBuffer buf = new StringBuffer( "LatestConfig[" );
    for ( Iterator it = items.keySet().iterator(); it.hasNext(); )
    {
      String curId = (String) it.next();
      buf.append( "\n    ").append( this.getItem( curId).toString() );
    }
    buf.append( "\n]");
    return buf.toString();
  }

  public boolean equals( Object o )
  {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    final LatestConfig config = (LatestConfig) o;

    if ( items != null ? !items.equals( config.items ) : config.items != null ) return false;

    return true;
  }

  public int hashCode()
  {
    return ( items != null ? items.hashCode() : 0 );
  }

  /**
   * Configuration item for LatestDqcHandler.
   */
  public static class Item
  {
    private String id;
    private String name;
    private String dirLocation;
    private String datasetNameMatchPattern;
    private String datasetTimeSubstitutionPattern;
    // @todo Allow config of time format and name.
    //private String datasetTimeFormat;
    //private String datasetTimeForName;
    private String serviceBaseURL;
    private String invCatSpecVersion;
    private String dqcSpecVersion;

    private String refId;
    //private Item referencedItem;

    /** Constructor for Item that refers to another Item. */
    protected Item( String id, String refId )
    {
      if ( id == null || refId == null ) throw new IllegalArgumentException( "Neither Id nor RefID may be null.");
      this.id = id;
      this.refId = refId;
    }

    /** Constructor for Item that does not refer to another Item. */
    protected Item( String id, String name, String dirLocation, String datasetNameMatchPattern, String datasetTimeSubstitutionPattern, String serviceBaseURL, String invCatSpecVersion, String dqcSpecVersion )
    {
      if ( id == null || name == null || dirLocation == null || datasetNameMatchPattern == null ||
           datasetTimeSubstitutionPattern == null || serviceBaseURL == null ||
           invCatSpecVersion == null || dqcSpecVersion == null )
      {
        throw new IllegalArgumentException( "Null parameter not allowed." );
      }
      this.id = id;
      this.name = name;
      this.dirLocation = dirLocation;
      this.datasetNameMatchPattern = datasetNameMatchPattern;
      this.datasetTimeSubstitutionPattern = datasetTimeSubstitutionPattern;
      this.serviceBaseURL = serviceBaseURL;
      this.invCatSpecVersion = invCatSpecVersion;
      this.dqcSpecVersion = dqcSpecVersion;
    }

    protected void setReferencedItem( Item referencedItem )
    {
      this.name = referencedItem.getName();
      this.dirLocation = referencedItem.getDirLocation();
      this.datasetNameMatchPattern = referencedItem.getDatasetNameMatchPattern();
      this.datasetTimeSubstitutionPattern = referencedItem.getDatasetTimeSubstitutionPattern();
      this.serviceBaseURL = referencedItem.getServiceBaseURL();
      this.invCatSpecVersion = referencedItem.getInvCatSpecVersion();
      this.dqcSpecVersion = referencedItem.getDqcSpecVersion();
    }

    public String getId()
    {
      return id;
    }

    public String getRefId()
    {
      return refId;
    }

    public String getName()
    {
      return name;
    }

    public String getDirLocation()
    {
      return dirLocation;
    }

    public String getDatasetNameMatchPattern()
    {
      return datasetNameMatchPattern;
    }

    public String getDatasetTimeSubstitutionPattern()
    {
      return datasetTimeSubstitutionPattern;
    }

    public String getServiceBaseURL()
    {
      return serviceBaseURL;
    }

    public String getInvCatSpecVersion()
    {
      return invCatSpecVersion;
    }

    public String getDqcSpecVersion()
    {
      return dqcSpecVersion;
    }

    public String toString()
    {
      StringBuffer buf = new StringBuffer( "LatestConfig.Item[");
      buf.append( this.id ).append( ", ");
      if ( this.refId != null )
      {
        buf.append( this.refId ).append("]");
      }
      else
      {
        buf.append( this.name ).append( ", " )
                .append( this.dirLocation ).append( ", " )
                .append( this.datasetNameMatchPattern ).append( ", " )
                .append( this.datasetTimeSubstitutionPattern ).append( ", " )
                .append( this.serviceBaseURL ).append( ", " )
                .append( this.invCatSpecVersion ).append( ", " )
                .append( this.dqcSpecVersion ).append( "]" );
      }
      return buf.toString();
    }

    public boolean equals( Object o )
    {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final Item item = (Item) o;

      if ( datasetNameMatchPattern != null ? !datasetNameMatchPattern.equals( item.datasetNameMatchPattern ) : item.datasetNameMatchPattern != null ) return false;
      if ( datasetTimeSubstitutionPattern != null ? !datasetTimeSubstitutionPattern.equals( item.datasetTimeSubstitutionPattern ) : item.datasetTimeSubstitutionPattern != null ) return false;
      if ( dirLocation != null ? !dirLocation.equals( item.dirLocation ) : item.dirLocation != null ) return false;
      if ( dqcSpecVersion != null ? !dqcSpecVersion.equals( item.dqcSpecVersion ) : item.dqcSpecVersion != null ) return false;
      if ( id != null ? !id.equals( item.id ) : item.id != null ) return false;
      if ( invCatSpecVersion != null ? !invCatSpecVersion.equals( item.invCatSpecVersion ) : item.invCatSpecVersion != null ) return false;
      if ( name != null ? !name.equals( item.name ) : item.name != null ) return false;
      if ( refId != null ? !refId.equals( item.refId ) : item.refId != null ) return false;
      if ( serviceBaseURL != null ? !serviceBaseURL.equals( item.serviceBaseURL ) : item.serviceBaseURL != null ) return false;

      return true;
    }

    public int hashCode()
    {
      int result;
      result = ( id != null ? id.hashCode() : 0 );
      result = 29 * result + ( refId != null ? refId.hashCode() : 0 );
      result = 29 * result + ( name != null ? name.hashCode() : 0 );
      result = 29 * result + ( dirLocation != null ? dirLocation.hashCode() : 0 );
      result = 29 * result + ( datasetNameMatchPattern != null ? datasetNameMatchPattern.hashCode() : 0 );
      result = 29 * result + ( datasetTimeSubstitutionPattern != null ? datasetTimeSubstitutionPattern.hashCode() : 0 );
      result = 29 * result + ( serviceBaseURL != null ? serviceBaseURL.hashCode() : 0 );
      result = 29 * result + ( invCatSpecVersion != null ? invCatSpecVersion.hashCode() : 0 );
      result = 29 * result + ( dqcSpecVersion != null ? dqcSpecVersion.hashCode() : 0 );
      return result;
    }
  }
}
/*
 * $Log: LatestConfig.java,v $
 * Revision 1.3  2006/03/01 23:11:26  edavis
 * Minor fix.
 *
 * Revision 1.2  2006/01/20 20:42:03  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.1  2005/09/30 21:51:37  edavis
 * Improve "Latest" DqcHandler so it can deal with new IDD naming conventions:
 * new configuration file format; add LatestDqcHandler which handles new and old
 * config file formats; use LatestDqcHandler as a proxy for LatestModel.
 *
 */