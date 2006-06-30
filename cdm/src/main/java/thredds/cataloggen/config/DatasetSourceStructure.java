// $Id: DatasetSourceStructure.java,v 1.2 2003/08/20 17:55:25 edavis Exp $

package thredds.cataloggen.config;

/**
 * Type-safe enumeration of CatalogGen DatasetSource structures.
 *
 * @author Ethan Davis (from John Caron's thredds.catalog.ServiceType)
 * @version $Revision: 1.2 $
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
/*
 * $Log: DatasetSourceStructure.java,v $
 * Revision 1.2  2003/08/20 17:55:25  edavis
 * Made some minor changes, mostly renaming variables.
 *
 * Revision 1.1.1.1  2002/12/11 22:27:55  edavis
 * CatGen into reorged thredds CVS repository.
 *
 */