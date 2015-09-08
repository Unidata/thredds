/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.coord;

import ucar.nc2.constants.AxisType;
import ucar.nc2.util.Counters;
import ucar.nc2.util.Indent;

import java.util.Formatter;
import java.util.List;

/**
 * Abstract coordinate
 *
 * @author caron
 * @since 11/24/13
 */
public interface Coordinate {
  /**
   * Enumerated list of coordinate types
   */
  enum Type {
    runtime(0, AxisType.RunTime),
    time(1, AxisType.Time),
    timeIntv(1, AxisType.Time),
    vert(3, AxisType.Height),
    time2D(1, AxisType.TimeOffset),
    ens(2, AxisType.Ensemble);  // cant change order, protobuf uses the ordinal

    public final int order;
    public final AxisType axisType;

    Type(int order, AxisType axisType) {
      this.order = order;
      this.axisType = axisType;
    }
  }

  List<? extends Object> getValues(); // get sorted list of values
  Object getValue(int idx);  // get the ith value
  int getIndex(Object val);  // LOOK assumes the values are unique;
  int getSize();             // how many values ??
  // int findIndexContaining(Object need);

  int getCode();
  Type getType();
  String getName();
  String getUnit();
  int getNCoords();             // how many coords ??

  void showInfo(Formatter info, Indent indent);
  void showCoords(Formatter info);
  Counters calcDistributions();
  int estMemorySize();          // estimated memory size in bytes (debugging)
}
