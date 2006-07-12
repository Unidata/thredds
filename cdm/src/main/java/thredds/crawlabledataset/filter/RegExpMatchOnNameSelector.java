// $Id$
package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

/**
 * _more_
 *
 * @author edavis
 * @since Nov 4, 2005 9:28:30 PM
 */
public class RegExpMatchOnNameSelector implements Selector
{
  private boolean includer;
  private boolean applyToAtomicDataset;
  private boolean applyToCollectionDataset;

  private String regExp;
  private CrawlableDatasetFilter proxyFilter;


  public RegExpMatchOnNameSelector( String regExp, boolean includer,
                                    boolean applyToAtomicDataset, boolean applyToCollectionDataset )
  {
    this.regExp = regExp;
    proxyFilter = new RegExpMatchOnNameFilter( regExp );

    this.includer = includer;
    this.applyToAtomicDataset = applyToAtomicDataset;
    this.applyToCollectionDataset = applyToCollectionDataset;
  }

  public String getRegExpString() { return this.regExp; }
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
