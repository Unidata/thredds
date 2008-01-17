/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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

package ucar.nc2.dataset;

import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.transform.*;
import ucar.unidata.util.Parameter;
import ucar.ma2.DataType;
import ucar.ma2.Array;

import java.util.List;
import java.util.ArrayList;

/**
 * Manager for Coordinate Transforms.
 * @author john caron
 */
public class CoordTransBuilder {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordTransBuilder.class);
  static private List<Transform> transformList = new ArrayList<Transform>();
  static private boolean userMode = false;

  // search in the order added
  static {
    registerTransform("albers_conical_equal_area", AlbersEqualArea.class);
    registerTransform("lambert_azimuthal_equal_area", LambertAzimuthal.class);
    registerTransform("lambert_conformal_conic", LambertConformalConic.class);
    registerTransformMaybe("mcidas_area", "ucar.nc2.adde.McIDASAreaTransformBuilder"); // optional - needs visad.jar
    registerTransform("mercator", Mercator.class);
    registerTransform("orthographic", Orthographic.class);
    registerTransform("polar_stereographic", PolarStereographic.class);
    registerTransform("rotated_latitude_longitude", RotatedPole.class);
    registerTransform("stereographic", Stereographic.class);
    registerTransform("transverse_mercator", TransverseMercator.class);
    registerTransform("vertical_perspective", VerticalPerspective.class);
    registerTransform("UTM", UTM.class);

    registerTransform("atmosphere_sigma_coordinate", VAtmSigma.class);
    registerTransform("atmosphere_hybrid_sigma_pressure_coordinate", VAtmHybridSigmaPressure.class);
    registerTransform("atmosphere_hybrid_height_coordinate", VAtmHybridHeight.class);
    registerTransform("ocean_s_coordinate", VOceanS.class);
    registerTransform("ocean_sigma_coordinate", VOceanSigma.class);
    registerTransform("explicit_field", VExplicitField.class);
    registerTransform("existing3DField", VExplicitField.class); // deprecate
    registerTransform("flat_earth", FlatEarth.class);
    // further calls to registerTransform are by the user
    userMode = true;
  }

   /**
    * Register a class that implements a Coordinate Transform.
    * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
    * @param c class that implements CoordTransBuilderIF.
    */
  static public void registerTransform( String transformName, Class c) {
    if (!(CoordTransBuilderIF.class.isAssignableFrom( c)))
      throw new IllegalArgumentException("Class "+c.getName()+" must implement CoordTransBuilderIF");

    // fail fast - check newInstance works
    try {
      c.newInstance();
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("CoordTransBuilderIF Class "+c.getName()+" cannot instantiate, probably need default Constructor");
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("CoordTransBuilderIF Class "+c.getName()+" is not accessible");
    }

    // user stuff gets put at top
    if (userMode)
      transformList.add( 0, new Transform( transformName, c));
    else
      transformList.add( new Transform( transformName, c));

  }

  /**
   * Register a class that implements a Coordinate Transform.
   * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
   * @param className name of class that implements CoordTransBuilderIF.
   * @throws ClassNotFoundException if Class.forName( className) fails
   */
  static public void registerTransform( String transformName, String className) throws ClassNotFoundException {
    Class c = Class.forName( className);
    registerTransform( transformName, c);
  }

  /**
   * Register a class that implements a Coordinate Transform.
   * @param transformName name of transform. This name is used in the datasets to identify the transform, eg CF names.
   * @param className name of class that implements CoordTransBuilderIF.
   */
  static public void registerTransformMaybe( String transformName, String className) {
    Class c = null;
    try {
      c = Class.forName( className);
    } catch (ClassNotFoundException e) {
      log.warn("Coordinate Transform Class "+className+" not found.");
    }
    registerTransform( transformName, c);
  }

  static private class Transform {
    String transName;
    Class transClass;
    Transform(String transName,  Class transClass) {
      this.transName = transName;
      this.transClass = transClass;
    }
  }

  /**
   * Make a CoordinateTransform object from the paramaters in a Coordinate Transform Variable, using an intrinsic or
   * registered CoordTransBuilder.
   * @param ds enclosing dataset
   * @param ctv the Coordinate Transform Variable - container for the transform parameters
   * @param parseInfo pass back information about the parsing.
   * @param errInfo pass back error information.
   * @return CoordinateTransform, or null if failure.
   */
  static public CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable ctv, StringBuffer parseInfo, StringBuffer errInfo) {
    // standard name
    String transform_name = ds.findAttValueIgnoreCase(ctv, "transform_name", null);
    if (null == transform_name)
      transform_name = ds.findAttValueIgnoreCase(ctv, "Projection_Name", null);

    // these names are from CF - dont want to have to duplicate
    if (null == transform_name)
      transform_name = ds.findAttValueIgnoreCase(ctv, "grid_mapping_name", null);
    if (null == transform_name)
      transform_name = ds.findAttValueIgnoreCase(ctv, "standard_name", null);

    if (null == transform_name) {
      parseInfo.append("**Failed to find Coordinate Transform name from Variable= ").append(ctv).append("\n");
      return null;
    }

    transform_name = transform_name.trim();

    // do we have a transform registered for this ?
    Class builderClass = null;
    for (Transform transform : transformList) {
      if (transform.transName.equals(transform_name)) {
        builderClass = transform.transClass;
        break;
      }
    }
    if (null == builderClass) {
      parseInfo.append("**Failed to find CoordTransBuilder name= ").append(transform_name).append(" from Variable= ").append(ctv).append("\n");
      return null;
    }

      // get an instance of that class
    CoordTransBuilderIF builder = null;
    try {
      builder = (CoordTransBuilderIF) builderClass.newInstance();
    } catch (InstantiationException e) {
      log.error("Cant instantiate "+builderClass.getName(), e);
    } catch (IllegalAccessException e) {
      log.error("Cant access "+builderClass.getName(), e);
    }
    if (null == builder) { // cant happen - because this was tested in registerTransform()
      parseInfo.append("**Failed to build CoordTransBuilder object from class= ").append(builderClass.getName()).append(" for Variable= ").append(ctv).append("\n");
      return null;
    }

    builder.setErrorBuffer(errInfo);
    CoordinateTransform ct = builder.makeCoordinateTransform(ds, ctv);

    if (ct != null) {
      parseInfo.append(" Made Coordinate transform ").append(transform_name).append(" from variable ").append(ctv.getName()).append(": ").append(builder).append("\n");
    }

    return ct;
  }


  /**
   * Create a "dummy" Coordinate Transform Variable based on the given CoordinateTransform.
   * This creates a scalar Variable with dummy data, and adds the Parameters of the CoordinateTransform
   * as attributes.
   * @param ds for this dataset
   * @param ct based on the CoordinateTransform
   * @return the Coordinate Transform Variable. You must add it to the dataset.
   */
  static public VariableDS makeDummyTransformVariable(NetcdfDataset ds, CoordinateTransform ct) {
    VariableDS v = new VariableDS( ds, null, null, ct.getName(), DataType.CHAR, "", null, null);
    List<Parameter> params = ct.getParameters();
    for (Parameter p : params) {
      if (p.isString())
        v.addAttribute(new Attribute(p.getName(), p.getStringValue()));
      else {
        double[] data = p.getNumericValues();
        Array dataA = Array.factory(double.class, new int[]{data.length}, data);
        v.addAttribute(new Attribute(p.getName(), dataA));
      }
    }
    v.addAttribute( new Attribute(_Coordinate.TransformType, ct.getTransformType().toString()));

    // fake data
    Array data = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[] {}, new char[] {' '});
    v.setCachedData(data, true);

    return v;
  }

}
