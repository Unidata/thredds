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
 * Specialization of Index for rank 0 arrays.
 *
 * @see Index
 * @author caron
 */
public class Index0D extends Index {

  Index0D() { super(0); }
  public Index0D( int[] shape) {
    super(shape);
  }

  public int currentElement() {
    return offset;
  }

  protected int incr() {
    return offset;
  }


  public void setDim(int dim, int value) {
  }

  public Object clone() {
    return super.clone();
  }
}
