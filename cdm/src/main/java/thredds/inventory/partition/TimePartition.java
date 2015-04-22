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
package thredds.inventory.partition;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.time.CalendarPeriod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Partition the files by a user-defined time period.
 * The date is extracted from the filename using the non-optional dateExtractor.
 * All filenames must be read into memory at once, then partitioned.
 *
 * @author caron
 * @since 12/23/2014
 */
public class TimePartition extends CollectionPathMatcher implements PartitionManager {
  CalendarPeriod timePeriod;
  CalendarDateFormatter cdf;

  public TimePartition(FeatureCollectionConfig config, CollectionSpecParser specp, org.slf4j.Logger logger) {
    super(config, specp, logger);
    timePeriod = config.timePeriod;
    cdf = CalendarDateFormatter.factory(timePeriod);
  }

  public Iterable<MCollection> makePartitions(CollectionUpdateType forceCollection) throws IOException {

    List<MCollection> result = new ArrayList<>();
    CollectionListRange curr = null;
    CalendarDate startDate = null;
    CalendarDate endDate = null;

    for (MFile mfile : getFilesSorted()) {
      CalendarDate cdate = dateExtractor.getCalendarDate(mfile);
      if (cdate == null) continue; // skip - error should be logged
      if ((curr == null) || !endDate.isAfter(cdate)) {
        startDate = cdate.truncate(timePeriod.getField()); // start on a boundary
        endDate = startDate.add( timePeriod);
        String name = collectionName + "-"+ cdf.toString(startDate);
        curr = new CollectionListRange(name, root, startDate, endDate, this.logger);
        curr.putAuxInfo(FeatureCollectionConfig.AUX_CONFIG, this.config);
        if (!wasRemoved( curr))
          result.add(curr);   // skip if in removed list
      }
      //System.out.printf("%s%n", mfile);
      curr.addFile(mfile);
    }

    return result;
  }

  /////////////////////////////////////////////////////////////
  private List<String> removed;

  public void removePartition( MCollection partition) {
    if (removed == null) removed = new ArrayList<>();
    removed.add(partition.getCollectionName());
  }

  private boolean wasRemoved(MCollection partition) {
    return removed != null && (removed.contains(partition.getCollectionName()));
  }


}
