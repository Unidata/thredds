package thredds.inventory;

/**
 * Mixin interface for getting Collection Update events
 *
 * @author caron
 * @since 11/20/13
 */
public interface CollectionUpdateListener {
  //public enum TriggerType {updateNocheck, update, resetProto}

  public String getCollectionName();

  public void sendEvent(CollectionUpdateType type);
}
