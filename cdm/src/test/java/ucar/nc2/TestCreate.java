package ucar.nc2;

import java.io.IOException;

import junit.framework.TestCase;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

public class TestCreate extends TestCase {
    public void testCreateData() {
        NetcdfFileWriteable file = null;
        file = new NetcdfFileWriteable("TestCreate.nc", true);

        Dimension time = file.addDimension("time", 4, true, false, false);
        Dimension dims[] = {time};

        /* Add time */
        file.addVariable("time", DataType.DOUBLE, dims);
        file.addVariableAttribute("time", "quantity", "time");
        file.addVariableAttribute("time", "units", "s");

        /* Add a dependent variable */
        file.addVariable("h", DataType.DOUBLE, dims);
        file.addVariableAttribute("h", "quantity", "Height");
        file.addVariableAttribute("h", "units", "m");
        try {
            file.create();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            fail("IOException on creation");
        }

        double td[] = {1.0, 2.0, 3.0, 4.0};
        double hd[] = {0.0, 0.1, 0.3, 0.9};
        ArrayDouble.D1 ta = new ArrayDouble.D1(4);
        ArrayDouble.D1 ha = new ArrayDouble.D1(4);
        for(int i=0;i<4;i++) {
            ta.set(i, td[i]);
            ha.set(i, hd[i]);
        }

        try {
            file.write("time", ta);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            fail("IOException thrown while writing time");
        } catch (InvalidRangeException e) {
            e.printStackTrace(System.err);
            fail("InvalidRangeException thrown while writing time");
        }

        try {
            file.write("h", ha);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            fail("IOException thrown while writing h");
        } catch (InvalidRangeException e) {
            e.printStackTrace(System.err);
            fail("InvalidRangeException thrown while writing h");
        }
    }
}
