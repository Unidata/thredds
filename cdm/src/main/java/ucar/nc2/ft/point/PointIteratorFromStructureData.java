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
package ucar.nc2.ft.point;

import ucar.nc2.ft.*;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;

import java.io.IOException;

/**
 * A PointFeatureIterator which uses a StructureDataIterator to iterate over members of a Structure,
 * with optional filtering and calculation of time range and bounding box.
 * <p/>
 * Subclass must implement makeFeature() to turn the StructureData into a PointFeature.
 *
 * @author caron
 * @since Feb 29, 2008
 */
public abstract class PointIteratorFromStructureData extends PointIteratorAbstract {

  // makeFeature may return null, if so then skip it and go to next iteration
  protected abstract PointFeature makeFeature(int recnum, StructureData sdata) throws IOException;

  private Filter filter;
  private StructureDataIterator structIter;
  private PointFeature feature = null; // hasNext must cache
  private boolean finished = false;

  public PointIteratorFromStructureData(StructureDataIterator structIter, Filter filter) throws IOException {
    this.structIter = structIter;
    this.filter = filter;
  }

  public boolean hasNext() throws IOException {

    while (true) {
      StructureData sdata = nextStructureData();
      if (sdata == null) break;
      feature = makeFeature(structIter.getCurrentRecno(), sdata);
      if (feature == null) continue;
      if (feature.getLocation().isMissing()) {
        continue;
      }
      if (filter == null || filter.filter(feature))
        return true;
    }

    // all done
    feature = null;
    finish();
    return false;
  }

  public PointFeature next() throws IOException {
    if (feature == null) return null;
    calcBounds(feature);
    return feature;
  }

  public void setBufferSize(int bytes) {
    structIter.setBufferSize(bytes);
  }

  public void finish() {
    if (finished) return;
    finishCalcBounds();
    finished = true;
  }

  private StructureData nextStructureData() throws IOException {
    return structIter.hasNext() ? structIter.next() : null;
  }

}
