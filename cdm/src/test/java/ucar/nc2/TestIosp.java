package ucar.nc2;

import junit.framework.*;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;
import ucar.ma2.Section;
import ucar.ma2.Range;

import java.io.IOException;
import java.util.Random;

/**
 * TestSuite that runs IOSP tests
 *
 */
public class TestIosp {

  public static junit.framework.Test suite ( ) {
    TestSuite suite= new TestSuite();
    suite.addTest( new TestSuite( ucar.nc2.iosp.dmsp.TestDmspIosp.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.gini.TestGini.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.nexrad2.TestNexrad2.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.nexrad2.TestNexrad2HiResolution.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.nids.TestNids.class));
    suite.addTest( new TestSuite( ucar.nc2.iosp.dorade.TestDorade.class));
    return suite;
  }

  public static void testVariableSubset(String filename, String varName, int ntrials) throws InvalidRangeException, IOException {
    System.out.println("testVariableSubset="+filename+","+varName);

    NetcdfFile ncfile = NetcdfFile.open(filename);

    Variable v = ncfile.findVariable(varName);
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
    System.out.println("  Entire dataset="+all);

    for (int k = 0; k < ntrials; k++) {
      // create a random subset, read and compare
      testOne(v, randomSubset(all, 1), A);
      testOne(v, randomSubset(all, 2), A);
      testOne(v, randomSubset(all, 3), A);
    }

    ncfile.close();
  }

   public static void testVariableSubset(String filename, String varName, Section s) throws InvalidRangeException, IOException {
    System.out.println("testVariableSubset="+filename+","+varName);

    NetcdfFile ncfile = NetcdfFile.open(filename);
    Variable v = ncfile.findVariable(varName);
    assert (null != v);
    testOne(v, s, v.read());
  }

  public static void testOne(Variable v, Section s, Array fullData) throws IOException, InvalidRangeException {
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

      TestCompare.compareData(sdata, Asection);
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