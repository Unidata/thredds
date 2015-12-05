/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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

