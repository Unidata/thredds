// $Id: $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;

import java.util.Date;

/**
 * Filter datasets based on their last modidified date.
 *
 * @author caron
 */
public class LastModifiedLimitSelector implements Selector {
  private boolean includer;
  private boolean applyToAtomicDataset;
  private boolean applyToCollectionDataset;

  private long lastModifiedLimit;

  /**
   * Constructor. Can only be used for CrawlableDatasetFile that have a lastModified date.
   *
   * @param lastModifiedLimitMsecs match datasets whose lastModified() time is at least this many msecs in the past
   * @param includer if true, include files that match, else exclude
   * @param applyToAtomicDataset filter is applicable to atomic (leaf) datasets
   * @param applyToCollectionDataset filter is applicable to collection (directory) datasets
   */
  public LastModifiedLimitSelector(long lastModifiedLimitMsecs, boolean includer, boolean applyToAtomicDataset, boolean applyToCollectionDataset) {
    this.lastModifiedLimit = lastModifiedLimitMsecs;

    this.includer = includer;
    this.applyToAtomicDataset = applyToAtomicDataset;
    this.applyToCollectionDataset = applyToCollectionDataset;
  }

  public boolean isApplyToAtomicDataset() {
    return applyToAtomicDataset;
  }

  public boolean isApplyToCollectionDataset() {
    return applyToCollectionDataset;
  }

  /**
   * For now, we will match on negetive, but we need a way to combine AND and OR in a general way
   * @param dataset
   * @return
   */
  public boolean match(CrawlableDataset dataset) {
    if (dataset instanceof CrawlableDatasetFile) {
      CrawlableDatasetFile dsFile = (CrawlableDatasetFile) dataset;
      Date lastModified = dsFile.lastModified();
      if (lastModified != null) {
        long now = System.currentTimeMillis();
        if (now - lastModified.getTime() > lastModifiedLimit)
          return false;
      }
    }
    return true;
  }

  public boolean isApplicable(CrawlableDataset dataset) {
    if (this.applyToAtomicDataset && ! dataset.isCollection()) return true;
    if (this.applyToCollectionDataset && dataset.isCollection()) return true;
    return false;
  }

  public boolean isIncluder() {
    return includer;
  }

  public boolean isExcluder() {
    return ! includer;
  }
}

