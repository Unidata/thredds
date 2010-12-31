package ucar.ma2;

import net.jcip.annotations.Immutable;

/**
 * Helper class for StructureDataAscii
 */
@Immutable
public class ArrayScalar extends Array {
  private final Object value;

  public ArrayScalar(Object value) {
    super(new int [] {});
    this.value = value;
  }

  @Override
  public Class getElementType() {
    return value.getClass();
  }

  @Override
  Array createView(Index index) {
    return this;
  }

  @Override
  public Object getStorage() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public double getDouble(Index ima) {
    return getDouble(0);
  }

  @Override
  public void setDouble(Index ima, double value) {
  }

  @Override
  public float getFloat(Index ima) {
    return getFloat(0);
  }

  @Override
  public void setFloat(Index ima, float value) {
  }

  @Override
  public long getLong(Index ima) {
    return getLong(0);
  }

  @Override
  public void setLong(Index ima, long value) {
  }

  @Override
  public int getInt(Index ima) {
    return getInt(0);
  }

  @Override
  public void setInt(Index ima, int value) {
  }

  @Override
  public short getShort(Index ima) {
    return getShort(0);
  }

  @Override
  public void setShort(Index ima, short value) {
  }

  @Override
  public byte getByte(Index ima) {
    return getByte(0);
  }

  @Override
  public void setByte(Index ima, byte value) {
  }

  @Override
  public char getChar(Index ima) {
    return getChar(0);
  }

  @Override
  public void setChar(Index ima, char value) {
  }

  @Override
  public boolean getBoolean(Index ima) {
    return getBoolean(0);
  }

  @Override
  public void setBoolean(Index ima, boolean value) {
  }

  @Override
  public Object getObject(Index ima) {
    return value;
  }

  @Override
  public void setObject(Index ima, Object value) {
  }

  @Override
  public double getDouble(int elem) {
    return ((Number) value).doubleValue();
  }

  @Override
  public void setDouble(int elem, double val) {
  }

  @Override
  public float getFloat(int elem) {
    return ((Number) value).floatValue();
  }

  @Override
  public void setFloat(int elem, float val) {
  }

  @Override
  public long getLong(int elem) {
    return ((Number) value).longValue();
  }

  @Override
  public void setLong(int elem, long value) {
  }

  @Override
  public int getInt(int elem) {
    return ((Number) value).intValue();
  }

  @Override
  public void setInt(int elem, int value) {
  }

  @Override
  public short getShort(int elem) {
    return ((Number) value).shortValue();
  }

  @Override
  public void setShort(int elem, short value) {
  }

  @Override
  public byte getByte(int elem) {
    return ((Number) value).byteValue();
  }

  @Override
  public void setByte(int elem, byte value) {
  }

  @Override
  public char getChar(int elem) {
    return (Character) value;
  }

  @Override
  public void setChar(int elem, char value) {
  }

  @Override
  public boolean getBoolean(int elem) {
    return (Boolean) value;
  }

  @Override
  public void setBoolean(int elem, boolean value) {
  }

  @Override
  public Object getObject(int elem) {
    return value;
  }

  @Override
  public void setObject(int elem, Object value) {
  }
}
