/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Formatter;

/** Test TestStandardVar in JUnit framework. */

public class TestStandardVar extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private String filename = TestDir.cdmLocalTestDataDir +"standardVar.nc";

  public TestStandardVar( String name) {
    super(name);
  }

  public void testWriteStandardVar() throws Exception {
    NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(filename, false);

    // define dimensions
    Dimension latDim = ncfile.addDimension("lat", 2);
    Dimension lonDim = ncfile.addDimension("lon", 3);

    ArrayList dims = new ArrayList();
    dims.add(latDim);
    dims.add(lonDim);

    // case 1
    ncfile.addVariable("t1", DataType.DOUBLE, dims);
    ncfile.addVariableAttribute("t1", CDM.SCALE_FACTOR, new Double(2.0));
    ncfile.addVariableAttribute("t1", "add_offset", new Double(77.0));

    // case 2
    ncfile.addVariable("t2", DataType.BYTE, dims);
    ncfile.addVariableAttribute("t2", CDM.SCALE_FACTOR, new Short( (short) 2));
    ncfile.addVariableAttribute("t2", "add_offset", new Short( (short) 77));

    // case 3
    ncfile.addVariable("t3", DataType.BYTE, dims);
    ncfile.addVariableAttribute("t3", "_FillValue", new Byte( (byte) 255));

    // case 4
    ncfile.addVariable("t4", DataType.SHORT, dims);
    ncfile.addVariableAttribute("t4", CDM.MISSING_VALUE, new Short( (short) -9999));

    // case 5
    ncfile.addVariable("t5", DataType.SHORT, dims);
    ncfile.addVariableAttribute("t5", CDM.MISSING_VALUE, new Short( (short) -9999));
    ncfile.addVariableAttribute("t5", CDM.SCALE_FACTOR, new Short( (short) 2));
    ncfile.addVariableAttribute("t5", "add_offset", new Short( (short) 77));

    // case 1
    ncfile.addVariable("m1", DataType.DOUBLE, dims);
    ncfile.addVariableAttribute("m1", CDM.MISSING_VALUE, -999.99);


    // create the file
    ncfile.create();

    // write t1
    ArrayDouble A = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
    int i,j;
    Index ima = A.getIndex();
    // write
    for (i=0; i<latDim.getLength(); i++)
      for (j=0; j<lonDim.getLength(); j++)
        A.setDouble(ima.set(i,j), (double) (i*10.0+j));
    int[] origin = new int[2];
    ncfile.write("t1", origin, A);

    // write t2
    ArrayByte Ab = new ArrayByte.D2(latDim.getLength(), lonDim.getLength(), false);
    ima = Ab.getIndex();
    for (i=0; i<latDim.getLength(); i++)
      for (j=0; j<lonDim.getLength(); j++)
        Ab.setByte(ima.set(i,j), (byte) (i*10+j));
    ncfile.write("t2", origin, Ab);

    // write t3
    ncfile.write("t3", origin, Ab);


     // write t4
    Array As = new ArrayShort.D2(latDim.getLength(), lonDim.getLength(), false);
    ima = As.getIndex();
    for (i=0; i<latDim.getLength(); i++)
      for (j=0; j<lonDim.getLength(); j++)
        As.setShort(ima.set(i,j), (short) (i*10+j));
    ncfile.write("t4", origin, As);

    As.setShort(ima.set(0, 0), (short) -9999);
    ncfile.write("t5", origin, As);

    // write m1
    ArrayDouble.D2 Ad = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
    for (i=0; i<latDim.getLength(); i++)
      for (j=0; j<lonDim.getLength(); j++)
        Ad.setDouble(ima.set(i,j), (double) (i*10.0+j));
    Ad.set(1,1,-999.99);
    ncfile.write("m1", new int[2], Ad);

    // all done
    ncfile.close();

    System.out.println( "**************TestStandardVar Write done");
  }

  private NetcdfFile ncfileRead;
  private NetcdfDataset dsRead;
  public void testReadStandardVar() throws Exception {
    ncfileRead = NetcdfFile.open(filename);
    dsRead = NetcdfDataset.openDataset(filename);

    readDouble();
    readByte2Short();
    readByte();
    readShortMissing();
    readShort2FloatMissing();

    readDoubleMissing();

    ncfileRead.close();
    dsRead.close();
  }

  public void readDouble() throws Exception {

    Variable t1 = null;
    assert(null != (t1 = ncfileRead.findVariable("t1")));
    assert( t1.getDataType() == DataType.DOUBLE);

    Attribute att = t1.findAttribute(CDM.SCALE_FACTOR);
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 2.0 == att.getNumericValue().doubleValue());
    assert( DataType.DOUBLE == att.getDataType());

    // read
    Array A = t1.read();
    int i,j;
    Index ima = A.getIndex();
    int[] shape = A.getShape();

    for (i=0; i<shape[0]; i++) {
      for (j=0; j<shape[1]; j++) {
        assert( A.getDouble(ima.set(i,j)) == (double) (i*10.0+j));
      }
    }

    assert(null != (t1 = dsRead.findVariable("t1")));
    assert t1 instanceof VariableEnhanced;
    VariableEnhanced dsVar = (VariableEnhanced) t1;
    assert( dsVar.getDataType() == DataType.DOUBLE);

    A = dsVar.read();
    ima = A.getIndex();
    shape = A.getShape();

    for (i=0; i<shape[0]; i++) {
      for (j=0; j<shape[1]; j++) {
        assert( A.getDouble(ima.set(i,j)) == (2.0 * (i*10.0+j) + 77.0));
      }
    }

    assert( null == t1.findAttribute(CDM.SCALE_FACTOR));
    assert( null == t1.findAttribute("add_offset"));

    System.out.println( "**************TestStandardVar ReadDouble");
  }

 public void readByte2Short() throws Exception {
    Variable t2 = null;
    assert(null != (t2 = ncfileRead.findVariable("t2")));
    assert( t2.getDataType() == DataType.BYTE);

    Attribute att = t2.findAttribute(CDM.SCALE_FACTOR);
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 2 == att.getNumericValue().doubleValue());
    assert( DataType.SHORT == att.getDataType());

    assert(null != (t2 = dsRead.findVariable("t2")));
    assert t2 instanceof VariableEnhanced;
    VariableDS vs = (VariableDS) t2;
    assert( vs.getDataType() == DataType.FLOAT) : vs.getDataType();
    assert( vs.hasMissing());

    Array A = vs.read();
    assert( A.getElementType() == float.class) : A.getElementType();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    int i,j;
    for (i=0; i<shape[0]; i++) {
      for (j=0; j<shape[1]; j++) {
        assert( A.getShort(ima.set(i,j)) == (2 * (i*10+j) + 77));
      }
    }
    System.out.println( "**************TestStandardVar readByte2Short");
  }

  public void readByte() throws Exception  {
    Variable v = null;
    assert(null != (v = ncfileRead.findVariable("t3")));
    assert( v.getDataType() == DataType.BYTE);

    assert(null != (v = dsRead.findVariable("t3")));
    assert v instanceof VariableEnhanced;
    assert v instanceof VariableDS;
    VariableDS vs = (VariableDS) v;
    assert( vs.getDataType() == DataType.BYTE);

    Attribute att = vs.findAttribute("_FillValue");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    System.out.println("_FillValue = "+att.getNumericValue().byteValue());
    assert( ((byte) 255) == att.getNumericValue().byteValue());
    assert( DataType.BYTE == att.getDataType());

    assert( vs.hasMissing());
    assert( vs.hasFillValue());
    assert( vs.isMissing( (double) ((byte) 255)));
    assert( vs.isFillValue( (double) ((byte) 255)));

    Array A = vs.read();
    assert( A.getElementType() == byte.class) : A.getElementType();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    int i,j;
    for (i=0; i<shape[0]; i++) {
      for (j=0; j<shape[1]; j++) {
        assert( A.getFloat(ima.set(i,j)) == (i*10+j));
      }
    }
    System.out.println( "**************TestStandardVar ReadByte");
  }

  public void readShortMissing() throws Exception {
    Variable v = null;
    assert(null != (v = ncfileRead.findVariable("t4")));
    assert( v.getDataType() == DataType.SHORT);

    // default use of missing_value
    assert(null != (v = dsRead.findVariable("t4")));
    assert v instanceof VariableEnhanced;
    assert v instanceof VariableDS;
    VariableDS vs = (VariableDS) v;
    assert( vs.getDataType() == DataType.SHORT);

    Attribute att = vs.findAttribute(CDM.MISSING_VALUE);
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    System.out.println("missing_value = "+att.getNumericValue().shortValue());
    assert( ((short) -9999) == att.getNumericValue().shortValue());
    assert( DataType.SHORT == att.getDataType());

    assert( vs.hasMissing());
    assert( vs.hasMissingValue());
    assert( vs.isMissing( (double) ((short) -9999)));
    assert( vs.isMissingValue( (double) ((short) -9999)));

    Array A = vs.read();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    int i,j;
    for (i=0; i<shape[0]; i++) {
      for (j=0; j<shape[1]; j++) {
        assert( A.getFloat(ima.set(i,j)) == (i*10+j));
      }
    }

    // turn off missing data
    vs.setMissingDataIsMissing( false);
    vs.setFillValueIsMissing(false);
    assert( vs.getDataType() == DataType.SHORT);

    assert( !vs.hasMissing());
    assert( vs.hasMissingValue());
    assert( !vs.isMissing( (double) ((short) -9999)));
    assert( vs.isMissingValue( (double) ((short) -9999)));

    vs.setMissingDataIsMissing(true);
    assert( vs.hasMissing());
    assert( vs.isMissing( (double) ((short) -9999)));

    System.out.println( "**************TestStandardVar Read readShortMissing");
  }


  public void readShort2FloatMissing() throws Exception {
    Variable v = null;
    assert(null != (v = ncfileRead.findVariable("t5")));
    assert( v.getDataType() == DataType.SHORT);

    // standard convert with missing data
    assert(null != (v = dsRead.findVariable("t5")));
    assert v instanceof VariableEnhanced;
    assert v instanceof VariableDS;
    VariableDS vs = (VariableDS) v;
    assert( vs.getDataType() == DataType.FLOAT);

    assert( vs.hasMissing());
    assert( vs.hasMissingValue());
    double mv = 2 * (-9999) + 77;
    assert( vs.isMissing( mv));
    assert( vs.isMissingValue( mv));

    Array A = vs.read();
    Index ima = A.getIndex();
    int[] shape = A.getShape();
    int i,j;

    assert (vs.isMissing(A.getFloat(ima.set(0,0))));

    for (i=0; i<shape[0]; i++) {
      for (j=1; j<shape[1]; j++) {
        float val = A.getFloat(ima.set(i,j));
        float want = 2* (i*10+j) + 77;
        if( val != want)
          System.out.println(i+" "+j+" "+val+" "+ want);
        assert( val == want);
      }
    }

    // useNaNs
    vs.setUseNaNs(true);
    assert( vs.getDataType() == DataType.FLOAT);

    assert( vs.hasMissing());
    assert( vs.hasMissingValue());
    double mv2 = 2 * (-9999) + 77;
    assert( vs.isMissing( mv2));
    assert( vs.isMissingValue( mv2));

    Array A2 = vs.read();
    Index ima2 = A2.getIndex();
    int[] shape2 = A2.getShape();

    double mval = A2.getFloat(ima2.set(0,0));
    assert vs.isMissing(mval);
    assert Double.isNaN(mval);

    for (i=0; i<shape2[0]; i++) {
      for (j=1; j<shape2[1]; j++) {
        float val = A2.getFloat(ima2.set(i,j));
        float want = 2* (i*10+j) + 77;
        if( val != want)
          System.out.println(i+" "+j+" "+val+" "+ want);
        assert( val == want) : val+" != "+ want;
      }
    }

    assert( null == vs.findAttribute(CDM.SCALE_FACTOR));
    assert( null == vs.findAttribute("add_offset"));
    assert( null == vs.findAttribute(CDM.MISSING_VALUE));

    System.out.println( "**************TestStandardVar Read readShort2FloatMissing");
  }

  public void readDoubleMissing() throws Exception {
    VariableDS v = null;
    assert( null != (v = (VariableDS) dsRead.findVariable("m1")));
    assert( v.getDataType() == DataType.DOUBLE);

    Array A = v.read();
    Index ima = A.getIndex();

    double val = A.getFloat(ima.set(1,1));
    assert Double.isNaN(val);
    assert v.isMissing(val);

    // reread with useNans off
    v.setUseNaNs(false);
    v.createNewCache();
    A = v.read();
    ima = A.getIndex();

    val = A.getFloat(ima.set(1,1));
    assert Misc.nearlyEquals(val, -999.99) : val;
    assert v.isMissing(val);
  }

  public void testEnhanceDefer() throws IOException {
    DatasetUrl durl = new DatasetUrl(null, filename);

    NetcdfDataset ncd = NetcdfDataset.openDataset(durl, EnumSet.of(NetcdfDataset.Enhance.ScaleMissing), -1, null, null);
    VariableDS enhancedVar = (VariableDS) ncd.findVariable("t1");

    NetcdfDataset ncdefer = NetcdfDataset.openDataset(durl, EnumSet.of(NetcdfDataset.Enhance.ScaleMissingDefer), -1, null, null);
    VariableDS deferVar = (VariableDS) ncdefer.findVariable("t1");

    Array data = enhancedVar.read();
    Array dataDefer =  deferVar.read();

    System.out.printf("Enhanced=");
    logger.debug(NCdumpW.toString(data));
    System.out.printf("%nDeferred=");
    logger.debug(NCdumpW.toString(dataDefer));
    System.out.printf("%nProcessed=");

    CompareNetcdf2 nc = new CompareNetcdf2(new Formatter(System.out), false, false, true);
    assert !nc.compareData(enhancedVar.getShortName(), data, dataDefer, false);

    IndexIterator ii = dataDefer.getIndexIterator();
    while (ii.hasNext()) {
      double val = deferVar.convertScaleOffsetMissing(ii.getDoubleNext());
      ii.setDoubleCurrent(val);
    }
    logger.debug(NCdumpW.toString(dataDefer));

    assert nc.compareData(enhancedVar.getShortName(), data, dataDefer, false);

    ncd.close();
    ncdefer.close();
  }

  // for jon blower
  private Array getEnhancedArray(VariableDS vds) throws IOException {
    Array data = vds.read();
    EnumSet<NetcdfDataset.Enhance> mode = vds.getEnhanceMode();
    if (mode.contains(NetcdfDataset.Enhance.ScaleMissing))
      return data;
    if (!mode.contains(NetcdfDataset.Enhance.ScaleMissingDefer))
      throw new IllegalStateException("Must include "+NetcdfDataset.Enhance.ScaleMissingDefer);

    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      double val = vds.convertScaleOffsetMissing(ii.getDoubleNext());
      ii.setDoubleCurrent(val);
    }
    return data;
  }





}
