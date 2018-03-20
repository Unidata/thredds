/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.constants;

/**
 * Constants for _Coordinate Conventions.
 * Used to annotate Variables, using Attributes.
 *
 * @see <a href="http://www.unidata.ucar.edu/software/netcdf-java/reference/CoordinateAttributes.html">_Coordinate Conventions</a>
 * @author caron
 */
public class _Coordinate {
  static public final String AliasForDimension = "_CoordinateAliasForDimension";
  static public final String Axes = "_CoordinateAxes";
  static public final String AxisType = "_CoordinateAxisType";
  static public final String AxisTypes = "_CoordinateAxisTypes";
  static public final String Convention = "_Coordinates";
  static public final String ModelBaseDate = "_CoordinateModelBaseDate"; // experimental
  static public final String ModelRunDate = "_CoordinateModelRunDate"; // experimental
  static public final String Stagger = "_CoordinateStagger";
  static public final String SystemFor = "_CoordinateSystemFor";
  static public final String Systems = "_CoordinateSystems";
  static public final String Transforms = "_CoordinateTransforms";
  static public final String TransformType = "_CoordinateTransformType";
  static public final String ZisLayer = "_CoordinateZisLayer";
  static public final String ZisPositive = "_CoordinateZisPositive";

  // global attributes
  static public final String _CoordSysBuilder = "_CoordSysBuilder";

  // class not interface, per Bloch edition 2 item 19
  private _Coordinate() {} // disable instantiation
}
