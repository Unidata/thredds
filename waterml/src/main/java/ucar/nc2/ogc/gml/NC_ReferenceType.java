package ucar.nc2.ogc.gml;


import net.opengis.gml.v_3_2_1.ReferenceType;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ogc.Factories;

/**
 * Created by cwardgar on 3/7/14.
 */
public abstract class NC_ReferenceType {
    // om:Observation/om:observedProperty
    public static ReferenceType createObservedProperty(VariableSimpleIF dataVar) {
        ReferenceType observedProperty = Factories.GML.createReferenceType();

        // @xlink:title
        observedProperty.setTitle(dataVar.getShortName());

        return observedProperty;
    }

    private NC_ReferenceType() { }
}
