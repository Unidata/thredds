package ucar.nc2.ogc2.waterml;

import net.opengis.waterml.x20.MeasureType;
import ucar.ma2.Array;
import ucar.ma2.StructureMembers;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.PointFeature;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NC_MeasureType {
    // wml2:Collection/wml2:observationMember/om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP/wml2:value
    public static MeasureType createValue(PointFeature pointFeat, VariableSimpleIF dataVar)
            throws IOException {
        StructureMembers.Member firstDataMember = pointFeat.getData().findMember(dataVar.getShortName());
        assert firstDataMember != null : String.format(
                "%s appeared in the list of data variables but not in the StructureData.", dataVar.getShortName());

        Array dataArray = pointFeat.getData().getArray(firstDataMember);
        assert dataArray.getSize() == 1 : String.format("Expected array to be scalar, but its shape was %s.",
                Arrays.toString(dataArray.getShape()));
        double dataVal = dataArray.getDouble(0);

        // TEXT
        MeasureType value = MeasureType.Factory.newInstance();
        value.setDoubleValue(dataVal);

        return value;
    }

    private NC_MeasureType() { }
}
