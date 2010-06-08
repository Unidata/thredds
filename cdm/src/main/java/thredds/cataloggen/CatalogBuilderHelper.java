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
    // Make sure requested path is descendant of ancestor dataset.
    if ( ! ancestorCrDs.isCollection() )
      throw new IllegalArgumentException( "Ancestor dataset <" + ancestorCrDs.getPath() + "> not a collection." );
    if ( ! path.startsWith( ancestorCrDs.getPath() ) )
      throw new IllegalArgumentException( "Dataset path <" + path + "> not descendant of given dataset <" + ancestorCrDs.getPath() + ">." );

    // If path and ancestor are the same, return ancestor.
    if ( path.length() == ancestorCrDs.getPath().length() )
      return ancestorCrDs;

    // Crawl into the dataset collection through each level of the given path
    // checking that each level is accepted by the given CrawlableDatasetFilter.
    String remainingPath = path.substring( ancestorCrDs.getPath().length() );
    if ( remainingPath.startsWith( "/" ) )
      remainingPath = remainingPath.substring( 1 );

    String[] pathSegments = remainingPath.split( "/" );
    CrawlableDataset curCrDs = ancestorCrDs;
    for ( int i = 0; i < pathSegments.length; i++ )
    {
      curCrDs = curCrDs.getDescendant( pathSegments[i]);
      if ( filter != null )
        if ( ! filter.accept( curCrDs ) )
          return null;
    }
    // Only check complete path for existence since speed of check depends on implementation.
    if ( ! curCrDs.exists() )
      return null;
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
