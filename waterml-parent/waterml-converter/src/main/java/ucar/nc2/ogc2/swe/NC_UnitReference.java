package ucar.nc2.ogc2.swe;

import gov.noaa.pfel.erddap.util.EDUnits;
import net.opengis.swe.x20.UnitReference;
import ucar.nc2.VariableSimpleIF;

/**
 * Created by cwardgar on 2014/03/06.
 */
public class NC_UnitReference {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseriesType/wml2:defaultPointMetadata/wml2:DefaultTVPMetadata/wml2:uom
    public static UnitReference initUom(UnitReference uom, VariableSimpleIF dataVar) {
        // @code
        String udunits = dataVar.getUnitsString();
        String ucum = EDUnits.udunitsToUcum(udunits);
        uom.setCode(ucum);

        return uom;
    }

    private NC_UnitReference() { }
}
