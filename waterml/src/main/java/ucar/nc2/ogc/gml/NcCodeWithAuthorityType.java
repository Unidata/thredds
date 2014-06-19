package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.CodeWithAuthorityType;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NcCodeWithAuthorityType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/wml2:MonitoringPoint/gml:identifier
    public static CodeWithAuthorityType initIdentifier(
            CodeWithAuthorityType identifier, StationTimeSeriesFeature stationFeat) {
        // @codespace
        identifier.setCodeSpace("http://unidata.ucar.edu/");

        // TEXT
        identifier.setStringValue(stationFeat.getName());

        return identifier;
    }

    private NcCodeWithAuthorityType() { }
}
