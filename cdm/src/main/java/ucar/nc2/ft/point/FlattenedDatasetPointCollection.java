package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureCCC;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;

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
        super(fdPoint.getLocation(), CalendarDateUnit.unixDateUnit, null);  // Default dateUnit and altUnits.
        this.fdPoint = fdPoint;

        List<DsgFeatureCollection> featCols = fdPoint.getPointFeatureCollectionList();

        if (!featCols.isEmpty()) {
            DsgFeatureCollection firstFeatCol = featCols.get(0);

            // Replace this.dateUnit, this.altUnits, and this.extras with "typical" values from firstFeatCol.
            // We can't be certain that those values are representative of ALL collections in the dataset, but it's
            // a decent bet because in practice, firstFeatCol is so often the ONLY collection.
            copyFieldsFrom(firstFeatCol);
        }
    }

    private void copyFieldsFrom(DsgFeatureCollection featCol) {
        this.timeUnit = featCol.getTimeUnit();
        this.altUnits = featCol.getAltUnits();
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
        PointFeatureIterator iter = new FlattenedDatasetPointIterator(fdPoint);
        iter.setBufferSize(bufferSize);
        return iter;
    }


    protected class FlattenedDatasetPointIterator extends PointIteratorAbstract {
        private final Iterator<DsgFeatureCollection> dsgFeatColIter;

        private PointFeatureIterator pfIter;
        private IOIterator<PointFeatureCollection> pfcIter;
        private IOIterator<PointFeatureCC> pfccIter;

        private boolean finished = false;  // set to "true" when close() is called.

        public FlattenedDatasetPointIterator(FeatureDatasetPoint fdPoint) {
            this.dsgFeatColIter = fdPoint.getPointFeatureCollectionList().iterator();
            setCalculateBounds(FlattenedDatasetPointCollection.this.getInfo());
        }

        @Override
        public boolean hasNext() {
            try {
                // pfIterHasNext() will fail the first time hasNext() is called because no DsgFeatureCollection has
                // been loaded yet.
                while (!pfIterHasNext()) {
                    if (!loadNextDsgFeatureCollection()) {
                        close();  // May not be called otherwise if iter is being used in a for-each.
                        return false;
                    }
                }

                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Attempts to find a PointFeatureIterator in the currently-loaded DsgFeatureCollection that has another
         * available element (i.e. {@code hasNext() == true}). Such an iterator may already be loaded into
         * {@code pfIter}. If not, we'll have to look through {@code pfcIter} and/or {@code pfccIter} to find one.
         * <p>
         * That iterator, if it's found, will be assigned to {@code pfIter} and this method will return {@code true}.
         * Otherwise, it'll return {@code false}, meaning that there are no more unread PointFeatures available in the
         * currently-loaded DsgFeatureCollection.
         *
         * @return  {@code true} if {@code pfIter.hasNext()} will now return {@code true}.
         * @throws IOException  if an I/O error occurs.
         */
        private boolean pfIterHasNext() throws IOException {
            if (pfIter != null) {
                if (pfIter.hasNext()) {
                    return true;
                } else {
                    // We'll need to load a new PointFeatureIterator below. But first, close the old one.
                    pfIter.close();
                }
            }

            while (pfcIterHasNext()) {
                this.pfIter = pfcIter.next().getPointFeatureIterator(-1);
                if (pfIter.hasNext()) {
                    return true;
                }
                // else: Iterator could be empty, in which case we proceed to the next loop iteration.
            }

            return false;
        }

        /**
         * Attempts to find a {@code IOIterator<PointFeatureCollection>} in the currently-loaded DsgFeatureCollection
         * that has another available element (i.e. {@code hasNext() == true}). Such an iterator may already be loaded
         * into {@code pfcIter}. If not, we'll have to look through {@code pfccIter} to find one.
         * <p>
         * The iterator, if it's found, will be assigned to {@code pfcIter} and this method will return {@code true}.
         * Otherwise, it'll return {@code false}, meaning that there are no more unread PointFeatureCollection
         * iterators available in the currently-loaded DsgFeatureCollection
         *
         * @return  {@code true} if {@code pfcIter.hasNext()} will now return {@code true}.
         * @throws IOException  if an I/O error occurs.
         */
        private boolean pfcIterHasNext() throws IOException {
            if (pfcIter != null && pfcIter.hasNext()) {
                return true;
            }

            while (pfccIter != null && pfccIter.hasNext()) {
                pfcIter = pfccIter.next().getCollectionIterator(-1);
                if (pfcIter.hasNext()) {
                    return true;
                }
                // else: Iterator could be empty, in which case we proceed to the next loop iteration.
            }

            return false;
        }

        /**
         * Retrieves the next DsgFeatureCollection from {@code dsgFeatColIter} and assigns it to the appropriate data
         * member. The DsgFeatureCollections returned by {@link FeatureDatasetPoint#getPointFeatureCollectionList} will
         * be one of the following 3 subtypes:
         * <ul>
         *     <li>{@link PointFeatureCollection}: will be assigned to {@code pfIter}</li>
         *     <li>{@link PointFeatureCC}: will be assigned to {@code pfcIter}</li>
         *     <li>{@link PointFeatureCCC}: will be assigned to {@code pfccIter}</li>
         * </ul>
         *
         * @return  {@code true} if the next DsgFeatureCollection was successfully loaded into the appropriate data
         *          member, or {@code false} if no more remain.
         * @throws IOException  if an I/O error occurs.
         */
        private boolean loadNextDsgFeatureCollection() throws IOException {
            if (!dsgFeatColIter.hasNext()) {
                return false;
            }

            // Clear out any iterators belonging to the previous DsgFeatureCollection
            pfIter = null;
            pfcIter = null;
            pfccIter = null;

            DsgFeatureCollection dsgFeatCol = dsgFeatColIter.next();
            if (dsgFeatCol instanceof PointFeatureCollection) {
                pfIter = ((PointFeatureCollection) dsgFeatCol).getPointFeatureIterator(-1);
            } else if (dsgFeatCol instanceof PointFeatureCC) {
                pfcIter = ((PointFeatureCC) dsgFeatCol).getCollectionIterator(-1);
            } else if (dsgFeatCol instanceof PointFeatureCCC) {
                pfccIter = ((PointFeatureCCC) dsgFeatCol).getCollectionIterator(-1);
            } else {
                throw new AssertionError("CAN'T HAPPEN: FeatureDatasetPoint.getPointFeatureCollectionList() " +
                        "only contains PointFeatureCollection, PointFeatureCC, or PointFeatureCCC.");
            }

            return true;
        }

        @Override
        public PointFeature next() {
            if (pfIter == null) {  // Could be null if hasNext() == false or wasn't called at all.
                return null;
            } else {
                PointFeature pointFeat = pfIter.next();
                calcBounds(pointFeat);
                return pointFeat;
            }
        }

        @Override
        public void close() {
            if (finished) {
                return;
            }

            // If hasNext() was repeatedly called until it returned "false", all PointFeatureIterators should've
            // already been closed. However, this may be useful in exceptional circumstances.
            if (pfIter != null) {
                pfIter.close();
            }

            finishCalcBounds();
            finished = true;
        }
    }
}
