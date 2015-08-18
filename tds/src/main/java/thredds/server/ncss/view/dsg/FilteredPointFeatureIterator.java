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
package thredds.server.ncss.view.dsg;

import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.PointIteratorAbstract;

import java.io.IOException;

/**
 * Wraps an ordinary {@link ucar.nc2.ft.PointFeatureIterator}, but only retains elements that satisfy a provided
 * {@link ucar.nc2.ft.PointFeatureIterator.Filter filter}.
 *
 * @author cwardgar
 */
public class FilteredPointFeatureIterator extends PointIteratorAbstract {
    private final PointFeatureIterator origIter;
    private final PointFeatureIterator.Filter filter;
    private PointFeature pointFeature;

    public FilteredPointFeatureIterator(PointFeatureIterator origIter, PointFeatureIterator.Filter filter) {
        this.origIter = origIter;
        this.filter = filter;
    }

    ///////////////////////////////////////////// PointFeatureIterator /////////////////////////////////////////////

    @Override
    public boolean hasNext() throws IOException {
        pointFeature = nextFilteredDataPoint();
        if (pointFeature == null) {
            finish();
            return false;
        } else {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return  the next PointFeature, or {@code null} if this iterator contains no more elements.
     */
    // PointFeatureIterator.next() doesn't actually specify the behavior for next() when there are no more elements,
    // but we can define a stronger contract.
    @Override
    public PointFeature next() throws IOException {
        if (pointFeature == null) {
            return null;
        }

        calcBounds(pointFeature);
        return pointFeature;
    }

    @Override
    public void finish() {
        origIter.finish();
        finishCalcBounds();
    }

    @Override
    public void setBufferSize(int bufferSize) {
        origIter.setBufferSize(bufferSize);
    }

    /**
     * Returns the next point that satisfies the filter, or {@code null} if no such point exists.
     *
     * @return  the next point that satisfies the filter, or {@code null} if no such point exists.
     * @throws java.io.IOException  if an I/O error occurs.
     */
    private PointFeature nextFilteredDataPoint() throws IOException {
        while (origIter.hasNext()) {
            PointFeature pointFeat = origIter.next();
            if (filter.filter(pointFeat)) {
                return pointFeat;
            }
        }

        return null;
    }
}
