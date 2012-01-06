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

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.grib.TimePartition;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A collection of GribCollection objects which are Time Partitioned.
 * A TimePartition is the collection; a TimePartition.Partition represents one of the GribCollection.
 * Everything is done with lazy instantiation.
 *
 * @author caron
 * @since 4/17/11
 */
public class Grib2TimePartition extends TimePartition {
  static private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Grib2TimePartition.class);

  Grib2TimePartition(String name, File directory) {
    super(name, directory);
  }

  // public abstract ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(String groupName, String filename) throws IOException;
  // public abstract ucar.nc2.dt.GridDataset getGridDataset(String groupName, String filename) throws IOException;


  // LOOK - needs time partition collection iosp or something
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(String groupName) throws IOException {
    GroupHcs want = findGroup(groupName);
    if (want == null) return null;

    Grib2Iosp iosp = new Grib2Iosp(want);
    NetcdfFile ncfile = new MyNetcdfFile(iosp, null, getIndexFile().getPath(), null);
    return new NetcdfDataset(ncfile);
  }

  public ucar.nc2.dt.GridDataset getGridDataset(String groupName) throws IOException {
    GroupHcs want = findGroup(groupName);
    if (want == null) return null;

    Grib2Iosp iosp = new Grib2Iosp(want);
    NetcdfFile ncfile = new MyNetcdfFile(iosp, null, getIndexFile().getPath(), null);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);
    return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??
  }

}
