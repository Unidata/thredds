// $Id: VerticalCT.java,v 1.7 2006/05/24 00:12:59 caron Exp $
/*
 * Copyright 2002-2004 Unidata Program Center/University Corporation for
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

import ucar.unidata.geoloc.vertical.*;
import ucar.nc2.Dimension;


/**
 * A VerticalCT is a CoordinateTransform function CT: (GeoZ) -> Height or Pressure.
 * Typically it may be dependent also on X,Y and/or Time. CT: (X,Y,GeoZ,Time) -> Height or Pressure.
 * This class just records the transformation parameters. The mathematical transformation itself is
 * delegated to a class implementing ucar.unidata.geoloc.vertical.VerticalTransform;
 *
 * @author caron
 * @version $Revision: 1.7 $ $Date: 2006/05/24 00:12:59 $
 */

public class VerticalCT extends CoordinateTransform {
   private VerticalTransform vt;
   private VerticalCT.Type type;
   private CoordTransBuilderIF builder;

  /**
   * Create a Vertical Coordinate Transform.
   * @param name name of transform, must be unique within the dataset.
   * @param authority naming authority.
   * @param type type of vertical transform
   * @param builder creates the VerticalTransform
   */
  public VerticalCT (String name, String authority, VerticalCT.Type type, CoordTransBuilderIF builder) {
    super( name, authority, TransformType.Vertical);
    this.type = type;
    this.builder = builder;
  }

  /**
   * Copy Constructor
   * @param from copy from this one
   */
  public VerticalCT (VerticalCT from) {
    super( from.getName(), from.getAuthority(), from.getTransformType());
    this.type = from.getVerticalTransformType();
    this.builder = from.getBuilder();
    this.vt = from.getVerticalTransform();
  }

  /** get the Vertical Transform type */
  public VerticalCT.Type getVerticalTransformType() { return type; }

  /** get the Vertical Transform function */
  public VerticalTransform getVerticalTransform() { return vt; }

  /** get the Vertical Transform function */
  public CoordTransBuilderIF getBuilder() { return builder; }

  /** set the Vertical Transform function */
  public void setVerticalTransform(VerticalTransform vt ) { this.vt = vt; }

  /** use the builder to make the Vertical Transform function */
  public VerticalTransform makeVerticalTransform(NetcdfDataset ds, Dimension timeDim) {
    this.vt = builder.makeMathTransform(ds, timeDim, this);
    return this.vt;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // inner class VerticalCT.Type
  private static java.util.HashMap hash = new java.util.HashMap(10);

  /**
   * Enumeration of known Vertical transformations.
   */
  public static class Type {
    // These are from CF-1.0: not all are implemented because we dont have an example to test
    public final static Type Sigma = new Type("atmosphere_sigma");
    public final static Type HybridSigmaPressure = new Type("atmosphere_hybrid_sigma_pressure");
    public final static Type HybridHeight = new Type("atmosphere_hybrid_height");
    public final static Type Sleve = new Type("atmosphere_sleve");
    public final static Type OceanSigma = new Type("ocean_sigma");
    public final static Type OceanS = new Type("ocean_s");
    public final static Type OceanSigmaZ = new Type("ocean_sigma_z");

    // others
    public final static Type Existing3DField = new Type("Existing3DField");
    public final static Type WRFEta = new Type("WRFEta");

    private String name;

    /** Constructor */
    public Type(String s) {
      this.name = s;
      hash.put( s, this);
    }

    /**
     * Find the VerticalCT.Type that matches this name.
     * @param name
     * @return VerticalCT.Type or null if no match.
     */
    public static Type getType(String name) {
      if (name == null) return null;
      return (Type) hash.get( name);
    }
    public String toString() { return name; }
  }

}