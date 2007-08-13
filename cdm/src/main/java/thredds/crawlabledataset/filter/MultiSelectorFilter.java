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

  private List<Selector> selectorGroup;

  public MultiSelectorFilter( List<Selector> selectorGroup )
  {
    if ( selectorGroup == null )
      throw new IllegalArgumentException( "Selector group parameter must not be null." );
    this.selectorGroup = selectorGroup;
  }

  public MultiSelectorFilter( Selector selector)
  {                         
    if ( selector == null )
      selectorGroup = Collections.emptyList();
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
    for ( Selector curSelector: selectorGroup )
    {
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
  * Used by a MultiSelectorFilter to determine whether to include
  * or exclude a CrawlableDataset.
  */
 public static class Selector
  {
    private boolean includer;
    private boolean applyToAtomicDataset;
    private boolean applyToCollectionDataset;

    private CrawlableDatasetFilter filter;


    /**
     * Construct a Selector which uses the given CrawlableDatasetFilter to determine a dataset match.
     *
     * @param filter the filter used by this Selector to match against datasets.
     * @param includer if true, matching datasets will be included, otherwise they will be excluded.
     * @param applyToAtomicDataset if true, this selector applies to atomic datasets.
     * @param applyToCollectionDataset if true, this selector applies to collection datasets.
     */
    public Selector( CrawlableDatasetFilter filter, boolean includer,
                     boolean applyToAtomicDataset, boolean applyToCollectionDataset )
    {
      this.filter = filter;

      this.includer = includer;
      this.applyToAtomicDataset = applyToAtomicDataset;
      this.applyToCollectionDataset = applyToCollectionDataset;
    }

    public CrawlableDatasetFilter getFilter()
    {
      return filter;
    }

    public boolean isApplyToAtomicDataset()
    {
      return applyToAtomicDataset;
    }

    public boolean isApplyToCollectionDataset()
    {
      return applyToCollectionDataset;
    }

    /**
     * Determine if the given dataset matches this selector.
     *
     * @return true if the given dataset matches this selector, otherwise false.
     * @param dataset the CrawlableDataset to test if this selector matches.
     */
    public boolean match( CrawlableDataset dataset )
    {
      return filter.accept( dataset );
    }

    /**
     * Test if this selector applies to the given dataset.
     *
     * @param dataset the CrawlableDataset to test if this selector applies.
     * @return true if this selector applies to the given dataset, false otherwise.
     */
    public boolean isApplicable( CrawlableDataset dataset )
    {
      if ( this.applyToAtomicDataset && !dataset.isCollection() ) return true;
      if ( this.applyToCollectionDataset && dataset.isCollection() ) return true;
      return false;
    }

    /**
     * Return true if this selector is an inclusion rather than exclusion selector.
     * 
     * @return true if this selector is an inclusion selector.
     */
    public boolean isIncluder()
    {
      return includer;
    }

    /**
     * Return true if this selector is an exclusion rather than inclusion selector.
     *
     * @return true if this selector is an exclusion selector.
     */
    public boolean isExcluder()
    {
      return !includer;
    }

  }
}
