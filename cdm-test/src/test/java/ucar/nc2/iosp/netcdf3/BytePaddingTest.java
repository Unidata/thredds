package ucar.nc2.iosp.netcdf3;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.TestFileDirUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * testing netcdf3 byte padding
 *
 * @author edavis
 * @since 4.1
 */
public class BytePaddingTest
{

  public static String testDir = TestDir.cdmUnitTestDir + "formats/netcdf3/";

  @Test
  public void checkReadOfFileWrittenWithIncorrectPaddingOfOneDimByteArrayOnlyRecordVar()
          throws IOException, InvalidRangeException
  {
    //File testDataDir = new File( TestDir.cdmLocalTestDataDir, "ucar/nc2/iosp/netcdf3");
    File testFile = new File( TestDir.cdmLocalTestDataDir, "byteArrayRecordVarPaddingTest-bad.nc" );
    assertTrue( testFile.exists());
    assertTrue( testFile.canRead());

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable readVar = ncf.findVariable( "V" );
    assertEquals( readVar.getDataType(), DataType.BYTE );
    assertEquals( 1, readVar.getElementSize() );

    N3header.Vinfo vinfo = (N3header.Vinfo) readVar.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 1, vinfo.vsize );

    Array byteData = readVar.read();

    // File was created with the following data
    //     byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
    // But extra padding (see issue CDM-52) caused each byte to be padded out to 4 bytes.
    assertEquals( 1, byteData.getByte( 0 ));
    assertEquals( 0, byteData.getByte( 1 ));
    assertEquals( 0, byteData.getByte( 2 ));
    assertEquals( 0, byteData.getByte( 3 ));
    assertEquals( 2, byteData.getByte( 4 ));
    assertEquals( 0, byteData.getByte( 5 ));
    assertEquals( 0, byteData.getByte( 6 ));
    assertEquals( 0, byteData.getByte( 7 ));
    assertEquals( 3, byteData.getByte( 8 ));
    assertEquals( 0, byteData.getByte( 9 ));
    assertEquals( 0, byteData.getByte( 10 ));
    assertEquals( 0, byteData.getByte( 11 ));
    assertEquals( 4, byteData.getByte( 12 ));
    assertEquals( 0, byteData.getByte( 13 ));
    assertEquals( 0, byteData.getByte( 14 ));
    assertEquals( 0, byteData.getByte( 15 ));
    assertEquals( 5, byteData.getByte( 16 ));
    assertEquals( 0, byteData.getByte( 17 ));

    try {
      byteData.getByte( 18 );
    } catch ( ArrayIndexOutOfBoundsException e ) {
      return;
    } catch ( Exception e) {
      fail( "Unexpected exception: " + e.getMessage());
      return;
    }
    fail( "Failed to throw expected ArrayIndexOutOfBoundsException.");
  }

  @Test
  public void checkPaddingOnWriteReadOneDimByteArrayOnlyRecordVar()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest_checkPaddingOnWriteReadOneDimByteArrayOnlyRecordVar", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "file.nc" );
    if ( testFile.exists() ) {
      testFile.delete();
    }

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    Dimension recDim = ncfWriteable.addUnlimitedDimension( "v" );
    Variable var = ncfWriteable.addVariable( "v", DataType.BYTE, new Dimension[]{recDim} );
    assertEquals( 1, var.getElementSize() );
    ncfWriteable.create();
    
    N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
    assertTrue( vinfo.isRecord);
    assertEquals( 1, vinfo.vsize );

    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
    Array dataArray = Array.factory( DataType.BYTE, new int[]{data.length}, data );
    ncfWriteable.write( var.getFullNameEscaped(), dataArray );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable readVar = ncf.findVariable( "v" );
    assertEquals( readVar.getDataType(), DataType.BYTE );
    assertEquals( 1, readVar.getElementSize() );

    vinfo = (N3header.Vinfo) readVar.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 1, vinfo.vsize );

    int[] org = {0};
    byte[] readdata = (byte[]) readVar.read( org, readVar.getShape() ).copyTo1DJavaArray();

    assertArrayEquals( data, readdata );
    ncf.close();
  }

  @Test
  public void checkPaddingOnWriteReadOneDimByteArrayOneOfTwoRecordVars()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest_checkPaddingOnWriteReadOneDimByteArrayOneOfTwoRecordVars", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "file.nc" );
    if ( testFile.exists() ) {
      testFile.delete();
    }

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    Dimension recDim = ncfWriteable.addUnlimitedDimension( "v" );
    Variable var = ncfWriteable.addVariable( "v", DataType.BYTE, new Dimension[]{recDim} );
    assertEquals( 1, var.getElementSize() );
    Variable var2 = ncfWriteable.addVariable( "v2", DataType.BYTE, new Dimension[]{recDim});
    ncfWriteable.create();

    N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
    assertTrue( vinfo.isRecord);
    assertEquals( 4, vinfo.vsize );

    vinfo = (N3header.Vinfo) var2.getSPobject();
    assertTrue( vinfo.isRecord);
    assertEquals( 4, vinfo.vsize );

    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
    Array dataArray = Array.factory( DataType.BYTE, new int[]{data.length}, data );
    ncfWriteable.write( var.getFullNameEscaped(), dataArray );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable readVar = ncf.findVariable( "v" );
    assertEquals( readVar.getDataType(), DataType.BYTE );
    assertEquals( 1, readVar.getElementSize() );

    vinfo = (N3header.Vinfo) readVar.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    Variable readVar2 = ncf.findVariable( "v2") ;
    assertEquals( readVar2.getDataType(), DataType.BYTE );
    assertEquals( 1, readVar2.getElementSize() );

    vinfo = (N3header.Vinfo) readVar2.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    int[] org = {0};
    byte[] readdata = (byte[]) readVar.read( org, readVar.getShape() ).copyTo1DJavaArray();

    assertArrayEquals( data, readdata );
    ncf.close();
  }

  @Test
  public void checkPaddingOnWriteReadTwoDimByteArrayOnlyRecordVar()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest_checkPaddingOnWriteReadTwoDimByteArrayOnlyRecordVar", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "file.nc" );
    if ( testFile.exists() ) {
      testFile.delete();
    }

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    Dimension recDim = ncfWriteable.addUnlimitedDimension( "v" );
    Dimension secondDim = ncfWriteable.addDimension( "s", 3 );
    Variable var = ncfWriteable.addVariable( "v", DataType.BYTE, new Dimension[]{recDim,secondDim} );
    assertEquals( 1, var.getElementSize() );
    ncfWriteable.create();

    N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
    assertTrue( vinfo.isRecord);
    assertEquals( 3, vinfo.vsize );

    byte[] data = {1, 2, 3, 11, 12, 13, 21, 22, 23, -1, -2, -3};
    Array dataArray = Array.factory( DataType.BYTE, new int[]{4,3}, data );
    ncfWriteable.write( var.getFullNameEscaped(), dataArray );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable readVar = ncf.findVariable( "v" );
    assertEquals( readVar.getDataType(), DataType.BYTE );
    assertEquals( 1, readVar.getElementSize() );

    vinfo = (N3header.Vinfo) readVar.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 3, vinfo.vsize );

    int[] org = {0,0};
    byte[] readdata = (byte[]) readVar.read( org, readVar.getShape() ).copyTo1DJavaArray();

    assertArrayEquals( data, readdata );
    ncf.close();
  }

  @Test
  public void checkPaddingOnWriteReadTwoDimByteArrayOneOfTwoRecordVars()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest_checkPaddingOnWriteReadTwoDimByteArrayOneOfTwoRecordVar", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "file.nc" );
    if ( testFile.exists() ) {
      testFile.delete();
    }

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    Dimension recDim = ncfWriteable.addUnlimitedDimension( "v" );
    Dimension secondDim = ncfWriteable.addDimension( "s", 3 );
    Variable var = ncfWriteable.addVariable( "v", DataType.BYTE, new Dimension[]{recDim, secondDim} );
    assertEquals( 1, var.getElementSize() );
    Variable var2 = ncfWriteable.addVariable( "v2", DataType.BYTE, new Dimension[]{recDim} );
    ncfWriteable.create();

    N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    vinfo = (N3header.Vinfo) var2.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    byte[] data = {1, 2, 3, 11, 12, 13, 21, 22, 23, -1, -2, -3};
    Array dataArray = Array.factory( DataType.BYTE, new int[]{4, 3}, data );
    ncfWriteable.write( var.getFullNameEscaped(), dataArray );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable readVar = ncf.findVariable( "v" );
    assertEquals( readVar.getDataType(), DataType.BYTE );
    assertEquals( 1, readVar.getElementSize() );
    Variable readVar2 = ncf.findVariable( "v2" );
    assertEquals( readVar2.getDataType(), DataType.BYTE );
    assertEquals( 1, readVar2.getElementSize() );

    vinfo = (N3header.Vinfo) readVar.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    vinfo = (N3header.Vinfo) readVar2.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    int[] org = {0, 0};
    byte[] readdata = (byte[]) readVar.read( org, readVar.getShape() ).copyTo1DJavaArray();

    assertArrayEquals( data, readdata );
    ncf.close();
  }

  @Test
  public void checkPaddingOnWriteReadOneDimCharArrayOnlyRecordVar()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest_checkPaddingOnWriteReadOneDimCharArrayOnlyRecordVar", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "file.nc" );
    if ( testFile.exists() )
    {
      testFile.delete();
    }

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    Dimension recDim = ncfWriteable.addUnlimitedDimension( "v" );
    Variable var = ncfWriteable.addVariable( "v", DataType.CHAR, new Dimension[]{recDim} );
    assertEquals( 1, var.getElementSize() );
    ncfWriteable.create();

    N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 1, vinfo.vsize );

    char[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50};
    Array dataArray = Array.factory( DataType.CHAR, new int[]{data.length}, data );
    ncfWriteable.write( var.getFullNameEscaped(), dataArray );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable readVar = ncf.findVariable( "v" );
    assertEquals( readVar.getDataType(), DataType.CHAR );
    assertEquals( 1, readVar.getElementSize() );

    vinfo = (N3header.Vinfo) readVar.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 1, vinfo.vsize );

    int[] org = {0};
    char[] readdata = (char[]) readVar.read( org, readVar.getShape() ).copyTo1DJavaArray();

    assertArrayEquals( data, readdata );
    ncf.close();
  }

  @Test
  public void checkPaddingOnWriteReadOneDimCharArrayOneOfTwoRecordVars()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest_checkPaddingOnWriteReadOneDimCharArrayOneOfTwoRecordVar", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "file.nc" );
    if ( testFile.exists() )
    {
      testFile.delete();
    }

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    Dimension recDim = ncfWriteable.addUnlimitedDimension( "v" );
    Dimension secondDim = ncfWriteable.addDimension( "s", 3 );
    Variable var = ncfWriteable.addVariable( "v", DataType.CHAR, new Dimension[]{recDim, secondDim} );
    assertEquals( 1, var.getElementSize() );
    Variable var2 = ncfWriteable.addVariable( "v2", DataType.CHAR, new Dimension[]{recDim} );
    assertEquals( 1, var2.getElementSize() );
    ncfWriteable.create();

    N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    vinfo = (N3header.Vinfo) var2.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    char[] data = {1, 2, 3, 40, 41, 42, 50, 51, 52, 60, 61, 62};
    Array dataArray = Array.factory( DataType.CHAR, new int[]{4,3}, data );
    ncfWriteable.write( var.getFullNameEscaped(), dataArray );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable readVar = ncf.findVariable( "v" );
    assertEquals( readVar.getDataType(), DataType.CHAR );
    assertEquals( 1, readVar.getElementSize() );

    vinfo = (N3header.Vinfo) readVar.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    Variable readVar2 = ncf.findVariable( "v2" );
    assertEquals( readVar2.getDataType(), DataType.CHAR );
    assertEquals( 1, readVar2.getElementSize() );

    vinfo = (N3header.Vinfo) readVar2.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    int[] org = {0, 0};
    char[] readdata = (char[]) readVar.read( org, readVar.getShape() ).copyTo1DJavaArray();

    assertArrayEquals( data, readdata );
    ncf.close();
  }

  @Test
  public void checkPaddingOnWriteReadOneDimShortArrayOnlyRecordVar()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest_checkPaddingOnWriteReadOneDimShortArrayOnlyRecordVar", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "file.nc" );
    if ( testFile.exists() )
    {
      testFile.delete();
    }

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    Dimension recDim = ncfWriteable.addUnlimitedDimension( "v" );
    Variable var = ncfWriteable.addVariable( "v", DataType.SHORT, new Dimension[]{recDim} );
    assertEquals( 2, var.getElementSize() );
    ncfWriteable.create();

    N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 2, vinfo.vsize );

    short[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
    Array dataArray = Array.factory( DataType.SHORT, new int[]{data.length}, data );
    ncfWriteable.write( var.getFullNameEscaped(), dataArray );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable readVar = ncf.findVariable( "v" );
    assertEquals( readVar.getDataType(), DataType.SHORT );
    assertEquals( 2, readVar.getElementSize() );

    vinfo = (N3header.Vinfo) readVar.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 2, vinfo.vsize );

    int[] org = {0};
    short[] readdata = (short[]) readVar.read( org, readVar.getShape() ).copyTo1DJavaArray();

    assertArrayEquals( data, readdata );
    ncf.close();
  }

  @Test
  public void checkPaddingOnWriteReadOneDimShortArrayOneOfTwoRecordVars()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest_checkPaddingOnWriteReadOneDimShortArrayOneOfTwoRecordVar", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "file.nc" );
    if ( testFile.exists() )
    {
      testFile.delete();
    }

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    Dimension recDim = ncfWriteable.addUnlimitedDimension( "v" );
    Dimension secondDim = ncfWriteable.addDimension( "s", 3 );
    Variable var = ncfWriteable.addVariable( "v", DataType.SHORT, new Dimension[]{recDim, secondDim} );
    assertEquals( 2, var.getElementSize() );
    Variable var2 = ncfWriteable.addVariable( "v2", DataType.SHORT, new Dimension[]{recDim} );
    assertEquals( 2, var2.getElementSize() );
    ncfWriteable.create();

    N3header.Vinfo vinfo = (N3header.Vinfo) var.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 8, vinfo.vsize );

    vinfo = (N3header.Vinfo) var2.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    short[] data = {1, 2, 3, 10, 11, 12, -1, -2, -3, -7, -8, -9};
    Array dataArray = Array.factory( DataType.SHORT, new int[]{4,3}, data );
    ncfWriteable.write( var.getFullNameEscaped(), dataArray );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable readVar = ncf.findVariable( "v" );
    assertEquals( readVar.getDataType(), DataType.SHORT );
    assertEquals( 2, readVar.getElementSize() );

    vinfo = (N3header.Vinfo) readVar.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 8, vinfo.vsize );

    Variable readVar2 = ncf.findVariable( "v2" );
    assertEquals( readVar2.getDataType(), DataType.SHORT );
    assertEquals( 2, readVar2.getElementSize() );

    vinfo = (N3header.Vinfo) readVar2.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 4, vinfo.vsize );

    int[] org = {0, 0};
    short[] readdata = (short[]) readVar.read( org, readVar.getShape() ).copyTo1DJavaArray();

    assertArrayEquals( data, readdata );
    ncf.close();
  }

  @Test
  public void checkPaddingOnWriteReadOriginalByteArrayPaddingTest()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestDir.temporaryLocalDataDir );
    File tmpDataDir = TestFileDirUtils.createTempDirectory( "BytePaddingTest_writeReadOriginalByteArrayPaddingTest", tmpDataRootDir );
    File testFile = new File( tmpDataDir, "file.nc" );
    if ( testFile.exists() ) {
      testFile.delete();
    }

    NetcdfFileWriteable ncfWriteable = NetcdfFileWriteable.createNew( testFile.getPath() );
    Dimension d0 = ncfWriteable.addDimension( "X", 5 );
    Dimension d = ncfWriteable.addUnlimitedDimension( "D" );
    Variable v0 = ncfWriteable.addVariable( "X", DataType.DOUBLE, new Dimension[]{d0} );
    Variable v = ncfWriteable.addVariable( "V", DataType.BYTE, new Dimension[]{d} );
    assertEquals( 1, v.getElementSize() );
    ncfWriteable.create();

    N3header.Vinfo vinfo = (N3header.Vinfo) v.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 1, vinfo.vsize );

    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
    Array arr = Array.factory( DataType.BYTE, new int[]{data.length}, data );
    ncfWriteable.write( v.getFullNameEscaped(), arr );
    ncfWriteable.close();

    NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    Variable inv = ncf.findVariable( "V" );
    assertEquals( inv.getDataType(), DataType.BYTE );
    assertEquals( 1, inv.getElementSize() );

    vinfo = (N3header.Vinfo) inv.getSPobject();
    assertTrue( vinfo.isRecord );
    assertEquals( 1, vinfo.vsize );

    int[] org = {0};
    byte[] readdata = (byte[]) inv.read( org, inv.getShape() ).copyTo1DJavaArray();

    assertArrayEquals( data, readdata );
    ncf.close();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void checkReadWithPaddingInVsize() throws IOException, InvalidRangeException
  {
    File dataFile = new File( testDir, "files/tst_small.nc");
    NetcdfFile ncFile = NetcdfFile.open( dataFile.getPath(), null );
    Variable readVar = ncFile.findVariable( "Times" );

    assertDataAsExpected( readVar );

    ncFile.close();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void checkReadWithoutPaddingInVsize() throws IOException, InvalidRangeException
  {
    File dataFile = new File( testDir, "files/tst_small_withoutPaddingInVsize.nc");
    NetcdfFile ncFile = NetcdfFile.open( dataFile.getPath(), null );
    Variable readVar = ncFile.findVariable( "Times" );

    assertDataAsExpected( readVar );

    ncFile.close();
  }

  private void assertDataAsExpected( Variable var )
          throws IOException
  {
    ArrayChar cdata = (ArrayChar) var.read();

    assert cdata.getString( 0 ).equals( "2005-04-11_12:00:00" );
    assert cdata.getString( 1 ).equals( "2005-04-11_13:00:00" );
  }
}
