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
package ucar.nc2;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.StructureDS;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

public class TestReadRecord extends TestCase {

  public TestReadRecord(String name) {
    super(name);
  }

  public void testNC3ReadRecordV3mode() {

    try {
      NetcdfFile nc = TestDir.openFileLocal("testWriteRecord.nc");

      /* Get the value of the global attribute named "title" */
      String title = nc.findAttValueIgnoreCase(null, "title", "N/A");

      /* Read the latitudes into an array of double.
         This works regardless of the external
         type of the "lat" variable. */
      Variable lat = nc.findVariable("lat");
      assert (lat.getRank() == 1);  // make sure it's 1-dimensional
      int nlats = lat.getShape()[0]; // number of latitudes
      double[] lats = new double[nlats];  // where to put them

      Array values = lat.read(); // read all into memory
      Index ima = values.getIndex(); // index array to specify which value
      for (int ilat = 0; ilat < nlats; ilat++) {
        lats[ilat] = values.getDouble(ima.set0(ilat));
      }
      /* Read units attribute of lat variable */
      String latUnits = nc.findAttValueIgnoreCase(lat, "units", "N/A");
      assert (latUnits.equals("degrees_north"));

      /* Read the longitudes. */
      Variable lon = nc.findVariable("lon");
      values = lon.read();
      assert (values instanceof ArrayFloat.D1);
      ArrayFloat.D1 fa = (ArrayFloat.D1) values;
      assert (Misc.closeEnough(fa.get(0), -109.0f)) : fa.get(0);
      assert (Misc.closeEnough(fa.get(1), -107.0f)) : fa.get(1);
      assert (Misc.closeEnough(fa.get(2), -105.0f)) : fa.get(2);
      assert (Misc.closeEnough(fa.get(3), -103.0f)) : fa.get(3);

      /* Now we can just use the MultiArray to access values, or
         we can copy the MultiArray elements to another array with
         toArray(), or we can get access to the MultiArray storage
         without copying.  Each of these approaches to accessing
         the data are illustrated below. */

      /* Read the times: unlimited dimension */
      Variable time = nc.findVariable("time");
      assert time != null;
      Array timeValues = time.read();
      assert (timeValues instanceof ArrayInt.D1);
      ArrayInt.D1 ta = (ArrayInt.D1) timeValues;
      assert (ta.get(0) == 6) : ta.get(0);
      assert (ta.get(1) == 18) : ta.get(1);

      /* Read the relative humidity data */
      Variable rh = nc.findVariable("rh");
      Array rhValues = rh.read();
      assert (rhValues instanceof ArrayInt.D3);
      ArrayInt.D3 rha = (ArrayInt.D3) rhValues;
      int[] shape = rha.getShape();
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          for (int k = 0; k < shape[2]; k++) {
            int want = 20 * i + 4 * j + k + 1;
            int val = rha.get(i, j, k);
            //System.out.println(" "+i+" "+j+" "+k+" "+want+" "+val);
            assert (want == val) : val;
          }
        }
      }

      /* Read the temperature data */
      Variable t = nc.findVariable("T");
      Array tValues = t.read();
      assert (tValues instanceof ArrayDouble.D3);
      ArrayDouble.D3 Ta = (ArrayDouble.D3) tValues;
      assert Misc.closeEnough(Ta.get(0, 0, 0), 1.0f) : Ta.get(0, 0, 0);
      assert Misc.closeEnough(Ta.get(1, 1, 1), 10.0f) : Ta.get(1, 1, 1);

      /* Read subset of the temperature data */
      tValues = t.read(new int[3], new int[]{2, 2, 2});
      assert (tValues instanceof ArrayDouble.D3);
      Ta = (ArrayDouble.D3) tValues;
      assert Misc.closeEnough(Ta.get(0, 0, 0), 1.0f) : Ta.get(0, 0, 0);
      assert Misc.closeEnough(Ta.get(1, 1, 1), 10.0f) : Ta.get(1, 1, 1);

      nc.close();

    } catch (InvalidRangeException e) {
      e.printStackTrace();
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    System.out.println("**** testNC3ReadRecordV3mode done");

  }

  public void testNC3ReadRecordN4mode() {
    try {
      NetcdfFile nc = TestDir.openFileLocal("testWriteRecord.nc");
      nc.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

      /* Get the value of the global attribute named "title" */
      String title = nc.findAttValueIgnoreCase(null, "title", "N/A");

      /* Read the latitudes into an array of double.
         This works regardless of the external
         type of the "lat" variable. */
      Variable lat = nc.findVariable("lat");
      assert (lat.getRank() == 1);  // make sure it's 1-dimensional
      int nlats = lat.getShape()[0]; // number of latitudes
      double[] lats = new double[nlats];  // where to put them

      Array values = lat.read(); // read all into memory
      Index ima = values.getIndex(); // index array to specify which value
      for (int ilat = 0; ilat < nlats; ilat++) {
        lats[ilat] = values.getDouble(ima.set0(ilat));
      }
      /* Read units attribute of lat variable */
      String latUnits = nc.findAttValueIgnoreCase(lat, "units", "N/A");
      assert (latUnits.equals("degrees_north"));

      /* Read the longitudes. */
      Variable lon = nc.findVariable("lon");
      values = lon.read();
      assert (values instanceof ArrayFloat.D1);
      ArrayFloat.D1 fa = (ArrayFloat.D1) values;
      assert (Misc.closeEnough(fa.get(0), -109.0f)) : fa.get(0);
      assert (Misc.closeEnough(fa.get(1), -107.0f)) : fa.get(1);
      assert (Misc.closeEnough(fa.get(2), -105.0f)) : fa.get(2);
      assert (Misc.closeEnough(fa.get(3), -103.0f)) : fa.get(3);

      /* Now we can just use the MultiArray to access values, or
         we can copy the MultiArray elements to another array with
         toArray(), or we can get access to the MultiArray storage
         without copying.  Each of these approaches to accessing
         the data are illustrated below. */

      // record variable
      Variable record = nc.findVariable("record");
      assert record instanceof Structure;
      Structure rs = (Structure) record;
      assert rs.getRank() == 1;
      assert rs.getDimension(0).getLength() == 2;

      /* Read the records */
      Array rsValues = rs.read();
      assert (rsValues instanceof ArrayStructure);
      assert rsValues.getRank() == 1;
      assert rsValues.getShape()[0] == 2;

      /* Read the times: unlimited dimension */
      Variable time = rs.findVariable("time");
      Array timeValues = time.read();
      assert (timeValues instanceof ArrayInt.D0) : timeValues.getClass().getName();
      ArrayInt.D0 ta = (ArrayInt.D0) timeValues;
      assert (ta.get() == 6) : ta.get();

      /* Read the relative humidity data */
      Variable rh = rs.findVariable("rh");
      Array rhValues = rh.read();
      assert (rhValues instanceof ArrayInt.D2);
      ArrayInt.D2 rha = (ArrayInt.D2) rhValues;
      int[] shape = rha.getShape();
      //for (int i=0; i<shape[0]; i++) {
      for (int j = 0; j < shape[0]; j++) {
        for (int k = 0; k < shape[1]; k++) {
          int want = 4 * j + k + 1;
          int val = rha.get(j, k);
          //System.out.println(" "+i+" "+j+" "+k+" "+want+" "+val);
          assert (want == val) : val;
        }
      }
      //}

      /* Read the temperature data
      Variable t = nc.findVariable("T");
      Array tValues = t.read();
      assert( tValues instanceof ArrayDouble.D3);
      ArrayDouble.D3 Ta = (ArrayDouble.D3) tValues;
      assert TestH5.close( Ta.get(0,0,0), 1.0f) : Ta.get(0, 0, 0);
      assert TestH5.close( Ta.get(1,1,1), 10.0f) : Ta.get(1, 1, 1);

      /* Read subset of the temperature data
      tValues = t.read(new int[3], new int[] {2,2,2});
      assert( tValues instanceof ArrayDouble.D3);
      Ta = (ArrayDouble.D3) tValues;
      assert TestH5.close( Ta.get(0,0,0), 1.0f) : Ta.get(0, 0, 0);
      assert TestH5.close( Ta.get(1,1,1), 10.0f) : Ta.get(1, 1, 1); */

      nc.close();

      //} catch (InvalidRangeException e) {
      //    e.printStackTrace();
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }
    System.out.println("*** testNC3ReadRecordN4mode done");

  }


  public void testDatasetAddRecord() throws InvalidRangeException, IOException {
    NetcdfDataset nc = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc", NetcdfDataset.getDefaultEnhanceMode(),
        -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    // record variable
    Variable record = nc.findVariable("record");
    assert record instanceof StructureDS;
    StructureDS rs = (StructureDS) record;
    assert rs.getRank() == 1;
    assert rs.getDimension(0).getLength() == 2;

    /* Read a record */
    Array rsValues = rs.read("1:1:2");
    assert (rsValues instanceof ArrayStructure);
    assert rsValues.getRank() == 1;
    assert rsValues.getShape()[0] == 1;

    StructureData sdata = (StructureData) rsValues.getObject(rsValues.getIndex());
    Array gdata = sdata.getArray("time");
    assert gdata instanceof ArrayInt.D0 : gdata.getClass().getName();
    ArrayInt.D0 tdata = (ArrayInt.D0) gdata;
    int t = tdata.get();
    assert t == 18;

    int t2 = sdata.getScalarInt("time");
    assert t2 == 18;

    /* Read the times: unlimited dimension */
    Variable time = rs.findVariable("time");
    assert time != null;
    Array timeValues = time.read();
    assert (timeValues instanceof ArrayInt.D0);
    ArrayInt.D0 ta = (ArrayInt.D0) timeValues;
    assert (ta.get() == 6) : ta.get();

    nc.close();
  }

  public void testDatasetAddRecordAfter() throws InvalidRangeException, IOException {
    NetcdfDataset nc = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc");
    assert (Boolean) nc.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    // record variable
    Variable record = nc.findVariable("record");
    assert record instanceof StructureDS;
    StructureDS rs = (StructureDS) record;
    assert rs.getRank() == 1;
    assert rs.getDimension(0).getLength() == 2;

    /* Read a record */
    Array rsValues = rs.read("1:1:2");
    assert (rsValues instanceof ArrayStructure);
    assert rsValues.getRank() == 1;
    assert rsValues.getShape()[0] == 1;

    StructureData sdata = (StructureData) rsValues.getObject(rsValues.getIndex());
    Array gdata = sdata.getArray("time");
    assert gdata instanceof ArrayInt.D0 : gdata.getClass().getName();
    ArrayInt.D0 tdata = (ArrayInt.D0) gdata;
    int t = tdata.get();
    assert t == 18;

    int t2 = sdata.getScalarInt("time");
    assert t2 == 18;

    /* Read the times: unlimited dimension */
    Variable time = rs.findVariable("time");
    Array timeValues = time.read();
    assert (timeValues instanceof ArrayInt.D0);
    ArrayInt.D0 ta = (ArrayInt.D0) timeValues;
    assert (ta.get() == 6) : ta.get();

    nc.close();
  }

  public void testNC3ReadRecordStrided() throws InvalidRangeException, IOException {
    NetcdfFile nc = TestDir.openFileLocal("testWriteRecord.nc");
    nc.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    // record variable
    Variable record = nc.findVariable("record");
    assert record instanceof Structure;
    Structure rs = (Structure) record;
    assert rs.getRank() == 1;
    assert rs.getDimension(0).getLength() == 2;

    /* Read a record */
    Array rsValues = rs.read("1:1:2");
    assert (rsValues instanceof ArrayStructure);
    assert rsValues.getRank() == 1;
    assert rsValues.getShape()[0] == 1;

    StructureData sdata = (StructureData) rsValues.getObject(rsValues.getIndex());
    Array gdata = sdata.getArray("time");
    assert gdata instanceof ArrayInt.D0 : gdata.getClass().getName();
    ArrayInt.D0 tdata = (ArrayInt.D0) gdata;
    int t = tdata.get();
    assert t == 18;

    int t2 = sdata.getScalarInt("time");
    assert t2 == 18;

    nc.close();

  }

}
