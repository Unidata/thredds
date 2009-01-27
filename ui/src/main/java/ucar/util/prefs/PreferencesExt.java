// $Id: PreferencesExt.java,v 1.5 2005/08/22 17:13:58 caron Exp $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package ucar.util.prefs;

import java.util.*;
import java.util.prefs.*;
import java.io.ByteArrayOutputStream;


/**
 * An extension of java.util.prefs.Preferences (jdk 1.4) that provides a
 * platform-independent implementation using XML files as backing store.
 * <p> To save Java beans, use putBean() and putBeanCollection(). This uses
 *  reflection to get/set properties that have simple single-valued accessor methods
 *  of primitive and String type.
 * <p> For arbitrary objects, use putBeanObject(), which uses the
 * XMLEncode/XMLDecode API (jdk 1.4).
 *
 * To obtain a PreferencesExt object, instantiate an XMLStore object and
 * call XMLStore.getPreferences().
 *
 * @see ucar.util.prefs.XMLStore
 * @see java.util.prefs.Preferences
 * @author John Caron
 * @version $Revision: 1.5 $ $Date: 2005/08/22 17:13:58 $
 */

public class PreferencesExt extends java.util.prefs.AbstractPreferences implements ucar.util.prefs.ui.PersistenceManager {
    static Preferences userRoot = new PreferencesExt(null, ""); // ??
    static Preferences systemRoot = new PreferencesExt(null, "");

    /** Set the user root you get when you call Preferences.userRoot(). */
    static public void setUserRoot( PreferencesExt prefs) { userRoot = prefs; }
    /** Set the system root you get when you call Preferences.systemRoot(). */
    static public void setSystemRoot( PreferencesExt prefs) {
      systemRoot = prefs;
    }

    private boolean isBackingStoreAvailable = true;

    private PreferencesExt parent;
    private HashMap keyValues, children;
    private PreferencesExt storedDefaults = null;

    /**
     * Constructor. Usually you get a PreferencesExt object from XMLStore.getPrefs(),
     *  rather than constructing one directly.
     *  For the root node, parent = null and name = "".
     */
    public PreferencesExt(PreferencesExt parent, String name) {
      super(parent, name);
      this.parent = parent;

      keyValues = new HashMap(20);
      children = new HashMap(10);
    }
    void setStoredDefaults( PreferencesExt storedDefaults) {
      this.storedDefaults = storedDefaults;
    }
    private PreferencesExt getStoredDefaults() {
      return (parent == null) ? storedDefaults : parent.getStoredDefaults();
    }

    /** return true unless this is the systemRoot node */
    public boolean isUserNode() { return this != systemRoot; } // ??

    ////////////////////////////////////////////////////////
    // get/put beans

    /**
     * Get the object that has the specified key.
     * This returns the object itself, not a copy, so
     * if you change the bean and call store.save(), any changes to the object will be saved,
     * even without calling putBean(). If you want to change the object without saving the
     * changes, you must make a copy of the object yourself.
     *
     * @param key get the object with this key.
     * @param def the default value to be returned in the event that this
     *        preference node has no value associated with <tt>key</tt>.
     * @return the value associated with <tt>key</tt>, or <tt>def</tt>
     *         if no value is associated with <tt>key</tt>.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     * @throws NullPointerException if key is <tt>null</tt>.  (A
     *         <tt>null</tt> default <i>is</i> permitted.)
     */
    public Object getBean(String key, Object def) {
      if (key==null)
        throw new NullPointerException("Null key");
      if (isRemoved())
        throw new IllegalStateException("Node has been removed.");

      synchronized(lock) {
        Object result = null;
        try {
          result = _getObject( key);
          if (result != null) {
            if (result instanceof Bean.Collection)
              result = ((Bean.Collection)result).getCollection();
            else if (result instanceof Bean)
              result = ((Bean)result).getObject();
          }
        } catch (Exception e) {
          // Ignoring exception causes default to be returned
        }
        return (result==null ? def : result);
      }
    }

    /**
     *  Stores an object using simple bean properties.
     *  If the exact key and value are already in
     *  the storedDefaults (using equals() to test for equality), then it is not
     *  stored.
     *
     * @param key key with which the specified value is to be associated.
     * @param newValue store this bean.
     * @throws NullPointerException if key or value is <tt>null</tt>.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void putBean(String key, Object newValue) {
      // if matches a stored Default, dont store
      Object oldValue = getBean(key, null);
      if ((oldValue == null) || !oldValue.equals( newValue))
        keyValues.put( key, new Bean(newValue));
    }

    /**
     *  Stores a Collection of beans. The beans are stored using simple bean properties.
     *   The collection of beans must all be of the same class.
     *
     * @param key key with which the specified collection is to be associated.
     * @param newValue store this collection of beans.
     * @throws NullPointerException if key or value is <tt>null</tt>.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void putBeanCollection(String key, Collection newValue) {
      // if matches a stored Default, dont store
      Object oldValue = getBean(key, null);
      if ((oldValue == null) || !oldValue.equals( newValue))
        keyValues.put( key, new Bean.Collection(newValue));
    }


    /**
     *  Stores an object using XMLEncoder/XMLDecoder. Use this for arbitrary objects.
     *  If the exact key and value are already in
     *  the storedDefaults (using equals() to test for equality), then it is not
     *  stored.
     *
     * @param key key with which the specified value is to be associated.
     * @param newValue store this bean object.
     * @throws NullPointerException if key or value is <tt>null</tt>.
     * @throws IllegalStateException if this node (or an ancestor) has been
     *         removed with the {@link #removeNode()} method.
     */
    public void putBeanObject(String key, Object newValue) {
      // if matches a stored Default, dont store
      Object oldValue = getBean(key, null);
      if ((oldValue == null) || !oldValue.equals( newValue))
        keyValues.put( key, newValue);
    }

    ////////////////////////////////////////////////////////
    // get/put list

    /**
     * Get an arrayList. This returns a copy of the stored list.
     *
     * @param key key whose associated value is to be returned.
     * @param def the value to be returned in the event that this
     *        preference node has no value associated with <tt>key</tt>.
     * @return the value associated with <tt>key</tt>, or <tt>def</tt>
     *         if no value is associated with <tt>key</tt>.
     */
    public List getList(String key, List def) {
      try {
        Object bean = getBean(key, def);
        return (List) bean;
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    }

    /**
     *  Stores the value with this key, if the exact key and value are not already in
     *  the storedDefaults (using equals() to test for equality).
     *  "Two lists are defined to be equal if they contain the same elements in the same order."
     *
     * @param key key with which the specified value is to be associated.
     * @param newValue value to be associated with the specified key.
     */
    public void putList(String key, List newValue) {
      putBeanObject(key, newValue);
    }

    /////// the SPI interface
    /**
     * Implements <tt>AbstractPreferences</tt> <tt>childrenNamesSpi()</tt> method.
     * Find all children nodes of this node (or of identically named nodes in
     * storedDefaults)
     */
    protected String[] childrenNamesSpi() {
      HashSet allKids = new HashSet(children.keySet());
      PreferencesExt sd = getStoredDefaults();
      if (sd != null)
        allKids.addAll(  sd.childrenNamesSpi(absolutePath()));

      ArrayList list = new ArrayList( allKids);
      Collections.sort(list);
      String result[] = new String[list.size()];
      for (int i=0; i<list.size(); i++)
        result[i] = list.get(i).toString();
      return result;
    }

    /* Find all children nodes of named node (or of identically named nodes in
       storedDefaults) */
    protected Collection childrenNamesSpi(String nodePath) {
      HashSet allKids = new HashSet();
      try {
        if (nodeExists( nodePath)) {
          PreferencesExt node = (PreferencesExt) node( nodePath);
          allKids.addAll( node.children.keySet());
        }
      } catch (java.util.prefs.BackingStoreException e) { }

      PreferencesExt sd = getStoredDefaults();
      if (sd != null)
        allKids.addAll(  sd.childrenNamesSpi(nodePath));
      return allKids;
    }

   String[] keysNoDefaults() throws BackingStoreException{
      HashSet allKeys = new HashSet(keyValues.keySet());

      ArrayList list = new ArrayList( allKeys);
      Collections.sort(list);
      String result[] = new String[list.size()];
      for (int i=0; i<list.size(); i++)
        result[i] = list.get(i).toString();
      return result;
    }


    /* Find all key names of this node (or of identically named nodes in
      storedDefaults)

      (The returned array will be of size zero if this node has no preferences.)
      It is guaranteed that this node has not been removed.

      This method is invoked with the lock on this node held.
     */
    protected String[] keysSpi() throws BackingStoreException{
      HashSet allKeys = new HashSet(keyValues.keySet());
      //show( "allKeys1 ", allKeys);

      PreferencesExt sd = getStoredDefaults();
      if (sd != null)
        allKeys.addAll(  sd.keysSpi(absolutePath()));
      //show( "allKeys2 ", allKeys);

      ArrayList list = new ArrayList( allKeys);
      Collections.sort(list);
      //show( "allKeys3 ", list);

      String result[] = new String[list.size()];
      for (int i=0; i<list.size(); i++)
        result[i] = list.get(i).toString();
      return result;
    }

    /* Find all keys of named node (or of identically named nodes in
       storedDefaults) */
    protected Collection keysSpi(String nodePath) {
      HashSet allKeys = new HashSet();
      try {
        if (nodeExists( nodePath)) {
          PreferencesExt node = (PreferencesExt) node( nodePath);
          //show( "subKeys1 "+nodePath, node.keyValues.keySet());
          allKeys.addAll( node.keyValues.keySet());
        }
      } catch (java.util.prefs.BackingStoreException e) { }

      PreferencesExt sd = getStoredDefaults();
      if (sd != null) {
        allKeys.addAll( sd.keysSpi(nodePath));
        //show( "subKeys2 ", allKeys);
      }

      return allKeys;
    }

    void dump() throws BackingStoreException {
         // Put map in xml element
      String[] keys = keys();
      for (int i=0; i<keys.length; i++) {
          System.out.println("key = " + keys[i]+" value= "+ get(keys[i], null));
      }
      String[] kidNames = childrenNames();
      for (int i = 0; i <  kidNames.length; i++)
        ((PreferencesExt) node(kidNames[i])).dump();
  }

  void show( String what, Collection c) {
    try {
      System.out.println("---"+what+":");
      Iterator iter = c.iterator();
      while(iter.hasNext()) {
        Object o = iter.next();
        System.out.println("  "+o.toString()+" "+o.getClass().getName());
      }
    } catch (Exception e) {
      System.out.println(e);
      e.printStackTrace();
    }
    System.out.println("***");
  }
    /*
      Returns the named child of this preference node, creating it if it does not
      already exist. It is guaranteed that name is non-null, non-empty, does not
      contain the slash character ('/'), and is no longer than
      Preferences.MAX_NAME_LENGTH characters. Also, it is guaranteed that this node
      has not been removed. (The implementor needn't check for any of these things.)

      Finally, it is guaranteed that the named node has not been returned by a
      previous invocation of this method or getChild(String) after the last time
      that it was removed. In other words, a cached value will always be used in
      preference to invoking this method. Subclasses need not maintain their own
      cache of previously returned children.

      The implementer must ensure that the returned node has not been removed.
      If a like-named child of this node was previously removed, the implementer
      must return a newly constructed AbstractPreferences node; once removed, an
      AbstractPreferences node cannot be "resuscitated."

      If this method causes a node to be created, this node is not guaranteed to
      be persistent until the flush method is invoked on this node or one of its
      ancestors (or descendants).

      This method is invoked with the lock on this node held.
     */
    protected AbstractPreferences childSpi(String name) {
      PreferencesExt child;

      if (null != (child = (PreferencesExt) children.get(name)))
        return child;

      child = new PreferencesExt(this, name);
      children.put( name, child);
      child.newNode = true;
      return child;
    }

   /**
    * Empty, never used implementation  of AbstractPreferences.flushSpi().
    */
    protected void flushSpi() throws BackingStoreException {
        // assert false;
    }

    /* Gets the value with this keyName, if not found, look in storedDefaults.

      Return the value associated with the specified key at this preference node,
      or null if there is no association for this key, or the association cannot be
      determined at this time. It is guaranteed that key is non-null. Also, it is
      guaranteed that this node has not been removed. (The implementor needn't
      check for either of these things.)

      Generally speaking, this method should not throw an exception under any
      circumstances. If, however, if it does throw an exception, the exception
      will be intercepted and treated as a null return value.

      This method is invoked with the lock on this node held.
     */
    protected String getSpi(String keyName) {
      Object o = _getObject( keyName);
      return (o == null) ? null : o.toString();
    }

    /* Stores the value with this key, if not already in storedDefaults.

      Put the given key-value association into this preference node. It is guaranteed
      that key and value are non-null and of legal length. Also, it is guaranteed
      that this node has not been removed. (The implementor needn't check for any of these things.)
      This method is invoked with the lock on this node held.
    */
    protected void putSpi(String key,  String newValue) {
      // if matches a stored Default, dont store
      String oldValue = getSpi(key);
      if ((oldValue == null) || !oldValue.equals( newValue))
        keyValues.put( key, newValue);
    }

    /* removes node, no effect on storedDefaults

      Removes this preference node, invalidating it and any preferences that
      it contains. The named child will have no descendants at the time this
      invocation is made (i.e., the Preferences.removeNode() method invokes
      this method repeatedly in a bottom-up fashion, removing each of a node's
      descendants before removing the node itself).

      This method is invoked with the lock held on this node and its parent (
      and all ancestors that are being removed as a result of a single invocation
      to Preferences.removeNode()).

      The removal of a node needn't become persistent until the flush method is
      invoked on this node (or an ancestor).
     */
    protected void removeNodeSpi() throws BackingStoreException {
      //System.out.println(" removeNodeSpi :"+name());
      if (parent != null) {
        if (null == parent.children.remove( name()))
          System.out.println("ERROR PreferencesExt.removeNodeSpi :"+name());
      }
    }

    /** removes key/value if exists, no effect on storedDefaults
     *
      Remove the association (if any) for the specified key at this preference node.
      It is guaranteed that key is non-null. Also, it is guaranteed that this node
      has not been removed. (The implementor needn't check for either of these things.)
      This method is invoked with the lock on this node held.
     */
    protected void removeSpi(String key) {
      keyValues.remove( key);
    }

    /*
      This method is invoked with this node locked. The contract of this method is
      to synchronize any cached preferences stored at this node with any stored
      in the backing store. (It is perfectly possible that this node does not
      exist on the backing store, either because it has been deleted by another
      VM, or because it has not yet been created.) Note that this method should
      not synchronize the preferences in any subnodes of this node. If the backing
      store naturally syncs an entire subtree at once, the implementer is encouraged
      to override sync(), rather than merely overriding this method.

      If this node throws a BackingStoreException, the exception will propagate
      out beyond the enclosing sync() invocation.
    */
    protected void syncSpi() throws BackingStoreException {
        // assert false;
    }

    ////////////////////////////////////////////////////////////////////////////
    // low level

    Object getObjectNoDefaults(String keyName) {
      return keyValues.get( keyName);
    }

    public void putObject(String keyName, Object value) {
      if (keyName == null)
        throw new IllegalArgumentException("PreferencesExt try to store null keyname");
      keyValues.put( keyName, value);
    }

    public Object getObject(String key) {
      synchronized(lock) {
        Object result = null;
        try {
          result = _getObject(key);
        }
        catch (Exception e) {
          // Ignoring exception
        }
        return result;
      }
    }

    // assume key non-null, locked node
    private Object _getObject(String keyName) {
      Object result = null;
      try {
        result = keyValues.get( keyName);
        if (result == null) {
          // if failed, check the stored Defaults
          PreferencesExt sd = getStoredDefaults();
          if (sd != null)
            result = sd.getObjectFromNode( absolutePath(), keyName);
        }
      } catch (Exception e) {
              // Ignoring exception causes default to be returned
      }
      return result;
    }

    private Object getObjectFromNode(String nodePath, String keyName) {
      Object result = null;
      try {
        if (nodeExists( nodePath)) {
          PreferencesExt node = (PreferencesExt) node( nodePath);
          synchronized (node) {
            result = node._getObject( keyName);
          }
        }
      } catch (java.util.prefs.BackingStoreException e) { }

      // if failed, check the stored Defaults
      PreferencesExt sd = getStoredDefaults();
      if ((result == null) && (sd != null)) {
        synchronized (sd) {
          result = sd.getObjectFromNode( nodePath, keyName);
        }
      }
      return result;
    }

}

/* Change History:
   $Log: PreferencesExt.java,v $
   Revision 1.5  2005/08/22 17:13:58  caron
   minor fixes from intelliJ analysis

   Revision 1.4  2004/08/26 17:55:18  caron
   no message

   Revision 1.3  2003/01/06 23:21:17  john
   system root

   Revision 1.2  2002/12/24 22:04:48  john
   add bean, beanObject methods

   Revision 1.1.1.1  2002/12/20 16:40:25  john
   start new cvs root: prefs

   Revision 1.3  2002/02/26 18:35:49  caron
   convert to use JAXB

   Revision 1.2  2001/11/12 19:36:11  caron
   version 0.3

   Revision 1.1.1.1  2001/11/10 16:01:23  caron
   checkin prefs

*/
