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
package ucar.nc2.util.xml;

import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.ma2.DataType;
import ucar.nc2.util.Misc;

import java.util.HashMap;

public class MetarField {

  static HashMap<String, MetarField> fields = new HashMap<String, MetarField>();
  boolean showFields = false;

  String name;
  boolean isText;
  double sum = 0.0;

  MetarField(String name) {
    this.name = name;
    fields.put(name, this);
    if (showFields) System.out.println(name + " added");
  }

  void sum(StructureData sdata, StructureMembers.Member m) {
    if (m.getDataType() == DataType.DOUBLE)
      sum(sdata.getScalarDouble(m));
    else if (m.getDataType() == DataType.FLOAT)
      sum(sdata.getScalarFloat(m));
    else if (m.getDataType() == DataType.INT)
      sum(sdata.getScalarInt(m));
  }

  void sum(String text) {
    if (isText) return;
    try {
      sum(Double.parseDouble(text));
    } catch (NumberFormatException e) {
      if (showFields) System.out.println(name + " is text");
      isText = true;
    }
  }

  void sum(double d) {
    if (!Misc.closeEnough(d, -99999.0))
      sum += d; // LOOK kludge for missing data
  }
}
