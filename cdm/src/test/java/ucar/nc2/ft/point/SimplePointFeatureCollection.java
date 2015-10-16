package ucar.nc2.ft.point;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateUnit;

/**
 * @author cwardgar
 * @since 2014/10/07
 */
public class SimplePointFeatureCollection extends PointCollectionImpl {
    private final List<PointFeature> pointFeats;

    public SimplePointFeatureCollection(String name, CalendarDateUnit timeUnit, String altUnits) {
        super(name, timeUnit, altUnits);
        this.pointFeats = new LinkedList<>();
    }

    public PointFeature add(PointFeature pointFeat) {
        this.pointFeats.add(pointFeat);
        return pointFeat;
    }

    @Override
    public PointFeatureIterator getPointFeatureIterator() throws IOException {
        return new PointIteratorAdapter(pointFeats.iterator());
    }
}
