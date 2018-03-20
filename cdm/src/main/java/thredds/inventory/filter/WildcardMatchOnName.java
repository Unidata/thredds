/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory.filter;

import thredds.inventory.MFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A wildcard expression that matches on the MFile name.
 *
 * @author caron
 * @since Jun 26, 2009
 */
public class WildcardMatchOnName extends WildcardMatchOnPath {
  public WildcardMatchOnName(String wildcardString) {
    super(wildcardString);
  }

  public WildcardMatchOnName(Pattern pattern) {
    super(pattern);
  }

  public boolean accept(MFile file) {
    Matcher matcher = this.pattern.matcher(file.getName());
    return matcher.matches();
  }

  public static void main(String[] args) {
    //WildcardMatchOnName m = new WildcardMatchOnName("ECMWF_GNERA_d000..........");
    WildcardMatchOnName m = new WildcardMatchOnName("ECMWF_GNERA_d000..20121001");
    Matcher matcher =             m.pattern.matcher("ECMWF_GNERA_d0002.20121001");
    System.out.printf("%s%n", matcher.matches());
  }
}
