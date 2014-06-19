package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.MeasurementTimeseriesType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.MarshallingUtil;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/05.
 */
public abstract class NcMeasurementTimeseriesType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries
    public static MeasurementTimeseriesType initMeasurementTimeseries(MeasurementTimeseriesType measurementTimeseries,
            StationTimeSeriesFeature stationFeat, VariableSimpleIF dataVar) throws IOException {
        // @gml:id
        String id = MarshallingUtil.createIdForType(MeasurementTimeseriesType.class);
        measurementTimeseries.setId(id);

        // wml2:defaultPointMetadata
        NcTVPDefaultMetadataPropertyType.initDefaultPointMetadata(
                measurementTimeseries.addNewDefaultPointMetadata(), dataVar);

        // wml2:point[0..*]
        stationFeat.resetIteration();
        try {
            while (stationFeat.hasNext()) {
                // wml2:point
                Point.initPoint(measurementTimeseries.addNewPoint(), stationFeat.next(), dataVar);
            }
        } finally {
            stationFeat.finish();
        }

        return measurementTimeseries;
    }

    public abstract static class Point {
        // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point
        public static MeasurementTimeseriesType.Point initPoint(MeasurementTimeseriesType.Point point,
                PointFeature pointFeat, VariableSimpleIF dataVar) throws IOException {
            // wml2:MeasurementTVP
            NcMeasureTVPType.initMeasurementTVP(point.addNewMeasurementTVP(), pointFeat, dataVar);

            return point;
        }

        private Point() { }
    }

    private NcMeasurementTimeseriesType() { }
}
