package ucar.nc2.ogc.om;

import net.opengis.om.x20.OMProcessPropertyType;

/**
 * Created by cwardgar on 2014-04-27.
 */
public class NC_OMProcessPropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:procedure
    public static OMProcessPropertyType initProcedure(OMProcessPropertyType procedure) {
        // @xlink:title
        procedure.setTitle("Unknown");

        // @xlink:href
        procedure.setHref("http://www.opengis.net/def/nil/OGC/0/unknown");

        return procedure;
    }
}
