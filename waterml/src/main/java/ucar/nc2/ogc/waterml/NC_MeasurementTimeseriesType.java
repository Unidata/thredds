package ucar.nc2.ogc.waterml;

import net.opengis.waterml.v_2_0_1.MeasureTVPType;
import net.opengis.waterml.v_2_0_1.MeasurementTimeseriesType;
import net.opengis.waterml.v_2_0_1.TVPDefaultMetadataPropertyType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.FeatureDatasetUtil;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NC_MeasurementTimeseriesType {
    // om:OM_Observation/om:result/wml2:MeasurementTimeseries
    public static MeasurementTimeseriesType createMeasurementTimeseries(
            FeatureDatasetPoint fdPoint, StationTimeSeriesFeature stationFeat) throws IOException {
        MeasurementTimeseriesType measurementTimeseries = Factories.WATERML.createMeasurementTimeseriesType();

        // gml:id
        String id = generateId();
        measurementTimeseries.setId(id);

        // wml2:defaultPointMetadata
        VariableSimpleIF dataVar = FeatureDatasetUtil.getOnlyDataVariable(fdPoint);
        TVPDefaultMetadataPropertyType defaultPointMetadata =
                NC_TVPDefaultMetadataPropertyType.createDefaultPointMetadata(dataVar);
        measurementTimeseries.getDefaultPointMetadatas().add(defaultPointMetadata);

        // wml2:point[0..*]
        PointFeatureIterator pointFeatIter = stationFeat.getPointFeatureIterator(-1);
        while (pointFeatIter.hasNext()) {
            // wml2:point
            MeasurementTimeseriesType.Point point = createPoint(fdPoint, pointFeatIter.next());
            measurementTimeseries.getPoints().add(point);
        }

        return measurementTimeseries;
    }

    // om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point
    public static MeasurementTimeseriesType.Point createPoint(FeatureDatasetPoint fdPoint, PointFeature pointFeat)
            throws IOException {
        MeasurementTimeseriesType.Point point = Factories.WATERML.createMeasurementTimeseriesTypePoint();

        // wml2:MeasurementTVP
        MeasureTVPType measurementTVP = NC_MeasureTVPType.createMeasurementTVP(fdPoint, pointFeat);
        point.setMeasurementTVP(measurementTVP);

        return point;
    }

    private static int numIds = 0;

    private static String generateId() {
        return MeasurementTimeseriesType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_MeasurementTimeseriesType() { }
}
