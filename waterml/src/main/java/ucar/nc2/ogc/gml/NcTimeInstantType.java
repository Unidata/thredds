package ucar.nc2.ogc.gml;

import net.opengis.gml.x32.TimeInstantType;
import ucar.nc2.ogc.MarshallingUtil;

/**
 * Created by cwardgar on 2014-04-27.
 */
public class NcTimeInstantType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:resultTime/gml:TimeInstant
    public static TimeInstantType initTimeInstant(TimeInstantType timeInstant) {
        // @gml:id
        String id = MarshallingUtil.createIdForType(TimeInstantType.class);
        timeInstant.setId(id);

        // gml:timePosition
        NcTimePositionType.initTimePosition(timeInstant.addNewTimePosition());

        return timeInstant;
    }

    private NcTimeInstantType() { }
}
