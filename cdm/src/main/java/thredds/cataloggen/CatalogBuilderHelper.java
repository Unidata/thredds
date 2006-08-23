// $Id: CatalogBuilderHelper.java 57 2006-07-12 20:20:43Z edavis $
package thredds.cataloggen;

import thredds.catalog.*;
import thredds.catalog.parser.jdom.InvCatalogFactory10;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDatasetFactory;

import java.io.IOException;

import org.jdom.Document;

/**
 * _more_
 *
 * @author edavis
 * @since Dec 7, 2005 2:04:28 PM
 */
class CatalogBuilderHelper
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( CatalogBuilderHelper.class );

  /**
   * Return the requested dataset if it is the ancestor dataset or an allowed
   * descendant of the ancestor dataset, otherwise return null. The given
   * filter determines whether a dataset is allowed or not.
   *
   * @param ancestorCrDs the dataset from which the requested dataset must be descended (or self).
   * @param path the path of the requested dataset.
   * @param filter the CrawlableDatasetFilter that determines which datasets are allowed.
   * @return the CrawlableDataset that represents the given path or null.
   *
   * @throws NullPointerException if the given path or ancestor dataset are null.
   * @throws IllegalArgumentException if the abstract dataset is not a descendant of the ancestor dataset.
   */
  static CrawlableDataset verifyDescendantDataset( CrawlableDataset ancestorCrDs,
                                                   String path,
                                                   CrawlableDatasetFilter filter )
  {
    String tmpPath = CrawlableDatasetFactory.normalizePath( path);

    // Make sure requested path is descendant of ancestor dataset.
    if ( ! ancestorCrDs.isCollection() )
      throw new IllegalArgumentException( "Ancestor dataset <" + ancestorCrDs.getPath() + "> not a collection." );
    if ( ! tmpPath.startsWith( ancestorCrDs.getPath() ) )
      throw new IllegalArgumentException( "Dataset path <" + tmpPath + "> not descendant of given dataset <" + ancestorCrDs.getPath() + ">." );

    // If path and ancestor are the same, return ancestor.
    if ( tmpPath.length() == ancestorCrDs.getPath().length() )
      return ancestorCrDs;

    // Crawl into the dataset collection through each level of the given path
    // checking that each level is accepted by the given CrawlableDatasetFilter.
    String remainingPath = tmpPath.substring( ancestorCrDs.getPath().length() + 1 );
    String[] pathSegments = remainingPath.split( "/" );
    CrawlableDataset curCrDs = ancestorCrDs;
    for ( int i = 0; i < pathSegments.length; i++ )
    {
      curCrDs = curCrDs.getDescendant( pathSegments[i]);
      if ( filter != null )
        if ( ! filter.accept( curCrDs ) )
          return null;
      if ( ! curCrDs.exists())
        return null;
    }
    return curCrDs;
  }

  static Document convertCatalogToDocument( InvCatalog catalog )
  {
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    InvCatalogConvertIF converter = fac.getCatalogConverter( XMLEntityResolver.CATALOG_NAMESPACE_10 );
    InvCatalogFactory10 fac10 = (InvCatalogFactory10) converter;

    return fac10.writeCatalog( (InvCatalogImpl) catalog );
  }

  static String convertCatalogToString( InvCatalog catalog )
  {
    InvCatalogFactory fac = InvCatalogFactory.getDefaultFactory( false );
    try
    {
      return fac.writeXML( (InvCatalogImpl) catalog );
    }
    catch ( IOException e )
    {
      return null;
    }
  }
}
