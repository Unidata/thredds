/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A HashMap that removes the oldest member when it exceeds the maximum number of entries.
 * LOOK replace with something in guava?
 * @author caron
 * @see java.util.LinkedHashMap
 */
public class HashMapLRU<K,V> extends LinkedHashMap<K,V> {
  static private final Logger logger = LoggerFactory.getLogger(HashMapLRU.class);

  private int max_entries = 100;

  /**
   * Constructor.
   *
   * @param initialCapacity start with this size
   * @param max_entries     dont exceed this number of entries.
   */
  public HashMapLRU(int initialCapacity, int max_entries) {
    super(initialCapacity, (float) .50, true);
    this.max_entries = max_entries;
  }

  @Override
  protected boolean removeEldestEntry(Map.Entry eldest) {
    if (logger.isDebugEnabled() && size() > max_entries) logger.debug("HashMapLRU ejected entry, max_entries = {}", max_entries);
    return size() > max_entries;
  }

}