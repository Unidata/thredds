/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.mcidas;

import ucar.ma2.Array;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.ProjectionCT;

import ucar.nc2.dataset.transform.AbstractTransformBuilder;
import ucar.nc2.dataset.transform.HorizTransformBuilderIF;

/**
 * Projection based on Mcidas Area files.
 *
 * @author caron
 */
public class McIDASAreaTransformBuilder extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  /**
   * Get the transform name
   *
   * @return the transform name
   */
  public String getTransformName() {
    return McIDASAreaProjection.GRID_MAPPING_NAME;
  }

  /**
   * Make the coordinate transform
   *
   * @param ctv the coordinate transform variable
   * @return the coordinate transform
   */
  public ProjectionCT makeCoordinateTransform(AttributeContainer ctv, String units) {

    int[] area = getIntArray(ctv, McIDASAreaProjection.ATTR_AREADIR);
    int[] nav = getIntArray(ctv, McIDASAreaProjection.ATTR_NAVBLOCK);
    int[] aux = null;
    if (ctv.findAttributeIgnoreCase(McIDASAreaProjection.ATTR_AUXBLOCK) != null) {
      aux = getIntArray(ctv, McIDASAreaProjection.ATTR_AUXBLOCK);
    }
    // not clear if its ok if aux is null, coverity is complaining

    McIDASAreaProjection proj = new McIDASAreaProjection(area, nav, aux);
    return new ProjectionCT(ctv.getName(), "FGDC", proj);
  }

  /**
   * get the int array from the variable attribute
   *
   * @param ctv     coordinate transform variable
   * @param attName the attribute name
   * @return the int array
   */
  private int[] getIntArray(AttributeContainer ctv, String attName) {
    Attribute att = ctv.findAttribute(attName);
    if (att == null) {
      throw new IllegalArgumentException("McIDASArea coordTransformVariable " + ctv.getName() + " must have " + attName + " attribute");
    }

    Array arr = att.getValues();
    return (int[]) arr.get1DJavaArray(int.class);
  }
}

