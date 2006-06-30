// $Id: NewProjectionEvent.java,v 1.5 2004/09/24 03:26:41 caron Exp $
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

import ucar.unidata.geoloc.ProjectionImpl;

/** Used to notify listeners that there is a new Projection.
 * @author John Caron
 * @version $Id: NewProjectionEvent.java,v 1.5 2004/09/24 03:26:41 caron Exp $
 */
public class NewProjectionEvent extends java.util.EventObject {
  private ProjectionImpl project;

  public NewProjectionEvent(Object source, ProjectionImpl proj) {
    super(source);
    this.project = proj;
  }

  public ProjectionImpl getProjection() {
    return project;
  }

}

/* Change History:
   $Log: NewProjectionEvent.java,v $
   Revision 1.5  2004/09/24 03:26:41  caron
   merge nj22

   Revision 1.4  2003/05/29 23:07:52  john
   bug fixes

   Revision 1.3  2003/04/08 18:16:24  john
   nc2 v2.1

   Revision 1.2  2003/03/17 21:12:40  john
   new viewer

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:52  caron
   import sources

   Revision 1.4  2000/08/18 04:16:21  russ
   Licensed under GNU LGPL.

   Revision 1.3  1999/12/16 22:58:38  caron
   gridded data viewer checkin

   Revision 1.2  1999/06/03 01:44:28  caron
   remove the damn controlMs

   Revision 1.1.1.1  1999/06/02 20:36:01  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:49  caron
   startAgain

# Revision 1.4  1999/03/16  16:57:32  caron
# fix StationModel editing; add TopLevel
#
# Revision 1.3  1998/12/14  17:10:51  russ
# Add comment for accumulating change histories.
#
*/
