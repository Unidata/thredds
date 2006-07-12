// $Id$

package thredds.cataloggen.config;

/**
 * Type-safe enumeration of CatalogGen DatasetNamer types.
 *
 * @author Ethan Davis (from John Caron's thredds.catalog.ServiceType)
 * @version $Revision$
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
/*
 * $Log: DatasetNamerType.java,v $
 * Revision 1.2  2004/05/11 20:38:46  edavis
 * Update for changes to thredds.catalog object model (still InvCat 0.6).
 * Start adding some logging statements.
 *
 * Revision 1.1.1.1  2002/12/11 22:27:55  edavis
 * CatGen into reorged thredds CVS repository.
 *
 */