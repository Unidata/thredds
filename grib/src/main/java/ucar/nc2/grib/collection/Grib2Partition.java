/*
 *
 *  * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileSubclass;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.Closeable;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;

/**
 * PartitionCollection for Grib2.
 *
 * @author John
 * @since 12/7/13
 */
public class Grib2Partition extends PartitionCollectionImmutable implements Closeable {

  Grib2Partition( PartitionCollectionMutable pc) {
    super(pc);
  }

  // LOOK - needs time partition collection iosp or something
  @Override
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(Dataset ds, GroupGC group, String filename,
          FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    ucar.nc2.grib.collection.Grib2Iosp iosp = new ucar.nc2.grib.collection.Grib2Iosp(group, ds.getType());
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
    return new NetcdfDataset(ncfile);
  }

  @Override
  public ucar.nc2.dt.grid.GridDataset getGridDataset(Dataset ds, GroupGC group, String filename,
          FeatureCollectionConfig config, Formatter errlog, org.slf4j.Logger logger) throws IOException {

    ucar.nc2.grib.collection.Grib2Iosp iosp = new ucar.nc2.grib.collection.Grib2Iosp(group, ds.getType());
    NetcdfFile ncfile = new NetcdfFileSubclass(iosp, null, getLocation(), null);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);
    return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??
  }

  @Override
  protected void addGlobalAttributes(List<Attribute> result) {
    String val = cust.getGeneratingProcessTypeName(getGenProcessType());
    if (val != null)
      result.add(new Attribute("Type_of_generating_process", val));
    val = cust.getGeneratingProcessName(getGenProcessId());
    if (val != null)
      result.add( new Attribute("Analysis_or_forecast_generating_process_identifier_defined_by_originating_centre", val));
    val = cust.getGeneratingProcessName(getBackProcessId());
    if (val != null)
      result.add( new Attribute("Background_generating_process_identifier_defined_by_originating_centre", val));
    result.add(new Attribute(CDM.FILE_FORMAT, DataFormatType.GRIB2.getDescription()));
  }

}
