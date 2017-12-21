/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
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
