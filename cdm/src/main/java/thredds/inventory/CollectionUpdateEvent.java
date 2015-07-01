/* Copyright */
package thredds.inventory;

import net.jcip.annotations.Immutable;

/**
 * Describe
 *
 * @author caron
 * @since 6/30/2015
 */
@Immutable
public class CollectionUpdateEvent {
  private final CollectionUpdateType type;
  private final String collectionName;

  public CollectionUpdateEvent(CollectionUpdateType type, String collectionName) {
    this.type = type;
    this.collectionName = collectionName;
  }

  public CollectionUpdateType getType() {
    return type;
  }

  public String getCollectionName() {
    return collectionName;
  }
}
