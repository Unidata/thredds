/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

public class TestRedefine3 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testRedefine3() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();
    NetcdfFileWriter ncFile = NetcdfFileWriter.createNew (filename, false);
    ncFile.setExtraHeaderBytes (64*1000);
    Dimension dim = ncFile.addDimension ("time", 100);

    double[] jackData = new double[100];
    for (int i = 0; i < 100; i++) jackData[i] = i;
    double[] jillData = new double[100];
    for (int i = 0; i < 100; i++) jillData[i] = 2*i;

    ncFile.addVariable ("jack", DataType.DOUBLE, "time");
    ncFile.addVariableAttribute ("jack", "where", "up the hill");
    ncFile.create();

    int[] start = new int[] {0};
    int[] count = new int[] {100};
    ncFile.write ("jack", start, Array.factory (DataType.DOUBLE, count, jackData));

    ncFile.setRedefineMode (true);
    ncFile.addVariable ("jill", DataType.DOUBLE, "time");
    ncFile.addVariableAttribute ("jill", "where", "up the hill");
    ncFile.setRedefineMode (false);

    Array jillArray = Array.factory (DataType.DOUBLE, count, jillData);
    ncFile.write ("jill", start, jillArray);

    ncFile.flush();
    ncFile.close();

    NetcdfFile nc = NetcdfFile.open(filename, null);
    Variable v = nc.findVariable("jill");
    Array jillRead = v.read();
    ucar.unidata.util.test.CompareNetcdf.compareData(jillArray, jillRead);

    nc.close();
  }
}
