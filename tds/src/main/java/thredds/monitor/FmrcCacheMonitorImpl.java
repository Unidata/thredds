/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.monitor;

import thredds.inventory.bdb.MetadataManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JMX bean
 *
 * @author caron
 * @since Apr 19, 2010
 */
public class FmrcCacheMonitorImpl implements FmrcCacheMonitor {

  public List<String> getCachedCollections() {
    return MetadataManager.getCollectionNames().stream().collect(Collectors.toList());
  }

  public String getCacheLocation() {
    return MetadataManager.getCacheLocation();
  }

  public void getCacheStatistics(Formatter f) {
    MetadataManager.showEnvStats(f);
  }

  public List<String> getFilesInCollection(String collectionName) {
    java.util.List<String> result = new ArrayList<>();
    MetadataManager mm = null;
    try {
      mm = new MetadataManager(collectionName);

      result.addAll(mm.getContent().stream().map(data -> data.key).collect(Collectors.toList()));

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (mm != null) mm.close();
    }
    return result;
  }

  public String getCachedFile(String collectionName, String name) {
    MetadataManager mm = null;
    try {
      mm = new MetadataManager(collectionName);
      return mm.get(name);

    } catch (Throwable e) {
      e.printStackTrace();
      return null;
    } finally {
      if (mm != null) mm.close();
    }
  }

  public void deleteCollection(String collectionName) throws Exception {
    MetadataManager.deleteCollection(collectionName);
  }

  public void sync() {
    MetadataManager.sync();
  }


}
