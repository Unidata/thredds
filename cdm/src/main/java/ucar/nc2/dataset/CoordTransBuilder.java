// $Id: CoordTransBuilder.java,v 1.3 2006/05/31 20:51:11 caron Exp $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import ucar.nc2.dataset.transform.*;
import ucar.unidata.util.Parameter;
import ucar.ma2.DataType;
import ucar.ma2.Array;

import java.util.List;
import java.util.ArrayList;

/**
 * @author john caron
 * @version $Revision: 1.3 $ $Date: 2006/02/13 19:51:26 $
 */
public class CoordTransBuilder {
  static private ArrayList transformList = new ArrayList();
  static private boolean userMode = false;

  // search in the order added
  static { // wont get loaded unless explicitly called
    registerTransform("albers_conical_equal_area", AlbersEqualArea.class);
    registerTransform("lambert_azimuthal_equal_area", LambertAzimuthal.class);
    registerTransform("lambert_conformal_conic", LambertConformalConic.class);
    registerTransform("mercator", Mercator.class);
    registerTransform("orthographic", Orthographic.class);
    registerTransform("polar_stereographic", Stereographic.class);  // LOOK
    registerTransform("stereographic", Stereographic.class);
    registerTransform("transverse_mercator", TransverseMercator.class);
    registerTransform("UTM", UTM.class);

    registerTransform("atmosphere_sigma_coordinate", VAtmSigma.class);
    registerTransform("atmosphere_hybrid_sigma_pressure_coordinate", VAtmHybridSigmaPressure.class);
    registerTransform("ocean_s_coordinate", VOceanS.class);
    registerTransform("ocean_sigma_coordinate", VOceanSigma.class);
    registerTransform("explicit_field", VExplicitField.class);
    registerTransform("existing3DField", VExplicitField.class); // deprecate

    // further calls to registerTransform are by the user
    userMode = true;
  }

   /**
    * Register a class that implements a Coordinate Transform.
    * @param transformName name of transform.
    *   This name will be used to look in the "Conventions" global attribute.
    *   Otherwise, you must implement the isMine() static method.
    * @param c implementation of CoordSysBuilderIF that parses those kinds of netcdf files.
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

  static private class Transform {
    String transName;
    Class transClass;
    Transform(String transName,  Class transClass) {
      this.transName = transName;
      this.transClass = transClass;
    }
  }

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
      parseInfo.append("**Failed to find Coordinate Transform name from Variable= "+ctv+"\n");
      return null;
    }

    transform_name = transform_name.trim();

    // do we have a transform registered for this ?
    Class builderClass = null;
    for (int i = 0; i < transformList.size(); i++) {
      Transform transform = (Transform) transformList.get(i);
      if (transform.transName.equals(transform_name)) {
        builderClass = transform.transClass;
        break;
      }
    }
    if (null == builderClass) {
      parseInfo.append("**Failed to find CoordTransBuilder name= "+transform_name+" from Variable= "+ctv+"\n");
      return null;
    }

      // get an instance of that class
    CoordTransBuilderIF builder = null;
    try {
      builder = (CoordTransBuilderIF) builderClass.newInstance();
    } catch (InstantiationException e) {
    } catch (IllegalAccessException e) {
    }
    if (null == builder) { // cant happen
      parseInfo.append("**Failed to build CoordTransBuilder object from class= "+builderClass.getName()+" for Variable= "+ctv+"\n");
      return null;
    }

    builder.setErrorBuffer(errInfo);
    CoordinateTransform ct = builder.makeCoordinateTransform(ds, ctv);

    if (ct != null) {
      parseInfo.append(" Made Coordinate transform "+transform_name+" from variable "+ctv.getName()+": "+builder+"\n");
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
    List params = ct.getParameters();
    for (int i = 0; i < params.size(); i++) {
      Parameter p = (Parameter) params.get(i);
      if (p.isString())
        v.addAttribute( new Attribute(p.getName(), p.getStringValue()));
      else {
        double[] data = p.getNumericValues();
        Array dataA = Array.factory(double.class, new int[] {data.length}, data);
        v.addAttribute( new Attribute(p.getName(), dataA));
      }
    }
    v.addAttribute( new Attribute("_CoordinateTransformType", ct.getTransformType().toString()));

    // fake data
    Array data = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[] {}, new char[] {' '});
    v.setCachedData(data, true);

    return v;
  }

}
