package ucar.nc2.ft.point;

import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.units.DateUnit;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * @author cwardgar
 * @since 2014/10/07
 */
public class SimplePointFeatureCollection extends PointCollectionImpl {
    private final List<PointFeature> pointFeats;

    public SimplePointFeatureCollection(String name, DateUnit timeUnit, String altUnits) {
        super(name, timeUnit, altUnits);
        this.pointFeats = new LinkedList<>();
    }

    public void add(PointFeature pointFeat) {
        this.pointFeats.add(pointFeat);
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
        return new PointIteratorAdapter(pointFeats.iterator());
    }
}
