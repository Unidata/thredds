/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.filter;

import thredds.inventory.MFile;
import thredds.inventory.MFileFilter;

/**
 * return the negation of the wrapped filter
 *
 * @author caron
 * @since 1/22/2015
 */
public class FilterNegate implements MFileFilter {
  private MFileFilter f;

  public FilterNegate (MFileFilter f) {
    this.f = f;
  }

  @Override
  public boolean accept(MFile mfile) {
    return !f.accept(mfile);
  }
}
