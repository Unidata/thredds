/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDataset;

/**
 * @author caron
 * @since Aug 10, 2007
 */
public class RegExpMatchOnPathFilter extends RegExpMatchOnNameFilter {

  public RegExpMatchOnPathFilter(String regExpString) {
    super( regExpString);
  }

  public boolean accept(CrawlableDataset dataset) {
    java.util.regex.Matcher matcher = this.pattern.matcher(dataset.getPath());
    return matcher.matches();
  }
}
