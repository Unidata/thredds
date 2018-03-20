/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDataset;

/**
 * CrawlableDatasetFilter implementation that accepts datasets whose
 * names are matched by the given regular expression.
 *
 * @author edavis
 * @since Nov 5, 2005 12:51:56 PM
 */
public class RegExpMatchOnNameFilter implements CrawlableDatasetFilter {


  private String regExpString;
  protected java.util.regex.Pattern pattern;

  public RegExpMatchOnNameFilter(String regExpString) {
    this.regExpString = regExpString;
    this.pattern = java.util.regex.Pattern.compile(regExpString);
  }

  public Object getConfigObject() {
    return regExpString;
  }

  public String getRegExpString() {
    return regExpString;
  }

  public boolean accept(CrawlableDataset dataset) {
    java.util.regex.Matcher matcher = this.pattern.matcher(dataset.getName());
    return matcher.matches();
  }
}
