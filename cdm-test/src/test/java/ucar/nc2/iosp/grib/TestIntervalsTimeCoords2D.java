/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
/**
 * User: rkambic
 * Date: Jul 7, 2010
 * Time: 9:41:44 AM
 */

package ucar.nc2.iosp.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
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

/*
 * TestIntervalsTimeCoords tests the different type of Grib intervals.
 *
 * For grib parameters that have intervals, the interval start and end
 * points are in the time bounds variables.
 *
 * - Grib1 parameters with constant size intervals use the constant interval length.
 * - Grib1 parameters with mixed intervals for instance of 6, 12 use interval
 *   lengths 6 and 12
 * - Grib2 parameters with constant size intervals use the constant interval length.
 * - Grib2 parameters with multiple intervals ending on the same forecast hour, the
 *   smallest interval is picked to represent the data because the other intervals
 *   can be calculated from the given interval.  For example forecast hour 15, there
 *   are intervals 12-15, 9-15, and 3-15 which results in three intervals of 3, 6, 12.
 *   The 3 hour interval is used because the 6 and 12 hour intervals can be calculated
 *   from the 3 hour interval.
 * - Grib2 parameters with multiple intervals but there is at least one interval for
 *   each forecast time that starts with 0. Intervals used are 0-1, 0-2, 0-3, ...
 *
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestIntervalsTimeCoords2D {

  @Parameterized.Parameters(name="{0}")
  public static Collection<Object[]> getTestParameters() throws IOException {
    Collection<Object[]> params = new ArrayList<>();
    params.add(new Object[]{TestDir.cdmUnitTestDir + "tds/ncep/GFS_Puerto_Rico_191km_20100515_0000.grib1", "VAR_7-0-2-61_L1_I12_Hour_S4", bounds0, 2}); // Total_precipitation_surface_12_Hour_Accumulation
    params.add(new Object[]{TestDir.cdmUnitTestDir + "tds/ncep/GFS_CONUS_80km_20100513_0600.grib1", "VAR_7-0-2-61_L1_Imixed_S4", bounds1, 2});          // Total_precipitation_surface_Mixed_Intervals_Accumulation
    params.add(new Object[]{TestDir.cdmUnitTestDir + "tds/ncep/NAM_CONUS_80km_20100508_1200.grib1", "VAR_7-0-2-61_L1_Imixed_S4", bounds2, 2});
    params.add(new Object[]{TestDir.cdmUnitTestDir + "tds/ncep/SREF_Alaska_45km_ensprod_20120213_1500.grib2", "VAR_0-1-15_L1_I12_Hour_S1_Prob_above_406p4", bounds7,2}); // Large-scale_snow_surface_12_Hour_Accumulation_probability_above_406p4
    params.add(new Object[]{TestDir.cdmUnitTestDir + "tds/ncep/RUC2_CONUS_20km_pressure_20100509_1300.grib2", "VAR_0-1-10_L1_Imixed_S1", bounds4, 2});
    params.add(new Object[]{TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_2p5deg_20100602_1200.grib2", "VAR_0-1-8_L1_I12_Hour_S1", bounds5, 2});  // Total_precipitation_surface_12_Hour_Accumulation
    // TestDir.testdataDir +"grid/grib/grib2/CFSR/pgbhnl.gdas.U_GRD.10mbar.grb2", "U-component_of_wind", "bounds6"

    return params;
  }

  String filename;
  String parameter;
  Object bounds;
  int ndim;

  public TestIntervalsTimeCoords2D(String filename, String parameter, Object bounds, int ndim) {
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
      int[][] tb = (int[][]) bounds;

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
        IndexIterator iter = data.getIndexIterator();
        int idx = 0;
        while (iter.hasNext()) {
          int start = iter.getIntNext();
          int end = iter.getIntNext();
          if (start != tb[idx][0] || end != tb[idx][1]) {
            System.out.printf("bounds %s for file %s, parameter %s failed%n", interval.getFullName(), filename, var.getFullName());
            System.out.printf("interval %d - %d  known %d - %d%n", start, end, tb[idx][0], tb[idx][1]);
          }
          assert (start == tb[idx][0]);
          assert (end == tb[idx][1]);
          idx++;
        }

      }

  }


  static int[][] bounds0 = {
          {0, 12}, {12, 24}, {24, 36}, {36, 48}, {48, 60}, {60, 72}, {72, 84}, {84, 96},
          {96, 108}, {108, 120}, {120, 132}, {132, 144}, {144, 156}, {156, 168}, {168, 180},
          {180, 192}, {192, 204}, {204, 216}, {216, 228}, {228, 240}
  };

  static int[][] bounds1 = {
          {0, 6},
          {6, 12},
          {12, 18},
          {18, 24},
          {24, 30},
          {30, 36},
          {36, 42},
          {42, 48},
          {48, 54},
          {54, 60},
          {60, 66},
          {66, 72},
          {72, 78},
          {78, 84},
          {84, 90},
          {90, 96},
          {96, 102},
          {102, 108},
          {108, 114},
          {114, 120},
          {120, 126},
          {126, 132},
          {132, 138},
          {138, 144},
          {144, 150},
          {150, 156},
          {156, 162},
          {162, 168},
          {168, 174},
          {174, 180},
          {180, 192},
          {192, 204},
          {204, 216},
          {216, 228},
          {228, 240}
  };

  static int[][] bounds2 = {
          {0, 6},
          {0, 12},
          {12, 18},
          {12, 24},
          {24, 30},
          {24, 36},
          {36, 42},
          {36, 48},
          {48, 54},
          {48, 60}
  };

  static int[][] bounds3 = {
          {0, 3},
          {0, 6},
          {0, 12},
          {0, 24},
          {3, 6},
          {3, 9},
          {3, 15},
          {3, 27},
          {6, 9},
          {6, 12},
          {6, 18},
          {6, 30},
          {9, 12},
          {9, 15},
          {9, 21},
          {9, 33},
          {12, 15},
          {12, 18},
          {12, 24},
          {12, 36},
          {15, 18},
          {15, 21},
          {15, 27},
          {15, 39},
          {18, 21},
          {18, 24},
          {18, 30},
          {18, 42},
          {21, 24},
          {21, 27},
          {21, 33},
          {21, 45},
          {24, 27},
          {24, 30},
          {24, 36},
          {24, 48},
          {27, 30},
          {27, 33},
          {27, 39},
          {27, 51},
          {30, 33},
          {30, 36},
          {30, 42},
          {30, 54},
          {33, 36},
          {33, 39},
          {33, 45},
          {33, 57},
          {36, 39},
          {36, 42},
          {36, 48},
          {36, 60},
          {39, 42},
          {39, 45},
          {39, 51},
          {39, 63},
          {42, 45},
          {42, 48},
          {42, 54},
          {42, 66},
          {45, 48},
          {45, 51},
          {45, 57},
          {45, 69},
          {48, 51},
          {48, 54},
          {48, 60},
          {48, 72},
          {51, 54},
          {51, 57},
          {51, 63},
          {51, 75},
          {54, 57},
          {54, 60},
          {54, 66},
          {54, 78},
          {57, 60},
          {57, 63},
          {57, 69},
          {57, 81},
          {60, 63},
          {60, 66},
          {60, 72},
          {60, 84},
          {63, 66},
          {63, 69},
          {63, 75},
          {66, 69},
          {66, 72},
          {66, 78},
          {69, 72},
          {69, 75},
          {69, 81},
          {72, 75},
          {72, 78},
          {72, 84},
          {75, 78},
          {75, 81},
          {78, 81},
          {78, 84},
          {81, 84}
  };

  static int[][] bounds4 = {
          {0, 1},
          {0, 2},
          {1, 2},
          {0, 3},
          {2, 3},
          {0, 4},
          {3, 4},
          {0, 5},
          {3, 5},
          {4, 5},
          {0, 6},
          {3, 6},
          {5, 6},
          {0, 7},
          {6, 7},
          {0, 8},
          {6, 8},
          {7, 8},
          {0, 9},
          {6, 9},
          {8, 9},
          {0, 10},
          {9, 10},
          {0, 11},
          {9, 11},
          {10, 11},
          {0, 12},
          {9, 12},
          {11, 12},
          {0, 13},
          {12, 13},
          {0, 14},
          {12, 14},
          {13, 14},
          {0, 15},
          {12, 15},
          {14, 15},
          {0, 16},
          {15, 16},
          {0, 17},
          {15, 17},
          {16, 17},
          {0, 18},
          {15, 18},
          {17, 18}
  };

  static int[][] bounds5 = {
          {180, 192},
          {192, 204},
          {204, 216},
          {216, 228},
          {228, 240},
          {240, 252},
          {252, 264},
          {264, 276},
          {276, 288},
          {288, 300},
          {300, 312},
          {312, 324},
          {324, 336},
          {336, 348},
          {348, 360},
          {360, 372},
          {372, 384}
  };

  static int[][] bounds6 = {
          {0, 744}, {0, 1416}, {0, 2160}, {0, 2880}, {0, 3624}, {0, 4344}, {0, 5088}, {0, 5832},
          {0, 6552}, {0, 7296}, {0, 8016}, {0, 8760}, {0, 9504}, {0, 10200}, {0, 10944}, {0, 11664},
          {0, 12408}, {0, 13128}, {0, 13872}, {0, 14616}, {0, 15336}, {0, 16080}, {0, 16800},
          {0, 17544}, {0, 18288}, {0, 18960}, {0, 19704}, {0, 20424}, {0, 21168}, {0, 21888},
          {0, 22632}, {0, 23376}, {0, 24096}, {0, 24840}, {0, 25560}, {0, 26304}, {0, 27048},
          {0, 27720}, {0, 28464}, {0, 29184}, {0, 29928}, {0, 30648}, {0, 31392}, {0, 32136},
          {0, 32856}, {0, 33600}, {0, 34320}, {0, 35064}, {0, 35808}, {0, 36480}, {0, 37224},
          {0, 37944}, {0, 38688}, {0, 39408}, {0, 40152}, {0, 40896}, {0, 41616}, {0, 42360},
          {0, 43080}, {0, 43824}, {0, 44568}, {0, 45264}, {0, 46008}, {0, 46728}, {0, 47472},
          {0, 48192}, {0, 48936}, {0, 49680}, {0, 50400}, {0, 51144}, {0, 51864}, {0, 52608},
          {0, 53352}, {0, 54024}, {0, 54768}, {0, 55488}, {0, 56232}, {0, 56952}, {0, 57696},
          {0, 58440}, {0, 59160}, {0, 59904}, {0, 60624}, {0, 61368}, {0, 62112}, {0, 62784},
          {0, 63528}, {0, 64248}, {0, 64992}, {0, 65712}, {0, 66456}, {0, 67200}, {0, 67920},
          {0, 68664}, {0, 69384}, {0, 70128}, {0, 70872}, {0, 71544}, {0, 72288}, {0, 73008},
          {0, 73752}, {0, 74472}, {0, 75216}, {0, 75960}, {0, 76680}, {0, 77424}, {0, 78144},
          {0, 78888}, {0, 79632}, {0, 80328}, {0, 81072}, {0, 81792}, {0, 82536}, {0, 83256},
          {0, 84000}, {0, 84744}, {0, 85464}, {0, 86208}, {0, 86928}, {0, 87672}, {0, 88416},
          {0, 89088}, {0, 89832}, {0, 90552}, {0, 91296}, {0, 92016}, {0, 92760}, {0, 93504},
          {0, 94224}, {0, 94968}, {0, 95688}, {0, 96432}, {0, 97176}, {0, 97848}, {0, 98592},
          {0, 99312}, {0, 100056}, {0, 100776}, {0, 101520}, {0, 102264}, {0, 102984}, {0, 103728},
          {0, 104448}, {0, 105192}, {0, 105936}, {0, 106608}, {0, 107352}, {0, 108072}, {0, 108816},
          {0, 109536}, {0, 110280}, {0, 111024}, {0, 111744}, {0, 112488}, {0, 113208}, {0, 113952},
          {0, 114696}, {0, 115392}, {0, 116136}, {0, 116856}, {0, 117600}, {0, 118320}, {0, 119064},
          {0, 119808}, {0, 120528}, {0, 121272}, {0, 121992}, {0, 122736}, {0, 123480}, {0, 124152},
          {0, 124896}, {0, 125616}, {0, 126360}, {0, 127080}, {0, 127824}, {0, 128568}, {0, 129288},
          {0, 130032}, {0, 130752}, {0, 131496}, {0, 132240}, {0, 132912}, {0, 133656}, {0, 134376},
          {0, 135120}, {0, 135840}, {0, 136584}, {0, 137328}, {0, 138048}, {0, 138792}, {0, 139512},
          {0, 140256}, {0, 141000}, {0, 141672}, {0, 142416}, {0, 143136}, {0, 143880}, {0, 144600},
          {0, 145344}, {0, 146088}, {0, 146808}, {0, 147552}, {0, 148272}, {0, 149016}, {0, 149760},
          {0, 150456}, {0, 151200}, {0, 151920}, {0, 152664}, {0, 153384}, {0, 154128}, {0, 154872},
          {0, 155592}, {0, 156336}, {0, 157056}, {0, 157800}, {0, 158544}, {0, 159216}, {0, 159960},
          {0, 160680}, {0, 161424}, {0, 162144}, {0, 162888}, {0, 163632}, {0, 164352}, {0, 165096},
          {0, 165816}, {0, 166560}, {0, 167304}, {0, 167976}, {0, 168720}, {0, 169440}, {0, 170184},
          {0, 170904}, {0, 171648}, {0, 172392}, {0, 173112}, {0, 173856}, {0, 174576}, {0, 175320},
          {0, 176064}, {0, 176736}, {0, 177480}, {0, 178200}, {0, 178944}, {0, 179664}, {0, 180408},
          {0, 181152}, {0, 181872}, {0, 182616}, {0, 183336}, {0, 184080}, {0, 184824}, {0, 185520},
          {0, 186264}, {0, 186984}, {0, 187728}, {0, 188448}, {0, 189192}, {0, 189936}, {0, 190656},
          {0, 191400}, {0, 192120}, {0, 192864}, {0, 193608}, {0, 194280}, {0, 195024}, {0, 195744},
          {0, 196488}, {0, 197208}, {0, 197952}, {0, 198696}, {0, 199416}, {0, 200160}, {0, 200880},
          {0, 201624}, {0, 202368}, {0, 203040}, {0, 203784}, {0, 204504}, {0, 205248}, {0, 205968},
          {0, 206712}, {0, 207456}, {0, 208176}, {0, 208920}, {0, 209640}, {0, 210384}, {0, 211128},
          {0, 211800}, {0, 212544}, {0, 213264}, {0, 214008}, {0, 214728}, {0, 215472}, {0, 216216},
          {0, 216936}, {0, 217680}, {0, 218400}, {0, 219144}, {0, 219888}, {0, 220584}, {0, 221328},
          {0, 222048}, {0, 222792}, {0, 223512}, {0, 224256}, {0, 225000}, {0, 225720}, {0, 226464},
          {0, 227184}, {0, 227928}, {0, 228672}, {0, 229344}, {0, 230088}, {0, 230808}, {0, 231552},
          {0, 232272}, {0, 233016}, {0, 233760}, {0, 234480}, {0, 235224}, {0, 235944}, {0, 236688},
          {0, 237432}, {0, 238104}, {0, 238848}, {0, 239568}, {0, 240312}, {0, 241032}, {0, 241776},
          {0, 242520}, {0, 243240}, {0, 243984}, {0, 244704}, {0, 245448}, {0, 246192}, {0, 246864},
          {0, 247608}, {0, 248328}, {0, 249072}, {0, 249792}, {0, 250536}, {0, 251280}, {0, 252000},
          {0, 252744}, {0, 253464}, {0, 254208}, {0, 254952}, {0, 255648}, {0, 256392}, {0, 257112},
          {0, 257856}, {0, 258576}, {0, 259320}, {0, 260064}, {0, 260784}, {0, 261528}, {0, 262248},
          {0, 262992}, {0, 263736}, {0, 264408}, {0, 265152}, {0, 265872}, {0, 266616}, {0, 267336},
          {0, 268080}, {0, 268824}, {0, 269544}
  };

  static int[][] bounds7 = {
          {0, 12},
          {3, 15},
          {6, 18},
          {9, 21},
          {12, 24},
          {15, 27},
          {18, 30},
          {21, 33},
          {24, 36},
          {27, 39},
          {30, 42},
          {33, 45},
          {36, 48},
          {39, 51},
          {42, 54},
          {45, 57},
          {48, 60},
          {51, 63},
          {54, 66},
          {57, 69},
          {60, 72},
          {63, 75},
          {66, 78},
          {69, 81},
          {72, 84},
          {75, 87}
  };

}
