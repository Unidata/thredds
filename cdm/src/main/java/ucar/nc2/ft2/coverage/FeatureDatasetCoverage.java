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

package ucar.nc2.ft2.coverage;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import com.google.common.collect.Lists;
import ucar.nc2.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.geoloc.LatLonRect;

import javax.annotation.Nullable;

/**
 * A FeatureDataset with Coverage Features.
 * Some endpoints (eg files) can have multiple CoverageCollections.
 * A CoverageCollection must have a single HorizCoordSys and Calendar.
 * Grib collections often have multiple CoverageCollections (TwoD, Best).
 *
 * @author John
 * @since 8/8/2015
 */
public class FeatureDatasetCoverage implements FeatureDataset, Closeable {
  private final String location;
  private final AttributeContainer gatts;
  private final Closeable closer;
  private final List<CoverageCollection> covCollections;
  private final FeatureType featureType;
  private final CalendarDateRange calendarDateRange;

  public FeatureDatasetCoverage(String location, Closeable closer, CoverageCollection covCollection) {
    this.location = location;
    this.gatts = new AttributeContainerHelper(location, covCollection.getGlobalAttributes());
    this.closer = closer;
    this.covCollections = Lists.newArrayList(covCollection);
    this.featureType = covCollection.getCoverageType();
    this.calendarDateRange = covCollection.getCalendarDateRange();
  }

  public FeatureDatasetCoverage(String location, AttributeContainer gatts, Closeable closer, List<CoverageCollection> covCollections) {
    this.location = location;
    this.gatts = gatts;
    this.closer = closer;
    this.covCollections = covCollections;

    CalendarDateRange cdr = null;
    FeatureType ft = null;
    for (CoverageCollection cc : covCollections) {
      FeatureType ftCheck = cc.getCoverageType();
      if (ft == null) ft = ftCheck;
      else if (ftCheck != ft) ft = FeatureType.COVERAGE;

      CalendarDateRange cdrCheck = cc.getCalendarDateRange();
      if (cdr == null) cdr = cdrCheck;
      else if (cdrCheck != null) cdr = cdr.extend( cdrCheck);

    }
    this.featureType = ft;
    this.calendarDateRange = cdr;
  }

  public List<CoverageCollection> getCoverageCollections() {
    return covCollections;
  }
  public CoverageCollection getSingleCoverageCollection() {
    if (covCollections.size() != 1) throw new RuntimeException("multiple collection in the dataset");
    return covCollections.get(0);
  }

  public CoverageCollection findCoverageDataset( FeatureType type) {
    for (CoverageCollection cd : covCollections)
      if (cd.getCoverageType() == type) return cd;
    return null;
  }

  public CoverageCollection findCoverageDataset( String name) {
    for (CoverageCollection cd : covCollections)
      if (cd.getName().equals(name)) return cd;
    return null;
  }

  /////////////////////////////////////////////
  // FeatureDataset

  @Override
  public FeatureType getFeatureType() {
    return featureType;
  }

  @Override
  public String getTitle() {
    return location;
  }

  @Override
  public String getDescription() {
    return location;
  }

  @Override
  public String getLocation() {
    return location;
  }

  @Override
  public CalendarDateRange getCalendarDateRange() {
    return calendarDateRange;
  }

  @Override
  public CalendarDate getCalendarDateStart() {
    return calendarDateRange == null ? null : calendarDateRange.getStart();
  }

  @Override
  public CalendarDate getCalendarDateEnd() {
    return calendarDateRange == null ? null : calendarDateRange.getEnd();
  }

  @Override
  public LatLonRect getBoundingBox() {
    return null;
  }

  @Override
  public List<Attribute> getGlobalAttributes() {
    return gatts.getAttributes();
  }

  @Override
  public Attribute findGlobalAttributeIgnoreCase(String name) {
    return gatts.findAttributeIgnoreCase(name);
  }

  @Override
  public List<VariableSimpleIF> getDataVariables() {
    List<VariableSimpleIF> result = new ArrayList<>();
    for (CoverageCollection cc : covCollections) {
      for (Coverage cov : cc.getCoverages())
        result.add(cov);
    }
    return result;
  }

  @Override
  public VariableSimpleIF getDataVariable(String shortName) {
    for (CoverageCollection cc : covCollections) {
      VariableSimpleIF result =  cc.findCoverage(shortName);
      if (result != null) return result;
    }
    return null;
  }

  @Nullable
  @Override
  public NetcdfFile getNetcdfFile() {
    return null;
  }

  @Override
  public void getDetailInfo(Formatter sf) {
    for (CoverageCollection cc : covCollections) {
      cc.toString(sf);
    }
  }

  @Override
  public String getImplementationName() {
    return closer.getClass().getName();
  }

  @Override
  public long getLastModified() {
    return 0; // LOOK
  }

  private FileCacheIF fileCache; // LOOK mutable

  @Override
  public synchronized void setFileCache(FileCacheIF fileCache) {
    this.fileCache = fileCache;
  }

  @Override
  public void release() throws IOException {
    // reader.release()
  }

  @Override
  public void reacquire() throws IOException {
    // reader.reacquire()
  }

  public synchronized void close() throws java.io.IOException {
    if (fileCache != null) {
      if (fileCache.release(this)) return;
    }
    reallyClose();
  }

  private void reallyClose() throws IOException {
    try {
      closer.close();
    } catch (IOException ioe) {
      throw ioe;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

}
