// $Id: Selector.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDataset;

/**
 * Used by a MultiSelectorFilter to determine whether to include
 * or exclude a CrawlableDataset.
 *
 * @author edavis
 * @since Nov 4, 2005 9:15:15 PM
 */
public interface Selector
{
  /**
   * Test if this selector applies to the given dataset.
   *
   * @param dataset the CrawlableDataset to test if this selector applies.
   * @return true if this selector applies to the given dataset, false otherwise.
   */
  public boolean isApplicable( CrawlableDataset dataset );

  /** Include any datasets that match this selector. */
  public boolean isIncluder();

  /** Exclude any datasets that match this selector. */
  public boolean isExcluder();

  /** Test if the given dataset matches this selector. */
  public boolean match( CrawlableDataset dataset );
}
