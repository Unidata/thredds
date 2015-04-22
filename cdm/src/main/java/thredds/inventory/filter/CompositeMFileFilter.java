/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
