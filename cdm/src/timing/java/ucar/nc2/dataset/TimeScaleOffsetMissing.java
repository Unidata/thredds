/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.nc2.dataset;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.*;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.EnumSet;

import junit.framework.TestCase;
import timing.Average;

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

    NetcdfFile ncfile = NetcdfDataset.openFile(TestAll.upcShareTestDataDir +"grid/netcdf/AZ.000000000.nc", null);
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

  public void openDataset(EnumSet<NetcdfDataset.Enhance> enhance, Average avg) throws IOException, InvalidRangeException {
    long start = System.nanoTime();

    NetcdfDataset ncd = NetcdfDataset.openDataset(TestAll.upcShareTestDataDir +"grid/netcdf/AZ.000000000.nc", enhance, -1, null, null);
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
    NetcdfDataset ncd = NetcdfDataset.openDataset(TestAll.upcShareTestDataDir +"grid/netcdf/AZ.000000000.nc", true, null);
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
