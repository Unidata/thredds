/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import ucar.nc2.ft.PointFeature;

/**
 * An empty PointIterator.
 *
 * @author caron
 * @since Oct 1, 2009
 */
public class PointIteratorEmpty extends PointIteratorAbstract {

  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public PointFeature next() {
    return null;
  }

  @Override
  public void close() {
  }
}
