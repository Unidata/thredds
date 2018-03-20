/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

/**
 * DO NOT USE
 * @author caron
 * @since Nov 15, 2008
 */
public class ArrayRagged extends Array {

  protected ArrayRagged(int[] shape) {
    super(DataType.OBJECT, shape);
  }


  public Class getElementType() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * DO NOT USE, throws UnsupportedOperationException
   */
  protected Array createView(Index index) {
    if (index.getSize() == getSize()) return this;
    throw new UnsupportedOperationException();
  }

  public Object getStorage() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }// used to create Array from java array

  protected void copyFrom1DJavaArray(IndexIterator iter, Object javaArray) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  protected void copyTo1DJavaArray(IndexIterator iter, Object javaArray) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * DO NOT USE, throws UnsupportedOperationException
   */
  public Array copy() {
    throw new UnsupportedOperationException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public double getDouble(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setDouble(Index i, double value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public float getFloat(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setFloat(Index i, float value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public long getLong(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setLong(Index i, long value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public int getInt(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setInt(Index i, int value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public short getShort(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setShort(Index i, short value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public byte getByte(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setByte(Index i, byte value) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public boolean getBoolean(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setBoolean(Index i, boolean value) {
    throw new ForbiddenConversionException();
  }

  public Object getObject(Index ima) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setObject(Index ima, Object value) {
//To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public char getChar(Index i) {
    throw new ForbiddenConversionException();
  }

  /**
   * DO NOT USE, throw ForbiddenConversionException
   */
  public void setChar(Index i, char value) {
    throw new ForbiddenConversionException();
  }

  // trusted, assumes that individual dimension lengths have been checked
  // package private : mostly for iterators
  public double getDouble(int index) {
    throw new ForbiddenConversionException();
  }

  public void setDouble(int index, double value) {
    throw new ForbiddenConversionException();
  }

  public float getFloat(int index) {
    throw new ForbiddenConversionException();
  }

  public void setFloat(int index, float value) {
    throw new ForbiddenConversionException();
  }

  public long getLong(int index) {
    throw new ForbiddenConversionException();
  }

  public void setLong(int index, long value) {
    throw new ForbiddenConversionException();
  }

  public int getInt(int index) {
    throw new ForbiddenConversionException();
  }

  public void setInt(int index, int value) {
    throw new ForbiddenConversionException();
  }

  public short getShort(int index) {
    throw new ForbiddenConversionException();
  }

  public void setShort(int index, short value) {
    throw new ForbiddenConversionException();
  }

  public byte getByte(int index) {
    throw new ForbiddenConversionException();
  }

  public void setByte(int index, byte value) {
    throw new ForbiddenConversionException();
  }

  public char getChar(int index) {
    throw new ForbiddenConversionException();
  }

  public void setChar(int index, char value) {
    throw new ForbiddenConversionException();
  }

  public boolean getBoolean(int index) {
    throw new ForbiddenConversionException();
  }

  public void setBoolean(int index, boolean value) {
    throw new ForbiddenConversionException();
  }

  public Object getObject(int elem) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void setObject(int elem, Object value) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

}
