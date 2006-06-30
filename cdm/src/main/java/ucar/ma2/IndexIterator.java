// $Id: IndexIterator.java,v 1.6 2006/02/16 23:02:31 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
    Iteration through each element of a Array in "canonical order".
    The user obtains this by calling getIndexIterator() on an Array.

    Canonical order for A[i][j][k] has k varying fastest, then j, then i.<br>
    <p> Example: Replace array with its square:
    <br>
    <pre>
    IndexIterator iter = A.getIndexIterator();
    while (iter.hasNext()) {
      double val = iter.getDoubleNext();
      iter.setDoubleCurrent( val * val);
    }
    </pre>

    Note that logical order may not be physical order.

 * @author caron
 * @version $Revision: 1.6 $ $Date: 2006/02/16 23:02:31 $
 */

public interface IndexIterator  {

  /** Return true if there are more elements in the iteration. */
  public boolean hasNext();

  /** Get next value as a double */
  public double getDoubleNext();
  /** Set next value with a double */
  public void setDoubleNext(double val);
  /** Get current value as a double */
  public double getDoubleCurrent();
  /** Set current value with a double */
  public void setDoubleCurrent(double val);

  /** Get next value as a float */
  public float getFloatNext();
  /** Set next value with a float */
  public void setFloatNext(float val);
  /** Get current value as a float */
  public float getFloatCurrent();
  /** Set current value with a float */
  public void setFloatCurrent(float val);

  /** Get next value as a long */
  public long getLongNext();
  /** Set next value with a long */
  public void setLongNext(long val);
  /** Get current value as a long */
  public long getLongCurrent();
  /** Set current value with a long */
  public void setLongCurrent(long val);

  /** Get next value as a int */
  public int getIntNext();
  /** Set next value with a int */
  public void setIntNext(int val);
  /** Get current value as a int */
  public int getIntCurrent();
  /** Set current value with a int */
  public void setIntCurrent(int val);

  /** Get next value as a short */
  public short getShortNext();
  /** Set next value with a short */
  public void setShortNext(short val);
  /** Get current value as a short */
  public short getShortCurrent();
  /** Set current value with a short */
  public void setShortCurrent(short val);

  /** Get next value as a byte */
  public byte getByteNext();
  /** Set next value with a byte */
  public void setByteNext(byte val);
  /** Get current value as a byte */
  public byte getByteCurrent();
  /** Set current value with a byte */
  public void setByteCurrent(byte val);

  /** Get next value as a char */
  public char getCharNext();
  /** Set next value with a char */
  public void setCharNext(char val);
  /** Get current value as a char */
  public char getCharCurrent();
  /** Set current value with a char */
  public void setCharCurrent(char val);

  /** Get next value as a boolean */
  public boolean getBooleanNext();
  /** Set next value with a boolean */
  public void setBooleanNext(boolean val);
  /** Get current value as a boolean */
  public boolean getBooleanCurrent();
  /** Set current value with a boolean */
  public void setBooleanCurrent(boolean val);

  /** Get next value as an Object */
  public Object getObjectNext();
  /** Set next value with a Object */
  public void setObjectNext(Object val);
  /** Get current value as a Object */
  public Object getObjectCurrent();
  /** Set current value with a Object */
  public void setObjectCurrent(Object val);

  /** Get next value as an Object */
  public Object next();

  /** get the current counter, use for debugging */
  public int[] getCurrentCounter();
}

/* Change History:
   $Log: IndexIterator.java,v $
   Revision 1.6  2006/02/16 23:02:31  caron
   *** empty log message ***

   Revision 1.5  2006/02/06 21:17:03  caron
   more fixes to dods parsing.
   ArraySequence.flatten()
   ncml.xml use default namespace. Only way I can get ncml in catalog to validate.
   ThreddsDataFactory refactor

   Revision 1.4  2005/12/15 00:29:09  caron
   *** empty log message ***

   Revision 1.3  2005/12/09 04:24:35  caron
   Aggregation
   caching
   sync

   Revision 1.2  2004/07/12 23:40:14  caron
   2.2 alpha 1.0 checkin

 */
