/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
    return (tableCdesc[index] == null) ? "unknown" : tableCdesc[index];
  }

}
