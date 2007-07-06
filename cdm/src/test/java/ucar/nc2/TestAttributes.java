package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;

import java.io.IOException;

/** Test reading attributes */

public class TestAttributes extends TestCase {

  public TestAttributes( String name) {
    super(name);
  }

  public void testNC3ReadAttributes() throws IOException {
    NetcdfFile ncfile = TestLocalNC2.openFile("testWrite.nc");

    // global attributes
    assert("face".equals(ncfile.findAttValueIgnoreCase(null, "yo", "barf")));

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));
    assert("K".equals(ncfile.findAttValueIgnoreCase(temp, "units", "barf")));

    Attribute att = temp.findAttribute("scale");
    assert( null != att);
    assert( att.isArray());
    assert( 3 == att.getLength());
    assert( 3 == att.getNumericValue(2).intValue());

    Array aa = att.getValues();
    assert(att.getDataType() == DataType.INT);
    assert(aa.getElementType() == int.class);
    assert(aa.getSize() == 3);

    att = temp.findAttribute("versionD");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1.2 == att.getNumericValue().doubleValue());
    assert( DataType.DOUBLE == att.getDataType());

    aa = att.getValues();
    assert(att.getDataType() == DataType.DOUBLE);
    assert(aa.getElementType() == double.class);
    assert(aa.getSize() == 1);

    att = temp.findAttribute("versionF");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1.2f == att.getNumericValue().floatValue());
    assert( close(1.2, att.getNumericValue().doubleValue()));
    assert( DataType.FLOAT == att.getDataType());

    aa = att.getValues();
    assert(att.getDataType() == DataType.FLOAT);
    assert(aa.getElementType() == float.class);
    assert(aa.getSize() == 1);

    att = temp.findAttribute("versionI");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 1 == att.getNumericValue().intValue());
    assert( DataType.INT == att.getDataType());

    aa = att.getValues();
    assert(att.getDataType() == DataType.INT);
    assert(aa.getElementType() == int.class);
    assert(aa.getSize() == 1);

    att = temp.findAttribute("versionS");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 2 == att.getNumericValue().shortValue());
    assert( DataType.SHORT == att.getDataType());

    aa = att.getValues();
    assert(att.getDataType() == DataType.SHORT);
    assert(aa.getElementType() == short.class);
    assert(aa.getSize() == 1);

    att = temp.findAttribute("versionB");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( 3 == att.getNumericValue().byteValue());
    assert( DataType.BYTE == att.getDataType());

    aa = att.getValues();
    assert(att.getDataType() == DataType.BYTE);
    assert(aa.getElementType() == byte.class);
    assert(aa.getSize() == 1);

    att = temp.findAttribute("versionString");
    assert( null != att);
    assert( !att.isArray());
    assert( 1 == att.getLength());
    assert( DataType.STRING == att.getDataType());

    Number n = att.getNumericValue();
    assert (n == null);

    ncfile.close();
  }

  boolean close( double d1, double d2) {
    //System.out.println(d1+" "+d2);
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }


}
