/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.filter;

import thredds.inventory.MFileFilter;
import thredds.inventory.MFile;

import java.util.List;
import java.util.ArrayList;

/**
 * Composite of MFileFilter
 * Used by DatasetScan, FMRC, ?
 *
 * @author caron
 * @since Jul 8, 2009
 */

public class CompositeMFileFilter implements MFileFilter {
  private List<MFileFilter> includeFilters;
  private List<MFileFilter> excludeFilters;
  private List<MFileFilter> andFilters;

  public CompositeMFileFilter() {
  }

  /* public CompositeMFileFilter(List<MFileFilter> filters) {
    for (MFileFilter ff : filters)
      addIncludeFilter(ff);
  } */

  public void addFilter(MFileFilter filter, boolean include) {
    if (include)
      addIncludeFilter(filter);
    else
      addExcludeFilter(filter);
  }

  public void addIncludeFilter(MFileFilter filter) {
    if (includeFilters == null) includeFilters = new ArrayList<>();
    includeFilters.add(filter);
  }

  public void addExcludeFilter(MFileFilter filter) {
    if (excludeFilters == null) excludeFilters = new ArrayList<>();
    excludeFilters.add(filter);
  }

  public void addAndFilter(MFileFilter filter) {
    if (andFilters == null) andFilters = new ArrayList<>();
    andFilters.add(filter);
  }

  public boolean accept(MFile mfile) {
    return include(mfile) && !exclude(mfile) && andFilter(mfile);
  }

  // inclusion is an OR
  private boolean include(MFile mfile) {
    if (includeFilters == null) return true;
    for (MFileFilter filter : includeFilters) {
      if (filter.accept(mfile))
        return true;
    }
    return false;
  }

  // exclusion is an AND
  private boolean exclude(MFile mfile) {
    if (excludeFilters == null) return false;
    for (MFileFilter filter : excludeFilters) {
      if (filter.accept(mfile))
        return true;
    }
    return false;
  }

  // all AND filters must be satisfied
  private boolean andFilter(MFile mfile) {
    if (andFilters == null) return true;
    for (MFileFilter filter : andFilters) {
      if (!filter.accept(mfile))
        return false;
    }
    return true;
  }

}
