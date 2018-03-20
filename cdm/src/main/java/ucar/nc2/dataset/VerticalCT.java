/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import ucar.nc2.constants.CF;
import ucar.nc2.dataset.transform.VertTransformBuilderIF;
import ucar.unidata.geoloc.vertical.*;
import ucar.nc2.Dimension;
import javax.annotation.concurrent.Immutable;


/**
 * A VerticalCT is a CoordinateTransform function CT: (GeoZ) -> Height or Pressure.
 * Typically it may be dependent also on X,Y and/or Time. CT: (X,Y,GeoZ,Time) -> Height or Pressure.
 * This class just records the transformation parameters. The mathematical transformation itself is
 * delegated to a class implementing ucar.unidata.geoloc.vertical.VerticalTransform.
 *
 * @author caron
 */
@Immutable
public class VerticalCT extends CoordinateTransform {
  private final VerticalCT.Type type;
  private final VertTransformBuilderIF builder;

  /**
   * Create a Vertical Coordinate Transform.
   *
   * @param name      name of transform, must be unique within the dataset.
   * @param authority naming authority.
   * @param type      type of vertical transform
   * @param builder   creates the VerticalTransform
   */
  public VerticalCT(String name, String authority, VerticalCT.Type type, VertTransformBuilderIF builder) {
    super(name, authority, TransformType.Vertical);
    this.type = type;
    this.builder = builder;
  }

  /**
   * Copy Constructor
   *
   * @param from copy from this one
   */
  public VerticalCT(VerticalCT from) {
    super(from.getName(), from.getAuthority(), from.getTransformType());
    this.type = from.getVerticalTransformType();
    this.builder = from.getBuilder();
  }

  /**
   * get the Vertical Transform type
   *
   * @return the Vertical Transform Type
   */
  public VerticalCT.Type getVerticalTransformType() {
    return type;
  }

  /**
   * Use the builder to make the Vertical Transform function
   *
   * @param ds      containing dataset
   * @param timeDim time Dimension
   * @return VerticalTransform
   * @see VertTransformBuilderIF#makeMathTransform
   */
  public VerticalTransform makeVerticalTransform(NetcdfDataset ds, Dimension timeDim) {
    return builder.makeMathTransform(ds, timeDim, this);
  }

  /**
   * get the CoordTransBuilderIF
   *
   * @return builder
   */
  public VertTransformBuilderIF getBuilder() {
    return builder;
  }

  @Override
  public String toString() {
    return "VerticalCT {" +
            "type=" + type +
            ", builder=" + builder.getTransformName() +
            '}';
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  // inner class VerticalCT.Type
  // private static final java.util.Map<String,Type> hash = new java.util.HashMap<>(10);

  /**
   * Enumeration of known Vertical transformations.
   */
  public enum Type {
    // These are from CF-1.0: not all are implemented because we dont have an example to test
    HybridSigmaPressure(CF.atmosphere_hybrid_sigma_pressure_coordinate),
    HybridHeight(CF.atmosphere_hybrid_height_coordinate),
    LnPressure(CF.atmosphere_ln_pressure_coordinate),
    OceanSigma(CF.ocean_sigma_coordinate),
    OceanS(CF.ocean_s_coordinate),
    Sleve(CF.atmosphere_sleve_coordinate),
    Sigma(CF.atmosphere_sigma_coordinate),

    //-Sachin 03/25/09
    OceanSG1("ocean_s_g1"),
    OceanSG2("ocean_s_g2"),

    // others
    Existing3DField("atmosphere_sigma"),
    WRFEta("WRFEta");

    private final String name;

    /**
     * Constructor
     *
     * @param s name of Type
     */
    Type(String s) {
      this.name = s;
    }

    /**
     * Find the VerticalCT.Type that matches this name.
     *
     * @param name find this name
     * @return VerticalCT.Type or null if no match.
     */
    public static Type getType(String name) {
      for (Type t : Type.values()) {
        if (t.name.equalsIgnoreCase(name)) return t;
      }
      return null;
    }

    public String toString() {
      return name;
    }
  }

}
