/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib.grib2;

import thredds.inventory.CollectionManager;
import thredds.inventory.DatasetCollectionMFiles;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grib.GribCollection;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Grib2 specific part of GribCollection
 *
 * @author John
 * @since 9/5/11
 */
public class Grib2Collection extends ucar.nc2.grib.GribCollection {

  public Grib2Collection(String name, File directory) {
    super(name, directory);
  }

  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(String groupName, String filename) throws IOException {
    GroupHcs want = findGroup(groupName);
    if (want == null) return null;

    if (filename == null) {  // LOOK thread-safety : sharing this, raf
      Grib2Iosp iosp = new Grib2Iosp(want);
      NetcdfFile ncfile = new MyNetcdfFile(iosp, null, getIndexFile().getPath(), null);
      return new NetcdfDataset(ncfile);

    } else {

      for (String file : filenames) { // LOOK linear lookup
        if (file.endsWith(filename)) {
          Formatter f = new Formatter();
          GribCollection gc = Grib2CollectionBuilder.createFromSingleFile(new File(file), CollectionManager.Force.nocheck,f);  // LOOK thread-safety : creating ncx

          Grib2Iosp iosp = new Grib2Iosp(gc);
          NetcdfFile ncfile = new MyNetcdfFile(iosp, null, getIndexFile().getPath(), null);
          return new NetcdfDataset(ncfile);
        }
      }
      return null;
    }
  }

  public ucar.nc2.dt.GridDataset getGridDataset(String groupName, String filename) throws IOException {
    GroupHcs want = findGroup(groupName);
    if (want == null) return null;

    if (filename == null) { // LOOK thread-safety : sharing this, raf
      Grib2Iosp iosp = new Grib2Iosp(want);
      NetcdfFile ncfile = new MyNetcdfFile(iosp, null, getIndexFile().getPath(), null);
      NetcdfDataset ncd = new NetcdfDataset(ncfile);
      return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??

    } else {
      for (String file : filenames) {  // LOOK linear lookup
        if (file.endsWith(filename)) {
          Formatter f = new Formatter();
          GribCollection gc = Grib2CollectionBuilder.createFromSingleFile(new File(file), CollectionManager.Force.nocheck,f);  // LOOK thread-safety : creating ncx

          Grib2Iosp iosp = new Grib2Iosp(gc);
          NetcdfFile ncfile = new MyNetcdfFile(iosp, null, getIndexFile().getPath(), null);
          NetcdfDataset ncd = new NetcdfDataset(ncfile);
          return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??
        }
      }
      return null;
    }
  }

    ///////////////////////////////////////////////////////////////////////////////

  static public void make(String name, String spec) throws IOException {
    long start = System.currentTimeMillis();
    Formatter f = new Formatter();
    CollectionManager dcm = new DatasetCollectionMFiles(name, spec, f);
    File idxFile = new File( dcm.getRoot(), name);
    boolean ok = Grib2CollectionBuilder.writeIndexFile(idxFile, dcm, f);
    System.out.printf("GribCollectionBuilder.writeIndexFile ok = %s%n", ok);

    long took = System.currentTimeMillis() - start;
    System.out.printf("%s%n", f);
    System.out.printf("That took %d msecs%n", took);
  }

  public static void main(String[] args) throws IOException {
    for (int i=0; i<args.length; i++) {
      String arg = args[i];
      if (arg.equalsIgnoreCase("-help")) {
        System.out.printf("usage: ucar.nc2.grib.GribCollection [-make name collSpec] [-read filename]%n");
        break;
      }
      if (arg.equalsIgnoreCase("-make")) {
        make(args[i+1], args[i+2]);
        break;

      } else if (arg.equalsIgnoreCase("-read")) {
        File f = new File(args[i+1]);
        RandomAccessFile raf = new RandomAccessFile(f.getPath(), "r");
        GribCollection gc = Grib2CollectionBuilder.createFromIndex(f.getName(), f.getParentFile(), raf);
        gc.showIndex(new Formatter(System.out));
        break;
      }
    }
    // "G:/nomads/timeseries/200808/.*grb2$"
    // readIndex2("G:/nomads/timeseries/200808/GaussLatLon-576X1152.ncx");
  }

}
