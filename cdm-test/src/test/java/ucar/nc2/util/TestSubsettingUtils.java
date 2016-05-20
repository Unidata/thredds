package ucar.nc2.util;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Random;

/**
 * Utilities to read and subset data
 *
 * @author caron
 * @since 3/25/12
 */
public class TestSubsettingUtils {

  public static void subsetVariables(String filename, String varName, int ntrials) throws InvalidRangeException, IOException {
    //varName = NetcdfFile.makeValidCdmObjectName(varName);
    System.out.println("testVariableSubset="+filename+","+varName);

    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {

      Variable v = ncfile.findVariable(varName);
      if (v == null) {
        System.out.printf("Cant Find %s%n", varName);
        for (Variable v2 : ncfile.getVariables())
          System.out.printf("  %s%n", v2.getFullName());
      }
      assert (null != v);
      int[] shape = v.getShape();

      // read entire array
      Array A;
      try {
        A = v.read();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }

      int[] dataShape = A.getShape();
      assert dataShape.length == shape.length;
      for (int i = 0; i < shape.length; i++)
        assert dataShape[i] == shape[i];
      Section all = v.getShapeAsSection();
      System.out.println("  Entire dataset=" + all);

      for (int k = 0; k < ntrials; k++) {
        // create a random subset, read and compare
        subsetVariable(v, randomSubset(all, 1), A);
        subsetVariable(v, randomSubset(all, 2), A);
        subsetVariable(v, randomSubset(all, 3), A);
      }

    }
  }

   public static void subsetVariables(String filename, String varName, Section s) throws InvalidRangeException, IOException {
    System.out.println("testVariableSubset="+filename+","+varName);

    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      Variable v = ncfile.findVariable(varName);
      assert (null != v);
      subsetVariable(v, s, v.read());
    }
  }

  public static void subsetVariable(Variable v, Section s, Array fullData) throws IOException, InvalidRangeException {
      System.out.println("   section="+s);

      // read just that
      Array sdata = v.read(s);
      assert sdata.getRank() == s.getRank();
      int[] sshape = sdata.getShape();
      for (int i = 0; i < sshape.length; i++)
        assert sshape[i] == s.getShape(i);

      // compare with logical section
      Array Asection = fullData.sectionNoReduce(s.getRanges());
      int[] ashape = Asection.getShape();
      assert (ashape.length == sdata.getRank());
      for (int i = 0; i < ashape.length; i++)
        assert sshape[i] == ashape[i];

      ucar.unidata.util.test.CompareNetcdf.compareData(sdata, Asection);
  }

  private static Section randomSubset(Section all, int stride) throws InvalidRangeException {
    Section s = new Section();
    for (Range r : all.getRanges()) {
      int first = random(r.first(), r.last() / 2);
      int last = random(r.last() / 2, r.last());
      s.appendRange(first, last, stride);
    }
    return s;
  }

  private static Random r = new Random(System.currentTimeMillis());
  private static int random(int first, int last) {
    return first + r.nextInt(last - first + 1);
  }

}
