/*
 * Copyright (c) 1998 - 2014. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.catalog;

import thredds.inventory.MFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements filtering for DatasetScan
 * LOOK replaced with DatasetScanConfig.Filter, which is used to create MFileFilter's in DatasetScan
 *
 * @author John
 * @since 1/14/2015
 */
public class DatasetScanFilter {

  private final List<Selector> selectorGroup;
  private final boolean containsAtomicIncluders;
  private final boolean containsAtomicExcluders;
  private final boolean containsCollectionIncluders;
  private final boolean containsCollectionExcluders;

  public DatasetScanFilter() {
              /* Determine if regExp or wildcard
            if (regExpAttVal != null) {
              selectorList.add(new MultiSelectorFilter.Selector(new RegExpMatchOnNameFilter(regExpAttVal), includer, atomic, collection));
            } else if (wildcardAttVal != null) {
              selectorList.add(new MultiSelectorFilter.Selector(new WildcardMatchOnNameFilter(wildcardAttVal), includer, atomic, collection));
            } else if (lastModLimitAttVal != null) {
              selectorList.add(new MultiSelectorFilter.Selector(new LastModifiedLimitFilter(Long.parseLong(lastModLimitAttVal)), includer, atomic, collection));
            } */
    this.selectorGroup = Collections.emptyList();
    this.containsAtomicIncluders = false;
    this.containsAtomicExcluders = false;
    this.containsCollectionIncluders = false;
    this.containsCollectionExcluders = false;
  }

  public DatasetScanFilter(List<Selector> selectorGroup) {
    if (selectorGroup == null || selectorGroup.isEmpty()) {
      this.selectorGroup = Collections.emptyList();
      this.containsAtomicIncluders = false;
      this.containsAtomicExcluders = false;
      this.containsCollectionIncluders = false;
      this.containsCollectionExcluders = false;

    } else {
      boolean anyAtomicIncluders = false;
      boolean anyAtomicExcluders = false;
      boolean anyCollectionIncluders = false;
      boolean anyCollectionExcluders = false;
      List<Selector> tmpSelectorGroup = new ArrayList<Selector>();
      for (Selector curSelector : selectorGroup) {
        if (curSelector.isIncluder()) {
          if (curSelector.isApplyToAtomicDataset())
            anyAtomicIncluders = true;
          if (curSelector.isApplyToCollectionDataset())
            anyCollectionIncluders = true;
        } else { // curSelector.isExcluder()
          if (curSelector.isApplyToAtomicDataset())
            anyAtomicExcluders = true;
          if (curSelector.isApplyToCollectionDataset())
            anyCollectionExcluders = true;
        }
        tmpSelectorGroup.add(curSelector);
      }

      this.selectorGroup = tmpSelectorGroup;
      this.containsAtomicIncluders = anyAtomicIncluders;
      this.containsAtomicExcluders = anyAtomicExcluders;
      this.containsCollectionIncluders = anyCollectionIncluders;
      this.containsCollectionExcluders = anyCollectionExcluders;
    }
  }

  public DatasetScanFilter(Selector selector) {
    if (selector == null) {
      this.selectorGroup = Collections.emptyList();
      this.containsAtomicIncluders = false;
      this.containsAtomicExcluders = false;
      this.containsCollectionIncluders = false;
      this.containsCollectionExcluders = false;
    } else {
      boolean anyAtomicIncluders = false;
      boolean anyAtomicExcluders = false;
      boolean anyCollectionIncluders = false;
      boolean anyCollectionExcluders = false;

      if (selector.isIncluder()) {
        if (selector.isApplyToAtomicDataset())
          anyAtomicIncluders = true;
        if (selector.isApplyToCollectionDataset())
          anyCollectionIncluders = true;
      } else { // curSelector.isExcluder()
        if (selector.isApplyToAtomicDataset())
          anyAtomicExcluders = true;
        if (selector.isApplyToCollectionDataset())
          anyCollectionExcluders = true;
      }

      this.selectorGroup = Collections.singletonList(selector);
      this.containsAtomicIncluders = anyAtomicIncluders;
      this.containsAtomicExcluders = anyAtomicExcluders;
      this.containsCollectionIncluders = anyCollectionIncluders;
      this.containsCollectionExcluders = anyCollectionExcluders;
    }
  }

  public Object getConfigObject() {
    return selectorGroup;
  }

  public boolean accept(MFile dataset) {
    if (dataset == null)
      return false;

    // If no Selectors, accept all datasets.
    if (this.selectorGroup.isEmpty())
      return true;

    if (dataset.isDirectory()) {
      // If no collection selectors, accept all collection datasets.
      if (!this.containsCollectionIncluders && !this.containsCollectionExcluders)
        return true;
    } else {
      // If no atomic selectors, accept all atomic datasets.
      if (!this.containsAtomicIncluders && !this.containsAtomicExcluders)
        return true;
    }

    boolean include = false;
    boolean exclude = false;

    for (Selector curSelector : this.selectorGroup) {
      if (curSelector.isApplicable(dataset)) {
        if (curSelector.match(dataset)) {
          if (curSelector.isIncluder())
            include = true;
          else
            exclude = true;
        }
      }
    }

    // Deal with atomic datasets
    if (!dataset.isDirectory()) {
      // If have only inclusion Selectors, accept any dataset that is explicitly included.
      if (this.containsAtomicIncluders && !this.containsAtomicExcluders)
        return include;

      // If have only exclusion Selectors, accept any dataset not explicitly excluded.
      if (this.containsAtomicExcluders && !this.containsAtomicIncluders)
        return !exclude;

      // If have both inclusion and exclusion Selectors, accept datasets that are
      // explicitly included but not explicitly excluded.
      if (this.containsAtomicIncluders && this.containsAtomicExcluders && include)
        return !exclude;
      // Deal with collection datasets
    } else {
      // If have only inclusion Selectors, accept any dataset that is explicitly included.
      if (this.containsCollectionIncluders && !this.containsCollectionExcluders)
        return include;

      // If have only exclusion Selectors, accept any dataset not explicitly excluded.
      if (this.containsCollectionExcluders && !this.containsCollectionIncluders)
        return !exclude;

      // If have both inclusion and exclusion Selectors, accept datasets that are
      // explicitly included but not explicitly excluded.
      if (this.containsCollectionIncluders && this.containsCollectionExcluders && include)
        return !exclude;
    }

    // Otherwise, don't accept.
    return false;
  }

  /**
   * Used by a MultiSelectorFilter to determine whether to include
   * or exclude a CrawlableDataset.
   */
  public static class Selector {

    public class RegExpMatchOnNameFilter {
      private String regExpString;
      protected java.util.regex.Pattern pattern;

      public RegExpMatchOnNameFilter(String regExpString) {
        this.regExpString = regExpString;
        this.pattern = java.util.regex.Pattern.compile(regExpString);
      }

      public boolean accept(MFile mfile) {
        java.util.regex.Matcher matcher = this.pattern.matcher(mfile.getName());
        return matcher.matches();
      }
    }

    public class WildcardMatchOnNameFilter {
      protected String wildcardString;
      protected java.util.regex.Pattern pattern;

      public WildcardMatchOnNameFilter(String wildcardString) {
        // Keep original wildcard string.
        this.wildcardString = wildcardString;

        // Map wildcard to regular expresion.
        String regExp = mapWildcardToRegExp(wildcardString);

        // Compile regular expression pattern
        this.pattern = java.util.regex.Pattern.compile(regExp);
      }

      private String mapWildcardToRegExp(String wildcardString) {
        // Replace "." with "\.".
        wildcardString = wildcardString.replaceAll("\\.", "\\\\.");

        // Replace "*" with ".*".
        wildcardString = wildcardString.replaceAll("\\*", ".*");

        // Replace "?" with ".?".
        wildcardString = wildcardString.replaceAll("\\?", ".?");

        return wildcardString;
      }

      public boolean accept(MFile dataset) {
        java.util.regex.Matcher matcher = this.pattern.matcher(dataset.getName());
        return matcher.matches();
      }
    }

    public class LastModifiedLimitFilter {
      private long lastModifiedLimitInMillis;

      /**
       * Constructor.
       *
       * @param lastModifiedLimitInMillis accept datasets whose lastModified() time is at least this many msecs in the past
       */
      public LastModifiedLimitFilter(long lastModifiedLimitInMillis) {
        this.lastModifiedLimitInMillis = lastModifiedLimitInMillis;
      }

      /**
       * Accept datasets whose last modified date is at least the
       * last modified limit of milliseconds in the past.
       *
       * @param dataset the dataset to filter
       * @return true if the datasets last modified date is at least lastModifiedLimitInMillis in the past.
       */
      public boolean accept(MFile dataset) {
        long lastModDate = dataset.getLastModified();
        if (lastModDate > 0) {
          long now = System.currentTimeMillis();
          if (now - lastModDate > lastModifiedLimitInMillis)
            return true;
        }
        return false;
      }
    }
    private final boolean includer;
    private final boolean applyToAtomicDataset;
    private final boolean applyToCollectionDataset;

    private final DatasetScanFilter filter;


    /**
     * Construct a Selector which uses the given CrawlableDatasetFilter to determine a dataset match.
     *
     * @param filter                   the filter used by this Selector to match against datasets.
     * @param includer                 if true, matching datasets will be included, otherwise they will be excluded.
     * @param applyToAtomicDataset     if true, this selector applies to atomic datasets.
     * @param applyToCollectionDataset if true, this selector applies to collection datasets.
     */
    public Selector(DatasetScanFilter filter, boolean includer,
                    boolean applyToAtomicDataset, boolean applyToCollectionDataset) {
      this.filter = filter;

      this.includer = includer;
      this.applyToAtomicDataset = applyToAtomicDataset;
      this.applyToCollectionDataset = applyToCollectionDataset;
    }

    public DatasetScanFilter getFilter() {
      return filter;
    }

    public boolean isApplyToAtomicDataset() {
      return applyToAtomicDataset;
    }

    public boolean isApplyToCollectionDataset() {
      return applyToCollectionDataset;
    }

    /**
     * Determine if the given dataset matches this selector.
     *
     * @param dataset the CrawlableDataset to test if this selector matches.
     * @return true if the given dataset matches this selector, otherwise false.
     */
    public boolean match(MFile dataset) {
      return filter.accept(dataset);
    }

    /**
     * Test if this selector applies to the given dataset.
     *
     * @param dataset the CrawlableDataset to test if this selector applies.
     * @return true if this selector applies to the given dataset, false otherwise.
     */
    public boolean isApplicable(MFile dataset) {
      if (this.applyToAtomicDataset && !dataset.isDirectory()) return true;
      if (this.applyToCollectionDataset && dataset.isDirectory()) return true;
      return false;
    }

    /**
     * Return true if this selector is an inclusion rather than exclusion selector.
     *
     * @return true if this selector is an inclusion selector.
     */
    public boolean isIncluder() {
      return includer;
    }

    /**
     * Return true if this selector is an exclusion rather than inclusion selector.
     *
     * @return true if this selector is an exclusion selector.
     */
    public boolean isExcluder() {
      return !includer;
    }

  }
}
