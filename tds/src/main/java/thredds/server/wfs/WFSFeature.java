package thredds.server.wfs;

import java.util.ArrayList;

/**
 * A simple container for a WFS Feature Type
 * 
 * @author wchen@usgs.gov
 *
 */
public class WFSFeature {

	private final String name;
	private final String title;
	private String fileDSName;
	private final String type;
	private final ArrayList<WFSFeatureAttribute> attributes;	
	
	/**
	 * Gets the name of this WFS feature.
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the title of this WFS feature.
	 * 
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * gets the type of this WFS Feature
	 *
	 * @return
	 */
	public String getType() { return type; }

	/**
	 * gets the list of attributes of this WFS Feature
	 *
	 * @return
	 */
	public ArrayList<WFSFeatureAttribute> getAttributes() {return attributes;}
	
	/**
	 * Gets the file / dataset name from which the WFS feature was retrieved.
	 * 
	 * The File DS name is used for the namespace of the feature type.
	 * 
	 * @return
	 */
	public String getFileDSName() {
		return fileDSName;
	}

	/**
	 * Create a new WFS Feature with the given name and title
	 *
	 * @param name
	 * @param title
	 */
	public WFSFeature(String name, String title) {
		this(name, title, null, null);
	}

	/**
	 * Create a new WFS Feature with given name, title, type, and attributes
	 *
	 * @param name
	 * @param title
	 * @param type
	 * @param attributes
	 */
	public WFSFeature(String name, String title, String type, ArrayList<WFSFeatureAttribute> attributes) {
		this.name = name;
		this.title = title;
		fileDSName = null;
		this.type = type;
		this.attributes = attributes;
	}

	/**
	 * add attributes to list of attributes
	 *
	 * @param attribute
	 */
	public void addAttribute(WFSFeatureAttribute attribute) {
		attributes.add(attribute);
	}
}
