package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;

import java.io.*;
import java.util.*;

/** Test nc2 read JUnit framework. */

public class TestH5ReadStructure2 extends TestCase {

  public TestH5ReadStructure2( String name) {
    super(name);
  }

  File tempFile;
  PrintStream out;
  protected void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new PrintStream( new FileOutputStream( tempFile));
  }
  protected void tearDown() throws Exception {
    out.close();
    tempFile.delete();
  }

  public void testReadH5Structure() throws java.io.IOException {
    NetcdfFile ncfile = TestH5.openH5("complex/compound_complex.h5");

    Variable dset = null;
    assert (null != (dset = ncfile.findVariable("CompoundComplex")));
    assert (dset.getDataType() == DataType.STRUCTURE);
    assert (dset.getRank() == 1);
    assert (dset.getSize() == 6);

    Dimension d = dset.getDimension(0);
    assert(d.getLength() == 6);

    Structure s = (Structure) dset;

     // read all with the iterator
    Structure.Iterator iter = s.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sd = (StructureData) iter.next();

      Iterator viter = sd.getMembers().iterator();
      while (viter.hasNext()) {
        StructureMembers.Member m = (StructureMembers.Member) viter.next();
        Array data = sd.getArray(m);
        NCdump.printArray( data, m.getName(), out, null);
      }
    }

    ncfile.close();
    System.out.println("*** testReadH5Structure ok");

  }

  public void testReadH5StructureArrayMembers() throws java.io.IOException {
    NetcdfFile ncfile = TestH5.openH5("complex/compound_native.h5");

    Variable dset = null;
    assert(null != (dset = ncfile.findVariable("CompoundNative")));
    assert(dset.getDataType() == DataType.STRUCTURE);
    assert(dset.getRank() == 1);
    assert(dset.getSize() == 15);

    Dimension d = dset.getDimension(0);
    assert(d.getLength() == 15);

    Structure s = (Structure) dset;

    // read all with the iterator
    Structure.Iterator iter = s.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sd = (StructureData) iter.next();

      Iterator viter = sd.getMembers().iterator();
      while (viter.hasNext()) {
        StructureMembers.Member m = (StructureMembers.Member) viter.next();
        Array data = sd.getArray(m);
        NCdump.printArray( data, m.getName(), out, null);
      }
    }

    ncfile.close();
    System.out.println("*** testReadH5StructureArrayMembers ok");
  }
}
