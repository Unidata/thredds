package ucar.nc2.iosp.netcdf3;

import org.junit.Test;
import static org.junit.Assert.*;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.unidata.util.TestFileDirUtils;

import java.io.File;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class BytePaddingTest
{
  @Test
  public void writeReadByteArrayWithUnlimitedDimension()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "byteArrayWithUnlimitedDimensions.nc" );
    if ( testFile.exists() ) {
      testFile.delete();
    }
    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    //Dimension d = ncfWriteable.addDimension("D", data.length);
    Dimension d0 = ncfWriteable.addDimension( "X", 5 );
    Dimension d = ncfWriteable.addUnlimitedDimension( "D" );
    //Dimension d = ncfWriteable.addDimension( "D", 18 );
    Variable v0 = ncfWriteable.addVariable( "X", DataType.DOUBLE, new Dimension[]{d0} );
    Variable v = ncfWriteable.addVariable( "V", DataType.BYTE, new Dimension[]{d} );
    assertEquals( 1, v.getElementSize() );
    ncfWriteable.create();

    Array arr = Array.factory( DataType.BYTE, new int[]{data.length}, data );
    ncfWriteable.write( v.getName(), arr );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable inv = ncf.findVariable( "V" );
    assertEquals( inv.getDataType(), DataType.BYTE );
    int[] org = {0};
    byte[] readdata = (byte[]) inv.read( org, inv.getShape() ).copyTo1DJavaArray();

    assertEquals( 1, inv.getElementSize() );
    assertArrayEquals( data, readdata );
    // this test passes, but ncdump shows improper zero-padding 
  }
}
