// $Id: DatasetSourceStructure.java 63 2006-07-12 21:50:51Z edavis $

package thredds.cataloggen.config;

/**
 * Type-safe enumeration of CatalogGen DatasetSource structures.
 *
 * @author Ethan Davis (from John Caron's thredds.catalog.ServiceType)
 * @version $Revision: 63 $
 */
public final class DatasetSourceStructure
{
  private static java.util.HashMap hash = new java.util.HashMap(20);

  public final static DatasetSourceStructure FLAT =
    new DatasetSourceStructure( "Flat");
  public final static DatasetSourceStructure DIRECTORY_TREE =
    new DatasetSourceStructure( "DirTree");

  private String structureName;
  private DatasetSourceStructure( String name)
  {
    this.structureName = name;
    hash.put( name, this);
  }

  /**
   * Find the DatasetSourceStructure that matches this name.
   * @param name
   * @return DatasetSourceStructure or null if no match.
   */
  public static DatasetSourceStructure getStructure( String name)
  {
    if ( name == null) return null;
    return (DatasetSourceStructure) hash.get( name);
  }

  /**
   * Return the string name.
   */
  public String toString()
  {
    return structureName;
  }

}
