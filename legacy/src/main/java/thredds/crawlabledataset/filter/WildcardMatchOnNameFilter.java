/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.crawlabledataset.filter;

import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.crawlabledataset.CrawlableDataset;

/**
 * CrawlableDatasetFilter implementation that accepts datasets whose
 * names are matched by the given wildcard string. The wildcard string
 * can contain astrisks ("*") which match 0 or more characters and
 * question marks ("?") which match 0 or 1 character.
 *
 * @author edavis
 * @since Nov 5, 2005 12:51:56 PM
 */
public class WildcardMatchOnNameFilter implements CrawlableDatasetFilter {


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

  public Object getConfigObject() {
    return wildcardString;
  }

  public String getWildcardString() {
    return wildcardString;
  }

  public boolean accept(CrawlableDataset dataset) {
    java.util.regex.Matcher matcher = this.pattern.matcher(dataset.getName());
    return matcher.matches();
  }
}
