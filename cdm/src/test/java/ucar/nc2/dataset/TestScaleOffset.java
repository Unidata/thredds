package ucar.nc2.dataset;

import ucar.nc2.*;
import ucar.ma2.*;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Dec 28, 2008
 * Time: 12:45:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestScaleOffset extends TestCase {
  private String filename = TestAll.cdmTestDataDir +"scaleOffset.nc";

  public TestScaleOffset( String name) {
    super(name);
  }


  public void testWrite() throws Exception {
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename);

     // define dimensions
    Dimension latDim = ncfile.addDimension("lat", 200);
    Dimension lonDim = ncfile.addDimension("lon", 300);
    int n = lonDim.getLength();

    // create an array
    ArrayDouble unpacked = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
    Index ima = unpacked.getIndex();
    for (int i=0; i<latDim.getLength(); i++)
      for (int j=0; j<lonDim.getLength(); j++)
        unpacked.setDouble(ima.set(i,j), (i*n+j)+30.0);

    MAMath.ScaleOffset so = MAMath.calcScaleOffsetSkipMissingData(unpacked, -9999, 15);
    System.out.println("scale/offset = "+so.scale+" "+so.offset);
    Array packed = MAMath.convert2Packed(unpacked, so, DataType.SHORT);

    ncfile.addVariable("unpacked", DataType.DOUBLE, "lat lon");

    ncfile.addVariable("packed", DataType.SHORT, "lat lon");
    ncfile.addVariableAttribute("packed", "_unsigned", "true");
    //ncfile.addVariableAttribute("packed", "missing_value", new Short( (short) -9999));
    ncfile.addVariableAttribute("packed", "scale_factor", so.scale);
    ncfile.addVariableAttribute("packed", "add_offset", so.offset);

    // create the file
    ncfile.create();

    ncfile.write("unpacked", unpacked);
    ncfile.write("packed", packed);

        // all done
    ncfile.close();

    NetcdfFile ncfileRead = NetcdfFile.open(filename);
    Variable v = ncfileRead.findVariable("packed");
    assert v != null;
    Array readPacked = v.read();
    TestCompare.compareData(readPacked, packed);
    ncfileRead.close();

    NetcdfDataset ncd = NetcdfDataset.openDataset(filename);
    Variable vs = ncd.findVariable("packed");
    assert vs != null;
    Array readEnhanced = vs.read();
    //TestCompare.compareData(readEnhanced, unpacked);
    testClose(packed, unpacked, readEnhanced, 1.0/so.scale);

    ncd.close();

    Array cnvertPacked = MAMath.convert2Unpacked(readPacked, so);
    //TestCompare.compareData(readUnpacked, unpacked);
    testClose(packed, cnvertPacked, readEnhanced, 1.0/so.scale);


  }

  void testClose(Array packed, Array data1, Array data2, double close) {
    IndexIterator iterp = packed.getIndexIterator();
    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    while (iter1.hasNext()) {
      double v1 = iter1.getDoubleNext();
      double v2 = iter2.getDoubleNext();
      double p = iterp.getDoubleNext();
      double diff = Math.abs(v1 - v2);
      assert (diff < close) : v1 + " != " + v2 + " count=" + iter1+" packed="+p;
      System.out.println(v1 + " == " + v2 + " count=" + iter1+" packed="+p);
    }
  }
}
