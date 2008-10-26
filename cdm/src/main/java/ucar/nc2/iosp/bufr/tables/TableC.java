/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
package ucar.nc2.iosp.bufr.tables;

/**
 *  BUFR Table C - Data operators
 * @author caron
 * @since Oct 25, 2008
 */
public class TableC {
  static private final String[] tableCdesc = new String[38];

  static {
    tableCdesc[1] = "change data width";
    tableCdesc[2] = "change scale";
    tableCdesc[3] = "change reference value";
    tableCdesc[4] = "add associated field";
    tableCdesc[5] = "signify character";
    tableCdesc[6] = "signify data width for next descriptor";
    tableCdesc[21] = "data not present";
    tableCdesc[22] = "quality information follows";
    tableCdesc[23] = "substituted values operator";
    tableCdesc[24] = "first order statistics";
    tableCdesc[25] = "difference statistics";
    tableCdesc[32] = "replaced/retained values";
    tableCdesc[35] = "cancel backward data reference";
    tableCdesc[36] = "define data present bit-map";
    tableCdesc[37] = "use/cancel data present bit-map";
  }

  static public String getOperatorName(int index) {
    if ((index < 0 ) || (index >= tableCdesc.length)) return "unknown";
    return (tableCdesc[index] == null) ? "unkown" : tableCdesc[index];
  }

}
