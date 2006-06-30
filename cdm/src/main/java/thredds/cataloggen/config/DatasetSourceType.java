// $Id: DatasetSourceType.java,v 1.4 2003/08/29 21:41:46 edavis Exp $

package thredds.cataloggen.config;

/**
 * Type-safe enumeration of CatalogGen DatasetSource types.
 *
 * @author Ethan Davis (from John Caron's thredds.catalog.ServiceType)
 * @version $Revision: 1.4 $
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
/*
 * $Log: DatasetSourceType.java,v $
 * Revision 1.4  2003/08/29 21:41:46  edavis
 * The following changes where made:
 *
 *  1) Added more extensive logging (changed from thredds.util.Log and
 * thredds.util.Debug to using Log4j).
 *
 * 2) Improved existing error handling and added additional error
 * handling where problems could fall through the cracks. Added some
 * catching and throwing of exceptions but also, for problems that aren't
 * fatal, added the inclusion in the resulting catalog of datasets with
 * the error message as its name.
 *
 * 3) Change how the CatGenTimerTask constructor is given the path to the
 * config files and the path to the resulting files so that resulting
 * catalogs are placed in the servlet directory space. Also, add ability
 * for servlet to serve the resulting catalogs.
 *
 * 4) Switch from using java.lang.String to using java.io.File for
 * handling file location information so that path seperators will be
 * correctly handled. Also, switch to java.net.URI rather than
 * java.io.File or java.lang.String where necessary to handle proper
 * URI/URL character encoding.
 *
 * 5) Add handling of requests when no path ("") is given, when the root
 * path ("/") is given, and when the admin path ("/admin") is given.
 *
 * 6) Fix the PUTting of catalogGenConfig files.
 *
 * 7) Start adding GDS DatasetSource capabilities.
 *
 * Revision 1.3  2003/08/20 17:55:25  edavis
 * Made some minor changes, mostly renaming variables.
 *
 * Revision 1.2  2003/03/18 21:12:46  edavis
 * Change text names for DODS_DIR and DODS_FILE_SERVER
 *
 * Revision 1.1.1.1  2002/12/11 22:27:55  edavis
 * CatGen into reorged thredds CVS repository.
 *
 */