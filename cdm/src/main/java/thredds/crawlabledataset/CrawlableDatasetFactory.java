// $Id$
package thredds.crawlabledataset;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

/**
 * A description
 *
 * @author edavis
 * @since Jun 23, 2005T10:29:26 PM
 */
public class CrawlableDatasetFactory
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( CrawlableDatasetFactory.class );

  private static String defaultClassName = "thredds.crawlabledataset.CrawlableDatasetFile";

  /**
   * Construct a CrawlableDataset for the given path using the
   * CrawlableDataset implementation indicated by the given class name.
   *
   * The class given by className is used to instantiate the requested
   * CrawlableDataset and so must be an implementation of
   * thredds.crawlabledataset.CrawlableDataset. The given class must also
   * supply a public constructor with a single String argument. The String
   * argument is the path for the CrawlableDataset being constructed.
   *  
   * If className is null, thredds.crawlabledataset.CrawlableDatasetFile will
   * be used.
   *
   * If the path is an alias containing wildcard characters ("*"),
   * the class thredds.crawlabledataset.CrawlableDatasetAlias will be used to
   * expand the alias into a collection of CrawlableDatasets of the type given
   * by className.
   *
   * @param path the path of the CrawlableDataset.
   * @param className the class name of the CrawlableDataset implementation to instantiate.
   * @param configObj
   *
   * @return a CrawlableDataset for the given path and of the type given by the class name.
   *
   * @throws IOException if a CrawlableDataset cannot be created due to IO problems.
   * @throws ClassNotFoundException if the given CrawlableDataset implementation was not found.
   * @throws NoSuchMethodException if the given CrawlableDataset implementation does not have a constructor with a single String parameter which is required.
   * @throws IllegalAccessException if the constructor is inaccessible due to Java language access control.
   * @throws InvocationTargetException if the constructor throws an exception.
   * @throws InstantiationException if the given CrawlableDataset implementation is an abstract class.
   *
   * @throws IllegalArgumentException if the given class name is not an implementation of CrawlableDataset.
   */
  public static CrawlableDataset createCrawlableDataset( String path, String className, Object configObj )
          throws IOException,
                 ClassNotFoundException, NoSuchMethodException,
                 IllegalAccessException, InvocationTargetException,
                 InstantiationException, IllegalArgumentException
  {
    String tmpPath = CrawlableDatasetFactory.normalizePath( path );
    String tmpClassName = ( className == null
                            ? defaultClassName
                            : className );

    // @todo Remove alias until sure how to handle things like ".scour*" being a regular file.
//    if ( CrawlableDatasetAlias.isAlias( tmpPath) )
//        return new CrawlableDatasetAlias( tmpPath, tmpClassName, configObj );

    // Get the Class instance for desired CrawlableDataset implementation.
    Class crDsClass = Class.forName( tmpClassName );

    // Check that the Class is a CrawlableDataset.
    if ( ! CrawlableDataset.class.isAssignableFrom( crDsClass ) )
    {
      throw new IllegalArgumentException( "Requested class <" + className + "> not an implementation of thredds.crawlabledataset.CrawlableDataset.");
    }

    // Instantiate the desired CrawlableDataset.
    Class [] argTypes = { String.class, Object.class };
    Object [] args = { tmpPath, configObj };
    Constructor constructor = crDsClass.getDeclaredConstructor( argTypes );

    try
    {
      return (CrawlableDataset) constructor.newInstance( args );
    }
    catch ( InvocationTargetException e )
    {
      if ( IOException.class.isAssignableFrom( e.getCause().getClass()) )
        throw (IOException) e.getCause();
      else throw e;
    }
  }

  /**
   * Normalize the given path so that it can be used in the creation of a CrawlableDataset.
   * This method can be used on absolute or relative paths.
   *
   * Normal uses slashes ("/") as path seperator, not backslashes ("\"), and does
   * not use trailing slashes. This function allows users to specify Windows
   * pathnames and UNC pathnames in there normal manner.
   *
   * @param path the path to be normalized.
   * @return the normalized path.
   * @throws NullPointerException if path is null.
   */
  public static String normalizePath( String path )
  {
    // Replace any occurance of a backslash ("\") with a slash ("/").
    // NOTE: Both String and Pattern escape backslash, so need four backslashes to find one.
    // NOTE: No longer replace multiple backslashes with one slash, which allows for UNC pathnames (Windows LAN addresses).
    //       Was path.replaceAll( "\\\\+", "/");
    String newPath = path.replaceAll( "\\\\", "/");

    // Remove trailing slashes.
    while ( newPath.endsWith( "/") && ! newPath.equals("/") )
      newPath = newPath.substring( 0, newPath.length() - 1 );

    return newPath;
  }
}

/*
 * $Log: CrawlableDatasetFactory.java,v $
 * Revision 1.7  2006/01/23 18:51:06  edavis
 * Move CatalogGen.main() to CatalogGenMain.main(). Stop using
 * CrawlableDatasetAlias for now. Get new thredds/build.xml working.
 *
 * Revision 1.6  2006/01/17 22:56:47  edavis
 * Remove use of CrawlableDatasetAlias for now until figure out how to deal with
 * things like ".scour*" being a regular file. Also, update some documentation.
 *
 * Revision 1.5  2005/12/30 00:18:54  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.4  2005/12/16 23:19:36  edavis
 * Convert InvDatasetScan to use CrawlableDataset and DatasetScanCatalogBuilder.
 *
 * Revision 1.3  2005/12/01 00:15:02  edavis
 * More work on move to using CrawlableDataset.
 *
 * Revision 1.2  2005/11/18 23:51:04  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.1  2005/11/15 18:40:48  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 * Revision 1.3  2005/08/22 17:40:23  edavis
 * Another round on CrawlableDataset: make CrawlableDatasetAlias a subclass
 * of CrawlableDataset; start generating catalogs (still not using in
 * InvDatasetScan or CatalogGen, yet).
 *
 * Revision 1.2  2005/07/13 22:54:22  edavis
 * Fix CrawlableDatasetAlias.
 *
 * Revision 1.1  2005/06/24 22:08:32  edavis
 * Second stab at the CrawlableDataset interface.
 *
 */