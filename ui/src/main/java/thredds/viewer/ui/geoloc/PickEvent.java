// $Id: PickEvent.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.viewer.ui.geoloc;

import java.awt.geom.Point2D;

/**
 * User wants to pick an object at 2D location.
 * @author John Caron
 * @version $Id: PickEvent.java 50 2006-07-12 16:30:06Z caron $
 */
public class PickEvent extends java.util.EventObject {
  Point2D where;

  public PickEvent(Object source, Point2D location) {
    super(source);
    this.where = location;
  }
  public Point2D getLocation() { return where; }
}

/* Change History:
   $Log: PickEvent.java,v $
   Revision 1.2  2004/09/24 03:26:41  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:53  caron
   import sources

   Revision 1.4  2000/08/18 04:16:21  russ
   Licensed under GNU LGPL.

   Revision 1.3  2000/05/16 22:59:58  caron
   return Point2D, not ProjectionPoint

   Revision 1.2  1999/06/03 01:44:28  caron
   remove the damn controlMs

   Revision 1.1.1.1  1999/06/02 20:36:02  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:49  caron
   startAgain

*/
