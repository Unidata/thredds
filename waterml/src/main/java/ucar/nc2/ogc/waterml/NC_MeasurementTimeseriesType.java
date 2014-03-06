package ucar.nc2.ogc.waterml;

import net.opengis.waterml.v_2_0_1.MeasureTVPType;
import net.opengis.waterml.v_2_0_1.MeasurementTimeseriesType;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_MeasurementTimeseriesType {
    // om:OM_Observation/om:result/wml2:MeasurementTimeseries
    public static MeasurementTimeseriesType createMeasurementTimeseries(StationTimeSeriesFeature stationFeat)
            throws IOException {
        MeasurementTimeseriesType measurementTimeseries = Factories.WATERML.createMeasurementTimeseriesType();

        // gml:id
        String id = MeasurementTimeseriesType.class.getSimpleName() + "." + "1";
        measurementTimeseries.setId(id);

        // wml2:point[0..*]
        PointFeatureIterator pointFeatIter = stationFeat.getPointFeatureIterator(-1);
        while (pointFeatIter.hasNext()) {
            // wml2:point
            MeasurementTimeseriesType.Point point = createPoint(pointFeatIter.next());
            measurementTimeseries.getPoints().add(point);
        }

        return measurementTimeseries;
    }

    // om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point
    public static MeasurementTimeseriesType.Point createPoint(PointFeature pointFeat) {
        MeasurementTimeseriesType.Point point = Factories.WATERML.createMeasurementTimeseriesTypePoint();

        // wml2:MeasurementTVP
        MeasureTVPType measurementTVP = NC_MeasureTVPType.createMeasurementTVP(pointFeat);
        point.setMeasurementTVP(measurementTVP);

        return point;
    }

    private NC_MeasurementTimeseriesType() { }
}
