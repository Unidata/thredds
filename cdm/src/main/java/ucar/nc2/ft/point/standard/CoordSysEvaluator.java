/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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
   * search for Axis by Type, assign to TableConfig if found.
   * search for Lat, Lon, Time, Height.
   * @param nt set coordinates short names in this table.
   * @param ds search in this dataset's "Best" coordinate system. If no CoordSystem, try list of coordinate axes
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
   * search for Axis by Type.
   * @param ds search in this dataset's "Best" coordinate system.
   * @param atype search for this type of CoordinateAxis. takes the first one it finds.
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
   * Search for Axis by Type.
   * @param ds search in this dataset's "Best" coordinate system.
   * @param atype search for this type of CoordinateAxis. takes the first one it finds.
   * @return the found CoordinateAxis, or null if none
   */
  static public CoordinateAxis findCoordByType(NetcdfDataset ds, AxisType atype) {
    return findCoordByType(ds, atype, null);
  }

  /**
   * search for Axis by Type and test against a predicate
   * @param ds search in this dataset's "Best" coordinate system.
   * @param atype search for this type of CoordinateAxis.
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
    // first search for matching AxisType and "CF axis" attribute
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

  public interface Predicate {
    boolean match(CoordinateAxis axis);
  }

  /**
   * search for Dimension used by axis of given by Type.
   * @param ds search in this dataset's "Best" coordinate system.
   * @param atype search for this type of CoordinateAxis. takes the first one it finds.
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
   * @param ds search in this dataset
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
