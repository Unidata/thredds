package ucar.nc2.iosp.noaa;

import ucar.ma2.*;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.util.TableParser;

/**
 * Created by IntelliJ IDEA.
 * User: caron
 * Date: Dec 8, 2010
 * Time: 5:23:26 PM
 */
public class StructureDataAscii extends StructureData {
  private String line;

  public StructureDataAscii(StructureMembers members, String line) {
    super(members);
    this.line = line;
  }

  @Override
  public Array getArray(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    if (m.getDataType() == DataType.STRING) {
      String result = (String) f.parse(line);
      return new ArrayObject(String.class, new int[] {},  new Object[] {result.trim()});

    } else if (m.getDataType() == DataType.CHAR) {
      String result = (String) f.parse(line);
      return new ArrayChar(result);

    } else
      return new ArrayScalar(f.parse(line));
  }

  @Override
  public float convertScalarFloat(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    return ((Number) f.parse(line)).floatValue();
  }

  @Override
  public double convertScalarDouble(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    return ((Number) f.parse(line)).doubleValue();
  }

  @Override
  public int convertScalarInt(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    return ((Number) f.parse(line)).intValue();
  }

  @Override
  public double getScalarDouble(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    return (Double) f.parse(line);
  }

  @Override
  public double[] getJavaArrayDouble(StructureMembers.Member m) {
    return new double[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public float getScalarFloat(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    Object result = f.parse(line);
    return (result instanceof Float) ? (Float) f.parse(line) : ((Double) f.parse(line)).floatValue();
  }

  @Override
  public float[] getJavaArrayFloat(StructureMembers.Member m) {
    return new float[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public byte getScalarByte(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    return (Byte) f.parse(line);
  }

  @Override
  public byte[] getJavaArrayByte(StructureMembers.Member m) {
    return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public int getScalarInt(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    return (Integer) f.parse(line);
  }

  @Override
  public int[] getJavaArrayInt(StructureMembers.Member m) {
    return new int[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public short getScalarShort(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    return (Short) f.parse(line);
  }

  @Override
  public short[] getJavaArrayShort(StructureMembers.Member m) {
    return new short[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public long getScalarLong(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    return (Long) f.parse(line);  }

  @Override
  public long[] getJavaArrayLong(StructureMembers.Member m) {
    return new long[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public char getScalarChar(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    String result = (String) f.parse(line);
    return result.charAt(0);
  }

  @Override
  public char[] getJavaArrayChar(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    String result = (String) f.parse(line);
    return IospHelper.convertByteToChar(result.getBytes()); // kinda lame - well just convert back
  }

  @Override
  public String getScalarString(StructureMembers.Member m) {
    TableParser.Field f = (TableParser.Field) m.getDataObject();
    return ((String) f.parse(line)).trim();
  }

  @Override
  public String[] getJavaArrayString(StructureMembers.Member m) {
    return new String[] {getScalarString(m)};
  }

  @Override
  public StructureData getScalarStructure(StructureMembers.Member m) {
    return null;
  }

  @Override
  public ArrayStructure getArrayStructure(StructureMembers.Member m) {
    return null;
  }

  @Override
  public ArraySequence getArraySequence(StructureMembers.Member m) {
    return null;
  }
}
