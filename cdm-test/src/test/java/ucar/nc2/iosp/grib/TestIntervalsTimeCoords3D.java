package ucar.nc2.iosp.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Describe
 *
 * @author caron
 * @since 12/1/2014
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestIntervalsTimeCoords3D {

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> getTestParameters() throws IOException {
      Collection<Object[]> params = new ArrayList<>();
      params.add(new Object[]{TestDir.cdmUnitTestDir + "formats/grib1/QPE.20101005.009.157", "VAR_9-157-2-61_L1_Imixed_S4", boundsQPE, 3});
      params.add(new Object[]{TestDir.cdmUnitTestDir + "formats/grib1/QPE.20101005.009.157", "VAR_9-157-128-237_L1_I1_Hour_S4", boundsQPE2, 3});

      return params;
    }

    String filename;
    String parameter;
    Object bounds;
    int ndim;

    public TestIntervalsTimeCoords3D(String filename, String parameter, Object bounds, int ndim) {
      this.filename = filename;
      this.parameter = parameter;
      this.bounds = bounds;
      this.ndim = ndim;
    }

    /*
       * Compare the timeCoordinates to known values
       */
    @Test
    public void checkTimeIntervalCoordinates() throws Exception {
      double[][][] tb = (double[][][]) bounds;

        System.out.printf("Open %s (%s)%n", filename, parameter);

        try (NetcdfFile ncf = NetcdfFile.open(filename)) {
          Group best = ncf.findGroup("Best"); // use best group if it exists, may be null
          Variable var = ncf.findVariableByAttribute(best, GribIosp.VARIABLE_ID_ATTNAME, parameter);
          assert var != null : parameter;
          System.out.printf(" using variable %s%n", var.getFullName());

          Dimension dim = var.getDimension(0);
          if (dim.getShortName().startsWith("reftime"))
            dim = var.getDimension(1);
          String bounds = dim.getShortName() + "_bounds";
          Variable interval = ncf.findVariable(best, bounds);
          assert interval != null : bounds;

          Array data = interval.read();
          assert data.getRank() == 3;
          assert data.getDataType() == DataType.DOUBLE;
          int[] dataShape = data.getShape();
          assert dataShape[0] == tb.length;
          assert dataShape[1] == tb[0].length;
          assert dataShape[2] == tb[0][0].length;

          Index ima = data.getIndex();

          for (int i=0; i<dataShape[0]; i++)
          for (int j=0; j<dataShape[1]; j++)
          for (int k=0; k<dataShape[2]; k++) {
            double val = data.getDouble(ima.set(i,j,k));
            assert val == tb[i][j][k];
          }
        }

    }

    static double[][][] boundsQPE = {
            {
                    {0.0, 1.0},
                    {0.0, 6.0},
                    {6.0, 12.0},
                    {12.0, 18.0},
                    {18.0, 24.0}
            },
            {
                    {1.0, 2.0},
                    {1.0, 7.0},
                    {7.0, 13.0},
                    {13.0, 19.0},
                    {19.0, 25.0}
            },
            {
                    {2.0, 3.0},
                    {2.0, 8.0},
                    {8.0, 14.0},
                    {14.0, 20.0},
                    {20.0, 26.0}
            },
            {
                    {3.0, 4.0},
                    {3.0, 9.0},
                    {9.0, 15.0},
                    {15.0, 21.0},
                    {21.0, 27.0}
            },
            {
                    {4.0, 5.0},
                    {4.0, 10.0},
                    {10.0, 16.0},
                    {16.0, 22.0},
                    {22.0, 28.0}
            },
            {
                    {5.0, 6.0},
                    {5.0, 11.0},
                    {11.0, 17.0},
                    {17.0, 23.0},
                    {23.0, 29.0}
            },
            {
                    {6.0, 7.0},
                    {6.0, 12.0},
                    {12.0, 18.0},
                    {18.0, 24.0},
                    {24.0, 30.0}
            },
            {
                    {7.0, 8.0},
                    {7.0, 13.0},
                    {13.0, 19.0},
                    {19.0, 25.0},
                    {25.0, 31.0}
            },
            {
                    {8.0, 9.0},
                    {8.0, 14.0},
                    {14.0, 20.0},
                    {20.0, 26.0},
                    {26.0, 32.0}
            },
            {
                    {9.0, 10.0},
                    {9.0, 15.0},
                    {15.0, 21.0},
                    {21.0, 27.0},
                    {27.0, 33.0}
            },
            {
                    {10.0, 11.0},
                    {10.0, 16.0},
                    {16.0, 22.0},
                    {22.0, 28.0},
                    {28.0, 34.0}
            },
            {
                    {11.0, 12.0},
                    {11.0, 17.0},
                    {17.0, 23.0},
                    {23.0, 29.0},
                    {29.0, 35.0}
            },
            {
                    {12.0, 13.0},
                    {12.0, 18.0},
                    {18.0, 24.0},
                    {24.0, 30.0},
                    {30.0, 36.0}
            },
            {
                    {13.0, 14.0},
                    {13.0, 19.0},
                    {19.0, 25.0},
                    {25.0, 31.0},
                    {31.0, 37.0}
            },
            {
                    {14.0, 15.0},
                    {14.0, 20.0},
                    {20.0, 26.0},
                    {26.0, 32.0},
                    {32.0, 38.0}
            },
            {
                    {15.0, 16.0},
                    {15.0, 21.0},
                    {21.0, 27.0},
                    {27.0, 33.0},
                    {33.0, 39.0}
            },
            {
                    {16.0, 17.0},
                    {16.0, 22.0},
                    {22.0, 28.0},
                    {28.0, 34.0},
                    {34.0, 40.0}
            },
            {
                    {17.0, 18.0},
                    {17.0, 23.0},
                    {23.0, 29.0},
                    {29.0, 35.0},
                    {35.0, 41.0}
            },
            {
                    {18.0, 19.0},
                    {18.0, 24.0},
                    {24.0, 30.0},
                    {30.0, 36.0},
                    {36.0, 42.0}
            },
            {
                    {19.0, 20.0},
                    {19.0, 25.0},
                    {25.0, 31.0},
                    {31.0, 37.0},
                    {37.0, 43.0}
            },
            {
                    {20.0, 21.0},
                    {20.0, 26.0},
                    {26.0, 32.0},
                    {32.0, 38.0},
                    {38.0, 44.0}
            },
            {
                    {21.0, 22.0},
                    {21.0, 27.0},
                    {27.0, 33.0},
                    {33.0, 39.0},
                    {39.0, 45.0}
            },
            {
                    {22.0, 23.0},
                    {22.0, 28.0},
                    {28.0, 34.0},
                    {34.0, 40.0},
                    {40.0, 46.0}
            },
            {
                    {23.0, 24.0},
                    {23.0, 29.0},
                    {29.0, 35.0},
                    {35.0, 41.0},
                    {41.0, 47.0}
            }
    };

    static double[][][] boundsQPE2 =   {
        {
          {0.0, 1.0}
        },
        {
          {1.0, 2.0}
        },
        {
          {2.0, 3.0}
        },
        {
          {3.0, 4.0}
        },
        {
          {4.0, 5.0}
        },
        {
          {5.0, 6.0}
        },
        {
          {6.0, 7.0}
        },
        {
          {7.0, 8.0}
        },
        {
          {8.0, 9.0}
        },
        {
          {9.0, 10.0}
        },
        {
          {10.0, 11.0}
        },
        {
          {11.0, 12.0}
        },
        {
          {12.0, 13.0}
        },
        {
          {13.0, 14.0}
        },
        {
          {14.0, 15.0}
        },
        {
          {15.0, 16.0}
        },
        {
          {16.0, 17.0}
        },
        {
          {17.0, 18.0}
        },
        {
          {18.0, 19.0}
        },
        {
          {19.0, 20.0}
        },
        {
          {20.0, 21.0}
        },
        {
          {21.0, 22.0}
        },
        {
          {22.0, 23.0}
        },
        {
          {23.0, 24.0}
        }
      };


}
