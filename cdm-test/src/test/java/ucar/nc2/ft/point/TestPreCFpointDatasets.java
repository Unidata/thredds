/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Synthetic (Ncml) datasets for testing point feature variants
 *
 * @author caron
 * @since 6/27/2014
 */
@RunWith(Parameterized.class)
public class TestPreCFpointDatasets {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  static public String CFpointObs_pre16 = TestDir.cdmLocalTestDataDir + "pointPre1.6/";

  @Parameterized.Parameters(name="{0}")
  public static List<Object[]> getTestParameters() {
    return TestPointDatasets.getAllFilesInDirectory(CFpointObs_pre16, new FileFilter() {
      public boolean accept(File f) {
        return !f.getPath().contains("Flat");
      }
    });
  }

  String location;
  FeatureType ftype;
  boolean show = false;

  public TestPreCFpointDatasets(String location, FeatureType ftype) {
    this.location = location;
    this.ftype = ftype;
  }

  @Test
  public void checkPointDataset() throws IOException {
    Assert.assertTrue("npoints", 0 < TestPointDatasets.checkPointFeatureDataset(location, ftype, show));
  }

}
