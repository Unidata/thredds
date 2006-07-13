// $Id: DatasetSourceType.java 63 2006-07-12 21:50:51Z edavis $

package thredds.cataloggen.config;

/**
 * Type-safe enumeration of CatalogGen DatasetSource types.
 *
 * @author Ethan Davis (from John Caron's thredds.catalog.ServiceType)
 * @version $Revision: 63 $
 */
public final class DatasetSourceType
{
  private static java.util.HashMap hash = new java.util.HashMap(20);

  public final static DatasetSourceType LOCAL = new DatasetSourceType( "Local");
  public final static DatasetSourceType DODS_FILE_SERVER = new DatasetSourceType( "DodsFileServer");
  public final static DatasetSourceType DODS_DIR = new DatasetSourceType( "DodsDir");
  public final static DatasetSourceType GRADS_DATA_SERVER = new DatasetSourceType( "GrADSDataServer");

  private String typeName;
  private DatasetSourceType( String name)
  {
    this.typeName = name;
    hash.put( name, this);
  }

  /**
   * Find the DatasetSourceType that matches this name.
   * @param name
   * @return DatasetSourceType or null if no match.
   */
  public static DatasetSourceType getType( String name)
  {
    if ( name == null) return null;
    return ( (DatasetSourceType) hash.get( name));
  }

  /**
   * Return the string name.
   */
  public String toString()
  {
    return typeName;
  }

}
