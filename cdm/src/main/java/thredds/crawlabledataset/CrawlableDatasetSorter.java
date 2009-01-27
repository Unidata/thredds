/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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
// $Id: CrawlableDatasetSorter.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset;

import java.util.List;

/**
 * The CrawlableDatasetSorter interface provides for sorting a list of
 * CrawlableDatasets. An instance of the CrawlableDatasetSorter interface
 * defines an ordering for a list of CrawlableDatasets.
 *
 * The CrawlableDatasetSorter interface is used by the CollectionLevelScanner
 * class to sort the datasets it is cataloging.
 *
 * @author edavis
 * @since Nov 18, 2005 4:12:50 PM
 */
public interface CrawlableDatasetSorter
{
  /**
   * Sort the given CrawlableDataset list into the order defined by this sorter.
   *
   * @param datasetList the CrawlableDataset list to be sorted.
   * 
   * @throws ClassCastException if the list contains elements that are not CrawlableDatasets.
   * @throws UnsupportedOperationException if the given list does not allow the necessary list manipulation.
   */
  public void sort( List<CrawlableDataset> datasetList );

  /**
   * Return the configuration object.
   *
   * @return the configuration Object (may be null).
   */
  public Object getConfigObject();
}
