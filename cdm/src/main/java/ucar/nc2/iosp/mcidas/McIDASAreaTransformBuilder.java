/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */



package ucar.nc2.iosp.mcidas;


import ucar.ma2.Array;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.ProjectionCT;

import ucar.nc2.dataset.TransformType;
import ucar.nc2.dataset.transform.AbstractCoordTransBuilder;


/**
 * Projection based on Mcidas Area files.
 *
 * @author caron
 */
public class McIDASAreaTransformBuilder extends AbstractCoordTransBuilder {

    /**
     * Get the transform name
     *
     * @return the transform name
     */
    public String getTransformName() {
        return McIDASAreaProjection.GRID_MAPPING_NAME;
    }

    /**
     * Get the Transform Type
     *
     * @return TransformType.Projection
     */
    public TransformType getTransformType() {
        return TransformType.Projection;
    }

    /**
     * Make the coordinate transform
     *
     * @param ds  the dataset
     * @param ctv the coordinate transform variable
     *
     * @return  the coordinate transform
     */
    public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds,
            Variable ctv) {

        int[] area = getIntArray(ctv, McIDASAreaProjection.ATTR_AREADIR);
        int[] nav  = getIntArray(ctv, McIDASAreaProjection.ATTR_NAVBLOCK);
        int[] aux  = null;
        if (ctv.findAttributeIgnoreCase(McIDASAreaProjection.ATTR_AUXBLOCK)
                != null) {
            aux = getIntArray(ctv, McIDASAreaProjection.ATTR_AUXBLOCK);
        }

        McIDASAreaProjection proj = new McIDASAreaProjection(area, nav, aux);
        return new ProjectionCT(ctv.getShortName(), "FGDC", proj);
    }

    /**
     * get the int array from the variable attribute
     *
     * @param ctv   coordinate transform variable
     * @param attName  the attribute name
     *
     * @return the int array
     */
    private int[] getIntArray(Variable ctv, String attName) {
        Attribute att = ctv.findAttribute(attName);
        if (att == null) {
            throw new IllegalArgumentException(
                "McIDASArea coordTransformVariable " + ctv.getName()
                + " must have " + attName + " attribute");
        }

        Array arr = att.getValues();
        return (int[]) arr.get1DJavaArray(int.class);
    }
}

