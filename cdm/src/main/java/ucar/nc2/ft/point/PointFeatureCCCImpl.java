/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import ucar.nc2.Variable;
import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonRect;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract superclass for multiply nested NestedPointFeatureCollection
 * Subclass must supply getNestedPointFeatureCollectionIterator().
 *
 * @author caron
 * @since Mar 26, 2008
 */
public abstract class PointFeatureCCCImpl extends DsgCollectionImpl implements PointFeatureCCC {
  protected FeatureType collectionFeatureType;

  protected PointFeatureCCCImpl(String name, CalendarDateUnit timeUnit, String altUnits, FeatureType collectionFeatureType) {
    super( name, timeUnit, altUnits);
    this.collectionFeatureType = collectionFeatureType;
  }

  // All features in this collection have this feature type
  @Nonnull
  @Override
  public FeatureType getCollectionFeatureType() {
    return collectionFeatureType;
  }

}
