/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.ft.point.standard;

import ucar.nc2.Attribute;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.constants.AxisType;
import ucar.nc2.Dimension;

import java.util.List;

/**
 * CoordinateSystem Evaluation utilities.
 *
 * @author caron
 * @since Dec 16, 2008
 */
public class CoordSysEvaluator {

  /**
   * Look for Axis by Type, assign to TableConfig if found.
   * Looks for Lat, Lon, Time, Height.
   * @param nt set coordinates short names in this table.
   * @param ds look in this dataset's "Best" coordinate system. If no CoordSystem, try list of coordinate axes
   */
  static public void findCoords(TableConfig nt, NetcdfDataset ds, Predicate p) {
    nt.lat =  findCoordShortNameByType(ds, AxisType.Lat, p);
    nt.lon =  findCoordShortNameByType(ds, AxisType.Lon, p);
    nt.time =  findCoordShortNameByType(ds, AxisType.Time, p);
    nt.elev =  findCoordShortNameByType(ds, AxisType.Height, p);
    if (nt.elev == null)
      nt.elev =  findCoordShortNameByType(ds, AxisType.Pressure, p);
  }

   /**
   * Look for Axis by Type.
   * @param ds look in this dataset's "Best" coordinate system.
   * @param atype look for this type of CoordinateAxis. takes the first one it finds.
    * @return the found CoordinateAxis name, or null if none
   */
   static public String findCoordNameByType(NetcdfDataset ds, AxisType atype) {
     CoordinateAxis coordAxis = findCoordByType(ds, atype);
     return coordAxis == null ? null : coordAxis.getFullName();
   }

  static public String findCoordShortNameByType(NetcdfDataset ds, AxisType atype) {
    CoordinateAxis coordAxis = findCoordByType(ds, atype);
    return coordAxis == null ? null : coordAxis.getShortName();
  }

  static public String findCoordShortNameByType(NetcdfDataset ds, AxisType atype, Predicate p) {
    CoordinateAxis coordAxis = findCoordByType(ds, atype, p);
    return coordAxis == null ? null : coordAxis.getShortName();
  }

  /**
   * Look for Axis by Type.
   * @param ds look in this dataset's "Best" coordinate system.
   * @param atype look for this type of CoordinateAxis. takes the first one it finds.
   * @return the found CoordinateAxis, or null if none
   */
  static public CoordinateAxis findCoordByType(NetcdfDataset ds, AxisType atype) {
    return findCoordByType(ds, atype, null);
  }

  /**
   * Look for Axis by Type and test against a predicate
   * @param ds look in this dataset's "Best" coordinate system.
   * @param atype look for this type of CoordinateAxis.
   * @param p match this predicate; may be null
   * @return the found CoordinateAxis, or null if none
   */
  static public CoordinateAxis findCoordByType(NetcdfDataset ds, AxisType atype, Predicate p) {
    // try the "best" coordinate system
    CoordinateSystem use = findBestCoordinateSystem(ds);
    if (use == null) return null;
    CoordinateAxis result = findCoordByType(use.getCoordinateAxes(), atype, p);
    if (result != null) return result;

    // try all the axes
    return findCoordByType(ds.getCoordinateAxes(), atype, p);
  }

  static public CoordinateAxis findCoordByType(List<CoordinateAxis> axes, AxisType atype, Predicate p) {
    // first look for matching AxisType and "CF axis" attribute
    for (CoordinateAxis axis : axes) {
      if (axis.getAxisType() == atype) {
        Attribute att = axis.findAttribute(CF.AXIS);
        if (att != null && att.getStringValue().equals(atype.getCFAxisName()) && (p == null || p.match(axis)))
          return axis;
      }
    }

    // now match on just the AxisType
    for (CoordinateAxis axis : axes) {
      if (axis.getAxisType() == atype && (p == null || p.match(axis)))
        return axis;
    }

    return null;
  }

  static public interface Predicate {
    boolean match(CoordinateAxis axis);
  }

  /**
   * Look for Dimension used by axis of given by Type.
   * @param ds look in this dataset's "Best" coordinate system.
   * @param atype look for this type of CoordinateAxis. takes the first one it finds.
   * @return the found CoordinateAxis' first Dimension, or null if none or scalar
   */
  static public Dimension findDimensionByType(NetcdfDataset ds, AxisType atype) {
    CoordinateAxis axis = findCoordByType(ds, atype);
    if (axis == null) return null;
    if (axis.isScalar()) return null;
    return axis.getDimension(0);
  }

  /**
   * Find the CoordinateSystem with the most number of CoordinateAxes
   * @param ds look in this dataset
   * @return CoordinateSystem or null if none
   */
  static private CoordinateSystem findBestCoordinateSystem(NetcdfDataset ds) {
        // find coordinate system with highest rank (largest number of axes)
    CoordinateSystem use = null;
    for (CoordinateSystem cs : ds.getCoordinateSystems()) {
      if (use == null) use = cs;
      else if (cs.getCoordinateAxes().size() > use.getCoordinateAxes().size())
        use = cs;
    }
    return use;
  }

}
