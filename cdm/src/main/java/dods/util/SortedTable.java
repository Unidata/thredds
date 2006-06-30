/////////////////////////////////////////////////////////////////////////////
// Copyright (c) 1998, California Institute of Technology.  
// ALL RIGHTS RESERVED.   U.S. Government Sponsorship acknowledged. 
//
// Please read the full copyright notice in the file COPYRIGHT
// in this directory.
//
// Author: Jake Hamby, NASA/Jet Propulsion Laboratory
//         Jake.Hamby@jpl.nasa.gov
/////////////////////////////////////////////////////////////////////////////

package dods.util;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;

/** SortedTable looks exactly like Hashtable but preserves the insertion order
    of elements.  While this results in slower performance, it ensures that
    the DAS will always be printed in the same order in which it was read. */
public final class SortedTable extends Dictionary {
  private Vector keys, elements;

  public SortedTable() {
    keys = new Vector();
    elements = new Vector();
  }

  /** Returns the number of keys in this table. */
  public int size() {
    return keys.size();
  }

  /** Tests if this table is empty. */
  public boolean isEmpty() {
    return keys.isEmpty();
  }

  /** Returns an enumeration of the keys in this table. */
  public Enumeration keys() {
    return keys.elements();
  }

  /** Returns an enumeration of the values in this table. */
  public Enumeration elements() {
    return elements.elements();
  }

  /** Returns the value to which the key is mapped in this table.
    @param key a key in this table.
    @return the value to which the key is mapped, or null if the key is not
            mapped to any value in the table.
    */
  public synchronized Object get(Object key) {
    int index = keys.indexOf(key);
    if (index != -1)
      return elements.elementAt(index);
    else
      return null;
  }

  /** Returns the key at the specified index.
    @param index the index to return
    @return the key at the specified index.
    */
  public synchronized Object getKey(int index) {
    return keys.elementAt(index);
  }

  /** Returns the element at the specified index.
    @param index the index to return
    @return the element at the specified index.
    */
  public synchronized Object elementAt(int index) {
    return elements.elementAt(index);
  }

  /** Maps the specified key to the specified value in this table.
    @param key the key
    @param value the value
    @return the previous value to which the key is mapped, or null if the
            key did not have a previous mapping.
    @throw NullPointerException if the key or value is null.
    */
  public synchronized Object put(Object key, Object value) throws NullPointerException{
    if (key == null || value == null)
      throw new NullPointerException();

    int index = keys.indexOf(key);
    if (index != -1) {
      Object prev = elements.elementAt(index);
      elements.setElementAt(value, index);
      return prev;
    } else {
      keys.addElement(key);
      elements.addElement(value);
      return null;
    }
  }

  /** Removes the key (and its corresponding value) from this table.  If the
      key is not in the table, do nothing.
    @param key the key to remove.
    @return the value to which the key had been mapped, or null if the key
            did not have a mapping.
    */
  public synchronized Object remove(Object key) {
    int index = keys.indexOf(key);
    if(index != -1) {
      Object prev = elements.elementAt(index);
      keys.removeElementAt(index);
      elements.removeElementAt(index);
      return prev;
    } else {
      return null;
    }
  }

  /** Returns a Vector containing the elements in the SortedTable.  This is
    used for more efficient implementation of dods.dap.Util.uniqueNames() by
    dods.dap.DDS.checkSemantics()
    @return A Vector containing the elements in this SortedTable.
    */
  public Vector getElementVector() {
    return elements;
  }
}
