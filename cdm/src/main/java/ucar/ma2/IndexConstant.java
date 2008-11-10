/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.ma2;

/**
 * An Index into an Array that always returns 0. It can have any shape, so it allows you to create a
 * constant Array of any shape.
 *  
 * @author caron
 */
 // LOOK : need to override section, etc !!
public class IndexConstant extends Index {

  protected IndexConstant(int rank) {
    super( rank);
  }

  protected IndexConstant( int[] shape) {
    super( shape);
  }

  protected IndexConstant( int[] shape, int[] stride) {
    super( shape, stride);
  }

  /** always return 0 */
  public int currentElement() {
    return 0;
  }

  IndexIterator getIndexIterator(Array maa) {
    return new IteratorConstant(size, maa);
  }

  private class IteratorConstant implements IndexIterator {

    private int currElement = -1;
    private final Array maa;
    private long size;

    IteratorConstant(long size, Array maa) {
      this.size = size;
      this.maa = maa;
    }

    public boolean hasNext() {
      return currElement < size-1;
    }

    public boolean hasMore(int howMany) {
      return currElement < size-howMany;
    }

    private Index counter = null; // usually not used
    public String toString() {
      if (counter == null)
        counter = new Index(maa.getShape());
      counter.setCurrentCounter( currElement);
      return counter.toString();
    }
    public int[] getCurrentCounter() {
      if (counter == null)
        counter = new Index(maa.getShape());
      counter.setCurrentCounter( currElement);
      return counter.current;
    }
    
    public double getDoubleCurrent() { return maa.getDouble(0); }
    public double getDoubleNext() { currElement++; return maa.getDouble(0); }
    public void setDoubleCurrent(double val) { maa.setDouble(0, val); }
    public void setDoubleNext(double val) { currElement++; maa.setDouble(0, val); }

    public float getFloatCurrent() { return maa.getFloat(currElement); }
    public float getFloatNext() { currElement++; return maa.getFloat(0); }
    public void setFloatCurrent(float val) { maa.setFloat(currElement, val); }
    public void setFloatNext(float val) { currElement++; maa.setFloat(0, val); }

    public long getLongCurrent() { return maa.getLong(currElement); }
    public long getLongNext() { currElement++; return maa.getLong(0); }
    public void setLongCurrent(long val) { maa.setLong(currElement, val); }
    public void setLongNext(long val) { currElement++; maa.setLong(0, val); }

    public int getIntCurrent() { return maa.getInt(currElement); }
    public int getIntNext() { currElement++; return maa.getInt(0); }
    public void setIntCurrent(int val) { maa.setInt(currElement, val); }
    public void setIntNext(int val) { currElement++; maa.setInt(0, val); }

    public short getShortCurrent() { return maa.getShort(currElement); }
    public short getShortNext() { currElement++; return maa.getShort(0); }
    public void setShortCurrent(short val) { maa.setShort(currElement, val); }
    public void setShortNext(short val) { currElement++; maa.setShort(0, val); }

    public byte getByteCurrent() { return maa.getByte(currElement); }
    public byte getByteNext() { currElement++; return maa.getByte(0); }
    public void setByteCurrent(byte val) { maa.setByte(currElement, val); }
    public void setByteNext(byte val) { currElement++; maa.setByte(0, val); }

    public char getCharCurrent() { return maa.getChar(currElement); }
    public char getCharNext() { currElement++; return maa.getChar(0); }
    public void setCharCurrent(char val) { maa.setChar(currElement, val); }
    public void setCharNext(char val) { currElement++; maa.setChar(0, val); }

    public boolean getBooleanCurrent() { return maa.getBoolean(currElement); }
    public boolean getBooleanNext() { currElement++; return maa.getBoolean(0); }
    public void setBooleanCurrent(boolean val) { maa.setBoolean(currElement, val); }
    public void setBooleanNext(boolean val) {currElement++;  maa.setBoolean(0, val); }

    public Object getObjectCurrent() { return maa.getObject(currElement); }
    public Object getObjectNext() { currElement++; return maa.getObject(0); }
    public void setObjectCurrent(Object val) { maa.setObject(currElement, val); }
    public void setObjectNext(Object val) { currElement++; maa.setObject(0, val); }

    public Object next() { currElement++; return maa.getObject(0); }
  }
}