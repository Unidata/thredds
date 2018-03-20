/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: $


package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.List;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggExistingSSTA {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  String ncml =
    "<?xml version='1.0' encoding='UTF-8'?>\n" +
    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
    "    <aggregation dimName='time' type='joinExisting' recheckEvery='15 min'>\n" +
    "      <variableAgg name='ATssta' />\n" +
    "      <scan dateFormatMark='AT#yyyyDDD_HHmmss' location='" + TestDir.cdmUnitTestDir + "ncml/nc/pfeg/' suffix='.nc' />\n" +
    "    </aggregation>\n" +
    "</netcdf>";

  @Test
  public void testSSTA() throws IOException, InvalidRangeException {
    String filename = "file:" + TestDir.cdmUnitTestDir + "ncml/offsite/aggExistingSSTA.xml";

    RandomAccessFile.setDebugLeaks( true);
    List<String> openfiles = RandomAccessFile.getOpenFiles();
    int count = openfiles.size();
    System.out.println("count files at start="+count);

    NetcdfDataset.disableNetcdfFileCache();
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null);
    System.out.println(" TestNcmlAggExisting.open "+ filename);

    Array ATssta = ncfile.readSection("ATssta(:,0,0,0)");
    System.out.printf("array size=%d%n", ATssta.getSize());

    int count1 = RandomAccessFile.getOpenFiles().size();
    System.out.println("count files after open="+count1);

    ncfile.close();

    int count2 = RandomAccessFile.getOpenFiles().size();
    System.out.println("count files after close="+count2);
    assert count1 == count2 : "openFile count "+count +"!="+ count2;
  }
}
