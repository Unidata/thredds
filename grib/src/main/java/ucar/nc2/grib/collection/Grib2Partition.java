package ucar.nc2.grib.collection;

import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Description
 *
 * @author John
 * @since 12/7/13
 */
public class Grib2Partition extends PartitionCollection implements AutoCloseable {

  public Grib2Partition(String name, File directory, FeatureCollectionConfig.GribConfig config, org.slf4j.Logger logger) {
    super(name, directory, config, false, logger);
  }

  // LOOK - needs time partition collection iosp or something
  @Override
  public ucar.nc2.dataset.NetcdfDataset getNetcdfDataset(String datasetName, String groupName, String filename,
          FeatureCollectionConfig.GribConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Dataset ds = findDataset(datasetName);
    if (ds == null) return null;
    GroupHcs want = ds.findGroupById(groupName);
    if (want == null) return null;

    ucar.nc2.grib.collection.Grib2Iosp iosp = new ucar.nc2.grib.collection.Grib2Iosp(want, ds.getType());
    NetcdfFile ncfile = new GcNetcdfFile(iosp, null, getIndexFile().getPath(), null);
    return new NetcdfDataset(ncfile);
  }

  @Override
  public ucar.nc2.dt.grid.GridDataset getGridDataset(String datasetName, String groupName, String filename,
          FeatureCollectionConfig.GribConfig gribConfig, Formatter errlog, org.slf4j.Logger logger) throws IOException {
    Dataset ds = findDataset(datasetName);
    if (ds == null) return null;
    GroupHcs want = ds.findGroupById(groupName);
    if (want == null) return null;

    ucar.nc2.grib.collection.Grib2Iosp iosp = new ucar.nc2.grib.collection.Grib2Iosp(want, ds.getType());
    NetcdfFile ncfile = new GcNetcdfFile(iosp, null, getIndexFile().getPath(), null);
    NetcdfDataset ncd = new NetcdfDataset(ncfile);
    return new ucar.nc2.dt.grid.GridDataset(ncd); // LOOK - replace with custom GridDataset??
  }

}
