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
