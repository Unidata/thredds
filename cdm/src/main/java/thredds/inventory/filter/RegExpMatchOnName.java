/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.filter;

import thredds.inventory.MFileFilter;
import thredds.inventory.MFile;

/**
 * A regular expression that matches on the MFile name.
 *
 * @author caron
 * @since Jun 26, 2009
 */
public class RegExpMatchOnName implements MFileFilter {
    private String regExpString;
    private java.util.regex.Pattern pattern;

    public RegExpMatchOnName(String regExpString) {
      this.regExpString = regExpString;
      this.pattern = java.util.regex.Pattern.compile(regExpString);
    }

    public boolean accept(MFile file) {
      java.util.regex.Matcher matcher = this.pattern.matcher(file.getName());
      return matcher.matches();
    }

  @Override
  public String toString() {
    return regExpString;
  }
}