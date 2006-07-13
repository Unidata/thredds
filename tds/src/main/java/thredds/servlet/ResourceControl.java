// $Id: ResourceControl.java 51 2006-07-12 17:13:13Z caron $
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
package thredds.servlet;

import javax.servlet.http.HttpServletRequest;

/**
 * @author john
 */
public class ResourceControl {

  static public boolean accessAllowed( HttpServletRequest req, String resourceName) {

    boolean ok = req.isUserInRole(resourceName);

    // debug
    String s = ServletUtil.showSecurity(req);
    System.out.println(resourceName+" accessAllowed="+ok);
    System.out.println(" accessAllowed debug = "+s);

    return ok;
  }
}

/* Change History:
   $Log: ResourceControl.java,v $
   Revision 1.1  2005/11/03 19:37:47  caron
   no message

*/