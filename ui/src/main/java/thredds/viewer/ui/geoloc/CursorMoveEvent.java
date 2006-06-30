// $Id: CursorMoveEvent.java,v 1.2 2004/09/24 03:26:40 caron Exp $
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
package thredds.viewer.ui.geoloc;

import java.awt.geom.Point2D;

/**
 * Cursor has moved to a new location.
 * @author John Caron
 * @version $Id: CursorMoveEvent.java,v 1.2 2004/09/24 03:26:40 caron Exp $
 */
public class CursorMoveEvent extends java.util.EventObject {
  private Point2D world;

  public CursorMoveEvent(Object source, Point2D world) {
    super(source);
    this.world = world;
  }
  public Point2D getLocation() { return world; }
}

/* Change History:
   $Log: CursorMoveEvent.java,v $
   Revision 1.2  2004/09/24 03:26:40  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:53  caron
   import sources

   Revision 1.3  2000/08/18 04:16:18  russ
   Licensed under GNU LGPL.

   Revision 1.2  2000/05/16 23:00:56  caron
   return Point2D, not ProjectionPoint

   Revision 1.1  2000/02/07 18:06:08  caron
   NP throws CursorMoveEvent

*/
