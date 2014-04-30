package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.*;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_MeasurementTimeseriesType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries
    public static MeasurementTimeseriesType initMeasurementTimeseries(MeasurementTimeseriesType measurementTimeseries,
            StationTimeSeriesFeature stationFeat, VariableSimpleIF dataVar) throws IOException {
        // gml:id
        measurementTimeseries.setId(generateId());

        // wml2:defaultPointMetadata
        NC_TVPDefaultMetadataPropertyType.initDefaultPointMetadata(
                measurementTimeseries.addNewDefaultPointMetadata(), dataVar);

        // wml2:point[0..*]
        PointFeatureIterator pointFeatIter = stationFeat.getPointFeatureIterator(-1);
        while (pointFeatIter.hasNext()) {
            // wml2:point
            MeasurementTimeseriesType.Point point = measurementTimeseries.addNewPoint();

            // wml2:MeasurementTVP
            NC_MeasureTVPType.initMeasurementTVP(point.addNewMeasurementTVP(), pointFeatIter.next(), dataVar);
        }

        return measurementTimeseries;
    }

    private static int numIds = 0;

    private static String generateId() {
        return MeasurementTimeseriesType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_MeasurementTimeseriesType() { }
}
