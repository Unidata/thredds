// $Id: CatalogSetCallback.java,v 1.3 2004/09/24 03:26:27 caron Exp $
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

package thredds.catalog;

/**
 * Allows asynchrous reading of a catalog.
 * When the catalog is read, setCatalog() is called, else failed() is called.
 */
public interface CatalogSetCallback {
  /** Called when the catalog is done being read */
  public void setCatalog(InvCatalogImpl catalog);
  /** Called if the catalog reading fails */
  public void failed();
}

/* Change History:
   $Log: CatalogSetCallback.java,v $
   Revision 1.3  2004/09/24 03:26:27  caron
   merge nj22

   Revision 1.2  2004/06/09 00:27:24  caron
   version 2.0a release; cleanup javadoc

   Revision 1.1  2004/03/02 21:45:21  caron
   v 1.3.1

*/