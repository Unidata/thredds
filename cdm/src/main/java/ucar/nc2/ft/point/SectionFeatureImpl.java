/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.Iterator;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.TrajectoryProfileFeature;
import ucar.nc2.time.CalendarDateUnit;

/**
 * Abstract superclass for implementations of SectionFeature.
 * Subclass must implement getPointFeatureCollectionIterator();
 *
 * @author caron
 * @since Oct 22, 2009
 */


public abstract class SectionFeatureImpl extends PointFeatureCCImpl implements TrajectoryProfileFeature {

  protected SectionFeatureImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    super(name, timeUnit, altUnits, FeatureType.TRAJECTORY_PROFILE);
  }

  /////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Iterator<ProfileFeature> iterator() {
    try {
      PointFeatureCollectionIterator pfIterator = getPointFeatureCollectionIterator();
      return new CollectionIteratorAdapter<>(pfIterator);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated

  protected PointFeatureCollectionIterator localIterator;

  @Override
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  @Override
  public ProfileFeature next() throws IOException {
    return (ProfileFeature) localIterator.next();
  }

  @Override
  public void resetIteration() throws IOException {
    localIterator = getPointFeatureCollectionIterator();
  }

}
