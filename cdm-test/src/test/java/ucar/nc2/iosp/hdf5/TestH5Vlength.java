/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.*;
import java.lang.invoke.MethodHandles;

/**
 * Test nc2 read JUnit framework.
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5Vlength {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  File tempFile;
  PrintStream out;

  @Before
  public void setUp() throws Exception {
    tempFile = File.createTempFile("TestLongOffset", "out");
    out = new PrintStream(new FileOutputStream(tempFile));
  }

  @After
  public void tearDown() throws Exception {
    out.close();
    boolean status = tempFile.delete();
    if (!status) System.out.printf("delete failed%n");
  }

  @Test
  public void testVlengthAttribute() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/vlstra.h5")) {
      Attribute att = ncfile.findGlobalAttribute("test_scalar");
      assert (null != att);
      assert (!att.isArray());
      assert (att.isString());
      assert (att.getStringValue().equals("This is the string for the attribute"));
    }
  }

  @Test
  public void testVlengthVariableChunked() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/uvlstr.h5")) {

      Variable v = ncfile.findVariable("Space1");
      assert (null != v);
      assert (v.getDataType() == DataType.STRING);
      assert (v.getRank() == 1);
      assert (v.getShape()[0] == 9);

      try {
        Array data = v.read();
        assert (data.getElementType() == String.class);
        assert (data instanceof ArrayObject);
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          out.println(iter.next());
        }

      } catch (IOException e) {
        e.printStackTrace();
        assert false;
      }

      int[] origin = new int[]{3};
      int[] shape = new int[]{3};
      try {
        Array data2 = v.read(origin, shape);
        Index ima = data2.getIndex();
        assert (data2.getElementType() == String.class);
        assert (data2 instanceof ArrayObject);
        assert ((String) data2.getObject(ima.set(0))).startsWith("testing whether that nation");
        assert ((String) data2.getObject(ima.set(1))).startsWith("O Gloria inmarcesible!");
        assert ((String) data2.getObject(ima.set(2))).startsWith("bien germina ya!");
      } catch (IOException | InvalidRangeException e) {
        assert false;
      }

    }
  }

  @Test
  public void testVlengthVariable() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("support/vlslab.h5")) {

      Variable v = ncfile.findVariable("Space1");
      assert (null != v);
      assert (v.getDataType() == DataType.STRING);
      assert (v.getRank() == 1);
      assert (v.getShape()[0] == 12);

      try {
        Array data = v.read();
        assert (data.getElementType() == String.class);
        assert (data instanceof ArrayObject);
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          out.println(iter.next());
        }

      } catch (IOException e) {
        assert false;
      }

      int[] origin = new int[]{4};
      int[] shape = new int[]{1};
      try {
        Array data2 = v.read(origin, shape);
        Index ima = data2.getIndex();
        assert (data2.getElementType() == String.class);
        assert (data2 instanceof ArrayObject);
        assert ( data2.getObject(ima.set(0))).equals("Five score and seven years ago our forefathers brought forth on this continent a new nation,");
      } catch (IOException | InvalidRangeException e) {
        assert false;
      }

    }
  }

  // from bsantos@ipfn.ist.utl.pt
  /*
  netcdf Q\:/cdmUnitTest/formats/netcdf4/vlenBigEndian {
  types:
    uint(*) vlen_t ;
  dimensions:
          acqtime = UNLIMITED ; // (10 currently)
  variables:
          uint64 acqtime(acqtime) ;
                  acqtime:long_name = "Acquisition time" ;
          uint blocknumber(acqtime) ;
                  blocknumber:long_name = "Number of block" ;
          uint64 speriod(acqtime) ;
                  speriod:long_name = "Sample period for this data block" ;
                  speriod:units = "ns" ;
          uint64 srate(acqtime) ;
                  srate:long_name = "Sample rate for this data block" ;
                  srate:units = "samples/s" ;
          double scale_factor(acqtime) ;
                  scale_factor:long_name = "Scale factor for this data block" ;
          double offset(acqtime) ;
                  offset:long_name = "Offset value to be added after applying the scale_factor" ;
          int samplesize(acqtime) ;
          int nsamples(acqtime) ;
                  nsamples:long_name = "Number of elements of this data block" ;
          vlen_t levels(acqtime) ;
                  levels:long_name = "Acquired values array" ;

  // global attributes:
                  :sourceID = "test3" ;
                  :pulseID = "p1" ;
                  :title = "Acquisition channel data" ;
                  :version = 1. ;
                  :time_stamp_reference = "UTC" ;
                  :time_coverage_duration = 0 ;
                  :license = "Freely available" ;
                  :time_stamp_start_secs = 500L ;
                  :time_stamp_start_nanosecs = 1000 ;
  }
   */
  @Test
  public void testVlenEndian() throws IOException {
    testVlenEndian(TestN4reading.testDir+"vlenBigEndian.nc", 10);
    //testVlenEndian("C:/data/work/bruno/test3_p1_d1wave.nc", 10);
    testVlenEndian(TestN4reading.testDir+"vlenLittleEndian.nc", 100);
    //testVlenEndian("C:/data/work/bruno/fpscminicodac_1.nc", 100);
  }

  private void testVlenEndian(String filename, int n) throws IOException {
    try (NetcdfFile ncfile = NetcdfDataset.openFile(filename, null)) {

      Variable v = ncfile.findVariable("levels");
      assert (null != v);
      assert (v.getDataType() == DataType.UINT);
      assert (v.getRank() == 2);
      assert (v.getShape()[0] == n) : v.getShape()[0];

      try {
        Array data = v.read();
        // assert(data.getElementType() instanceof ucar.ma2.ArrayInt.class) : data.getElementType();
        assert (data instanceof ArrayObject);
        IndexIterator iter = data.getIndexIterator();
        while (iter.hasNext()) {
          Array inner = (Array) iter.next();
          assert (inner instanceof ArrayInt.D1);
          int firstVal = inner.getInt(0);
          System.out.printf("%d (%d) = %s%n", firstVal, inner.getSize(), inner);
          assert (firstVal < Short.MAX_VALUE) : firstVal;
        }

      } catch (IOException e) {
        assert false;
      }

    }
  }

}
