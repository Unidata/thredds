package ucar.nc2.iosp.nids;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;

import java.io.*;


public class TestNids extends TestCase {

  public static String basereflectFile = "test/data/nids/N0R_20041119_2147";
  public static String basereflect1File = "test/data/nids/N1R_20050119_1548";
  public static String basereflect2File = "test/data/nids/N2R_20050119_1528";
  public static String basereflect3File = "test/data/nids/N3R_20050119_1548";
  public static String basereflectCFile = "test/data/nids/NCR_20050119_1548";
  public static String basereflect248File = "test/data/nids/N0Z_20050119_1538";
  public static String radialVelocityFile = "test/data/nids/N0V_20041117_1646";
  public static String radialVelocity1File = "test/data/nids/N1V_20050119_1548";
  public static String echotopFile = "test/data/nids/NET_20041123_1648";
  public static String oneHourPrecipFile = "test/data/nids/N1P_20041122_1837";
  public static String StormRelMeanVel0File = "test/data/nids/N0S_20050119_1548";
  public static String StormRelMeanVel1File = "test/data/nids/N1S_20041117_1640";
  public static String StormRelMeanVel2File = "test/data/nids/N2S_20050120_1806";
  public static String StormRelMeanVel3File = "test/data/nids/N3S_20050120_1806";
  public static String totalPrecipFile = "test/data/nids/NTP_20050119_1528";
  public static String digitPrecipArrayFile = "test/data/nids/DPA_20041123_1709";
  public static String vertIntegLiquidFile = "test/data/nids/NVL_20041130_1946";
  public static String vadWindProfileFile = "test/data/nids/NVW_20041117_1657";

  public static boolean dumpFile = false;

  public void testNidsReadRadial() throws IOException {
    NetcdfFile ncfile = null;
    try {
      System.out.println("**** Open "+ basereflectFile);
      ncfile = NetcdfFile.open(basereflectFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }

    Variable v = null;
    v = ncfile.findVariable("BaseReflectivity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ basereflect1File);
      ncfile = NetcdfFile.open(basereflect1File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }
    v = ncfile.findVariable("BaseReflectivity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ basereflect2File);
      ncfile = NetcdfFile.open(basereflect2File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }
    v = ncfile.findVariable("BaseReflectivity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ basereflect3File);
      ncfile = NetcdfFile.open(basereflect3File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }
    v = ncfile.findVariable("BaseReflectivity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ basereflect248File);
      ncfile = NetcdfFile.open(basereflect248File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }

    v = ncfile.findVariable("BaseReflectivity248");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ StormRelMeanVel0File);
      ncfile = NetcdfFile.open(StormRelMeanVel0File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }

    v = ncfile.findVariable("StormMeanVelocity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ StormRelMeanVel1File);
      ncfile = NetcdfFile.open(StormRelMeanVel1File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }
    v = ncfile.findVariable("StormMeanVelocity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ StormRelMeanVel2File);
      ncfile = NetcdfFile.open(StormRelMeanVel2File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }
    v = ncfile.findVariable("StormMeanVelocity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ StormRelMeanVel3File);
      ncfile = NetcdfFile.open(StormRelMeanVel3File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }
    v = ncfile.findVariable("StormMeanVelocity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ radialVelocityFile);
      ncfile = NetcdfFile.open(radialVelocityFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }

    v = ncfile.findVariable("Velocity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ radialVelocity1File);
      ncfile = NetcdfFile.open(radialVelocity1File);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }
    v = ncfile.findVariable("Velocity");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));
    ncfile.close();
  }

  public void testNidsReadRadialN1P() throws IOException {
    NetcdfFile ncfile = null;
    try {
      System.out.println("**** Open "+ oneHourPrecipFile);
      ncfile = NetcdfFile.open(oneHourPrecipFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }

    Variable v = ncfile.findVariable("Precip1hr_RAW");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+ totalPrecipFile);
      ncfile = NetcdfFile.open(totalPrecipFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }

    v = ncfile.findVariable("PrecipAccum_RAW");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));
    ncfile.close();
  }

  public void testNidsReadRaster() throws IOException {
    NetcdfFile ncfile = null;
    try {
      System.out.println("**** Open "+echotopFile);
      ncfile = NetcdfFile.open(echotopFile);
    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }
    Variable v = ncfile.findVariable("EchoTop");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    ncfile.close();
    try {
      System.out.println("**** Open "+vertIntegLiquidFile);
      ncfile = NetcdfFile.open(vertIntegLiquidFile);
    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }

    v = ncfile.findVariable("VertLiquid");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));

    v = ncfile.findVariable("VertLiquid_RAW");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));
    ncfile.close();
    try {
      System.out.println("**** Open "+ basereflectCFile);
      ncfile = NetcdfFile.open(basereflectCFile);

    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }

    v = ncfile.findVariable("BaseReflectivityComp");
    testReadData(v);
    assert(null != v.getDimension(0));
    assert(null != v.getDimension(1));
    ncfile.close();
  }


  public void testNidsReadNVW() throws IOException {
      NetcdfFile ncfile = null;
      Variable v = null;
      Array a = null;
      try {
          System.out.println("**** Open "+vadWindProfileFile);
          ncfile = NetcdfFile.open(vadWindProfileFile);
      } catch (java.io.IOException e) {
          System.out.println(" fail = "+e);
          e.printStackTrace();
          assert(false);
      }

      assert(null != ncfile.findVariable("textStruct_code8"));
      assert(null != ncfile.findVariable("textStruct_code8").getDimension(0));

      v = ncfile.findVariable("unlinkedVectorStruct");
      testReadDataAsShort((Structure) v, "iValue");

      v = ncfile.findVariable("VADWindSpeed");
      testReadData(v);

      v = ncfile.findVariable("TabMessagePage");
      testReadData(v);
      ncfile.close();
  }

  public void testNidsReadDPA() throws IOException {
    NetcdfFile ncfile = null;
    Variable v = null;
    Array a = null;
    try {
      System.out.println("**** Open "+digitPrecipArrayFile);
      ncfile = NetcdfFile.open(digitPrecipArrayFile);
    } catch (java.io.IOException e) {
      System.out.println(" fail = "+e);
      e.printStackTrace();
      assert(false);
    }

    v = ncfile.findVariable("PrecipArray_0");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_1");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_2");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_3");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_4");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_5");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_6");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_7");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_8");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_9");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_10");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_11");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_12");
    testReadData(v);

    v = ncfile.findVariable("PrecipArray_13");
    testReadData(v);



    assert(null != ncfile.findVariable("textStruct_code1").getDimension(0));

    ncfile.close();
  }

  private void testReadData(Variable v) {
    Array a = null;
    assert(null != v);
    assert(null != v.getDimension(0));
    try {
        a = v.read();
        assert(null != a);
    } catch (java.io.IOException e) {
        e.printStackTrace();
        assert(false);
    }
    assert( v.getSize() == a.getSize() );
  }

    private void testReadDataAsShort(Structure v, String memberName) {
      Array a = null;
      assert(null != v);
      assert(null != v.getDimension(0));
      try {
          a = v.read();
          assert(null != a);
      } catch (java.io.IOException e) {
          e.printStackTrace();
          assert(false);
      }
      assert( v.getSize() == a.getSize() );
      assert (a instanceof ArrayStructure);

        int sum = 0;
      ArrayStructure as = (ArrayStructure) a;
      int n = (int) as.getSize();
        for (int i = 0; i < n; i++) {
          StructureData sdata = as.getStructureData(i);
          sum += sdata.getScalarShort(memberName);
        }

      System.out.println("test short sum = "+sum);
    }


}
