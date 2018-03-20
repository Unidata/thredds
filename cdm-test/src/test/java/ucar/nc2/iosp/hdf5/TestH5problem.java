/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf5;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Miscellaneaous problems with hdf5 reader
 *
 * @author caron
 * @since 10/27/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5problem {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // original file name is AG_100nt_even10k.biom
  // problem is that theres a deflate filter on array of strings
  // whats screwy about that is that the heapids get compressed, not the strings (!) doubt thats very useful.
  @Test
  public void problemStringsWithFilter() throws IOException, InvalidRangeException {
    String filename = TestH5.testDir + "StringsWFilter.h5";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      Variable v = ncfile.findVariable("/sample/ids");
      assert v != null;
      int[] shape = v.getShape();
      Assert.assertEquals(1, shape.length);
      Assert.assertEquals(3107, shape[0]);
      Array data = v.read();
      Assert.assertEquals(1, data.getRank());
      Assert.assertEquals(3107, data.getShape()[0]);
      //NCdumpW.printArray(data, "sample/ids", System.out, null);
    }
  }

  @Test
  public void sectionStringsWithFilter() throws IOException, InvalidRangeException {
    String filename = TestH5.testDir + "StringsWFilter.h5";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      Variable v = ncfile.findVariable("/sample/ids");
      assert v != null;
      int[] shape = v.getShape();
      Assert.assertEquals(1, shape.length);
      Assert.assertEquals(3107, shape[0]);

      Array dataSection = v.read("700:900:2");      // make sure to go acrross a chunk boundary
      Assert.assertEquals(1, dataSection.getRank());
      Assert.assertEquals(101, dataSection.getShape()[0]);
    }
  }

  // The HugeHeapId problem: java.io.IOException: java.lang.RuntimeException: Cant find DHeapId=0
  // fixes by rschmunk 4/30/2015
  @Test
  public void problemHugeHeapId() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = TestH5.testDir + "SMAP_L4_SM_aup_20140115T030000_V05007_001.h5";
    try (NetcdfFile ncfile = NetcdfFile.open(filename)) {
      Group g = ncfile.findGroup("Metadata");
      assert g != null;
      Attribute att = g.findAttribute("iso_19139_dataset_xml");
      assert att != null;
      assert att.isString();
      String val = att.getStringValue();
      System.out.printf(" len of %s is %d%n", att.getFullName(), val.length());
      assert val.length() > 200 * 1000; // silly rabbit
    }
  }



}
