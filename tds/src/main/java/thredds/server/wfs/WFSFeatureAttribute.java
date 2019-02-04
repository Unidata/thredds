package thredds.server.wfs;

/**
 * A simple container for a WFS Feature Attribute
 *
 * @author Stanley Kaymen
 *
 */
public class WFSFeatureAttribute {

    private final String name;
    private final String type;

    /**
     * Gets the name of this WFS feature attribute.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the type of this WFS feature attribute.
     *
     * @return type
     */
    public String getType() { return type; }

    /**
     * Create a new feature attribute with the given name and type
     *
     * @param name
     * @param type
     */
    public WFSFeatureAttribute(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
