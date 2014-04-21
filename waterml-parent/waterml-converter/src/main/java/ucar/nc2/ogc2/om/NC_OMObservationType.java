package ucar.nc2.ogc2.om;

import net.opengis.gml.x32.FeaturePropertyType;
import net.opengis.gml.x32.ReferenceType;
import net.opengis.gml.x32.TimeInstantPropertyType;
import net.opengis.om.x20.OMObservationType;
import net.opengis.om.x20.TimeObjectPropertyType;
import net.opengis.waterml.x20.MeasurementTimeseriesDocument;
import net.opengis.waterml.x20.MeasurementTimeseriesType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc2.gml.NC_FeaturePropertyType;
import ucar.nc2.ogc2.gml.NC_ReferenceType;
import ucar.nc2.ogc2.gml.NC_TimeInstantPropertyType;
import ucar.nc2.ogc2.waterml.NC_MeasurementTimeseriesType;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_OMObservationType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation
    public static OMObservationType createOmObservation(StationTimeSeriesFeature stationFeat, VariableSimpleIF dataVar)
            throws IOException {
        OMObservationType omObservation = OMObservationType.Factory.newInstance();

        // gml:id
        String id = generateId();
        omObservation.setId(id);

        // om:observedProperty
        ReferenceType observedProperty = NC_ReferenceType.createObservedProperty(dataVar);
        omObservation.setObservedProperty(observedProperty);

        // om:featureOfInterest
        FeaturePropertyType featureOfInterest = NC_FeaturePropertyType.createFeatureOfInterest(stationFeat);
        omObservation.setFeatureOfInterest(featureOfInterest);

        // om:result
        MeasurementTimeseriesType measurementTimeseries =
                NC_MeasurementTimeseriesType.createMeasurementTimeseries(stationFeat, dataVar);
//        omObservation.setResult(measurementTimeseries);
        MeasurementTimeseriesDocument measurementTimeseriesDoc = MeasurementTimeseriesDocument.Factory.newInstance();
        measurementTimeseriesDoc.setMeasurementTimeseries(measurementTimeseries);
        omObservation.setResult(measurementTimeseriesDoc);

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
