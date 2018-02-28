/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package ucar.nc2.ft;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.TestDir;

public class TestFeatureDatasetFactoryManager {

    /**
     * Tests a non-CF compliant trajectory file
     *
     * This tests a non-CF compliant trajectory file
     * which is read in using the ucar.nc2.ft.point.standard.plug.SimpleTrajectory
     * plug.
     */
    @Test
    public void testSimpleTrajectory() throws IOException {
        FeatureType type = FeatureType.ANY;
        Path location_path = Paths.get(TestDir.cdmLocalTestDataDir, "trajectory",
                "aircraft", "uw_kingair-2005-01-19-113957.nc");
        FeatureDataset featureDataset = FeatureDatasetFactoryManager.open(type,
                location_path.toString(), null, null);
        assert featureDataset != null;
        assert featureDataset.getFeatureType() == FeatureType.TRAJECTORY;
    }

}
