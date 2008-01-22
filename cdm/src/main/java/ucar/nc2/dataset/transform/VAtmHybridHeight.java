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
package ucar.nc2.dataset.transform;

import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import ucar.nc2.dataset.*;

import ucar.unidata.geoloc.vertical.HybridHeight;
import ucar.unidata.util.Parameter;

import java.util.StringTokenizer;

/**
 * Create a atmosphere_hybrid_height_coordinate Vertical Transform from
 * the information in the Coordinate Transform Variable.
 *
 * @author murray
 */
public class VAtmHybridHeight extends AbstractCoordTransBuilder {

    /** The name of the a term */
    private String a = "";

    /** The name of the a term */
    private String b = "";

    /** The name of the orog term */
    private String orog = "";

    /**
     * Get the standard name of this transform
     *
     * @return the name
     */
    public String getTransformName() {
        return "atmosphere_hybrid_height_coordinate";
    }

    /**
     * Get the type of the transform
     *
     * @return  the type
     */
    public TransformType getTransformType() {
        return TransformType.Vertical;
    }

    /**
     * Make the <code>CoordinateTransform</code> from the dataset
     *
     * @param ds  the dataset
     * @param ctv the variable with the formula
     *
     * @return  the <code>CoordinateTransform</code>
     */
    public CoordinateTransform makeCoordinateTransform(NetcdfDataset ds,
            Variable ctv) {
        String formula_terms = getFormula(ds, ctv);
        if (null == formula_terms) {
            return null;
        }

        // parse the formula string
        StringTokenizer stoke = new StringTokenizer(formula_terms);
        while (stoke.hasMoreTokens()) {
            String toke = stoke.nextToken();
            if (toke.equalsIgnoreCase("a:")) {
                a = stoke.nextToken();
            } else if (toke.equalsIgnoreCase("b:")) {
                b = stoke.nextToken();
            } else if (toke.equalsIgnoreCase("orog:")) {
                orog = stoke.nextToken();
            }
        }

        CoordinateTransform rs = new VerticalCT("AtmHybridHeight_Transform_"
                                     + ctv.getShortName(), getTransformName(),
                                         VerticalCT.Type.HybridHeight, this);
        rs.addParameter(new Parameter("standard_name", getTransformName()));
        rs.addParameter(new Parameter("formula_terms", formula_terms));

        rs.addParameter(
            new Parameter(
                "formula", "height(x,y,z) = a(z) + b(z)*orog(x,y)"));
        if ( !addParameter(rs, HybridHeight.A, ds, a)) {
            return null;
        }
        if ( !addParameter(rs, HybridHeight.B, ds, b)) {
            return null;
        }
        if ( !addParameter(rs, HybridHeight.OROG, ds, orog)) {
            return null;
        }
        return rs;
    }

    /**
     * Get a String representation of this object
     *
     * @return a String representation of this object
     */
    public String toString() {
        return "HybridHeight:" + "orog:" + orog + " a:" + a + " b:" + b;
    }

    /**
     * Make the vertical transform transform
     *
     * @param ds  the dataset
     * @param timeDim  the time dimention
     * @param vCT  the vertical coordinate transform
     *
     * @return  the VerticalTransform
     */
    public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(
            NetcdfDataset ds, Dimension timeDim, VerticalCT vCT) {
        return new HybridHeight(ds, timeDim, vCT.getParameters());
    }
}
