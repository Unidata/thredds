package ucar.nc2.ogc.om;

import net.opengis.om.v_2_0_0.OMObservationPropertyType;
import net.opengis.om.v_2_0_0.OMObservationType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

import java.io.IOException;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NC_OMObservationPropertyType {
    // wml2:Collection/wml2:observationMember
    public static OMObservationPropertyType createObservationMember(
            FeatureDatasetPoint fdPoint, StationTimeSeriesFeature stationFeat, VariableSimpleIF dataVar)
            throws IOException {
        OMObservationPropertyType observationMember = Factories.OM.createOMObservationPropertyType();

        // om:OM_Observation
        OMObservationType omObservation = NC_OMObservationType.createOmObservation(fdPoint, stationFeat, dataVar);
        observationMember.setOMObservation(omObservation);

        return observationMember;
    }

    private NC_OMObservationPropertyType() { }
}
