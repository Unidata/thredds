/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
// $Id: MultiLabeler.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset;

import java.util.List;

/**
 *
 * @author edavis
 * @since Nov 17, 2005 3:54:55 PM
 */
public class MultiLabeler implements CrawlableDatasetLabeler {

  private List<CrawlableDatasetLabeler> labelerList;

  public MultiLabeler(List<CrawlableDatasetLabeler> labelerList) {
    this.labelerList = labelerList;
  }

  public Object getConfigObject() {
    return null;
  }

  public List<CrawlableDatasetLabeler> getLabelerList() {
    return labelerList;
  }

  public String getLabel(CrawlableDataset dataset) {
    String name;
    for (CrawlableDatasetLabeler curNamer : labelerList) {
      name = curNamer.getLabel(dataset);
      if (name != null) return name;
    }
    return null;
  }
}
