package thredds.server.ncss.view.dsg;

import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.PointIteratorAbstract;

import java.io.IOException;

/**
 * Wraps an ordinary {@link ucar.nc2.ft.PointFeatureIterator}, but only retains elements that satisfy a provided
 * {@link ucar.nc2.ft.PointFeatureIterator.Filter filter}.
 *
 * @author cwardgar
 */
public class FilteredPointFeatureIterator extends PointIteratorAbstract {
    private final PointFeatureIterator origIter;
    private final PointFeatureIterator.Filter filter;
    private PointFeature pointFeature;

    public FilteredPointFeatureIterator(PointFeatureIterator origIter, PointFeatureIterator.Filter filter) {
        this.origIter = origIter;
        this.filter = filter;
    }

    ///////////////////////////////////////////// PointFeatureIterator /////////////////////////////////////////////

    @Override
    public boolean hasNext() throws IOException {
        pointFeature = nextFilteredDataPoint();
        if (pointFeature == null) {
            finish();
            return false;
        } else {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return  the next PointFeature, or {@code null} if this iterator contains no more elements.
     */
    // PointFeatureIterator.next() doesn't actually specify the behavior for next() when there are no more elements,
    // but we can define a stronger contract.
    @Override
    public PointFeature next() throws IOException {
        if (pointFeature == null) {
            return null;
        }

        calcBounds(pointFeature);
        return pointFeature;
    }

    @Override
    public void finish() {
        origIter.finish();
        finishCalcBounds();
    }

    @Override
    public void setBufferSize(int bufferSize) {
        origIter.setBufferSize(bufferSize);
    }

    /**
     * Returns the next point that satisfies the filter, or {@code null} if no such point exists.
     *
     * @return  the next point that satisfies the filter, or {@code null} if no such point exists.
     * @throws java.io.IOException  if an I/O error occurs.
     */
    private PointFeature nextFilteredDataPoint() throws IOException {
        while (origIter.hasNext()) {
            PointFeature pointFeat = origIter.next();
            if (filter.filter(pointFeat)) {
                return pointFeat;
            }
        }

        return null;
    }
}
