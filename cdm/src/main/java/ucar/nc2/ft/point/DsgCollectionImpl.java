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
 *
 */
package ucar.nc2.ft.point;

import ucar.nc2.Variable;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Common methods for DsgFeatureCollection.
 *
 * @author caron
 * @since 9/25/2015.
 */
public abstract class DsgCollectionImpl implements DsgFeatureCollection {

  protected String name;
  protected CalendarDateUnit timeUnit;
  protected String altUnits;
  protected CollectionInfo info;
  protected List<Variable> extras; // variables needed to make CF/DSG writing work

  protected DsgCollectionImpl(String name, CalendarDateUnit timeUnit, String altUnits) {
    this.name = name;
    this.timeUnit = timeUnit;
    this.altUnits = altUnits;
  }

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Nonnull
  @Override
  public CalendarDateUnit getTimeUnit() {
    return timeUnit;
  }

  @Nullable
  @Override
  public String getAltUnits() {
    return altUnits;
  }

  @Nonnull
  @Override
  public List<Variable> getExtraVariables() { return (extras == null) ? new ArrayList<>() : extras; }

  @Override
  public int size() {
    return getNobs();
  }

  public int getNobs() {
    return (info == null) ? -1 : info.npts;
  }

  @Nullable
  @Override
  public CalendarDateRange getCalendarDateRange() {
    return (info == null) ? null : info.getCalendarDateRange(timeUnit);
  }

  @Nullable
  @Override
  public ucar.unidata.geoloc.LatLonRect getBoundingBox() {
    return (info == null) ? null : info.bbox;
  }

  @Nonnull
  public CollectionInfo getInfo() { // LOOK exposes mutable fields
    if (info == null)
      info = new CollectionInfo();
    return info;
  }
}
