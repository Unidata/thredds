/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDateUnit;

import javax.annotation.Nonnull;

/**
 * Describe
 *
 * @author caron
 * @since 10/5/2015.
 */
public class SimpleDsgCollection extends DsgCollectionImpl {
  FeatureType ftype;

  protected SimpleDsgCollection(String name, CalendarDateUnit timeUnit, String altUnits, FeatureType ftype) {
    super(name, timeUnit, altUnits);
    this.ftype = ftype;
  }

  @Nonnull
  @Override
  public FeatureType getCollectionFeatureType() {
    return ftype;
  }
}
