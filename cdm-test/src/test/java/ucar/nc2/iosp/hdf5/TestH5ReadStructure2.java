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
package ucar.nc2.iosp.hdf5;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;

/**
 * Test nc2 read JUnit framework.
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadStructure2 {
  File tempFile;
  PrintWriter out;

  @Before
  public void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new PrintWriter(new FileOutputStream(tempFile));
  }

  @After
  public void tearDown() throws Exception {
    out.close();
    if (!tempFile.delete())
      System.out.printf("Delete failed on %s%n", tempFile);
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
  @Test
  public void testReadH5Structure() throws java.io.IOException {
    int a_name = 0;
    String[] b_name = new String[]{"A fight is a contract that takes two people to honor.",
        "A combative stance means that you've accepted the contract.",
        "In which case, you deserve what you get.",
        "  --  Professor Cheng Man-ch'ing"};
    String c_name = "Hello!";

    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("complex/compound_complex.h5")) {

      Variable dset = ncfile.findVariable("CompoundComplex");
      assert (null != dset);
      assert (dset.getDataType() == DataType.STRUCTURE);
      assert (dset.getRank() == 1);
      assert (dset.getSize() == 6);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 6);

      Structure s = (Structure) dset;

      // read all with the iterator
      StructureDataIterator iter = s.getStructureIterator();
      while (iter.hasNext()) {
        StructureData sd = iter.next();
        assert sd.getScalarInt("a_name") == a_name;
        a_name++;
        assert sd.getScalarString("c_name").equals(c_name);
        String[] results = sd.getJavaArrayString(sd.findMember("b_name"));
        assert results.length == b_name.length;
        int count = 0;
        for (String r : results)
          assert r.equals(b_name[count++]);

        for (StructureMembers.Member m : sd.getMembers()) {
          Array data = sd.getArray(m);
          NCdumpW.printArray(data, m.getName(), out, null);
        }
      }

    }
    System.out.println("*** testReadH5Structure ok");
  }

  @Test
  public void testH5StructureDS() throws java.io.IOException {
    int a_name = 0;
    String[] b_name = new String[]{"A fight is a contract that takes two people to honor.",
        "A combative stance means that you've accepted the contract.",
        "In which case, you deserve what you get.",
        "  --  Professor Cheng Man-ch'ing"};
    String c_name = "Hello!";

    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    try (NetcdfDataset ncfile = NetcdfDataset.openDataset(TestH5.testDir + "complex/compound_complex.h5")) {

      Variable dset = ncfile.findVariable("CompoundComplex");
      assert (null != dset);
      assert (dset.getDataType() == DataType.STRUCTURE);
      assert (dset.getRank() == 1);
      assert (dset.getSize() == 6);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 6);

      Structure s = (Structure) dset;

      // read all with the iterator
      StructureDataIterator iter = s.getStructureIterator();
      while (iter.hasNext()) {
        StructureData sd = iter.next();
        assert sd.getScalarInt("a_name") == a_name;
        a_name++;
        assert sd.getScalarString("c_name").equals(c_name);
        String[] results = sd.getJavaArrayString(sd.findMember("b_name"));
        assert results.length == b_name.length;
        int count = 0;
        for (String r : results)
          assert r.equals(b_name[count++]);

        for (StructureMembers.Member m : sd.getMembers()) {
          Array data = sd.getArray(m);
          NCdumpW.printArray(data, m.getName(), out, null);
        }
      }

    }
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
  @Test
  public void testReadH5StructureArrayMembers() throws java.io.IOException {
    try (NetcdfFile ncfile = TestH5.openH5("complex/compound_native.h5")) {

      Variable dset = ncfile.findVariable("CompoundNative");
      assert (null != dset);
      assert (dset.getDataType() == DataType.STRUCTURE);
      assert (dset.getRank() == 1);
      assert (dset.getSize() == 15);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 15);

      Structure s = (Structure) dset;

      // read all with the iterator
      StructureDataIterator iter = s.getStructureIterator();
      while (iter.hasNext()) {
        StructureData sd = iter.next();

        for (StructureMembers.Member m : sd.getMembers()) {
          Array data = sd.getArray(m);
          NCdumpW.printArray(data, m.getName(), out, null);
        }
      }

    }
    System.out.println("*** testReadH5StructureArrayMembers ok");
  }

  /* Structure {
       int LAT[0];
       ...
       int LAT[149];
   } IMAGE_LAT_ARRAY(3600);
      type = Layout(8);  type= 2 (chunked) storageSize = (1,600) dataSize=0 dataAddress=2548046
   */
  @Test
  public void testReadOneAtATime() throws java.io.IOException, InvalidRangeException {
    try (NetcdfFile ncfile = TestH5.openH5("IASI/IASI.h5")) {

      Variable dset = ncfile.findVariable("U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_LAT_ARRAY");
      assert (null != dset);
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

    }
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
  @Test
  public void testReadManyAtATime() throws java.io.IOException, InvalidRangeException {
    try (NetcdfFile ncfile = TestH5.openH5("IASI/IASI.h5")) {

      Variable dset = ncfile.findVariable("U-MARF/EPS/IASI_xxx_1C/DATA/TIME_DESCR");
      assert (null != dset);
      assert (dset.getDataType() == DataType.STRUCTURE);
      assert (dset.getRank() == 1);
      assert (dset.getSize() == 60);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 60);

      ArrayStructure data = (ArrayStructure) dset.read();
      StructureMembers.Member m = data.getStructureMembers().findMember("EntryName");
      assert m != null;
      for (int i = 0; i < dset.getSize(); i++) {
        String r = data.getScalarString(i, m);
        if (i % 2 == 0)
          assert r.equals("TIME[" + i / 2 + "]-days") : r + " at " + i;
        else
          assert r.equals("TIME[" + i / 2 + "]-milliseconds") : r + " at " + i;
      }
    }
    System.out.println("*** testReadManyAtATime ok");
  }

}
