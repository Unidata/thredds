package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.TimeInstantPropertyType;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;

/**
 * Created by cwardgar on 3/7/14.
 */
public abstract class NC_TimeInstantPropertyType {
    //om:OM_Observation/om:resultTime
    public static TimeInstantPropertyType createResultTime(StationTimeSeriesFeature stationFeat) {
        TimeInstantPropertyType resultTime = Factories.GML.createTimeInstantPropertyType();

        // StationTimeSeriesFeature has no comparable property.

        return resultTime;
    }

    private NC_TimeInstantPropertyType() { }
}
