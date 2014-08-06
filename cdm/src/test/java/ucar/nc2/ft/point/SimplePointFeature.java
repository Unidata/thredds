package ucar.nc2.ft.point;

import ucar.ma2.StructureData;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.EarthLocation;

import java.io.IOException;

/**
 * @author cwardgar
 * @since 2014/08/29
 */
public class SimplePointFeature extends PointFeatureImpl {
    private final StructureData featureData;

    public SimplePointFeature(
            EarthLocation location, double obsTime, double nomTime, DateUnit timeUnit, StructureData featureData) {
        super(location, obsTime, nomTime, timeUnit);
        this.featureData = featureData;
    }

    @Override
    public StructureData getFeatureData() throws IOException {
        return featureData;
    }

    @Override
    public StructureData getDataAll() throws IOException {
        return featureData;
    }
}
