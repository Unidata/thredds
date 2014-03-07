package ucar.nc2.ogc.waterml;

import net.opengis.waterml.v_2_0_1.Value;
import ucar.ma2.Array;
import ucar.ma2.StructureMembers;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.FeatureDatasetUtil;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by cwardgar on 2014/03/06.
 */
public abstract class NC_Value {
    // om:OM_Observation/om:result/wml2:MeasurementTimeseries/wml2:point/wml2:MeasurementTVP/wml2:value
    public static Value createValue(FeatureDatasetPoint fdPoint, PointFeature pointFeat) throws IOException {
        VariableSimpleIF onlyDataVar = FeatureDatasetUtil.getOnlyDataVariable(fdPoint);
        StructureMembers.Member firstDataMember = pointFeat.getData().findMember(onlyDataVar.getShortName());
        assert firstDataMember != null : String.format(
                "%s appeared in the list of data variables but not in the StructureData.", onlyDataVar.getShortName());

        Array dataArray = pointFeat.getData().getArray(firstDataMember);
        assert dataArray.getSize() == 1 : String.format("Expected array to be scalar, but its shape was %s.",
                Arrays.toString(dataArray.getShape()));
        double dataVal = dataArray.getDouble(0);

        // TEXT
        Value value = Factories.WATERML.createValue();
        value.setValue(dataVal);

        return value;
    }

    private NC_Value() { }
}
