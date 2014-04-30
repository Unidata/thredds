package ucar.nc2.ogc.waterml;

import net.opengis.waterml.x20.MeasurementTimeseriesType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeatureIterator;
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
        // gml:id
        String id = MarshallingUtil.createIdForType(MeasurementTimeseriesType.class);
        measurementTimeseries.setId(id);

        // wml2:defaultPointMetadata
        NcTVPDefaultMetadataPropertyType.initDefaultPointMetadata(
                measurementTimeseries.addNewDefaultPointMetadata(), dataVar);

        // wml2:point[0..*]
        PointFeatureIterator pointFeatIter = stationFeat.getPointFeatureIterator(-1);
        while (pointFeatIter.hasNext()) {
            // wml2:point
            MeasurementTimeseriesType.Point point = measurementTimeseries.addNewPoint();

            // wml2:MeasurementTVP
            NcMeasureTVPType.initMeasurementTVP(point.addNewMeasurementTVP(), pointFeatIter.next(), dataVar);
        }

        return measurementTimeseries;
    }

    private NcMeasurementTimeseriesType() { }
}
