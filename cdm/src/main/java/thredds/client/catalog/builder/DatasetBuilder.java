/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog.builder;

import thredds.client.catalog.Dataset;
import thredds.client.catalog.DatasetNode;
import thredds.client.catalog.Service;
import thredds.client.catalog.ThreddsMetadata;

import java.util.*;

/**
 * Builder of client catalog Dataset
 *
 * @author caron
 * @since 1/8/2015
 */
public class DatasetBuilder {

  /**
   * Utility routine to keep list of objects small.
   * add fldValue to the fldName list in flds.
   * fldValue may be a list or an object.
   * if no list, just keep the object without creating a list (common case).
   * otherwise add it to the existing list.
   */
  public static void addToList(Map<String, Object> flds, String fldName, Object fldValue) {
    if (fldValue == null) return;
    Object prevVal = flds.get(fldName);
    if (prevVal == null) {
      flds.put(fldName, fldValue);
      return;
    }

    List prevList;
    if (prevVal instanceof List) {
      prevList = (List) prevVal;
    } else {
      prevList = new ArrayList(5);
      prevList.add(prevVal);
      flds.put(fldName, prevList);
    }

    if (fldValue instanceof List) {
      prevList.addAll((List) fldValue);
    } else {
      prevList.add(fldValue);
    }
  }

  public static void addToNewList(Map<String, Object> flds, String fldName, Object fldValue) {
    if (fldValue == null) return;
    List prevList;
    Object prevVal = flds.get(fldName);
    if (prevVal == null) {
      prevList = new ArrayList(5);
      flds.put(fldName, prevList);
    } else {
      prevList = (List) prevVal;
    }

    if (fldValue instanceof List) {
      prevList.addAll((List) fldValue);
    } else {
      prevList.add(fldValue);
    }
  }

  //////////////////////////////////////////////////////////////////////////////////

  protected DatasetBuilder parent;  // null when its a top level dataset
  protected String name;
  protected Map<String, Object> flds = new HashMap<>(10);

  protected List<AccessBuilder> accessBuilders;
  protected List<DatasetBuilder> datasetBuilders;
  protected List<Service> services;

  public DatasetBuilder(DatasetBuilder parent) {
    this.parent = parent;
  }

  // turn a Dataset back into a DatasetBuilder so it mutable
  public DatasetBuilder(DatasetBuilder parent, Dataset from) {
    this.parent = parent;
    this.name = from.getName();
    for (Map.Entry<String, Object> entry : from.getFldIterator()) {
      if (!entry.getKey().equals(Dataset.Datasets) && !entry.getKey().equals(Dataset.Access)) // set seperately
        this.flds.put(entry.getKey(), entry.getValue());
    }
  }

  public DatasetBuilder getParent() {
    return parent;
  }

  private Object getInherited(String fldName) {
    ThreddsMetadata tmi = (ThreddsMetadata) get(Dataset.ThreddsMetadataInheritable);
    if (tmi != null) {
      Object value = tmi.getLocalField(fldName);
      if (value != null) return value;
    }
    if (parent != null) return parent.getInherited(fldName);
    return null;
  }

  public Object getFldOrInherited(String fldName) {
    Object value = flds.get(fldName);
    if (value != null) return value;
    return getInherited(fldName);
  }

  public Object get(String fldName) {
    return flds.get(fldName);
  }

  public void put(String fldName, Object fldValue) {
    if (fldValue != null)
      flds.put(fldName, fldValue);
    else
      flds.remove(fldName);
  }

  public void putInheritedField(String fldName, Object fldValue) {
    if (fldValue == null) return;

    ThreddsMetadata tmi = (ThreddsMetadata) get(Dataset.ThreddsMetadataInheritable);
    if (tmi == null) {
      tmi = new ThreddsMetadata();
      put(Dataset.ThreddsMetadataInheritable, tmi);
    }
    tmi.getFlds().put(fldName, fldValue);
  }

  public void addToList(String fldName, Object fldValue) {
    if (fldValue != null) addToList(flds, fldName, fldValue);
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void addDataset(DatasetBuilder d) {
    if (d == null) return;
    if (datasetBuilders == null) datasetBuilders = new ArrayList<>();
    datasetBuilders.add(d);
  }

  public void addAccess(AccessBuilder d) {
    if (accessBuilders == null) accessBuilders = new ArrayList<>();
    accessBuilders.add(d);
  }

  public void addServiceToCatalog(Service s) {
    if (s == null) return;
    if (services == null) services = new ArrayList<>();
    services.add(s);
  }

  public Dataset makeDataset(DatasetNode parent) {  // LOOK whats relationship of parent with this.parent ??
    ThreddsMetadata tmi = (ThreddsMetadata) get(Dataset.ThreddsMetadataInheritable);
    if (tmi != null) tmi.finish();
    return new Dataset(parent, name, flds, accessBuilders, datasetBuilders);
  }

  // make an immutable copy without changin DatasetBuilder
  public Dataset copyDataset(DatasetNode parent) {
    return new Dataset(parent, name, flds, accessBuilders, datasetBuilders);
  }

  public Iterable<DatasetBuilder> getDatasets() {
    if (datasetBuilders != null) return datasetBuilders;
    return new ArrayList<>(0);
  }

  public Iterable<Service> getServices() {
    if (services != null) return services;
    return new ArrayList<>(0);
  }


  /////////////////////////////////////////////////////////////////////////////

  // transfer all metadata, optionally also inheritable metadata from parents
  public void transferMetadata( DatasetNode from, boolean parentsAlso) {
    if (parentsAlso) {
      ThreddsMetadata inherit = getInheritableMetadata(); // make sure exists
      inheritMetadata(from, inherit.getFlds());
    }

    // local metadata
    for (Map.Entry<String, Object> entry : from.getFldIterator()) {
      if (parentsAlso && entry.getKey().equals(Dataset.ThreddsMetadataInheritable)) continue; // already did this
      if (Dataset.listFlds.contains(entry.getKey()))
        addToNewList(flds, entry.getKey(), entry.getValue());
      else
        flds.put(entry.getKey(), entry.getValue());
    }

    // tmi must be mutable, transfer if not
    ThreddsMetadata tmiOld = (ThreddsMetadata) get(Dataset.ThreddsMetadataInheritable);
    if (tmiOld != null && tmiOld.isImmutable()) {
      ThreddsMetadata tmiNew = new ThreddsMetadata(tmiOld);
      flds.put(Dataset.ThreddsMetadataInheritable, tmiNew);
    }
  }

  // transfer inherited metadata only, always include parents
  // place directly into flds (not in this.tmi) LOOK why not into tmi ?? LOOK put into tmi, see what breaks!
  public void transferInheritedMetadata( DatasetNode from) {
    ThreddsMetadata tmi = getInheritableMetadata();
    inheritMetadata(from, tmi.getFlds());
  }

  private void inheritMetadata(DatasetNode from, Map<String, Object> toFlds) {
   // depth first, so closer parents override;
    Dataset fromParent = from.getParentDataset();
    if (fromParent != null) {
      inheritMetadata(fromParent, toFlds);
    }

    ThreddsMetadata tmi = (ThreddsMetadata) from.get(Dataset.ThreddsMetadataInheritable);
    if (tmi == null) return;

    for (Map.Entry<String, Object> entry : tmi.getFldIterator()) {
      if (Dataset.listFlds.contains(entry.getKey()))
        addToNewList(toFlds, entry.getKey(), entry.getValue());
      else
        toFlds.put(entry.getKey(), entry.getValue());
    }
  }

  // get the inheritable ThreddsMetadata object. If doesnt exist, create new, empty one
  public ThreddsMetadata getInheritableMetadata() {
    ThreddsMetadata tmi = (ThreddsMetadata) get(Dataset.ThreddsMetadataInheritable);
    if (tmi == null) {
      tmi = new ThreddsMetadata();
      put(Dataset.ThreddsMetadataInheritable, tmi);
    }
    return tmi;
  }

}
