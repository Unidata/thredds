/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.CloseableIterator;
import ucar.nc2.util.ListenerManager;

import java.io.IOException;
import java.util.*;

/**
 * Abstract superclass for implementations of CollectionManager.
 *
 * @author caron
 * @since Jan 19, 2010
 */
public abstract class CollectionManagerAbstract extends CollectionAbstract implements CollectionManager  {

    // called from Aggregation, Fmrc, FeatureDatasetFactoryManager
  static public CollectionManager open(String collectionName, String collectionSpec, String olderThan, Formatter errlog) throws IOException {
    if (collectionSpec.startsWith(CATALOG))
      return new CollectionManagerCatalog(collectionName, collectionSpec, olderThan, errlog);
    else
      return MFileCollectionManager.open(collectionName, collectionSpec, olderThan, errlog);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  protected TimeDuration recheck;
  private ListenerManager lm; // lazy init
  private boolean isStatic; // true if theres no update element. It means dont scan if index already exists

  // these actually dont change, but are not set in the constructor  now set in CollectionAbstract
  //protected DateExtractor dateExtractor;
  //protected CalendarDate startCollection;

  protected CollectionManagerAbstract( String collectionName, org.slf4j.Logger logger) {
    super(collectionName, logger);
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean aStatic) {
    isStatic = aStatic;
  }

  @Override
  public TimeDuration getRecheck() {
    return recheck;
  }

  // fake default implementation
  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator( getFilesSorted().iterator(), null);
  }

  @Override
  public void close() {
    if (store != null) store.close();
  }


  @Override
  public boolean scanIfNeeded() throws IOException {
    // if (map == null && !isStatic()) return true;
    return isScanNeeded() && scan(false);
  }

  /////////////////////////////////////////////////////////////////////
  // experimental
  // use bdb to manage metadata associated with the collection. currently, only DatasetInv.xml files

  private static StoreKeyValue.Factory storeFactory;
  static public void setMetadataStore(StoreKeyValue.Factory _storeFactory) {
    storeFactory = _storeFactory;
  }

  private StoreKeyValue store;

  private void initMM() {
    if (getCollectionName() == null) return; // eg no scan in ncml
    try {
    	if (storeFactory != null)
    		store = storeFactory.open(getCollectionName());
    	
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  /* clean up deleted files in metadata manager
  protected void deleteOld(Map<String, MFile> newMap) {
    if (store == null && enableMetadataManager) initMM();
    if (store != null) store.delete(newMap);
  } */

  public void putMetadata(MFile file, String key, byte[] value) {
    if (store == null) initMM();
    if (store != null) store.put(file.getPath()+"#"+key, value);
  }

  public byte[] getMetadata(MFile file, String key) {
    if (store == null) initMM();
    return (store == null) ? null : store.getBytes(file.getPath()+"#"+key);
  }

  void sendEvent(TriggerEvent event) {
    if (lm != null)
      lm.sendEvent(event);
  }

  @Override
  public void addEventListener(TriggerListener l) {
    if (lm == null) createListenerManager();
    lm.addListener(l);
  }

  @Override
  public void removeEventListener(TriggerListener l) {
    if (lm != null)
      lm.removeListener(l);
  }

  protected void createListenerManager() {
    lm = new ListenerManager(
            "thredds.inventory.CollectionManager$TriggerListener",
            "thredds.inventory.CollectionManager$TriggerEvent",
            "handleCollectionEvent");
  }

}
