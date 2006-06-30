// $Id: JavaDateUnit.java,v 1.6 2006/02/13 19:51:36 caron Exp $
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
package ucar.nc2.units;

import java.util.Date;

/**
 * A DateUnit that uses the same units as java.util.Date, namely "secs since 1970-01-01T00:00:00Z".
 */
public class JavaDateUnit extends DateUnit {

  /** Factory */
  static public JavaDateUnit getJavaDateUnit() {
    try {
      return new JavaDateUnit( "secs since 1970-01-01T00:00:00Z");
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  JavaDateUnit( String text) throws Exception {
    super( text);
  }

}

/* Change History:
   $Log: JavaDateUnit.java,v $
   Revision 1.6  2006/02/13 19:51:36  caron
   javadoc

   Revision 1.5  2005/05/19 23:43:41  caron
   clean up javadoc

   Revision 1.4  2005/05/04 17:18:47  caron
   *** empty log message ***

   Revision 1.3  2005/05/01 19:16:05  caron
   move station to point package
   add implementations for common interfaces
   refactor station adapters

   Revision 1.2  2005/03/04 20:18:26  caron
   *** empty log message ***

   Revision 1.1  2005/03/04 17:12:05  caron
   add

*/