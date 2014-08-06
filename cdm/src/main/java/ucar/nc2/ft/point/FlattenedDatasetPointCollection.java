package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NestedPointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.LatLonRect;

/**
 * An aggregate collection of all the PointFeatures in a dataset formed by flattening the nested structures within.
 * This class's {@link #getPointFeatureIterator iterator} returns features in default order, with maximum read
 * efficiency as the goal.
 *
 * @author cwardgar
 * @since 2014/10/08
 */
public class FlattenedDatasetPointCollection extends PointCollectionImpl {
    private final FeatureDatasetPoint fdPoint;

    /**
     * Constructs a FlattenedDatasetPointCollection.
     *
     * @param fdPoint   a point dataset.
     * @throws IllegalArgumentException if any of the feature collections in the dataset are not of type
     *                                  {@code PointFeatureCollection} or {@code NestedPointFeatureCollection}.
     */
    public FlattenedDatasetPointCollection(FeatureDatasetPoint fdPoint) throws IllegalArgumentException {
        super(fdPoint.getLocation(), DateUnit.getUnixDateUnit(), null);  // Default dateUnit and altUnits.
        this.fdPoint = fdPoint;

        List<FeatureCollection> featCols = fdPoint.getPointFeatureCollectionList();
        checkFeatureCollections(featCols);

        if (!featCols.isEmpty()) {
            FeatureCollection firstFeatCol = featCols.get(0);

            // Replace this.dateUnit, this.altUnits, and this.extras with "typical" values from firstFeatCol.
            // We can't be certain that those values are representative of ALL collections in the dataset, but it's
            // a decent bet because in practice, firstFeatCol is so often the ONLY collection.
            copyFieldsFrom(firstFeatCol);
        }
    }

    /**
     * Checks that feature collections are the expected types.
     *
     * @param featCols  feature collections.
     * @throws IllegalArgumentException if any of the feature collections are not of type {@code PointFeatureCollection}
     *                                  or {@code NestedPointFeatureCollection}
     */
    private static void checkFeatureCollections(Iterable<FeatureCollection> featCols) throws IllegalArgumentException {
        for (FeatureCollection featCol : featCols) {
            if (!(featCol instanceof PointFeatureCollection) && !(featCol instanceof NestedPointFeatureCollection)) {
                String message = String.format("FeatureDatasetPoint.getPointFeatureCollectionList() states that its" +
                        "FeatureCollections must be of type PointFeatureCollection or NestedPointFeatureCollection, " +
                        "but we found a %s.", featCol.getClass().getName());
                throw new IllegalArgumentException(message);
            }
        }
    }

    private void copyFieldsFrom(FeatureCollection featCol) {
        assert featCol instanceof PointFeatureCollection || featCol instanceof NestedPointFeatureCollection :
                "We should have thrown an exception for this condition in the constructor.";

        if (featCol instanceof PointFeatureCollection) {
            PointFeatureCollection pointFeatCol = (PointFeatureCollection) featCol;
            this.timeUnit = pointFeatCol.getTimeUnit();
            this.altUnits = pointFeatCol.getAltUnits();
            this.extras   = pointFeatCol.getExtraVariables();
        } else {
            NestedPointFeatureCollection nestedPointFeatCol = (NestedPointFeatureCollection) featCol;
            this.timeUnit = nestedPointFeatCol.getTimeUnit();
            this.altUnits = nestedPointFeatCol.getAltUnits();
            this.extras   = nestedPointFeatCol.getExtraVariables();
        }
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
        PointFeatureIterator iter = new FlattenedDatasetPointIterator(fdPoint);
        iter.setBufferSize(bufferSize);
        return iter;
    }


    protected static class FlattenedDatasetPointIterator implements PointFeatureIterator {
        private final Iterator<FeatureCollection> featColIter;
        private PointFeatureIterator pointFeatIter;

        private PointFeatureCollection collection;

        private int bufferSize = -1;
        private int count = 0;
        private boolean calcBounds = false;
        private CalendarDateRange calendarDateRange;
        private LatLonRect boundingBox;

        public FlattenedDatasetPointIterator(FeatureDatasetPoint fdPoint) {
            this.featColIter = fdPoint.getPointFeatureCollectionList().iterator();
        }

        @Override
        public boolean hasNext() {
            while (pointFeatIter == null || !pointFeatIter.hasNext()) {
                if (pointFeatIter != null) {
                    pointFeatIter.close();  // Release the resources of the old iter.
                    updateBounds(pointFeatIter);
                }

                if (!featColIter.hasNext()) {
                    return false;
                } else {
                    pointFeatIter = getNextPointFeatureIterator();
                }
            }

            return true;
        }

        private void updateBounds(PointFeatureIterator pointFeatIter) {
            count += pointFeatIter.getCount();  // Always count obs. For parity with PointIteratorAbstract.calcBounds().

            if (calcBounds) {
                if (calendarDateRange == null) {
                    calendarDateRange = pointFeatIter.getCalendarDateRange();
                } else {
                    calendarDateRange = calendarDateRange.extend(pointFeatIter.getCalendarDateRange());
                }

                if (boundingBox == null) {
                    boundingBox = pointFeatIter.getBoundingBox();
                } else {
                    boundingBox.extend(pointFeatIter.getBoundingBox());
                }
            }

            if (collection != null) {
                collection.setSize(count);
                collection.setCalendarDateRange(calendarDateRange);
                collection.setBoundingBox(boundingBox);
            }
        }

        private PointFeatureIterator getNextPointFeatureIterator() {
            FeatureCollection featCol = featColIter.next();
            assert featCol != null : "featColIter.hasNext() was called above.";
            assert featCol instanceof PointFeatureCollection || featCol instanceof NestedPointFeatureCollection :
                    "We should have thrown an exception for this condition in the enclosing class's constructor.";

            try {
                PointFeatureCollection pointFeatCol;
                if (featCol instanceof PointFeatureCollection) {
                    pointFeatCol = (PointFeatureCollection) featCol;
                } else {
                    pointFeatCol = ((NestedPointFeatureCollection) featCol).flatten(null, (CalendarDateRange) null);
                }

                PointFeatureIterator pointFeatIter = pointFeatCol.getPointFeatureIterator(bufferSize);
                if (calcBounds) {
                    pointFeatIter.setCalculateBounds(pointFeatCol);
                }
                return pointFeatIter;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return  the next PointFeature, or {@code null} if this iterator contains zero elements.
         */
        @Override
        public PointFeature next() {
            if (pointFeatIter == null) {  // Could be null if featColIter is empty.
                return null;
            } else {
                return pointFeatIter.next();
            }
        }

        @Override
        public void close() {
            if (pointFeatIter != null) {
                pointFeatIter.close();
            }
        }

        @Override
        public void setBufferSize(int bytes) {
            this.bufferSize = bytes;
        }

        @Override
        public void setCalculateBounds(PointFeatureCollection collection) {
            this.calcBounds = true;
            this.collection = collection;
        }

        @Override
        public LatLonRect getBoundingBox() {
            return boundingBox;
        }

        @Override
        public DateRange getDateRange() {
            CalendarDateRange cdr = getCalendarDateRange();
            return (cdr != null) ? cdr.toDateRange() : null;
        }

        @Override
        public CalendarDateRange getCalendarDateRange() {
            return calendarDateRange;
        }

        @Override
        public int getCount() {
            return count;
        }
    }
}
