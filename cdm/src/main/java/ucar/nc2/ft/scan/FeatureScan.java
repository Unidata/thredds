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
package ucar.nc2.ft.scan;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.constants.FeatureType;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Formatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Scan a directory, try to open files as a Feature Type dataset.
 *
 * @author caron
 * @since Aug 15, 2009
 */


public class FeatureScan {
  private String top;
  private boolean subdirs;

  public FeatureScan(String top, boolean subdirs) {
    this.top = top;
    this.subdirs = subdirs;
  }

  public java.util.List<FeatureScan.Bean> scan(Formatter errlog) {

    List<Bean> result = new ArrayList<Bean>();

    File topFile = new File(top);
    if (!topFile.exists()) {
      errlog.format("File %s does not exist", top);
      return result;
    }

    if (topFile.isDirectory())
      scanDirectory(topFile, result, errlog);
    else {
      Bean fdb = new Bean(topFile);
      result.add(fdb);
    }

    return result;
  }

  private void scanDirectory(File dir, java.util.List<FeatureScan.Bean> result, Formatter errlog) {

    // get list of files
    List<File> files = new ArrayList<File>();
    for (File f : dir.listFiles()) {
      if (!f.isDirectory()) {
        files.add(f);
      }
    }

    // eliminate redundant files
    // ".Z", ".zip", ".gzip", ".gz", or ".bz2"
    if (files.size() > 0) {
      Collections.sort(files);
      ArrayList<File> files2 = new ArrayList<File>(files);

      File prev = null;
      for (File f : files) {
        if (prev != null) {
          String name = f.getName();
          String stem = stem(name);
          if (name.endsWith(".gbx") || name.endsWith(".gbx8")) {
            files2.remove(f);
          } else if (name.endsWith(".ncml")) {
            if (prev.getName().equals(stem+".nc"))
              files2.remove(prev);
          } else if (name.endsWith(".bz2")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          } else if (name.endsWith(".gz")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          } else if (name.endsWith(".gzip")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          } else if (name.endsWith(".zip")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          } else if (name.endsWith(".Z")) {
            if (prev.getName().equals(stem)) files2.remove(f);
          }
        }
        prev = f;
      }

      // do the remaining
      for (File f : files2) {
        result.add(new Bean(f));
      }
    }

    // do subdirs
    if (subdirs) {
      for (File f : dir.listFiles()) {
        if (f.isDirectory())
          scanDirectory(f, result, errlog);
      }
    }

  }

  private String stem(String name) {
    int pos = name.lastIndexOf(".");
    return (pos > 0) ? name.substring(0, pos) : name;
  }

  private boolean debug = false;
  public class Bean {
    public File f;
    String fileType;
    String coordMap;
    FeatureType featureType;
    String ftype;
    String info;
    String ftImpl;
    Throwable problem;

    // no-arg constructor
    public Bean() {
    }

    public Bean(File f) {
      this.f = f;

      NetcdfDataset ds = null;
      try {
        if (debug) System.out.printf(" featureScan=%s%n", f.getPath());
        ds = NetcdfDataset.openDataset(f.getPath());
        fileType = ds.getFileTypeId();
        setCoordMap(ds.getCoordinateSystems());

        Formatter errlog = new Formatter();
        try {
          FeatureDataset featureDataset = FeatureDatasetFactoryManager.wrap(null, ds, null, errlog);
          if (featureDataset != null) {
            featureType = featureDataset.getFeatureType();
            if (featureType != null)
              ftype = featureType.toString();
            ftImpl = featureDataset.getImplementationName();
            Formatter infof = new Formatter();
            featureDataset.getDetailInfo(infof);
            info = infof.toString();
          } else {
            ftype = "FAIL: " + errlog.toString();
          }
        } catch (Throwable t) {
          ftype = "ERR: " + t.getMessage();
          info = errlog.toString();
          problem = t;
        }

      } catch (Throwable t) {
        fileType = "ERR: " + t.getMessage();
        problem = t;

      } finally {
        if (ds != null) try {
          ds.close();
        } catch (IOException ioe) {
        }
      }
    }


    public String getName() {
      return f.getPath();
    }

    public String getFileType() {
      return fileType;
    }

    public String getCoordMap() {
      return coordMap;
    }

    public void setCoordMap(java.util.List<CoordinateSystem> csysList) {
      CoordinateSystem use = null;
      for (CoordinateSystem csys : csysList) {
        if (use == null) use = csys;
        else if (csys.getCoordinateAxes().size() > use.getCoordinateAxes().size())
          use = csys;
      }
      coordMap = (use == null) ? "" : "f:D(" + use.getRankDomain() + ")->R(" + use.getRankRange() + ")";
    }

    public String getFeatureType() {
      return ftype;
    }

    public String getFeatureImpl() {
      return ftImpl;
    }

    public String toString() {
      Formatter f = new Formatter();
      f.format("%s%n %s%n map = '%s'%n %s%n %s%n", getName(), getFileType(), getCoordMap(), getFeatureType(), getFeatureImpl());
      if (info != null) {
        f.format("\n%s", info);
      }
      if (problem != null) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        problem.printStackTrace(new PrintStream(bout));
        f.format("\n%s", bout.toString());
      }
      return f.toString();
    }

  }

  public static void main(String arg[]) {
    FeatureScan scanner = new FeatureScan("C:/data/datasets/modis", true);

    System.out.printf("Beans found %n");
    List<FeatureScan.Bean> beans = scanner.scan(new Formatter());
    for (Bean b : beans)
      System.out.printf(" %40s %20s %10s %10s%n", b.getName(), b.getFileType(), b.getFeatureType(), b.getFeatureImpl());

  }


}
