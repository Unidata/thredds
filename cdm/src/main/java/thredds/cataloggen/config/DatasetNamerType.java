// $Id: DatasetNamerType.java 63 2006-07-12 21:50:51Z edavis $

package thredds.cataloggen.config;

/**
 * Type-safe enumeration of CatalogGen DatasetNamer types.
 *
 * @author Ethan Davis (from John Caron's thredds.catalog.ServiceType)
 * @version $Revision: 63 $
 */
public final class DatasetNamerType
{
  private static java.util.HashMap hash = new java.util.HashMap(20);

  public final static DatasetNamerType REGULAR_EXPRESSION =
    new DatasetNamerType( "RegExp");
  public final static DatasetNamerType DODS_ATTRIBUTE =
    new DatasetNamerType( "DodsAttrib");

  private String DatasetNamerType;
  private DatasetNamerType( String name)
  {
    this.DatasetNamerType = name;
    hash.put(name, this);
  }

  /**
   * Find the DatasetNamerType that matches this name.
   * @param name
   * @return DatasetNamerType or null if no match.
   */
  public static DatasetNamerType getType( String name)
  {
    if ( name == null) return null;
    return (DatasetNamerType) hash.get( name);
  }

  /**
   * Return the string name.
   */
  public String toString()
  {
    return DatasetNamerType;
  }

}
