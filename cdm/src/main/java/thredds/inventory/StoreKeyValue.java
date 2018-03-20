/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

/**
 * Abstraction for object persistance using key/value stores.
 *
 * @author caron
 * @since 7/18/13
 */
public interface StoreKeyValue {

  byte[] getBytes(String key);
  void put(String key, byte[] value);
  void close();

  interface Factory {
    StoreKeyValue open(String name);
  }

}
