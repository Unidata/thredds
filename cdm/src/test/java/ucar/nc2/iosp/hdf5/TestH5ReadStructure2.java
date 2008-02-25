package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.*;
import java.util.*;

/**
 * Test nc2 read JUnit framework.
 */

public class TestH5ReadStructure2 extends TestCase {

  public TestH5ReadStructure2(String name) {
    super(name);
  }

  File tempFile;
  PrintStream out;

  protected void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new PrintStream(new FileOutputStream(tempFile));
  }

  protected void tearDown() throws Exception {
    out.close();
    tempFile.delete();
  }

  /*
     Structure {
     int a_name;
     String b_name(4);
     char c_name(6);
     short d_name(5, 4);
     float e_name;
     double f_name(10);
     byte g_name;
   } CompoundComplex(6);
    type = Layout(8);  type= 1 (contiguous) storageSize = (6,224) dataSize=0 dataAddress=2048
 */
  public void testReadH5Structure() throws java.io.IOException {
    int a_name = 0;
    String[] b_name = new String[]{"A fight is a contract that takes two people to honor.",
        "A combative stance means that you've accepted the contract.",
        "In which case, you deserve what you get.",
        "  --  Professor Cheng Man-ch'ing"};
    String c_name = "Hello!";

    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfFile ncfile = TestH5.openH5("complex/compound_complex.h5");

    Variable dset = null;
    assert (null != (dset = ncfile.findVariable("CompoundComplex")));
    assert (dset.getDataType() == DataType.STRUCTURE);
    assert (dset.getRank() == 1);
    assert (dset.getSize() == 6);

    Dimension d = dset.getDimension(0);
    assert (d.getLength() == 6);

    Structure s = (Structure) dset;

    // read all with the iterator
    StructureDataIterator iter = s.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sd = (StructureData) iter.next();
      assert sd.getScalarInt("a_name") == a_name;
      a_name++;
      assert sd.getScalarString("c_name").equals(c_name);
      String[] results = sd.getJavaArrayString(sd.findMember("b_name"));
      assert results.length == b_name.length;
      int count = 0;
      for (String r : results)
        assert r.equals(b_name[count++]);

      Iterator viter = sd.getMembers().iterator();
      while (viter.hasNext()) {
        StructureMembers.Member m = (StructureMembers.Member) viter.next();
        Array data = sd.getArray(m);
        NCdump.printArray(data, m.getName(), out, null);
      }
    }

    ncfile.close();
    System.out.println("*** testReadH5Structure ok");
  }

  public void testH5StructureDS() throws java.io.IOException {
    int a_name = 0;
    String[] b_name = new String[]{"A fight is a contract that takes two people to honor.",
        "A combative stance means that you've accepted the contract.",
        "In which case, you deserve what you get.",
        "  --  Professor Cheng Man-ch'ing"};
    String c_name = "Hello!";

    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfDataset ncfile = NetcdfDataset.openDataset(TestAll.upcShareTestDataDir + "hdf5/complex/compound_complex.h5");

    Variable dset = null;
    assert (null != (dset = ncfile.findVariable("CompoundComplex")));
    assert (dset.getDataType() == DataType.STRUCTURE);
    assert (dset.getRank() == 1);
    assert (dset.getSize() == 6);

    Dimension d = dset.getDimension(0);
    assert (d.getLength() == 6);

    Structure s = (Structure) dset;

    // read all with the iterator
    StructureDataIterator iter = s.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sd = (StructureData) iter.next();
      assert sd.getScalarInt("a_name") == a_name;
      a_name++;
      assert sd.getScalarString("c_name").equals(c_name);
      String[] results = sd.getJavaArrayString(sd.findMember("b_name"));
      assert results.length == b_name.length;
      int count = 0;
      for (String r : results)
        assert r.equals(b_name[count++]);

      Iterator viter = sd.getMembers().iterator();
      while (viter.hasNext()) {
        StructureMembers.Member m = (StructureMembers.Member) viter.next();
        Array data = sd.getArray(m);
        NCdump.printArray(data, m.getName(), out, null);
      }
    }

    ncfile.close();
    System.out.println("*** testH5StructureDS ok");

  }

  /*
    Structure {
     int a_name;
     byte b_name(3);
     byte c_name(3);
     short d_name(3);
     int e_name(3);
     long f_name(3);
     int g_name(3);
     short h_name(3);
     int i_name(3);
     long j_name(3);
     float k_name(3);
     double l_name(3);
   } CompoundNative(15);
       type = Layout(8);  type= 1 (contiguous) storageSize = (15,144) dataSize=0 dataAddress=2048   
   */
  public void testReadH5StructureArrayMembers() throws java.io.IOException {
    NetcdfFile ncfile = TestH5.openH5("complex/compound_native.h5");

    Variable dset = null;
    assert (null != (dset = ncfile.findVariable("CompoundNative")));
    assert (dset.getDataType() == DataType.STRUCTURE);
    assert (dset.getRank() == 1);
    assert (dset.getSize() == 15);

    Dimension d = dset.getDimension(0);
    assert (d.getLength() == 15);

    Structure s = (Structure) dset;

    // read all with the iterator
    StructureDataIterator iter = s.getStructureIterator();
    while (iter.hasNext()) {
      StructureData sd = (StructureData) iter.next();

      Iterator viter = sd.getMembers().iterator();
      while (viter.hasNext()) {
        StructureMembers.Member m = (StructureMembers.Member) viter.next();
        Array data = sd.getArray(m);
        NCdump.printArray(data, m.getName(), out, null);
      }
    }

    ncfile.close();
    System.out.println("*** testReadH5StructureArrayMembers ok");
  }

  /* Structure {
       int LAT[0];
       ...
       int LAT[149];
   } IMAGE_LAT_ARRAY(3600);
      type = Layout(8);  type= 2 (chunked) storageSize = (1,600) dataSize=0 dataAddress=2548046
   */
  public void testReadOneAtATime() throws java.io.IOException, InvalidRangeException {
    NetcdfFile ncfile = TestH5.openH5("IASI/IASI.h5");

    Variable dset = null;
    assert (null != (dset = ncfile.findVariable("U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_LAT_ARRAY")));
    assert (dset.getDataType() == DataType.STRUCTURE);
    assert (dset.getRank() == 1);
    assert (dset.getSize() == 3600);

    Dimension d = dset.getDimension(0);
    assert (d.getLength() == 3600);

    Structure s = (Structure) dset;

    // read last one - chunked
    StructureData sd = s.readStructure(3599);
    assert sd.getScalarInt("LAT[0]") == 70862722;
    assert sd.getScalarInt("LAT[149]") == 85302263;

    // read one at a time
    for (int i = 3590; i < d.getLength(); i++) {
      s.readStructure(i);
      System.out.println(" read structure " + i);
    }

    ncfile.close();
    System.out.println("*** testReadIASI ok");
  }

  /*  Structure {
     char EntryName(64);
     char Definition(1024);
     char Unit(1024);
     char Scale Factor(1024);
   } TIME_DESCR(60);
      type = Layout(8);  type= 2 (chunked) storageSize = (1,3136) dataSize=0 dataAddress=684294
   */
  public void testReadManyAtATime() throws java.io.IOException, InvalidRangeException {
    NetcdfFile ncfile = TestH5.openH5("IASI/IASI.h5");

    Variable dset = null;
    assert (null != (dset = ncfile.findVariable("U-MARF/EPS/IASI_xxx_1C/DATA/TIME_DESCR")));
    assert (dset.getDataType() == DataType.STRUCTURE);
    assert (dset.getRank() == 1);
    assert (dset.getSize() == 60);

    Dimension d = dset.getDimension(0);
    assert (d.getLength() == 60);

    ArrayStructure data = (ArrayStructure) dset.read();
    StructureMembers.Member m = data.getStructureMembers().findMember("EntryName");
    assert m != null;
    for (int i=0; i< dset.getSize(); i++) {
      String r = data.getScalarString(i,m);
      if (i % 2 == 0)
        assert r.equals("TIME["+i/2+"]-days") : r +" at "+i;
      else
        assert r.equals("TIME["+i/2+"]-milliseconds") : r +" at "+i;
    }
    ncfile.close();
    System.out.println("*** testReadManyAtATime ok");
  }
}
