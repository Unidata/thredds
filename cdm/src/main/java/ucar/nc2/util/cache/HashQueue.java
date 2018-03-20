/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.cache;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Describe
 *
 * @author caron
 * @since 10/29/2014
 */
public class HashQueue<K, V> {

  private NavigableMap<K, V> cache;

  public HashQueue(Comparator<? super K> comparator) {

    cache = new ConcurrentSkipListMap<>(comparator);
  }


  public V get(K key) {
    return cache.get(key);
  }

  public V remove(K key) {
    return cache.remove(key);
  }

  public void makeMRU(K key) {
    // Iterator<V> iter = cache.iterator();

  }


}
