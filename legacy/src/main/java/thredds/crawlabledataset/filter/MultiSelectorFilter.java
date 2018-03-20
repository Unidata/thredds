/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.crawlabledataset.filter;

import java.util.ArrayList;
import java.util.List;
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

  private final List<Selector> selectorGroup;
  private final boolean containsAtomicIncluders;
  private final boolean containsAtomicExcluders;
  private final boolean containsCollectionIncluders;
  private final boolean containsCollectionExcluders;

  public MultiSelectorFilter( List<Selector> selectorGroup )
  {
    if ( selectorGroup == null || selectorGroup.isEmpty() ) {
      this.selectorGroup = Collections.emptyList();
      this.containsAtomicIncluders = false;
      this.containsAtomicExcluders = false;
      this.containsCollectionIncluders = false;
      this.containsCollectionExcluders = false;
    }
    else {
      boolean anyAtomicIncluders = false;
      boolean anyAtomicExcluders = false;
      boolean anyCollectionIncluders = false;
      boolean anyCollectionExcluders = false;
      List<Selector> tmpSelectorGroup = new ArrayList<Selector>();
      for ( Selector curSelector : selectorGroup ) {
        if ( curSelector.isIncluder() ) {
          if ( curSelector.isApplyToAtomicDataset())
            anyAtomicIncluders = true;
          if ( curSelector.isApplyToCollectionDataset())
            anyCollectionIncluders = true;
        } else { // curSelector.isExcluder()
          if ( curSelector.isApplyToAtomicDataset())
            anyAtomicExcluders = true;
          if ( curSelector.isApplyToCollectionDataset())
            anyCollectionExcluders = true;
        }
          tmpSelectorGroup.add( curSelector);
      }

      this.selectorGroup = tmpSelectorGroup;
      this.containsAtomicIncluders = anyAtomicIncluders;
      this.containsAtomicExcluders = anyAtomicExcluders;
      this.containsCollectionIncluders = anyCollectionIncluders;
      this.containsCollectionExcluders = anyCollectionExcluders;
    }
  }

  public MultiSelectorFilter( Selector selector)
  {                         
    if ( selector == null ) {
      this.selectorGroup = Collections.emptyList();
      this.containsAtomicIncluders = false;
      this.containsAtomicExcluders = false;
      this.containsCollectionIncluders = false;
      this.containsCollectionExcluders = false;
    }
    else {
      boolean anyAtomicIncluders = false;
      boolean anyAtomicExcluders = false;
      boolean anyCollectionIncluders = false;
      boolean anyCollectionExcluders = false;

      if ( selector.isIncluder() ){
        if ( selector.isApplyToAtomicDataset())
          anyAtomicIncluders = true;
        if ( selector.isApplyToCollectionDataset())
          anyCollectionIncluders = true;
      } else { // curSelector.isExcluder()
        if ( selector.isApplyToAtomicDataset())
          anyAtomicExcluders = true;
        if ( selector.isApplyToCollectionDataset())
          anyCollectionExcluders = true;
      }

      this.selectorGroup = Collections.singletonList( selector);
      this.containsAtomicIncluders = anyAtomicIncluders;
      this.containsAtomicExcluders = anyAtomicExcluders;
      this.containsCollectionIncluders = anyCollectionIncluders;
      this.containsCollectionExcluders = anyCollectionExcluders;
    }
  }

  public Object getConfigObject() {
    return selectorGroup;
  }

  public boolean accept( CrawlableDataset dataset )
  {
    if ( dataset == null )
      return false;

    // If no Selectors, accept all datasets.
    if ( this.selectorGroup.isEmpty() )
      return true;

    if ( dataset.isCollection()) {
      // If no collection selectors, accept all collection datasets.
      if ( ! this.containsCollectionIncluders && ! this.containsCollectionExcluders )
        return true;
    } else {
      // If no atomic selectors, accept all atomic datasets.
      if ( ! this.containsAtomicIncluders && ! this.containsAtomicExcluders )
        return true;
    }

    boolean include = false;
    boolean exclude = false;

    for ( Selector curSelector: this.selectorGroup ) {
      if ( curSelector.isApplicable( dataset )) {
        if ( curSelector.match( dataset )) {
          if ( curSelector.isIncluder())
            include = true;
          else
            exclude = true;
        }
      }
    }

    // Deal with atomic datasets
    if( ! dataset.isCollection() ) {
      // If have only inclusion Selectors, accept any dataset that is explicitly included.
      if ( this.containsAtomicIncluders && ! this.containsAtomicExcluders )
        return include;

      // If have only exclusion Selectors, accept any dataset not explicitly excluded.
      if ( this.containsAtomicExcluders && ! this.containsAtomicIncluders )
        return ! exclude;

      // If have both inclusion and exclusion Selectors, accept datasets that are
      // explicitly included but not explicitly excluded.
      if ( this.containsAtomicIncluders && this.containsAtomicExcluders && include )
        return ! exclude;
    // Deal with collection datasets
    } else {
      // If have only inclusion Selectors, accept any dataset that is explicitly included.
      if ( this.containsCollectionIncluders && ! this.containsCollectionExcluders )
        return include;

      // If have only exclusion Selectors, accept any dataset not explicitly excluded.
      if ( this.containsCollectionExcluders && ! this.containsCollectionIncluders )
        return ! exclude;

      // If have both inclusion and exclusion Selectors, accept datasets that are
      // explicitly included but not explicitly excluded.
      if ( this.containsCollectionIncluders && this.containsCollectionExcluders && include )
        return ! exclude;
    }

    // Otherwise, don't accept.
    return false;
  }

  /**
  * Used by a MultiSelectorFilter to determine whether to include
  * or exclude a CrawlableDataset.
  */
 public static class Selector
  {
    private final boolean includer;
    private final boolean applyToAtomicDataset;
    private final boolean applyToCollectionDataset;

    private final CrawlableDatasetFilter filter;


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
