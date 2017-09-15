/*
 * Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package ucar.nc2.ncml;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.filesystem.MFileOS;
import thredds.inventory.DateExtractor;
import thredds.inventory.DateExtractorFromName;
import thredds.inventory.MFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.fmrc.Fmrc;
import ucar.nc2.util.CancelTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implement NcML Forecast Model Run Collection Aggregation
 * with files that are complete runs (have all forecast times in the same file)
 *
 * @author caron
 */
public class AggregationFmrc extends AggregationOuterDimension {
  static protected Set<NetcdfDataset.Enhance> fmrcEnhanceMode = NetcdfDataset.getDefaultEnhanceMode();

  private Fmrc fmrc;
  private String runMatcher; // , forecastMatcher, offsetMatcher; // scanFmrc

  public AggregationFmrc(NetcdfDataset ncd, String dimName, String recheckS) {
    super(ncd, dimName, Type.forecastModelRunCollection, recheckS);
  }

  public void addDirectoryScanFmrc(String dirName, String suffix, String regexpPatternString, String subdirs, String olderThan,
                                   String runMatcher, String forecastMatcher, String offsetMatcher) throws IOException {

    // only one
    this.runMatcher = runMatcher;
    //this.forecastMatcher = forecastMatcher;
    //this.offsetMatcher = offsetMatcher;

    // this.enhance = NetcdfDataset.getDefaultEnhanceMode();
    isDate = true;

    //DatasetScanner d = new DatasetScanner(null, dirName, suffix, regexpPatternString, subdirs, olderThan);
    //datasetManager.addDirectoryScan(d);
    datasetManager.addDirectoryScan(dirName, suffix, regexpPatternString, subdirs, olderThan, null);
    if (runMatcher != null) {
      DateExtractor dateExtractor = new DateExtractorFromName(runMatcher, false);
      datasetManager.setDateExtractor(dateExtractor);
    }
  }

  public void addExplicitFilesAndRunTimes(Map<String, String> filesRunTimeMap) {
    // Used to add explicitly defined data files to an FMRC (i.e. not a
    // directory scan).
    // Much like addDirectoryScanFmrc
    isDate = true;

    List<MFile> mfiles = new ArrayList<>();
    for (String file : filesRunTimeMap.keySet()) {
      MFile mfile = MFileOS.getExistingFile(file);
      mfiles.add(mfile);
    }

    datasetManager.setFiles(mfiles);
  }

  @Override
  protected void makeDatasets(CancelTask cancelTask) throws IOException {
    super.makeDatasets(cancelTask);
    for (Dataset ds : datasets)
      ds.enhance = fmrcEnhanceMode;
  }

  @Override
  public void getDetailInfo(Formatter f) {
    super.getDetailInfo(f);

    if (runMatcher != null)
      f.format("  runMatcher=%s%n", runMatcher);
   /*  if (forecastMatcher != null)
      f.format("  forecastMatcher=%s%n", forecastMatcher);
    if (offsetMatcher != null)
      f.format("  offsetMatcher=%s%n", offsetMatcher); */
  }

  @Override
  protected void buildNetcdfDataset(CancelTask cancelTask) throws IOException {
    DateExtractor dateExtractor = null;
    if (runMatcher != null)
      dateExtractor = new DateExtractorFromName(runMatcher, false); // uses path
    if (dateExtractor == null && dateFormatMark != null)
      dateExtractor = new DateExtractorFromName(dateFormatMark, true);
    fmrc = new Fmrc(datasetManager, new FeatureCollectionConfig());

    // fill in the ncDataset
    fmrc.getDataset2D( ncDataset);

    ncDataset.finish();
  }

  // we assume the variables are complete, but the time dimensions and values have to be recomputed
  @Override
  protected void rebuildDataset() throws IOException {
    throw new UnsupportedOperationException();
    // ncDataset.empty();
    // fmrc.getDataset2D(false, true, ncDataset);
  }

}
