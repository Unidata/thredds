package ucar.nc2.ogc.om;

import net.opengis.gml.v_3_2_1.FeaturePropertyType;
import net.opengis.om.v_2_0_0.OMObservationType;
import net.opengis.waterml.v_2_0_1.MeasurementTimeseriesType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.PointUtil;
import ucar.nc2.ogc.gml.NC_FeaturePropertyType;
import ucar.nc2.ogc.waterml.NC_MeasurementTimeseriesType;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_OMObservationType {
    // om:OM_Observation
    public static OMObservationType createObservationType(FeatureDatasetPoint fdPoint) throws IOException {
        OMObservationType obsType = Factories.OM.createOMObservationType();

        // gml:id
        String id = OMObservationType.class.getSimpleName() + "." + "1";
        obsType.setId(id);

        StationTimeSeriesFeature stationFeat = PointUtil.getSingleStationFeatureFromDataset(fdPoint);

        // om:featureOfInterest
        FeaturePropertyType featureOfInterest = NC_FeaturePropertyType.createFeatureOfInterest(stationFeat);
        obsType.setFeatureOfInterest(featureOfInterest);

        // om:result
        MeasurementTimeseriesType measurementTimeseries =
                NC_MeasurementTimeseriesType.createMeasurementTimeseries(fdPoint, stationFeat);
        obsType.setResult(measurementTimeseries);



        return obsType;
    }

    private NC_OMObservationType() { }
}
