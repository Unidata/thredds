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

import ucar.nc2.ft.NestedPointFeatureCollectionIterator;
import ucar.nc2.ft.NestedPointFeatureCollection;

import java.io.IOException;

/**
 * Implement NestedPointFeatureCollectionIterator interface
 * @author caron
 * @since Mar 20, 2008
 */
public class NestedPointCollectionIteratorFiltered implements NestedPointFeatureCollectionIterator {

  private NestedPointFeatureCollectionIterator npfciter;
  private NestedPointFeatureCollectionIterator.Filter filter;

  private NestedPointFeatureCollection pointFeatureCollection;
  private boolean done = false;

  NestedPointCollectionIteratorFiltered(NestedPointFeatureCollectionIterator npfciter, NestedPointFeatureCollectionIterator.Filter filter) {
    this.npfciter = npfciter;
    this.filter = filter;
  }

  public void setBufferSize(int bytes) {
    npfciter.setBufferSize(bytes);
  }

  public boolean hasNext() throws IOException {
    if (done) return false;
    pointFeatureCollection = nextFilteredPointFeatureCollection();
    return (pointFeatureCollection != null);
  }

  public NestedPointFeatureCollection next() throws IOException {
    return done ? null : pointFeatureCollection;
  }

  private boolean filter(NestedPointFeatureCollection pdata) {
    return (filter == null) || filter.filter(pdata);
  }

  private NestedPointFeatureCollection nextFilteredPointFeatureCollection() throws IOException {
    if ( npfciter == null) return null;
    if (!npfciter.hasNext()) return null;

    NestedPointFeatureCollection pdata = npfciter.next();
    if (!filter(pdata)) {
      if (!npfciter.hasNext()) return null;
      pdata = npfciter.next();
    }

    return pdata;
  }

}



