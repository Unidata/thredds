// $Id: CatalogBuilderHelper.java 57 2006-07-12 20:20:43Z edavis $
package thredds.cataloggen;

import thredds.catalog.*;
import thredds.catalog.parser.jdom.InvCatalogFactory10;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDatasetFactory;
import thredds.crawlabledataset.filter.RegExpMatchOnNameFilter;

import java.io.IOException;
import java.util.List;

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

  static CrawlableDataset verifyDescendentDataset( CrawlableDataset ancestorCrDs,
                                                          String path,
                                                          CrawlableDatasetFilter filter )
          throws IOException
  {
    String tmpPath = CrawlableDatasetFactory.normalizePath( path);

    // Make sure requested path is descendent of collection level dataset.
    if ( ! tmpPath.startsWith( ancestorCrDs.getPath() ) )
      throw new IllegalStateException( "Dataset path <" + tmpPath + "> not descendent of given dataset <" + ancestorCrDs.getPath() + ">." );

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
      CrawlableDatasetFilter curFilter = new RegExpMatchOnNameFilter( pathSegments[i] );
      List curCrDsList = curCrDs.listDatasets( curFilter );
      if ( curCrDsList.size() != 1 )
        return null;
      curCrDs = (CrawlableDataset) curCrDsList.get( 0 );
      if ( filter != null )
        if ( ! filter.accept( curCrDs ) )
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
