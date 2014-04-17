package ucar.nc2.ogc2.gml;

import net.opengis.gml.x32.ReferenceType;
import ucar.nc2.VariableSimpleIF;

/**
 * Created by cwardgar on 3/7/14.
 */
public abstract class NC_ReferenceType {
    // wml2:Collection/wml2:observationMember/om:Observation/om:observedProperty
    public static ReferenceType createObservedProperty(VariableSimpleIF dataVar) {
        ReferenceType observedProperty = ReferenceType.Factory.newInstance();

        // @xlink:title
        observedProperty.setTitle(dataVar.getShortName());

        return observedProperty;
    }

    private NC_ReferenceType() { }
}
