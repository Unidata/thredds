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

package thredds.inventory;

import ucar.nc2.units.DateRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

/**
 * Manage collections of files that we can assign date ranges to.
 * A wrap of DatasetCollectionManager.
 *
 * @author caron
 * @since May 19, 2009
 */

public class TimedCollection {
  private static final boolean debug = false;

  private final DatasetCollectionManager manager;
  private List<TimedCollection.Dataset> datasets;
  private DateRange dateRange;

  /**
   * Manage collections of files that we can assign date ranges to
   *
   * @param manager the collection manager
   * @param errlog         put error messsages here
   * @see CollectionSpecParser
   * @throws java.io.IOException on read error
   */
  public TimedCollection(DatasetCollectionManager manager, Formatter errlog) throws IOException {
    this.manager = manager;

    // get the inventory, sorted by path
    manager.scan(null);
    update();

    if (debug) {
      System.out.printf("Datasets in collection=%s%n", manager.getCollectionName());
      for (TimedCollection.Dataset d: datasets) {
        System.out.printf(" %s %n",d);
      }
      System.out.printf("%n");
    }

  }

  public void update() {
    List<MFile> fileList = manager.getFiles();
    datasets = new ArrayList<TimedCollection.Dataset>(fileList.size());
    for (MFile f : fileList)
      datasets.add(new Dataset(f));

    if (manager.hasDateExtractor()) {

      if (datasets.size() == 1) {
        Dataset ds = (Dataset) datasets.get(0);
        if (ds.start != null)
          dateRange = new DateRange(ds.start, ds.start); // LOOK ??

      } else if (datasets.size() > 1) {

        for (int i = 0; i < datasets.size() - 1; i++) {
          Dataset d1 = (Dataset) datasets.get(i);
          Dataset d2 = (Dataset) datasets.get(i + 1);
          d1.setDateRange(new DateRange(d1.start, d2.start));
          if (i == datasets.size() - 2) // last one
            d2.setDateRange(new DateRange(d2.start, d1.getDateRange().getDuration()));
        }

        Dataset first = (Dataset) datasets.get(0);
        Dataset last = (Dataset) datasets.get(datasets.size() - 1);
        dateRange = new DateRange(first.getDateRange().getStart().getDate(), last.getDateRange().getEnd().getDate());
      }
    }
  }

  private TimedCollection(TimedCollection from, DateRange want) {
    this.manager = from.manager;
    datasets = new ArrayList<TimedCollection.Dataset>(from.datasets.size());
    for (TimedCollection.Dataset d : from.datasets)
      if (want.intersects(d.getDateRange()))
        datasets.add(d);
    this.dateRange = want;
  }

  public TimedCollection.Dataset getPrototype() {
    int idx = manager.getProtoIndex();
    return datasets.get(idx);
  }

  public List<TimedCollection.Dataset> getDatasets() {
    return datasets;
  }

  public TimedCollection subset(DateRange range) {
    return new TimedCollection(this, range);
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

  /**
   ** The Dataset.getLocation() can be passed to FeatureDatasetFactoryManager.open().
   */
  public class Dataset {
    String location;
    DateRange dateRange;
    Date start;

    Dataset(MFile f) {
      this.location = f.getPath();
      this.start = manager.extractRunDate(f);
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

  //////////////////////////////////////////////////////////////////////////
  // debugging
  private static void doit(String spec, Formatter errlog) throws IOException {
    DatasetCollectionManager dcm = DatasetCollectionManager.open(spec, null, errlog);
    TimedCollection specp = new TimedCollection(dcm, errlog);
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

