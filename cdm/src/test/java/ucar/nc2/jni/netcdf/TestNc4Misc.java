package ucar.nc2.jni.netcdf;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.TestLocal;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test miscellaneous netcdf4 writing
 *
 * @author caron
 * @since 7/30/13
 */
public class TestNc4Misc {

  @Test
  public void testUnlimitedDimension() throws IOException, InvalidRangeException {

    String location = TestLocal.temporaryDataDir + "testNc4UnlimitedDim.nc";
    File f = new File(location);
    System.out.printf("%s%n", f.exists());
    boolean ok = f.delete();
    System.out.printf("%s%n", ok);

    NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, location);
    System.out.printf("write to file = %s%n", new File(location).getAbsolutePath());

    Dimension timeDim = writer.addUnlimitedDimension("time");
    List<Dimension> dims = new ArrayList<Dimension>();
    dims.add(timeDim);
    Variable time = writer.addVariable(null, "time", DataType.DOUBLE, dims);

    writer.create();

    Array data = Array.factory(new double[]{0, 1, 2, 3});
    writer.write(time, data);

    writer.close();

    NetcdfFileWriter file = NetcdfFileWriter.openExisting(location);

    time = file.findVariable("time");
    int[] origin = new int[1];
    origin[0] = (int) time.getSize();
    file.write(time, origin, data);

    file.close();

    }

}