package ucar.nc2.iosp.netcdf3;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

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
  public void checkReadOfFileWrittenWithIncorrectPaddingOfOneDimByteArrayOnlyRecordVar()
  {
    File testDataDir = new File( TestAll.cdmLocalTestDataDir, "ucar/nc2/iosp/netcdf3" );
    File testFile = new File( testDataDir, "byteArrayRecordVarPaddingTest-bad.nc" );
    assertTrue( testFile.exists());
    assertTrue( testFile.canRead());

    try {
      NetcdfFile ncf = NetcdfFile.open( testFile.getPath() );
    } catch ( IOException e ) {
      assertTrue( e.getMessage().contains( "file written with incorrect padding for record variable (CDM-52)" ));
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimByteArrayOnlyRecordVar()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
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
    ncfWriteable.write( var.getName(), dataArray );
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
  }

  @Test
  public void checkPaddingOnWriteReadOneDimByteArrayOneOfTwoRecordVars()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
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
    ncfWriteable.write( var.getName(), dataArray );
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
  }

  @Test
  public void checkPaddingOnWriteReadTwoDimByteArrayOnlyRecordVar()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
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
    ncfWriteable.write( var.getName(), dataArray );
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
  }

  @Test
  public void checkPaddingOnWriteReadTwoDimByteArrayOneOfTwoRecordVars()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
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
    ncfWriteable.write( var.getName(), dataArray );
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
  }

  @Test
  public void checkPaddingOnWriteReadOneDimCharArrayOnlyRecordVar()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
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
    ncfWriteable.write( var.getName(), dataArray );
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
  }

  @Test
  public void checkPaddingOnWriteReadOneDimCharArrayOneOfTwoRecordVars()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
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
    ncfWriteable.write( var.getName(), dataArray );
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
  }

  @Test
  public void checkPaddingOnWriteReadOneDimShortArrayOnlyRecordVar()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
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
    ncfWriteable.write( var.getName(), dataArray );
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
  }

  @Test
  public void checkPaddingOnWriteReadOneDimShortArrayOneOfTwoRecordVars()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
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
    ncfWriteable.write( var.getName(), dataArray );
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
  }

  @Test
  public void checkPaddingOnWriteReadOriginalByteArrayPaddingTest()
          throws IOException, InvalidRangeException
  {
    File tmpDataRootDir = new File( TestAll.temporaryLocalDataDir );
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
    ncfWriteable.write( v.getName(), arr );
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
  }
}
