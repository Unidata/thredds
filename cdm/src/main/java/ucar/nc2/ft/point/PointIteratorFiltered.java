/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.util.NoSuchElementException;
import com.google.common.base.Preconditions;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Decorate a PointFeatureIterator with filtering.
 * @author caron
 * @since Mar 20, 2008
 */
public class PointIteratorFiltered extends PointIteratorAbstract {
    private final PointFeatureIterator origIter;
    private final PointFeatureIterator.Filter filter;
    private PointFeature pointFeature;

    // Originally, this was the only constructor for this class.
    public PointIteratorFiltered(PointFeatureIterator orgIter, LatLonRect filter_bb, CalendarDateRange filter_date) {
        this(orgIter, new SpaceAndTimeFilter(filter_bb, filter_date));
    }

    public PointIteratorFiltered(PointFeatureIterator origIter, PointFeatureIterator.Filter filter) {
        this.origIter = Preconditions.checkNotNull(origIter);
        this.filter = Preconditions.checkNotNull(filter);
    }
    
    /**
     * Returns {@code true} if the iteration has more elements. (In other words, returns {@code true} if {@link #next}
     * would return an element rather than throwing an exception.)
     * <p/>
     * This method is <i>idempotent</i>, meaning that when it is called repeatedly without an intervening
     * {@link #next}, calls after the first will have no effect.
     *
     * @return {@code true} if the iteration has more elements
     */
    // PointFeatureIterator.hasNext() doesn't guarantee idempotency, but we do.
    @Override
    public boolean hasNext() {
        if (pointFeature != null) {
            return true;  // pointFeature hasn't yet been consumed.
        }
        
        pointFeature = nextFilteredDataPoint();
        if (pointFeature == null) {
            close();
            return false;
        } else {
            return true;
        }
    }
  
  /**
   * Returns the next element in the iteration.
   *
   * @return the next element in the iteration
   * @throws java.util.NoSuchElementException if the iteration has no more elements.
   */
  // PointFeatureIterator.next() doesn't actually specify the behavior of next() when there are no more elements,
  // but we can define a stronger contract.
  @Override
  public PointFeature next() throws NoSuchElementException {
    if (!hasNext()) {
      throw new NoSuchElementException("This iterator has no more elements.");
    }
    
    assert pointFeature != null;
    PointFeature ret = pointFeature;
    calcBounds(ret);
    
    pointFeature = null;  // Feature has been consumed.
    return ret;
  }
  
  @Override
  public void close() {
        origIter.close();
        finishCalcBounds();
    }

    /**
     * Returns the next point that satisfies the filter, or {@code null} if no such point exists.
     *
     * @return the next point that satisfies the filter, or {@code null} if no such point exists.
     */
    private PointFeature nextFilteredDataPoint() {
        while (origIter.hasNext()) {
            PointFeature pointFeat = origIter.next();
            if (filter.filter(pointFeat)) {
                return pointFeat;
            }
        }

        return null;
    }


    /**
     * A filter that only permits features whose lat/lon falls within a given bounding box AND whose
     * observation time falls within a given date range.
     */
    public static class SpaceAndTimeFilter implements PointFeatureIterator.Filter {
        private final LatLonRect filter_bb;
        private final CalendarDateRange filter_date;

      /**
       * @param filter_bb bounding box or null for all
       * @param filter_date date Range or null for all
       */
        public SpaceAndTimeFilter(LatLonRect filter_bb, CalendarDateRange filter_date) {
          this.filter_bb = filter_bb;
          this.filter_date = filter_date;
        }

        @Override
        public boolean filter(PointFeature pointFeat) {
          if ((filter_date != null) && !filter_date.includes(pointFeat.getObservationTimeAsCalendarDate()))
            return false;

          if ((filter_bb != null) && !filter_bb.contains(pointFeat.getLocation().getLatitude(), pointFeat.getLocation().getLongitude()))
            return false;

          return true;
        }
    }

}
