/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

/**
 * Test Sequences constructed when reading NLDN and BUFR datasets.
 *
 * @author caron
 * @since Nov 10, 2009
 */
public class TestSequence {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRead() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "ft/point/200929100.ingest")) {
      Sequence record = (Sequence) ncfile.findVariable("record");

      List<String> expectedMemberNames = Arrays.asList("tsec", "nsec", "lat", "lon", "sgnl", "mult", "fill",
              "majorAxis", "eccent", "ellipseAngle", "chisqr");
      Assert.assertEquals(Sets.newHashSet(expectedMemberNames), Sets.newHashSet(record.getVariableNames()));

      try (StructureDataIterator iter = record.getStructureIterator()) {
        int recordCount = 0;
        while (iter.hasNext()) {
          StructureData data = iter.next();

          // Assert that a single value from the first record equals an expected value.
          // Kinda lazy, but checking all values would be impractical.
          if (recordCount++ == 0) {
            Assert.assertEquals(-700, data.getScalarShort("sgnl"));
          }
        }

        Assert.assertEquals(1165, recordCount);
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testReadNestedSequence() throws IOException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "formats/bufr/userExamples/5900.20030601.rass")) {
      Sequence obs = (Sequence) ncfile.findVariable("obs");

      List<String> expectedMemberNames = Arrays.asList("WMO_block_number", "WMO_station_number", "Type_of_station",
              "Year", "Month", "Day", "Hour", "Minute", "Latitude_coarse_accuracy", "Longitude_coarse_accuracy",
              "Height_of_station", "Short_station_or_site_name", "Type_of_measuring_equipment_used",
              "Time_significance", "Time_period_or_displacement", "seq1");
      Assert.assertEquals(Sets.newHashSet(expectedMemberNames), Sets.newHashSet(obs.getVariableNames()));

      ArraySequence obsArray = (ArraySequence) obs.read();
      int obsCount = 0;

      try (StructureDataIterator iter = obsArray.getStructureDataIterator()) {
        for (; iter.hasNext(); ++obsCount) {
          ArraySequence nestedSequence = iter.next().getArraySequence("seq1");

          // All seq1 records will have the same members, so just examine the first.
          if (obsCount == 0) {
            List<String> expectedNestedMemberNames = Arrays.asList(
                "Height_above_station", "Virtual_temperature", "Wind_profiler_quality_control_test_results"
            );
            Assert.assertEquals(Sets.newHashSet(expectedNestedMemberNames),
                    Sets.newHashSet(nestedSequence.getStructureMemberNames()));
          }

          int seq1Count = 0;

          try (StructureDataIterator nestedIter = nestedSequence.getStructureDataIterator()) {
            for (; nestedIter.hasNext(); ++seq1Count) {
              StructureData nestedData = nestedIter.next();
              Assert.assertNotNull(nestedData);

              // Assert that obs[0].seq1[0].Height_above_station has expected value.
              // It would be impractical to assert values for all records, so we just picked the first.
              if (obsCount == 0 && seq1Count == 0) {
                Assert.assertEquals(500, nestedData.getScalarShort("Height_above_station"));
              }
            }
          }

          if (obsCount == 0) {
            Assert.assertEquals(11, seq1Count);  // Assert that obs[0].seq1 has expected number of records.
          }
        }
      }

      Assert.assertEquals(225, obsCount);  // Assert that obs has expected number of records.
    }
  }

  @Test
  // Demonstrates bug in https://andy.unidata.ucar.edu/esupport/staff/index.php?_m=tickets&_a=viewticket&ticketid=29223
  public void readStructureWithinSequence() throws IOException {
    File dataset = new File(TestDir.cdmTestDataDir + "ucar/nc2/bufr/IUPT02_KBBY_281400_522246081.bufr.2018032814");

    // We will populate this below. 14 is the number of records in "obs[0].struct1".
    ArrayFloat.D1 actualUcomponentValues = new ArrayFloat.D1(14);

    // Read the enhanced values of "obs.struct1.u-component". Of course, we could do this much more concisely with
    // "ncFile.findVariable("obs.struct1.u-component").read()", but we're trying to demonstrate a bug in
    // ArrayStructureMA.factoryMA() that only occurs when we iterate over a Sequence with unknown length ("obs").
    try (NetcdfFile ncFile = NetcdfDataset.openDataset(dataset.getAbsolutePath())) {
      /* The structure of the file (with irrelevant bits removed) is:
        netcdf {
          variables:
            Sequence {
              Structure {
                float u-component;
              } struct1(14);
            } obs(*);
        }
       */
      Structure obs = (Structure) ncFile.findVariable("obs");
      ArrayStructure obsArray = (ArrayStructure) obs.read();  // Before the bug fix, this threw a NullPointerException.

      try (StructureDataIterator obsIter = obsArray.getStructureDataIterator()) {
        Assert.assertTrue(obsIter.hasNext());
        StructureData obsData = obsIter.next();
        Assert.assertFalse("Expected to find only one 'obs' record.", obsIter.hasNext());

        ArrayStructure struct1Array = obsData.getArrayStructure("struct1");

        try (StructureDataIterator struct1Iter = struct1Array.getStructureDataIterator()) {
          for (int index = 0; struct1Iter.hasNext(); ++index) {
            actualUcomponentValues.set(index, struct1Iter.next().getScalarFloat("u-component"));
          }
        }
      }
    }

    Array expectedUcomponentValues = Array.makeFromJavaArray(new float[] {
            -1.1f, -1.0f, -0.7f, -0.8f, -0.5f, -0.2f, 0.0f, 0.3f, 0.5f, 1.3000001f, 1.6f, 1.7f, 2.2f, 2.8f
    });
    Assert.assertTrue(MAMath.nearlyEquals(expectedUcomponentValues, actualUcomponentValues));
  }
}
