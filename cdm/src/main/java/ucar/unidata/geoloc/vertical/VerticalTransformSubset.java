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
package ucar.unidata.geoloc.vertical;

import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;

import ucar.ma2.Range;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

/**
 * A subset of a vertical transform.
 *
 * @author  Unidata Development Team
 */
public class VerticalTransformSubset extends VerticalTransformImpl {

    private VerticalTransform original;
    private Range t_range;
    private List<Range> subsetList = new ArrayList<Range>();

    /**
     * Create a subset of an existing VerticalTransform
     * @param original make a subset of this
     * @param t_range subset the time dimension, or null if you want all of it
     * @param z_range subset the vertical dimension, or null if you want all of it
     * @param y_range subset the y dimension, or null if you want all of it
     * @param x_range subset the x dimension, or null if you want all of it
     */
    public VerticalTransformSubset(VerticalTransform original, Range t_range,
                                   Range z_range, Range y_range,
                                   Range x_range) {
        super(null);  // timeDim not used in this class

        this.original = original;
        this.t_range  = t_range;
        subsetList.add(z_range);
        subsetList.add(y_range);
        subsetList.add(x_range);

        units = original.getUnitString();
    }

    public ArrayDouble.D3 getCoordinateArray(int subsetIndex)
            throws IOException, InvalidRangeException {
        int orgIndex = subsetIndex;
        if (isTimeDependent() && (t_range != null)) {
            orgIndex = t_range.element(subsetIndex);
        }

        ArrayDouble.D3 data = original.getCoordinateArray(orgIndex);

        return (ArrayDouble.D3) data.sectionNoReduce(subsetList);
    }

    public boolean isTimeDependent() {
        return original.isTimeDependent();
    }
}

