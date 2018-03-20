/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.catalog;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 12/1/2015.
 */
public class DatasetTrackerTest implements Externalizable {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) throws IOException {

    ChronicleMapBuilder<String, Externalizable> builder = ChronicleMapBuilder.of(String.class, Externalizable.class)
            .averageValueSize(200).entries(100);

    File dbFile = new File("C:/Temp/cdb.tmp");

    ChronicleMap<String, Externalizable> datasetMap = builder.createPersistedTo(dbFile);
    System.out.printf("%s%n", datasetMap);

    String key = "esg_dataroot/obs4MIPs/observations/atmos/husNobs/mon/grid/NASA-JPL/AIRS/v20110608/husNobs_AIRS_L3_RetStd-v5_200209-201105.nc";
    datasetMap.put(key, new DatasetTrackerTest("value"));

    DatasetTrackerTest saved = (DatasetTrackerTest) datasetMap.get(key);
    assert saved.value.equals("value");
    System.out.printf("%s%n", saved.value);

    datasetMap.close();
  }

  String value;

  // Externalizable needs void constructor
  public DatasetTrackerTest() {
  }

  public DatasetTrackerTest(String value) {
    this.value = value;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeUTF(value);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    value = in.readUTF();
  }
}
