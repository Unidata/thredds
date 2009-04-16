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

package ucar.nc2.dataset;

import ucar.ma2.InvalidRangeException;
import ucar.ma2.*;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

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

    NetcdfFile ncfile = NetcdfDataset.openFile(TestAll.testdataDir +"grid/netcdf/AZ.000000000.nc", null);
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

    NetcdfDataset ncd = NetcdfDataset.openDataset(TestAll.testdataDir +"grid/netcdf/AZ.000000000.nc", enhance, -1, null, null);
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
    NetcdfDataset ncd = NetcdfDataset.openDataset(TestAll.testdataDir +"grid/netcdf/AZ.000000000.nc", true, null);
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
