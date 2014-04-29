package ucar.nc2.ogc2.om;

import net.opengis.om.x20.OMObservationType;
import net.opengis.waterml.x20.MeasurementTimeseriesDocument;
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
    public static OMObservationType initOmObservation(OMObservationType omObservation,
            StationTimeSeriesFeature stationFeat, VariableSimpleIF dataVar) throws IOException {
        // gml:id
        omObservation.setId(generateId());

        // om:observedProperty
        NC_ReferenceType.initObservedProperty(omObservation.addNewObservedProperty(), dataVar);

        // om:procedure
        NC_OMProcessPropertyType.initProcedure(omObservation.addNewProcedure());

        // om:featureOfInterest
        NC_FeaturePropertyType.initFeatureOfInterest(omObservation.addNewFeatureOfInterest(), stationFeat);

        // om:result
        MeasurementTimeseriesDocument measurementTimeseriesDoc = MeasurementTimeseriesDocument.Factory.newInstance();
        NC_MeasurementTimeseriesType.initMeasurementTimeseries(
                measurementTimeseriesDoc.addNewMeasurementTimeseries(), stationFeat, dataVar);
        omObservation.setResult(measurementTimeseriesDoc);

        // om:phenomenonTime
        // We must set this after om:result, because the calendar date range may not be available until we iterate
        // through stationFeat.
        NC_TimeObjectPropertyType.initPhenomenonTime(omObservation.addNewPhenomenonTime(), stationFeat);

        // om:resultTime
        NC_TimeInstantPropertyType.initResultTime(omObservation.addNewResultTime(), stationFeat);

        return omObservation;
    }

    private static int numIds = 0;

    private static String generateId() {
        return OMObservationType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_OMObservationType() { }
}
