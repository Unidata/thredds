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

package thredds.inventory;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.bdb.MetadataManager;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.ListenerManager;
import ucar.unidata.util.StringUtil2;

import java.io.IOException;
import java.util.*;

/**
 * Abstract superclass for implementations of CollectionManager.
 *
 * @author caron
 * @since Jan 19, 2010
 */
public abstract class CollectionManagerAbstract implements CollectionManager {
  static private org.slf4j.Logger defaultLog = org.slf4j.LoggerFactory.getLogger("featureCollectionScan");

  static public String cleanName(String name) {
    if (name == null)
      System.out.println("HEY cleanName");
    return StringUtil2.replace(name.trim(), ' ', "_");  // LOOK must be ok in URL - probably not sufficient here
  }

  protected String collectionName;
  protected final org.slf4j.Logger logger;

  protected TimeDuration recheck;
  protected FeatureCollectionConfig.ProtoChoice protoChoice = FeatureCollectionConfig.ProtoChoice.Penultimate;  // default

  protected Map<String, Object> auxInfo; // lazy init
  private ListenerManager lm; // lazy init
  private boolean isStatic; // true if theres no update element. It means dont scan if index already exists

  protected CollectionManagerAbstract( String collectionName, org.slf4j.Logger logger) {
    this.collectionName =  cleanName(collectionName);
    this.logger = logger != null ? logger : defaultLog;
    // this.logger = loggerFactory.getLogger("fc."+this.collectionName); // seperate log file for each feature collection (!!)
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean aStatic) {
    isStatic = aStatic;
  }

  @Override
  public String getCollectionName() {
    return collectionName;
  }

  @Override
  public TimeDuration getRecheck() {
    return recheck;
  }

  @Override
  public long getOlderThanFilterInMSecs() {
    return -1;
  }

  @Override
  public void close() {
    if (mm != null) mm.close();
  }

  public List<String> getFilenames() {
    List<String> result = new ArrayList<String>();
    for (MFile f: getFiles())
      result.add(f.getPath());
    return result;
  }

  public MFile getLatestFile() {
    MFile result = null;
    for (MFile f: getFiles()) // only have an Iterable
      result = f;
    return result;
  }

  ////////////////////////
  // experimental
  protected ChangeChecker changeChecker = null;

  @Override
  public void setChangeChecker(ChangeChecker strat) {
    this.changeChecker = strat;
  }

  ////////////////////////////////////////////////////
  // ability to pass arbitrary information in. kind of a kludge

  @Override
  public Object getAuxInfo(String key) {
    return auxInfo == null ? null : auxInfo.get(key);
  }

  @Override
  public void putAuxInfo(String key, Object value) {
    if (auxInfo == null) auxInfo = new HashMap<String, Object>();
    auxInfo.put(key, value);
  }

  ////////////////////////////////////////////////////
  // proto dataset choosing

  @Override
  public int getProtoIndex(int n) {
    if (n < 2) return 0;

    int protoIdx = 0;
    switch (protoChoice) {
      case First:
        protoIdx = 0;
        break;
      case Random:
        Random r = new Random(System.currentTimeMillis());
        protoIdx = r.nextInt(n - 1);
        break;
      case Run:
      case Penultimate:
        protoIdx = Math.max(n - 2, 0);
        break;
      case Latest:
        protoIdx = Math.max(n - 1, 0);
        break;
    }
    return protoIdx;
  }

  /////////////////////////////////////////////////////////////////////
  // experimental
  // use bdb to manage metadata associated with the collection. currently, only DatasetInv.xml files

  // explicit enabling - allows deleteOld to work first time
  static private boolean enableMetadataManager = false;
  static public void enableMetadataManager() {
    enableMetadataManager = true;
  }

  private MetadataManager mm;

  private void initMM() {
    if (getCollectionName() == null) return; // eg no scan in ncml
    try {
      mm = new MetadataManager(getCollectionName());
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  // clean up deleted files in metadata manager
  protected void deleteOld(Map<String, MFile> newMap) {
    if (mm == null && enableMetadataManager) initMM();
    if (mm != null) mm.delete(newMap);
  }

  public void putMetadata(MFile file, String key, byte[] value) {
    if (mm == null) initMM();
    if (mm != null) mm.put(file.getPath()+"#"+key, value);
  }

  public byte[] getMetadata(MFile file, String key) {
    if (mm == null) initMM();
    return (mm == null) ? null : mm.getBytes(file.getPath()+"#"+key);
  }


  ////////////////////////////////////////////////////
  // events; keep the code from getting too coupled

  @Override
  public void updateNocheck() throws IOException {
    sendEvent(new TriggerEvent(this, TriggerType.updateNocheck));
  }

  @Override
  public void resetProto() {
    if (lm != null)
      lm.sendEvent(new TriggerEvent(this, TriggerType.proto));
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
