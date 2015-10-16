package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.util.IOIterator;

/**
 * @author cwardgar
 * @since 2015/10/06
 */
public class SimplePointFeatureCC extends PointFeatureCCImpl {
    private final List<PointFeatureCollection> pointFeatCols;

    public SimplePointFeatureCC(
            String name, CalendarDateUnit timeUnit, String altUnits, FeatureType collectionFeatureType) {
        super(name, timeUnit, altUnits, collectionFeatureType);
        this.pointFeatCols = new LinkedList<>();
    }

    public PointFeatureCollection add(PointFeatureCollection pointFeatCol) {
        pointFeatCols.add(pointFeatCol);
        return pointFeatCol;
    }

    @Override
    public IOIterator<PointFeatureCollection> getCollectionIterator() throws IOException {
        return new IOIterator<PointFeatureCollection>() {
            private final Iterator<PointFeatureCollection> pfcIter = pointFeatCols.iterator();

            @Override
            public boolean hasNext() throws IOException {
                return pfcIter.hasNext();
            }

            @Override
            public PointFeatureCollection next() throws IOException {
                return pfcIter.next();
            }
        };
    }
}
