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
package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.OneNestedPointCollectionImpl;
import ucar.nc2.ft.point.ProfileFeatureImpl;
import ucar.nc2.ft.point.PointCollectionIteratorFiltered;
import ucar.nc2.ft.*;
import ucar.nc2.units.DateUnit;
import ucar.nc2.constants.FeatureType;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureData;
import ucar.unidata.geoloc.LatLonRect;

import java.io.IOException;
import java.util.Date;

/**
 * Nested Table implementation of ProfileCollection
 * @author caron
 * @since Jan 20, 2009
 */
public class StandardProfileCollectionImpl extends OneNestedPointCollectionImpl implements ProfileFeatureCollection {
  private DateUnit timeUnit;
  private NestedTable ft;

  protected StandardProfileCollectionImpl(String name) {
    super(name, FeatureType.PROFILE);
  }

  StandardProfileCollectionImpl(NestedTable ft, DateUnit timeUnit) {
    super(ft.getName(), FeatureType.PROFILE);
    this.ft = ft;
    this.timeUnit = timeUnit;
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    return new ProfileIterator( ft.getRootFeatureDataIterator(bufferSize));
  }

  private ProfileIterator localIterator = null;
  public boolean hasNext() throws IOException {
    if (localIterator == null) resetIteration();
    return localIterator.hasNext();
  }

  // need covariant return to allow superclass to implement
  public ProfileFeature next() throws IOException {
    return localIterator.next();
  }

  public void resetIteration() throws IOException {
    localIterator = (ProfileIterator) getPointFeatureCollectionIterator(-1);
  }

  public ProfileFeatureCollection subset(LatLonRect boundingBox) throws IOException {
    return new StandardProfileCollectionSubset( this, boundingBox);
  }

  private class ProfileIterator implements PointFeatureCollectionIterator {
    StructureDataIterator structIter;
    StructureData nextProfileData;

    ProfileIterator(ucar.ma2.StructureDataIterator structIter) throws IOException {
      this.structIter = structIter;
    }

    public boolean hasNext() throws IOException {
      while (true) {
        if(!structIter.hasNext()) return false;
        nextProfileData = structIter.next();
        if (!ft.isFeatureMissing(nextProfileData)) break;
      }
      return true;
    }


    public ProfileFeature next() throws IOException {
      Cursor cursor = new Cursor(ft.getNumberOfLevels());
      cursor.tableData[1] = nextProfileData;
      cursor.recnum[1] = structIter.getCurrentRecno();
      cursor.currentIndex = 1;
      ft.addParentJoin(cursor); // there may be parent joins
      return new StandardProfileFeature(cursor, ft.getObsTime(cursor));
    }

    public void setBufferSize(int bytes) { }

    public void finish() {
    }

  }

  private class StandardProfileFeature extends ProfileFeatureImpl {
    Cursor cursor;
    StandardProfileFeature( Cursor cursor, double time) {
      super( timeUnit.makeStandardDateString(time), ft.getLatitude(cursor), ft.getLongitude(cursor), time, -1);
      this.cursor = cursor;

      if (Double.isNaN(time)) { // gotta read an obs to get the time
        try {
          PointFeatureIterator iter = getPointFeatureIterator(-1);
          if (iter.hasNext()) {
            PointFeature pf = iter.next();
            this.time = pf.getObservationTime();
            this.name = timeUnit.makeStandardDateString(this.time);
          } else {
            this.name = "empty";
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      Cursor cursorIter = cursor.copy();
      StructureDataIterator siter = ft.getLeafFeatureDataIterator( cursorIter, bufferSize);
      StandardPointFeatureIterator iter = new StandardProfileFeatureIterator(ft, timeUnit, siter, cursorIter);
      if ((boundingBox == null) || (dateRange == null) || (npts < 0))
        iter.setCalculateBounds(this);
      return iter;
    }

    @Override
    public Date getTime() {
      return timeUnit.makeDate(time);
    }

    class StandardProfileFeatureIterator extends StandardPointFeatureIterator {

      StandardProfileFeatureIterator(NestedTable ft, DateUnit timeUnit, StructureDataIterator structIter, Cursor cursor) throws IOException {
        super(ft, timeUnit, structIter, cursor);
      }

      @Override
      protected boolean isMissing() throws IOException {
        // standard filter is to check for missing time data
        if (super.isMissing()) return true;

        // must also check for missing z values
        return ft.isAltMissing(this.cursor);

      }
    }
  }

  private class StandardProfileCollectionSubset extends StandardProfileCollectionImpl {
    StandardProfileCollectionImpl from;
      LatLonRect boundingBox;

    StandardProfileCollectionSubset(StandardProfileCollectionImpl from, LatLonRect boundingBox) {
      super(from.getName()+"-subset");
      this.from = from;
      this.boundingBox = boundingBox;
    }

    public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
      return new PointCollectionIteratorFiltered( from.getPointFeatureCollectionIterator(bufferSize), new Filter());
    }

    private class Filter implements PointFeatureCollectionIterator.Filter {

      public boolean filter(PointFeatureCollection pointFeatureCollection) {
        ProfileFeature profileFeature = (ProfileFeature) pointFeatureCollection;
        return boundingBox.contains(profileFeature.getLatLon());
      }
    }
  }

}
