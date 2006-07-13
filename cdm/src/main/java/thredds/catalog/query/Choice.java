// $Id: Choice.java 48 2006-07-12 16:15:40Z caron $
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

package thredds.catalog.query;

/**
 * Abstraction of a choice a user can make.
 *
 * @author john caron
 * @version $Revision: 48 $ $Date: 2006-07-12 16:15:40Z $
 */

public interface Choice {
  public String toString(); // human display
  public String getValue(); // value for the query
}

/* Change History:
   $Log: Choice.java,v $
   Revision 1.6  2004/09/24 03:26:29  caron
   merge nj22

   Revision 1.5  2004/06/18 21:54:26  caron
   update dqc 0.3

 */