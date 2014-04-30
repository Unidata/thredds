package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.TimeInstantType;

/**
 * Created by cwardgar on 2014-04-27.
 */
public class NC_TimeInstantType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:resultTime/gml:TimeInstant
    public static TimeInstantType initTimeInstant(TimeInstantType timeInstant) {
        // gml:id
        String id = generateId();
        timeInstant.setId(id);

        // gml:timePosition
        NC_TimePositionType.initTimePosition(timeInstant.addNewTimePosition());

        return timeInstant;
    }

    private static int numIds = 0;

    private static String generateId() {
        return TimeInstantType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_TimeInstantType() { }
}
