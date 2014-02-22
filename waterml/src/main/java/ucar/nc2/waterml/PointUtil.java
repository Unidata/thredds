package ucar.nc2.waterml;

import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.PointCollectionImpl;
import ucar.nc2.ft.point.PointIteratorAbstract;

import java.io.IOException;
import java.util.Formatter;

/**
 * Created by cwardgar on 2014/02/21.
 */
public class PointUtil {
    /**
     * Opens the dataset at {@code location} as a FeatureDatasetPoint.
     *
     * @param wantFeatureType open this kind of FeatureDataset; may be null, which means search all factories.
     *                        It should be one of the point types.
     * @param location        the URL or file location of the dataset.
     *                        See {@link ucar.nc2.ft.FeatureDatasetFactoryManager#open}.
     * @return a subclass of FeatureDatasetPoint.
     * @throws java.io.IOException     if an I/O error occurs.
     * @throws NoFactoryFoundException if no {@link ucar.nc2.ft.FeatureDatasetFactory} could be found that can open
     *                                 the dataset at {@code location}.
     */
    public static FeatureDatasetPoint openPointDataset(FeatureType wantFeatureType, String location)
            throws IOException, NoFactoryFoundException {
        Formatter errlog = new Formatter();
        FeatureDataset fDset = FeatureDatasetFactoryManager.open(wantFeatureType, location, null, errlog);

        if (fDset == null) {
            throw new NoFactoryFoundException(errlog.toString());
        } else {
            return (FeatureDatasetPoint) fDset;
        }
    }

    public static class EmptyPointFeatureCollection extends PointCollectionImpl {
        public EmptyPointFeatureCollection() {
            super("Empty");
        }

        @Override public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
            return new EmptyPointFeatureIterator();
        }
    }

    public static class EmptyPointFeatureIterator extends PointIteratorAbstract {
        @Override public boolean hasNext() throws IOException {
            return false;
        }

        @Override public PointFeature next() throws IOException {
            return null;
        }

        @Override public void finish() {
        }

        @Override public void setBufferSize(int bytes) {
        }
    }
}
