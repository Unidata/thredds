/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import thredds.client.catalog.builder.DatasetBuilder;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * node in a tree of datasets, superclass of Catalog and Dataset
 *
 * @author caron
 * @since 1/8/2015
 */
@Immutable
public class DatasetNode {
  protected final DatasetNode parent;
  protected final String name;
  protected final Map<String, Object> flds;     // keep memory small. dont store reference objects for nulls

  protected DatasetNode(DatasetNode parent, String name, Map<String, Object> flds, List<DatasetBuilder> datasetBuilders) {
    this.parent = parent;
    this.name = name;
    this.flds = Collections.unmodifiableMap(flds);
    if (datasetBuilders != null && datasetBuilders.size() > 0) {
      List<Dataset> datasets = new ArrayList<>(datasetBuilders.size());
      for (DatasetBuilder dsb : datasetBuilders)
        datasets.add(dsb.makeDataset(this));
      flds.put(Dataset.Datasets, Collections.unmodifiableList(datasets));
    }
  }

  public Iterable<Map.Entry<String, Object>> getFldIterator() {
    return flds.entrySet();
  }

  public Object get(String key) {
    return flds.get(key);
  }

  public DatasetNode getParent() {
    return parent;
  }

  public String getName() {
    return name;
  }

  /**
    Get top level datasets contained directly in this catalog.
    Do not dereference catRefs.
   */
  public List<Dataset> getDatasetsLocal() {
    List<Dataset> datasets = (List<Dataset>) flds.get(Dataset.Datasets);
    return datasets == null ? new ArrayList<>(0) : datasets;
  }

  /**
    Get top level datasets contained in this catalog, or if its a catref,
    get the datasets in the referenced catalog only if already read in.
 */
  public List<Dataset> getDatasets() {
    return getDatasetsLocal();
  }

  /**
    Get top level datasets logically contained in this catalog.
    If this is a catalogRef, read it in.
 */
  public List<Dataset> getDatasetsLogical() {
    return getDatasets();
  }

  // Look though all datasets here or under here. do not go into catrefs
  public Dataset findDatasetByName(String name) {
    for (Dataset ds : getDatasets()) {
      if (ds.getName().equals(name)) return ds;
      Dataset result = ds.findDatasetByName(name);
      if (result != null) return result;
    }
    return null;
  }

  public boolean hasNestedDatasets() {
    List<Dataset> datasets = getDatasets();
    return !datasets.isEmpty();
  }

  public Catalog getParentCatalog() {
    if (parent == null) return null;
    if (parent instanceof Catalog) return (Catalog) parent;
    return parent.getParentCatalog();
  }

  public Dataset getParentDataset() {
    if (parent == null) return null;
    return (parent instanceof Dataset) ? (Dataset) parent : null;
  }

  public List getLocalFieldAsList(String fldName) {
    Object value = flds.get(fldName);
    if (value != null) {
      if (value instanceof List) return (List) value;
      List result = new ArrayList(1);
      result.add(value);
      return result;
    }
    return new ArrayList(0);
  }
}
