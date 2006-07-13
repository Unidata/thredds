// $Id: WildcardMatchOnNameSelector.java 63 2006-07-12 21:50:51Z edavis $
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
