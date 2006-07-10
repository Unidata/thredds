// $Id: IteratorFast.java,v 1.5 2005/12/15 00:29:09 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
 * A "fast" iterator that can be used when the data is in canonical order.
 *
 * @author caron
 * @version $Revision: 1.18 $ $Date: 2006/05/24 00:12:56 $
 */
  /* the idea is IteratorFast can do the iteration without an Index */
  public class IteratorFast implements IndexIterator {

    private int currElement = -1;
    private final Array maa;
    private long size;

    IteratorFast(long size, Array maa) {
      this.size = size;
      this.maa = maa;
      //System.out.println("IteratorFast");
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
      counter.setCurrentElement( currElement);
      return counter.toString();
    }

    public int[] getCurrentCounter() {
      if (counter == null)
        counter = new Index(maa.getShape());
      counter.setCurrentElement( currElement);
      return counter.current;
    }

    public double getDoubleCurrent() { return maa.getDouble(currElement); }
    public double getDoubleNext() { return maa.getDouble(++currElement); }
    public void setDoubleCurrent(double val) { maa.setDouble(currElement, val); }
    public void setDoubleNext(double val) { maa.setDouble(++currElement, val); }

    public float getFloatCurrent() { return maa.getFloat(currElement); }
    public float getFloatNext() { return maa.getFloat(++currElement); }
    public void setFloatCurrent(float val) { maa.setFloat(currElement, val); }
    public void setFloatNext(float val) { maa.setFloat(++currElement, val); }

    public long getLongCurrent() { return maa.getLong(currElement); }
    public long getLongNext() { return maa.getLong(++currElement); }
    public void setLongCurrent(long val) { maa.setLong(currElement, val); }
    public void setLongNext(long val) { maa.setLong(++currElement, val); }

    public int getIntCurrent() { return maa.getInt(currElement); }
    public int getIntNext() { return maa.getInt(++currElement); }
    public void setIntCurrent(int val) { maa.setInt(currElement, val); }
    public void setIntNext(int val) { maa.setInt(++currElement, val); }

    public short getShortCurrent() { return maa.getShort(currElement); }
    public short getShortNext() { return maa.getShort(++currElement); }
    public void setShortCurrent(short val) { maa.setShort(currElement, val); }
    public void setShortNext(short val) { maa.setShort(++currElement, val); }

    public byte getByteCurrent() { return maa.getByte(currElement); }
    public byte getByteNext() { return maa.getByte(++currElement); }
    public void setByteCurrent(byte val) { maa.setByte(currElement, val); }
    public void setByteNext(byte val) { maa.setByte(++currElement, val); }

    public char getCharCurrent() { return maa.getChar(currElement); }
    public char getCharNext() { return maa.getChar(++currElement); }
    public void setCharCurrent(char val) { maa.setChar(currElement, val); }
    public void setCharNext(char val) { maa.setChar(++currElement, val); }

    public boolean getBooleanCurrent() { return maa.getBoolean(currElement); }
    public boolean getBooleanNext() { return maa.getBoolean(++currElement); }
    public void setBooleanCurrent(boolean val) { maa.setBoolean(currElement, val); }
    public void setBooleanNext(boolean val) { maa.setBoolean(++currElement, val); }

    public Object getObjectCurrent() { return maa.getObject(currElement); }
    public Object getObjectNext() { return maa.getObject(++currElement); }
    public void setObjectCurrent(Object val) { maa.setObject(currElement, val); }
    public void setObjectNext(Object val) { maa.setObject(++currElement, val); }

    public Object next() { return maa.getObject(++currElement); }
  }