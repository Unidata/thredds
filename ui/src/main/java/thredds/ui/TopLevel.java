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
package thredds.ui;

/** common toplevel for applets (JApplet) and applications (JFrames) */

public interface TopLevel {
    /** get the getRootPaneContainer */
  public javax.swing.RootPaneContainer getRootPaneContainer();

    /** get the underlying Frame; call only if !isApplet() */
  public javax.swing.JFrame getJFrame();

    /** close and exit the progem */
  public void close();

    /** save any persistant data */
  public void save();

    /** return true if this is an Applet */
  public boolean isApplet();
}

/* Change History:
   $Log: TopLevel.java,v $
   Revision 1.3  2004/09/24 03:26:35  caron
   merge nj22

   Revision 1.2  2003/05/29 23:03:28  john
   minor

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.1.1.1  2002/02/15 00:01:48  caron
   import sources

*/