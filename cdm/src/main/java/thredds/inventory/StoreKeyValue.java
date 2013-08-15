package thredds.inventory;

/**
 * Abstraction for object persistance using key/value stores.
 *
 * @author caron
 * @since 7/18/13
 */
public interface StoreKeyValue {

  public byte[] getBytes(String key);
  public void put(String key, byte[] value);
  public void close();

  public interface Factory {
    public StoreKeyValue open(String name);
  }

}
