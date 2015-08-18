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

import java.util.*;

/**
 * Builds one Coordinate of one Variable,
 * by keeping the Set of Values found in the records.
 *
 * @author caron
 * @since 11/27/13
 */
public abstract class CoordinateBuilderImpl<T> implements CoordinateBuilder<T> {
  protected Set<Object> valSet = new HashSet<>(100);
  protected Map<Object, Integer> valMap;    // map of values to index in Coordinate
  protected Coordinate coord;

  @Override
  public void addRecord(T gr) {
    Object val = extract(gr);
    valSet.add(val);
  }

  @Override
  public void addAll(Coordinate coord) {
   for (Object val : coord.getValues())
      valSet.add(val);
  }

  public void add(Object val) {
    valSet.add(val);
  }

  @Override
  public void addAll(List<Object> coords) {
   for (Object val : coords)
      valSet.add(val);
  }

  @Override
  public Coordinate finish() {
    List<Object> valList = new ArrayList<>(valSet.size());
    for (Object off : valSet) valList.add(off);
    coord =  makeCoordinate(valList);
    valSet = null;

    List<? extends Object> values = coord.getValues();
    if (values != null) {
      valMap = new HashMap<>(coord.getSize() * 2);
      for (int i = 0; i < values.size(); i++)
        valMap.put(values.get(i), i);
    }
    return coord;
  }

  // used by CoordinateND.makeSparseArray
  // not used by CoordinateTime2D
  @Override
  public int getIndex(T gr) {
    Integer result =  valMap.get( extract(gr));
    return (result == null) ? 0 : result;
  }

  @Override
  public Coordinate getCoordinate() {
    return coord;
  }

}
