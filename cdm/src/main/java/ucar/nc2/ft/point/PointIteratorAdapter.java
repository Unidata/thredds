package ucar.nc2.ft.point;

import java.util.Iterator;
import java.util.NoSuchElementException;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;

/**
 * Adapts an {@link Iterator}<{@link PointFeature}> to a {@link PointFeatureIterator}. The original iterator gains
 * bounds calculation and compatibility with the rest of the Point Feature API.
 *
 * @author cwardgar
 * @since 2014/08/30
 */
public class PointIteratorAdapter extends PointIteratorAbstract {
    private final Iterator<? extends PointFeature> pointIter;

    /**
     * Creates a {@link PointFeatureIterator} from the supplied basic iterator.
     *
     * @param pointIter  a basic iterator over point features.
     */
    public PointIteratorAdapter(Iterator<? extends PointFeature> pointIter) {
        this.pointIter = pointIter;
    }

    /**
     * Returns {@code true} if the iteration has more elements. (In other words, returns {@code true} if {@link #next}
     * would return an element rather than throwing an exception.)
     * <p/>
     * This method is <i>idempotent</i>, meaning that when it is called repeatedly without an intervening
     * {@link #next}, calls after the first will have no effect.
     *
     * @return {@code true} if the iteration has more elements, {@code false} otherwise. A {@code false} return value
     *         {@link #close finishes} this iteration.
     */
    // PointFeatureIterator.hasNext() doesn't guarantee idempotency, but we do.
    @Override
    public boolean hasNext() {
        if (!pointIter.hasNext()) {
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
     * @throws NoSuchElementException if the iteration has no more elements.
     */
    // PointFeatureIterator.next() doesn't actually specify the behavior of next() when there are no more elements,
    // but we can define a stronger contract.
    @Override
    public PointFeature next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException("The iteration has no more elements.");
        }

        PointFeature pointFeat = pointIter.next();
        assert pointFeat != null : "hasNext() should have been false. WTF?";

        calcBounds(pointFeat);
        return pointFeat;
    }

    /**
     * Finishes bounds calculation, {@link #setCalculateBounds if it has been enabled}. Do not attempt to retrieve
     * bounds information (via {@link #getBoundingBox}, {@link #getDateRange}, {@link #getCalendarDateRange}, or
     * {@link #getCount}) before this method has been invoked or {@link #hasNext} returns {@code false}.
     * <p/>
     * This method is idempotent, meaning that calls after the first have no effect.
     */
    @Override
    public void close() {
        finishCalcBounds();  // Method is idempotent.
    }

    /**
     * Does nothing in this implementation.
     *
     * @param bytes amount of memory in bytes
     */
    @Override
    public void setBufferSize(int bytes) {
        // No-op
    }
}
