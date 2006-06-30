// $Id: DateGMT.java,v 1.5 2005/01/05 22:47:13 caron Exp $
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
package thredds.util;

/**
 * Convenience routine for displaying java.util.Date in Greenwich Mean Time(GMT).
 * @deprecated use ucar.nc2.units.DateUnit
 * @author jcaron
 * @version $Revision: 1.5 $ $Date: 2005/01/05 22:47:13 $
 */

public class DateGMT extends java.util.Date {

  private static DateGMT current = new DateGMT();
  private static java.text.SimpleDateFormat dateFormat;
  static {
    dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
  }

  /** constructor */
  public DateGMT() { }

  /** constructor */
  public DateGMT(java.util.Date d) {
    setTime( d.getTime());
  }

  /** format date in GMT. */
  public String toString() {
    return dateFormat.format(this);
  }

  static public String toString( java.util.Date d) {
    return dateFormat.format(d);
  }

}

/**
 * $Log: DateGMT.java,v $
 * Revision 1.5  2005/01/05 22:47:13  caron
 * no message
 *
 * Revision 1.4  2004/09/24 03:26:35  caron
 * merge nj22
 *
 * Revision 1.3  2004/05/11 23:30:36  caron
 * release 2.0a
 *
 * Revision 1.2  2004/02/20 05:02:53  caron
 * release 1.3
 *
 * Revision 1.1.1.1  2002/11/23 17:49:48  caron
 * thredds reorg
 *
 * Revision 1.2  2002/09/27 16:05:54  caron
 * minor additions
 *
 * Revision 1.1.1.1  2002/02/26 17:24:41  caron
 * import sources
 *
 * Revision 1.1  2001/09/14 15:47:16  caron
 * checkin catalog 0.4
 *
 */