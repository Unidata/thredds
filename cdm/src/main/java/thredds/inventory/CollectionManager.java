/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

import org.slf4j.Logger;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.TimeDuration;

import java.io.IOException;
import java.util.List;

/**
 * Manages a dynamic collection of MFile objects.
 * An MFile must have the property that
 * <pre>  NetcdfDataset.open(MFile.getPath, ...); </pre>
 * should work.
 *
 * <p> A CollectionManager implements the <collection> element.
 *
<h3>collection element</h3>
<p>A <strong>collection</strong> element defines the collection of datasets. </p>
<pre>&lt;<strong>collection</strong> <strong>spec</strong>=&quot;/data/ldm/pub/native/satellite/3.9/WEST-CONUS_4km/WEST-CONUS_4km_3.9_#yyyyMMdd_HHmm#.gini$&quot;
            <strong>name</strong>=&quot;WEST-CONUS_4km&quot; <strong>olderThan</strong>=&quot;1 min&quot; <strong></strong><strong>olderThan</strong>=&quot;15 min&quot; /&gt;
</pre>

The XML Schema:
<pre>
&lt;xsd:complexType name=&quot;collectionType&quot;&gt;
  1)  &lt;xsd:attribute name=&quot;spec&quot; type=&quot;xsd:string&quot; use=&quot;required&quot;/&gt;
  2)  &lt;xsd:attribute name=&quot;name&quot; type=&quot;xsd:token&quot;/&gt;
  3)  &lt;xsd:attribute name=&quot;olderThan&quot; type=&quot;xsd:string&quot; /&gt;
  5)  &lt;xsd:attribute name=&quot;dateFormatMark&quot; type=&quot;xsd:string&quot;/&gt;
  6)  &lt;xsd:attribute name=&quot;timePartition&quot; type=&quot;xsd:string&quot;/&gt;
&lt;/xsd:complexType&gt;<br /></pre>
<p>where</p>
<ol>
  <li><strong>spec</strong>: <a href="CollectionSpecification.html">collection specification</a> string (required).</li>
  <li><strong>name</strong>: collection name <em><strong>must be unique in all of your TDS catalogs</strong></em>.
    This is used for external triggers and as an easy to read identifier for indexing, logging and debugging.
    If missing, the spec string is used (not a good idea in the context of the TDS). </li>
  <li><strong>olderThan</strong> (optional): Only files whose lastModified date is older than this are included.
 This excludes files that are in the process of being written. However, it only applies to newly found files, that is,
 once a file is in the collection it is not removed because it got updated.</li>
  <li><strong>dateFormatMark</strong> (optional): the collection specification string can only extract dates from the file name,
 as opposed to the file path, which includes all of the parent directory names. Use the <em>dateFormatMark</em> in order to extract
 the date from the full path. <em>Use this OR a date extrator in the specification string, but not both.</em></li>
  <li><strong>timePartition</strong> (optional):: experimental, not complete yet.</li>
</ol>
 *
 * @author caron
 * @since Jan 19, 2010
 */
public interface CollectionManager {

  public enum Force {always, // force new index
                     test,   // test if new index is needed
                     nocheck } // if index exists, use it

  /**
   * The name of the collection
   * @return name of the collection
   */
  public String getCollectionName();

  /**
   * Get common root directory of all MFiles in the collection - may be null
   *
   * @return root directory name, or null.
   */
  public String getRoot();

  /**
   * static means doesnt need to be monitored for changes; can be externally triggered, or read in at startup.
   * true if no recheckAfter and no update.rescan
   * @return if static
   */
  public boolean isStatic();

  /**
   * Get the last time scanned
   *
   * @return msecs since 1970
   */
  public long getLastScanned();

  /**
   * Get the last time the collection changed
   *
   * @return msecs since 1970
   */
  public long getLastChanged();

  /**
   * Get how often to rescan
   *
   * @return time duration of rescan period, or null if none.
   */
  public TimeDuration getRecheck();

  /**
   * Compute whether rescan is needed, based on getRecheck(), and the LastScanned value.
   *
   * @return true if rescan is needed.
   */
  public boolean isScanNeeded();

  /**
   * If isScanNeeded(), do a scan.
   * @return true if scan was done, and anything changed.
   * @throws IOException on io error
   */
  public boolean scanIfNeeded() throws IOException;

  /**
   * Scan the collection. Files may have been deleted or added since last time.
   * If the MFile already exists in the current list, leave it in the list.
   * If anything changes, send TriggerEvent(TriggerType.update) and return true
   * Get the results from getFiles()
   *
   * @return true if anything actually changed.
   * @throws IOException on I/O error
   */
  public boolean scan(boolean sendEvent) throws IOException;

  /**
   * send event TriggerType.updateNocheck, which calls
   * call InvDatasetFeatureCollection.update(CollectionManager.Force.nocheck)
   * @throws IOException
   */
  public void updateNocheck() throws IOException;

  /**
   * Get the current collection of MFile.
   * You must call scan() first - this does not call scan().
   * if hasDateExtractor() == true, these will be sorted by Date, otherwise by path.
   *
   * @return current collection of MFile as an Iterable. May be empty, not null.
   */
  public Iterable<MFile> getFiles();

  /**
   *   dcm must be updated when index is read in
   */
  public void setFiles(Iterable<MFile> files);

  /**
   * Use the date extractor to extract the date from the filename.
   * Only call if hasDateExtractor() == true.
   *
   * @param mfile extract from here
   * @return Date, or null if none
   */
  public CalendarDate extractRunDate(MFile mfile);

  /**
   * Does this CollectionManager have the ability to extract a date from the MFile ?
   * @return true if CollectionManager has a DateExtractor
   */
  public boolean hasDateExtractor();

  /**
   * The starting date of the collection.
   * Only call if hasDateExtractor() == true.
   * @return starting date of the collection
   */
  public CalendarDate getStartCollection();

  /**
   * Close and release any resources. Do not make further calls on this object.
   */
  public void close();

  //////////////////////////////
  // these 2 are kind of kludges

  /**
   * Choose Proto dataset as index from [0..n-1], based on configuration.
   * @param n size to choose from
   * @return index within range [0..n-1]
   */
  public int getProtoIndex(int n);

  /**
   * The "olderThan" amount in seconds.
   * Files are excluded if they have been modified within this amount of time.
   * However once in the collection they are not removed.
   * Really this is handled by the manager, but this is exposed so that others (eg DatasetScan) can be consistent.
   * @return olderThan" amount in seconds, or < 0 to mean this filter is not present
   */
  public long getOlderThanFilterInMSecs();

  public List<String> getFilenames();
  public MFile getLatestFile();

  ////////////////////////////////////////////////////
  // set Strategy for checking if MFile has changed

  public interface ChangeChecker {
    boolean hasChangedSince(MFile file, long when);
    boolean hasntChangedSince(MFile file, long when);
  }

  public void setChangeChecker(ChangeChecker strategy);

  ////////////////////////////////////////////////////
  // ability to pass arbitrary information to users of the collection manager. kind of a kludge

  public Object getAuxInfo(String key);

  public void putAuxInfo(String key, Object value);

  /////////////////////////////////////////////////////

  /**
   * Called by external program to tell the manager its time to switch the proto dataset.
   * a TriggerEvent.proto is sent to any listeners.
   */
  public void resetProto();

  /**
   * Register to get Trigger events
   * @param l listener
   */
  public void addEventListener(TriggerListener l);
  public void removeEventListener(TriggerListener l);

  /**
   * A TriggerEvent.proto is sent if protoDataset.change = "cron" has been specified
   * A TriggerEvent.update is sent if a scan has happened and a change in the list of MFiles has occurred,
   *  or an MFile has been updated
   */
  public static interface TriggerListener {
    public void handleCollectionEvent(TriggerEvent event);
  }

  public enum TriggerType {update, proto, updateNocheck }

  public class TriggerEvent extends java.util.EventObject {
     private final TriggerType type;

     TriggerEvent(Object source, TriggerType type) {
       super(source);
       this.type = type;
     }

     public TriggerType getType() {
       return type;
     }

     @Override
     public String toString() {
       return "TriggerEvent{" +
               "type='" + type + '\'' +
               '}';
     }
   }

}
