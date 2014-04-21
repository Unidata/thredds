package ucar.nc2.ogc2.waterml;

import net.opengis.waterml.x20.MeasureTVPType;
import net.opengis.waterml.x20.MeasurementTimeseriesType;
import net.opengis.waterml.x20.TVPDefaultMetadataPropertyType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_MeasurementTimeseriesType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries
    public static MeasurementTimeseriesType createMeasurementTimeseries(
            StationTimeSeriesFeature stationFeat, VariableSimpleIF dataVar)
            throws IOException {
        MeasurementTimeseriesType measurementTimeseries = MeasurementTimeseriesType.Factory.newInstance();

        // gml:id
        String id = generateId();
        measurementTimeseries.setId(id);

        // wml2:defaultPointMetadata
        TVPDefaultMetadataPropertyType defaultPointMetadata =
                NC_TVPDefaultMetadataPropertyType.createDefaultPointMetadata(dataVar);
        TVPDefaultMetadataPropertyType[] defaultPointMetadataArray =
                new TVPDefaultMetadataPropertyType[] { defaultPointMetadata };
        measurementTimeseries.setDefaultPointMetadataArray(defaultPointMetadataArray);

        // wml2:point[0..*]
        PointFeatureIterator pointFeatIter = stationFeat.getPointFeatureIterator(-1);
        while (pointFeatIter.hasNext()) {
            // wml2:point
            MeasurementTimeseriesType.Point point = measurementTimeseries.addNewPoint();

            // wml2:MeasurementTVP
            MeasureTVPType measurementTVP = NC_MeasureTVPType.createMeasurementTVP(pointFeatIter.next(), dataVar);
            point.setMeasurementTVP(measurementTVP);
        }

        return measurementTimeseries;
    }

    private static int numIds = 0;

    private static String generateId() {
        return MeasurementTimeseriesType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_MeasurementTimeseriesType() { }
}
