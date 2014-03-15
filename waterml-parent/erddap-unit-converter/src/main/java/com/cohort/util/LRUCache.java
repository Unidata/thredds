/* This file is Copyright (c) 2010, NOAA.
 * See the MIT/X-like license in LICENSE.txt.
 * For more information, bob.simons@noaa.gov.
 */
package com.cohort.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A HashMap/cache where, when size > tMaxSize and a new element is added, 
 *     the Least-Recently-Used element will be removed.
 *
 * <p>This not thread-safe. To make it thread-safe, use
 *   <tt>Map cache = Collections.synchronizedMap(new LRUCache(maxSize));</tt>
 *
 */
public class LRUCache extends LinkedHashMap {

    int maxSize;
 
    /** Constructor 
     * @param tMaxSize the maximum number of elements you want this to cache.
     *     When size > tMaxSize and a new element is added, 
     *     the Least-Recently-Used element will be removed.
     */
    public LRUCache(int tMaxSize) {
        super(tMaxSize + 1, 0.75f, true); //true means 'eldest' based on when last accessed (not when inserted) 
        maxSize = tMaxSize;
    }

    /** RemoveEldestEntry is over-ridden to enforce maxSize rule. */
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > maxSize;
     }

    /** Test this class. */
    public static void test() {
        String2.log("\n*** LRUCache.test");
        LRUCache cache = new LRUCache(5);
        for (int i = 0; i < 5; i++)
            cache.put("" + i, "" + (11 * i));
        Test.ensureEqual(cache.size(), 5, "");
        Test.ensureNotNull(cache.get("0"), ""); //0 was eldest. Now accessed so 1 is eldest

        //knock "1" out of cache
        cache.put("6", "66");
        Test.ensureEqual(cache.size(), 5, "");
        Test.ensureTrue(cache.get("1") == null, "");
        Test.ensureNotNull(cache.get("2"), "");

        //knock "3" out of cache
        cache.put("7", "77");
        Test.ensureEqual(cache.size(), 5, "");
        Test.ensureTrue(cache.get("3") == null, "");
        Test.ensureNotNull(cache.get("4"), "");

        String2.log("LRUCache.test finished");           
    }

} 
