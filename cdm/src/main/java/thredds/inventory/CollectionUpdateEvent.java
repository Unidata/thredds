/* Copyright */
package thredds.inventory;

import net.jcip.annotations.Immutable;

/**
 * Events when a fc should be updated.
 * guava.EventBus wires together listeners and sources, and
 *
 * @author caron
 * @since 6/30/2015
 */
@Immutable
public class CollectionUpdateEvent {
  private final CollectionUpdateType type;
  private final String collectionName;
  private final String source;

  public CollectionUpdateEvent(CollectionUpdateType type, String collectionName, String source) {
    this.type = type;
    this.collectionName = collectionName;
    this.source = source;
  }

  public CollectionUpdateType getType() {
    return type;
  }

  public String getCollectionName() {
    return collectionName;
  }

  @Override
  public String toString() {
    return collectionName+": "+ type + "source='" + source;
  }
}
