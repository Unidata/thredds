/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import ucar.nc2.units.TimeDuration;

import java.io.IOException;

/**
 * Manages a dynamic collection of MFile objects.
 * You must call scan() first.
 * This is the older stuff used by FMRC. GRIB uses lighter weight MCollection.
 *
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
public interface CollectionManager extends MCollection {

  /* public enum Force {always,  // force new index
                     test,    // test if new index is needed
                     nocheck, // if index exists, use it
                     never }  // only use existing  */



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
   * If isScanNeeded(), do a scan. Do not send an event.
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

  ////////////////////////////////////////////////////
  // set Strategy for checking if MFile has changed

  public interface ChangeChecker {
    boolean hasChangedSince(MFile file, long when);
    boolean hasntChangedSince(MFile file, long when);
  }

  ///////////////////////////////////////////////////////////

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

  public class TriggerEvent extends java.util.EventObject {
     private final CollectionUpdateType type;

     TriggerEvent(Object source, CollectionUpdateType type) {
       super(source);
       this.type = type;
     }

     public CollectionUpdateType getType() {
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
