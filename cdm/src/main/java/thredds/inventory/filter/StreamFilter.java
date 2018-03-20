/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.filter;

import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * A java.nio.file.DirectoryStream.Filter using a regexp on the last entry of the Path
 *
 * @author John
 * @since 1/28/14
 */
public class StreamFilter implements DirectoryStream.Filter<Path> {
  private Pattern pattern;
  private boolean nameOnly;

  public StreamFilter(Pattern pattern, boolean nameOnly) {
    this.pattern = pattern;
    this.nameOnly = nameOnly;
    // System.out.printf("Pattern %s%n", pattern);
  }

  @Override
  public boolean accept(Path entry) throws IOException {

    String matchOn = nameOnly ? entry.getName(entry.getNameCount()-1).toString() : StringUtil2.replace(entry.toString(), "\\", "/");

    java.util.regex.Matcher matcher = this.pattern.matcher(matchOn);
    boolean ok =  matcher.matches();
    //System.out.printf("%s %s%n", ok, matchOn);
    return ok;
  }
}
