/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.Sequence;
import ucar.nc2.NetcdfFile;
import ucar.nc2.iosp.bufr.BufrIosp2;
import ucar.ma2.StructureData;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.ArrayStructure;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test that nested structures get enhanced.
 * @author caron
 * @since Jul 5, 2008
 */
public class TestNestedStructuresEnhancement {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Ignore("cant deal with BUFR at the moment")
  @Test
  public void testNestedTable() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "dataset/nestedTable.bufr";
    try (NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDataset.openFile(filename, null)) {
      logger.debug("Open {}", ncfile.getLocation());
      Sequence outer = (Sequence) ncfile.findVariable(BufrIosp2.obsRecord);
      assert outer != null;

      try (StructureDataIterator iter = outer.getStructureIterator()) {
        StructureData data = null;
        if (iter.hasNext())
          data = iter.next();

        assert data != null;
        assert data.getScalarShort("Latitude_coarse_accuracy") == 32767;

        ArrayStructure as = data.getArrayStructure("Geopotential");
        assert as != null;
        assert as.getScalarShort(0, as.findMember("Wind_speed")) == 61;

      }
    }
  }

  @Ignore("cant deal with BUFR at the moment")
  @Test
  public void testNestedTableEnhanced() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "dataset/nestedTable.bufr";
    try (NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDataset.openDataset(filename)) {
      logger.debug("Open {}", ncfile.getLocation());
      SequenceDS outer = (SequenceDS) ncfile.findVariable(BufrIosp2.obsRecord);
      assert outer != null;

      try (StructureDataIterator iter = outer.getStructureIterator()) {
        StructureData data = null;
        if (iter.hasNext())
          data = iter.next();

        assert data != null;
        assert Double.isNaN(data.getScalarFloat("Latitude_coarse_accuracy"));

        ArrayStructure as = data.getArrayStructure("Geopotential");
        assert as != null;
        Assert2.assertNearlyEquals(as.getScalarFloat(0, as.findMember("Wind_speed")), 6.1);
      }
    }
  }
}
