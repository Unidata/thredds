/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.*;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.Set;

import junit.framework.TestCase;
import timing.Average;
import ucar.unidata.util.test.TestDir;

/**
 * Class Description.
 *
 * @author caron
 * @since Jun 4, 2008
 */
public class TimeScaleOffsetMissing extends TestCase {

  public TimeScaleOffsetMissing( String name) {
    super(name);
  }

  int N = 10;
  public void testScaleOffset() throws IOException, InvalidRangeException {
    Average all = new Average();
    Average alldefer = new Average();
    Average sm = new Average();
    Average smNoNans = new Average();
    Average coords = new Average();
    Average none = new Average();
    Average file = new Average();

    // warm up
    openDataset(NetcdfDataset.getEnhanceAll(), null);
    openFile(null);

    for (int i=0; i<N; i++) {
      openDataset(NetcdfDataset.getEnhanceAll(), all);
      openDataset(NetcdfDataset.parseEnhanceMode("AllDefer"), alldefer);
      openDataset(NetcdfDataset.parseEnhanceMode("ScaleMissing"), sm);
      NetcdfDataset.setUseNaNs(false);
      openDataset(NetcdfDataset.parseEnhanceMode("ScaleMissing"), smNoNans);
      openDataset(NetcdfDataset.parseEnhanceMode("CoordSystems"), coords);
      openDataset(null, none);
      openFile(file);
    }

    System.out.println(" all enhance="+all);
    System.out.println(" all defer  ="+alldefer);
    System.out.println("scaleMissing="+sm);
    System.out.println(" smNoNans   ="+smNoNans);
    System.out.println(" coords     ="+coords);
    System.out.println(" none       ="+none);
    System.out.println(" open File  ="+file);
  }

  public void openFile(Average avg) throws IOException, InvalidRangeException {
    long start = System.nanoTime();

    NetcdfFile ncfile = NetcdfDataset.openFile(TestDir.cdmUnitTestDir +"ft/grid/netcdf/AZ.000000000.nc", null);
    Variable v = ncfile.findVariable("qc");
    assert null != v;
    assert v.getDataType() == DataType.BYTE;

    Array data = v.read();
    assert data.getElementType() == byte.class;
    double sum = MAMath.sumDoubleSkipMissingData(data, Double.NaN);
    long end = System.nanoTime();
    double took = (double)((end - start))/1000/1000/1000;
    double perelem = (double)((end - start)) / data.getSize();
    //System.out.println(sum+" "+enhance+" took="+took+" secs; size= "+data.getSize()+"; nanosecs/elem= "+perelem);
    ncfile.close();

    if (avg != null) avg.add(took);
  }

  public void openDataset(Set<NetcdfDataset.Enhance> enhance, Average avg) throws IOException, InvalidRangeException {
    long start = System.nanoTime();

    NetcdfDataset ncd = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir +"ft/grid/netcdf/AZ.000000000.nc", enhance, -1, null, null);
    Variable v = ncd.findVariable("qc");
    assert null != v;

    Array data = v.read();
    double sum = MAMath.sumDoubleSkipMissingData(data, Double.NaN);
    long end = System.nanoTime();
    double took = (double)((end - start))/1000/1000/1000;
    double perelem = (double)((end - start)) / data.getSize();
    //System.out.println(sum+" "+enhance+" took="+took+" secs; size= "+data.getSize()+"; nanosecs/elem= "+perelem);
    ncd.close();

    if (avg != null) avg.add(took);
  }

  public void testNaNs() throws IOException, InvalidRangeException {
    NetcdfDataset.setUseNaNs(true);
    NetcdfDataset ncd = NetcdfDataset.openDataset(TestDir.cdmUnitTestDir +"ft/grid/netcdf/AZ.000000000.nc", true, null);
    VariableDS v = (VariableDS) ncd.findVariable("qc");
    assert null != v;

    long start = System.nanoTime();

    int count = 0;
    Array data = v.read();
    while (data.hasNext()) {
      if (v.isMissing( data.nextDouble()))
        count++;
    }
    System.out.println(" missing= "+count);

    long end = System.nanoTime();
    double took = (double)((end - start))/1000/1000/1000;
    double perelem = (double)((end - start)) / data.getSize();
    System.out.println(" took="+took+" secs; size= "+data.getSize()+"; nanosecs/elem= "+perelem);

  }

}
