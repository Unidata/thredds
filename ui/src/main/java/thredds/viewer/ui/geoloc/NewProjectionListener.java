// $Id: NewProjectionListener.java,v 1.2 2004/09/24 03:26:41 caron Exp $
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
/** Listeners for new Projection events
 * @author John Caron
 * @version $Id: NewProjectionListener.java,v 1.2 2004/09/24 03:26:41 caron Exp $
 */
public interface NewProjectionListener extends java.util.EventListener {
    public void actionPerformed( NewProjectionEvent e);
}

/* Change History:
   $Log: NewProjectionListener.java,v $
   Revision 1.2  2004/09/24 03:26:41  caron
   merge nj22

   Revision 1.1  2002/12/13 00:55:08  caron
   pass 2

   Revision 1.1.1.1  2002/02/26 17:24:53  caron
   import sources

   Revision 1.3  2000/08/18 04:16:21  russ
   Licensed under GNU LGPL.

   Revision 1.2  1999/06/03 01:44:28  caron
   remove the damn controlMs

   Revision 1.1.1.1  1999/06/02 20:36:02  caron
   another reorg

   Revision 1.1.1.1  1999/05/21 17:33:49  caron
   startAgain

# Revision 1.3  1999/03/16  16:57:38  caron
# fix StationModel editing; add TopLevel
#
# Revision 1.2  1998/12/14  17:10:52  russ
# Add comment for accumulating change histories.
#
*/
