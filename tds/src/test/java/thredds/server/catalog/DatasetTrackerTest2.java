/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.server.catalog.tracker.DatasetExt;

import java.io.*;
import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 12/1/2015.
 */
public class DatasetTrackerTest2 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) throws IOException {
    ChronicleMapBuilder<String, DatasetExt> builder = ChronicleMapBuilder.of(String.class, DatasetExt.class)
            .averageValueSize(200).entries(100);

    File dbFile = new File("C:/Temp/cdb2.tmp");

    ChronicleMap<String, DatasetExt> datasetMap = builder.createPersistedTo(dbFile);
    System.out.printf("%s%n", datasetMap);

    String key = "esg_dataroot/obs4MIPs/observations/atmos/husNobs/mon/grid/NASA-JPL/AIRS/v20110608/husNobs_AIRS_L3_RetStd-v5_200209-201105.nc";
    //datasetMap.put(key, new DatasetTrackerInfo(1, "value", null));

    DatasetExt saved = datasetMap.get(key);
    assert saved.getRestrictAccess().equals("value");
    System.out.printf("%s%n", saved);

    datasetMap.close();
  }
}
