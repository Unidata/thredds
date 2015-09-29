/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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

    @Override
    public void setBufferSize(int bufferSize) {
        origIter.setBufferSize(bufferSize);
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

        public SpaceAndTimeFilter(LatLonRect filter_bb, CalendarDateRange filter_date) {
            this.filter_bb = Preconditions.checkNotNull(filter_bb);
            this.filter_date = Preconditions.checkNotNull(filter_date);
        }

        @Override
        public boolean filter(PointFeature pointFeat) {
            return filter_bb.contains(pointFeat.getLocation().getLatLon()) &&
                    filter_date.includes(pointFeat.getObservationTimeAsCalendarDate());
        }
    }
}
