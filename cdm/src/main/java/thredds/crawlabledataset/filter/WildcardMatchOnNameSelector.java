// $Id: WildcardMatchOnNameSelector.java,v 1.2 2005/12/30 00:18:53 edavis Exp $
package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 4, 2005 9:28:30 PM
 */
public class WildcardMatchOnNameSelector implements Selector
{
  private boolean includer;
  private boolean applyToAtomicDataset;
  private boolean applyToCollectionDataset;

  private String wildcardString;
  private CrawlableDatasetFilter proxyFilter;


  public WildcardMatchOnNameSelector( String wildcardString, boolean includer,
                                      boolean applyToAtomicDataset, boolean applyToCollectionDataset )
  {
    this.wildcardString = wildcardString;
    proxyFilter = new WildcardMatchOnNameFilter( wildcardString );

    this.includer = includer;
    this.applyToAtomicDataset = applyToAtomicDataset;
    this.applyToCollectionDataset = applyToCollectionDataset;
  }

  public String getWildcardString() { return this.wildcardString; }
  public boolean isApplyToAtomicDataset() { return applyToAtomicDataset; }
  public boolean isApplyToCollectionDataset() { return applyToCollectionDataset; }

  public boolean match( CrawlableDataset dataset )
  {
    return proxyFilter.accept( dataset);
  }

  public boolean isApplicable( CrawlableDataset dataset )
  {
    if ( this.applyToAtomicDataset && ! dataset.isCollection() ) return true;
    if ( this.applyToCollectionDataset && dataset.isCollection() ) return true;
    return false;
  }

  public boolean isIncluder()
  {
    return includer;
  }

  public boolean isExcluder()
  {
    return ! includer;
  }
}
/*
 * $Log: WildcardMatchOnNameSelector.java,v $
 * Revision 1.2  2005/12/30 00:18:53  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.1  2005/11/15 18:40:48  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 */