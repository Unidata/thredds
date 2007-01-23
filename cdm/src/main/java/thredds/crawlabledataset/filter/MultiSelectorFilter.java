package thredds.crawlabledataset.filter;

import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;

/**
 * The standard CrawlableDatasetFilter which uses a list of
 * CrawlableDatasetSelectors to determine if a dataset is accepted. 
 *
 * @author edavis
 * @since Jul 25, 2005 11:15:43 -0600
 */
public class MultiSelectorFilter implements CrawlableDatasetFilter
{
//  private static org.apache.commons.logging.Log log =
//          org.apache.commons.logging.LogFactory.getLog( MultiSelectorFilter.class );

  private List selectorGroup;

  public MultiSelectorFilter( List selectorGroup )
  {
    if ( selectorGroup == null )
      throw new IllegalArgumentException( "Selector group parameter must not be null." );
    this.selectorGroup = selectorGroup;
  }

  public MultiSelectorFilter( thredds.crawlabledataset.filter.Selector selector)
  {                         
    if ( selector == null )
      selectorGroup = Collections.EMPTY_LIST;
    else
      selectorGroup = Collections.singletonList( selector);
  }

  public Object getConfigObject() { return selectorGroup; }

  public boolean accept( CrawlableDataset dataset )
  {
    if ( dataset == null ) throw new IllegalArgumentException( "Dataset parameter must not be null." );

    // If no selectors, accept all datasets.
    if ( selectorGroup.isEmpty() )
    {
      return ( true );
    }

    // Loop through selector group to check if current dataset should be accepted.
    boolean accept = false;
    boolean doAnyIncludersApply = false;
    boolean doAnySelectorsApply = false;
    for ( Iterator it = selectorGroup.iterator(); it.hasNext(); )
    {
      thredds.crawlabledataset.filter.Selector curSelector =
              (thredds.crawlabledataset.filter.Selector) it.next();

      if ( curSelector.isApplicable( dataset ) )
      {
        doAnySelectorsApply = true;
        if ( curSelector.isIncluder() )
        {
          doAnyIncludersApply = true;
          if ( curSelector.match( dataset ) )
          {
            accept = true; // Dataset accepted by current DatasetFilter.
          }
        }
        else // isExcluder()
        {
          if ( curSelector.match( dataset ) )
          {
            return ( false ); // Exclusion takes precedence over inclusion.
          }
        }
      }
    }

    // If at least one filter accepted (and none rejected), accept.
    if ( accept ) return ( true );

    // If no selectors apply to this dataset, accept it.
    if ( ! doAnySelectorsApply ) return ( true );

    // If no includers apply to this dataset (and no excluders
    // rejected it), accept. [Allows exclusion only.]
    if ( ! doAnyIncludersApply ) return ( true );

    // Dataset not accepted or rejected by any DatasetFilter (so reject).
    return ( false );
  }

  /**
   * NOT USED YET. First attempt at moving Selector into MultiSelectorFilter.
   * ToDo: Still need to think about how it would affect InvCatalogFactory10.writeDatasetScanFilter
   * Once start using, make class public.
   */
  private static class Selector
  {
    private boolean includer;
    private boolean applyToAtomicDataset;
    private boolean applyToCollectionDataset;

    private CrawlableDatasetFilter filter;


    public Selector( CrawlableDatasetFilter filter, boolean includer,
                     boolean applyToAtomicDataset, boolean applyToCollectionDataset )
    {
      this.filter = filter;

      this.includer = includer;
      this.applyToAtomicDataset = applyToAtomicDataset;
      this.applyToCollectionDataset = applyToCollectionDataset;
    }

    public boolean isApplyToAtomicDataset()
    {
      return applyToAtomicDataset;
    }

    public boolean isApplyToCollectionDataset()
    {
      return applyToCollectionDataset;
    }

    public boolean match( CrawlableDataset dataset )
    {
      return filter.accept( dataset );
    }

    public boolean isApplicable( CrawlableDataset dataset )
    {
      if ( this.applyToAtomicDataset && !dataset.isCollection() ) return true;
      if ( this.applyToCollectionDataset && dataset.isCollection() ) return true;
      return false;
    }

    public boolean isIncluder()
    {
      return includer;
    }

    public boolean isExcluder()
    {
      return !includer;
    }

  }
}
