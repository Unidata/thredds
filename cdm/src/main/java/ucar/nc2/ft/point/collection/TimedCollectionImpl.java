/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.ft.point.collection;

import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateFromString;

import java.util.*;
import java.io.IOException;

import thredds.inventory.*;

/**
 * Manages feature dataset collections. Do not cache, just recreate each time. Not updating the list of datasets.
 *
 * @author caron
 * @since May 20, 2009
 */


public class TimedCollectionImpl implements TimedCollection {
  private static final boolean debug = false;
  
  private CollectionSpecParser sp;
  private List<TimedCollection.Dataset> datasets;
  private DateRange dateRange;

  /**
   * * The idea is that you can cut and paste an actual file path, then edit it:
   * spec="/data/ldm/pub/decoded/netcdf/surface/metar/** /Surface_METAR_#yyyyMMdd_HHmm#.nc
   *
   * @param collectionSpec the collection spec
   * @param errlog         put error messsages here
   * @see CollectionSpecParser
   * @throws java.io.IOException on read error
   */
  public TimedCollectionImpl(String collectionSpec, Formatter errlog) throws IOException {
    sp = new CollectionSpecParser(collectionSpec, errlog);
    CollectionManager manager = new DatasetCollectionManager(sp, errlog);

    // get the inventory, sorted by path
    manager.scan(null);
    List<MFile> fileList = manager.getFiles();
    datasets = new ArrayList<TimedCollection.Dataset>(fileList.size());
    for (MFile f : fileList)
      datasets.add(new Dataset(f));

    if (sp.getDateFormatMark() != null) {
      for (int i = 0; i < datasets.size() - 1; i++) {
        Dataset d1 = (Dataset) datasets.get(i);
        Dataset d2 = (Dataset) datasets.get(i + 1);
        d1.setDateRange(new DateRange(d1.start, d2.start));
        if (i == datasets.size() - 2)
          d2.setDateRange(new DateRange(d2.start, d1.getDateRange().getDuration()));
      }
      if (datasets.size() > 0) {
        Dataset first = (Dataset) datasets.get(0);
        Dataset last = (Dataset) datasets.get(datasets.size() - 1);
        dateRange = new DateRange(first.getDateRange().getStart().getDate(), last.getDateRange().getEnd().getDate());
      }
    }

    if (debug) {
      System.out.printf("Datasets in collection for spec=%s%n", sp);
      for (TimedCollection.Dataset d: datasets) {
        System.out.printf(" %s %n",d);
      }
      System.out.printf("%n");
    }

  }

  private TimedCollectionImpl(TimedCollectionImpl from, DateRange want) {
    datasets = new ArrayList<TimedCollection.Dataset>(from.datasets.size());
    for (TimedCollection.Dataset d : from.datasets)
      if (want.intersects(d.getDateRange()))
        datasets.add(d);
  }

  public TimedCollection.Dataset getPrototype() {
    return (datasets.size() > 0 ) ? datasets.get(0) : null;
  }

  public List<TimedCollection.Dataset> getDatasets() {
    return datasets;
  }

  public TimedCollection subset(DateRange range) {
    return new TimedCollectionImpl(this, range);
  }

  public DateRange getDateRange() {
    return dateRange;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("CollectionManager{%n");
    for (TimedCollection.Dataset d : datasets)
      f.format(" %s%n", d);
    f.format("}%n");
    return f.toString();
  }

  private class Dataset implements TimedCollection.Dataset {
    String location;
    DateRange dateRange;
    Date start;

    Dataset(MFile f) {
      this.location = f.getPath();
      if (sp.getDateFormatMark() != null)
        start = DateFromString.getDateUsingDemarkatedCount(f.getName(), sp.getDateFormatMark(), '#');
    }

    public String getLocation() {
      return location;
    }

    public DateRange getDateRange() {
      return dateRange;
    }

    public void setDateRange(DateRange dateRange) {
      this.dateRange = dateRange;
    }

    @Override
    public String toString() {
      return "Dataset{" +
              "location='" + location + '\'' +
              ", dateRange=" + dateRange +
              '}';
    }
  }

  public static void doit(String spec, Formatter errlog) throws IOException {
    TimedCollectionImpl specp = new TimedCollectionImpl(spec, errlog);
    System.out.printf("spec= %s%n%s%n", spec, specp);
    String err = errlog.toString();
    if (err.length() > 0)
      System.out.printf("%s%n", err);
    System.out.printf("-----------------------------------%n");
  }

  public static void main(String arg[]) throws IOException {
    doit("C:/data/formats/gempak/surface/#yyyyMMdd#_sao.gem", new Formatter());
    //doit("C:/data/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", new Formatter());
    // doit("Q:/station/ldm/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc", new Formatter());
  }

}
