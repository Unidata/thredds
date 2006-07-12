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
/*
 * $Log: RegExpMatchOnNameSelector.java,v $
 * Revision 1.2  2005/12/30 00:18:53  edavis
 * Expand the datasetScan element in the InvCatalog XML Schema and update InvCatalogFactory10
 * to handle the expanded datasetScan. Add handling of user defined CrawlableDataset implementations
 * and other interfaces in thredds.crawlabledataset (e.g., CrawlableDatasetFilter). Add tests to
 * TestInvDatasetScan for refactored datasetScan.
 *
 * Revision 1.1  2005/11/15 18:40:47  edavis
 * More work on CrawlableDataset refactor of CatGen.
 *
 */