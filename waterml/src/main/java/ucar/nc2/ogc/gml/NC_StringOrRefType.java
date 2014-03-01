package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.StringOrRefType;
import ucar.nc2.ft.StationTimeSeriesFeature;

/**
 * In OGC 12-031r2, used at:
 *     om:OM_Observation/om:featureOfInterest/sam:SF_SamplingFeatureType/gml:description
 *
 * Created by cwardgar on 2014/02/26.
 */
public abstract class NC_StringOrRefType {
    private final static net.opengis.gml.v_3_2_1.ObjectFactory gmlObjectFactory =
            new net.opengis.gml.v_3_2_1.ObjectFactory();

    public static StringOrRefType createSamplingFeatureTypeDescription(StationTimeSeriesFeature stationFeat) {
        // OGC 12-031r2 calls for station_name.long_name, but getDescription() is actually the data we'd want to
        // set here.
        StringOrRefType description = gmlObjectFactory.createStringOrRefType();
        description.setValue(stationFeat.getDescription());
        return description;
    }

    private NC_StringOrRefType() { }
}
