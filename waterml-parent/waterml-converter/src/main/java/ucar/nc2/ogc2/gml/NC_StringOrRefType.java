package ucar.nc2.ogc2.gml;

import net.opengis.gml.x32.StringOrRefType;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_StringOrRefType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType/gml:description
    public static StringOrRefType createDescription(StationTimeSeriesFeature stationFeat) {
        // OGC 12-031r2 calls for station_name.long_name,
        // but getDescription() is actually the data we'd want to set here.
        StringOrRefType description = StringOrRefType.Factory.newInstance();
        description.setStringValue(stationFeat.getDescription());
        return description;
    }

    private NC_StringOrRefType() { }
}
