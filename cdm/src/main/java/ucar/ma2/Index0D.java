// $Id: Index0D.java,v 1.4 2005/12/15 00:29:08 caron Exp $
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
 * Specialization of Index for rank 0 arrays.
 *
 * @see Index
 * @author caron
 * @version $Revision: 1.4 $ $Date: 2005/12/15 00:29:08 $
 */
public class Index0D extends Index {

  Index0D() { super(0); }
  public Index0D( int[] shape) {
    super(shape);
  }

  public int currentElement() {
    return offset;
  }

  /* public int element(int [] index) {
    return offset;
  } */

  protected int incr() {
    return offset;
  }


  public void setDim(int dim, int value) {
  }

  public Object clone() {
    return super.clone();
  }
}

/* Change History:
   $Log: Index0D.java,v $
   Revision 1.4  2005/12/15 00:29:08  caron
   *** empty log message ***

   Revision 1.3  2005/12/09 04:24:34  caron
   Aggregation
   caching
   sync

   Revision 1.2  2004/07/12 23:40:13  caron
   2.2 alpha 1.0 checkin

 */
