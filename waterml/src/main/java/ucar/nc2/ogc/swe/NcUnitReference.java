package ucar.nc2.ogc.swe;

import ucar.nc2.ogc.erddap.util.ErddapEDUnits;
import net.opengis.swe.x20.UnitReference;
import ucar.nc2.VariableSimpleIF;

/**
 * Created by cwardgar on 2014/03/06.
 */
public class NcUnitReference {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/
    //         wml2:defaultPointMetadata/wml2:DefaultTVPMeasurementMetadata/wml2:uom
    public static UnitReference initUom(UnitReference uom, VariableSimpleIF dataVar) {
        // @code
        String udunits = dataVar.getUnitsString();
        if (udunits == null) {  // Variable may not have a "units" attribute.
            return null;
        }

        String ucum = ErddapEDUnits.udunitsToUcum(udunits);
        uom.setCode(ucum);

        return uom;
    }

    private NcUnitReference() { }
}
