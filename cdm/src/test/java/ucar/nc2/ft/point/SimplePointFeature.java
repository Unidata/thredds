package ucar.nc2.ft.point;

import java.io.IOException;
import javax.annotation.Nonnull;
import ucar.ma2.StructureData;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.EarthLocation;

/**
 * @author cwardgar
 * @since 2014/08/29
 */
public class SimplePointFeature extends PointFeatureImpl {
    private final StructureData featureData;

    public SimplePointFeature(DsgFeatureCollection dsg, EarthLocation location, double obsTime, double nomTime,
            CalendarDateUnit timeUnit, StructureData featureData) {
        super(dsg, location, obsTime, nomTime, timeUnit);
        this.featureData = featureData;
    }

    @Nonnull
    @Override
    public StructureData getFeatureData() throws IOException {
        return featureData;
    }

    @Nonnull
    @Override
    public StructureData getDataAll() throws IOException {
        return featureData;
    }
}
