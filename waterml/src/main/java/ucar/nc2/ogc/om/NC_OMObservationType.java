package ucar.nc2.ogc.om;

import net.opengis.gml.v_3_2_1.FeaturePropertyType;
import net.opengis.gml.v_3_2_1.ReferenceType;
import net.opengis.gml.v_3_2_1.TimeInstantPropertyType;
import net.opengis.om.v_2_0_0.OMObservationType;
import net.opengis.om.v_2_0_0.TimeObjectPropertyType;
import net.opengis.waterml.v_2_0_1.MeasurementTimeseriesType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.FeatureDatasetUtil;
import ucar.nc2.ogc.gml.NC_FeaturePropertyType;
import ucar.nc2.ogc.gml.NC_ReferenceType;
import ucar.nc2.ogc.gml.NC_TimeInstantPropertyType;
import ucar.nc2.ogc.waterml.NC_MeasurementTimeseriesType;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_OMObservationType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation
    public static OMObservationType createOmObservation(
            FeatureDatasetPoint fdPoint, StationTimeSeriesFeature stationFeat) throws IOException {
        OMObservationType omObservation = Factories.OM.createOMObservationType();

        // gml:id
        String id = generateId();
        omObservation.setId(id);

        // om:observedProperty
        VariableSimpleIF dataVar = FeatureDatasetUtil.getOnlyDataVariable(fdPoint);
        ReferenceType observedProperty = NC_ReferenceType.createObservedProperty(dataVar);
        omObservation.setObservedProperty(observedProperty);

        // om:featureOfInterest
        FeaturePropertyType featureOfInterest = NC_FeaturePropertyType.createFeatureOfInterest(stationFeat);
        omObservation.setFeatureOfInterest(featureOfInterest);

        // om:result
        MeasurementTimeseriesType measurementTimeseries =
                NC_MeasurementTimeseriesType.createMeasurementTimeseries(fdPoint, stationFeat);
        omObservation.setResult(measurementTimeseries);

        // om:phenomenonTime
        // We must set this after om:result, because the calendar date range may not be available until we iterate
        // through stationFeat.
        TimeObjectPropertyType phenomenonTime = NC_TimeObjectPropertyType.createPhenomenonTime(stationFeat);
        omObservation.setPhenomenonTime(phenomenonTime);

        // om:resultTime
        TimeInstantPropertyType resultTime = NC_TimeInstantPropertyType.createResultTime(stationFeat);
        omObservation.setResultTime(resultTime);

        return omObservation;
    }

    private static int numIds = 0;

    private static String generateId() {
        return OMObservationType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_OMObservationType() { }
}
