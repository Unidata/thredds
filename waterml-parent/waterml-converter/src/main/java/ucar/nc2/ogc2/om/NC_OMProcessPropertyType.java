package ucar.nc2.ogc2.om;

import net.opengis.om.x20.OMProcessPropertyType;

/**
 * Created by cwardgar on 2014-04-27.
 */
public class NC_OMProcessPropertyType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:procedure
    public static OMProcessPropertyType initProcedure(OMProcessPropertyType procedure) {
        // @xlink:title
        procedure.setTitle("Algorithm");

        // @xlink:href
        procedure.setHref("http://www.opengis.net/def/waterml/2.0/processType/Algorithm");

        return procedure;
    }
}
