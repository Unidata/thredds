/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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
    	if(storeFactory != null)
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


  ////////////////////////////////////////////////////
  // events; keep the code from getting too coupled

  @Override
  public void sendEvent(CollectionUpdateType type) {
    sendEvent(new TriggerEvent(this, type));
  }

  /*  switch (type) {
      case update:
        try {
          scan(true);    // LOOK
        } catch (IOException e) {
          logger.error("Error on scan", e);
        }
        break;

      case updateNocheck:
        sendEvent(new TriggerEvent(this, type));
        break;

      case resetProto:
        sendEvent(new TriggerEvent(this, TriggerType.resetProto));
        break;
    }
  } */

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
