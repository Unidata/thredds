// $Id: GeoSelectionEvent.java,v 1.2 2004/09/24 03:26:40 caron Exp $
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
/**
 * Used to notify listeners that there is a new geographic area selection.
 * @author John Caron
 * @version $Id: GeoSelectionEvent.java,v 1.2 2004/09/24 03:26:40 caron Exp $
 */
public class GeoSelectionEvent extends java.util.EventObject {
  private ucar.unidata.geoloc.ProjectionRect pr;

  public GeoSelectionEvent(Object source, ucar.unidata.geoloc.ProjectionRect pr) {
    super(source);
    this.pr = pr;
  }

  public ucar.unidata.geoloc.ProjectionRect getProjectionRect() { return pr; }
}

/* Change History:
   $Log: GeoSelectionEvent.java,v $
   Revision 1.2  2004/09/24 03:26:40  caron
   merge nj22

   Revision 1.1  2004/05/21 05:57:36  caron
   release 2.0b

*/
