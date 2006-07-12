// $Id$
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
package thredds.viewer.ui.event;

import java.awt.Rectangle;
/** Change events for UI objects.
 * @author John Caron
 * @version $Id$
 */
public class UIChangeEvent extends java.util.EventObject {
  private String property;
  private Object objectChanged;
  private Object newValue;

  public UIChangeEvent(Object source, String property, Object changed, Object newValue) {
    super(source);
    this.property = property;
    this.objectChanged = changed;
    this.newValue = newValue;
  }

  public String getChangedProperty() { return property; }
  public Object getChangedObject() { return objectChanged; }
  public Object getNewValue() { return newValue; }

  public String toString() {
    return "UIChangeEvent: "+ property+ " objectChanged: "+ objectChanged+ "  newValue: "+ newValue;
  }
}

/* Change History:
   $Log: UIChangeEvent.java,v $
   Revision 1.2  2004/09/24 03:26:40  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.4  2000/08/18 04:16:03  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/06/03 01:44:15  caron
   remove the damn controlMs

   Revision 1.2  1999/06/03 01:27:09  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:49  caron
   startAgain

# Revision 1.3  1999/03/16  17:00:59  caron
# fix StationModel editing; add TopLevel
#
# Revision 1.2  1998/12/14  17:12:03  russ
# Add comment for accumulating change histories.
#
*/
