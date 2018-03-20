/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureCCIterator;
import ucar.nc2.ft.TrajectoryProfileFeature;
import ucar.nc2.ft.TrajectoryProfileFeatureCollection;
import ucar.nc2.time.CalendarDateUnit;

/**
 * Superclass for implementations of SectionFeatureCollection: series of profiles along a trajectory
 * Concrete subclass must implement getNestedPointFeatureCollectionIterator();
 *
 * @author caron
 * @since Oct 22, 2009
 */

public abstract class SectionCollectionImpl extends PointFeatureCCCImpl implements TrajectoryProfileFeatureCollection {

  protected SectionCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    super(name, timeUnit, altUnits, FeatureType.TRAJECTORY_PROFILE);
  }

  /////////////////////////////////////////////////////////////////////////////////////
  // deprecated
  protected PointFeatureCCIterator localIterator;

  @Override
  public boolean hasNext() throws IOException {
     if (localIterator == null) resetIteration();
     return localIterator.hasNext();
   }

   @Override
   public TrajectoryProfileFeature next() throws IOException {
     return (TrajectoryProfileFeature) localIterator.next();
   }

   @Override
   public void resetIteration() throws IOException {
     localIterator = getNestedPointFeatureCollectionIterator();
   }



}
