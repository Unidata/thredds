/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
// $Id: CrawlableDatasetLabeler.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset;

/**
 * An interface for determining a String label for a CrawlableDataset.
 *
 * @author edavis
 * @since Nov 14, 2005 10:49:12 AM
 */
public interface CrawlableDatasetLabeler
{
  /**
   * Return a label for the given dataset or null if this CrawlableDatasetLabeler
   * does not know how to label the given dataset.
   *
   * @param dataset the CrawlableDataset to be labeled.
   * @return the label for the given dataset or null.
   * @throws NullPointerException if the given dataset is null.
   */
  public String getLabel( CrawlableDataset dataset );

  /**
   * Return the configuration object.
   *
   * @return the configuration Object (may be null).
   */
  public Object getConfigObject();

}
