// $Id: DatasetNamer.java 63 2006-07-12 21:50:51Z edavis $

package thredds.cataloggen.config;

import opendap.dap.*;
import thredds.catalog.InvAccess;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.ServiceType;
import thredds.catalog.InvDataset;

import java.util.ArrayList;


/**
 * <p>Title: Catalog Generator</p>
 * <p>Description: Tool for generating THREDDS catalogs.</p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Company: UCAR/Unidata</p>
 *
 * @author Ethan Davis
 * @version 1.0
 */

public class DatasetNamer {
  static private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatasetNamer.class);

  private InvDataset parentDataset = null;

  // attributes of the datasetNamer element
  private String name = null;
  private boolean addLevel = false;
  private DatasetNamerType type = null;
  private String matchPattern = null;
  private String substitutePattern = null;
  private String attribContainer = null;
  private String attribName = null;

  // for validation
  private boolean isValid = true;
  private StringBuffer msgLog = new StringBuffer();

  /**
   * Constructor
   *
   * @param parentDs
   * @param name
   * @param addLevelBoolean
   * @param typeName
   * @param matchPattern
   * @param substitutePattern
   * @param attribContainer
   * @param attribName
   */
  public DatasetNamer(InvDataset parentDs,
          String name, String addLevelBoolean, String typeName,
          String matchPattern, String substitutePattern,
          String attribContainer, String attribName) {
    this(parentDs, name,
            (new Boolean(addLevelBoolean)).booleanValue(),
            DatasetNamerType.getType(typeName),
            matchPattern, substitutePattern,
            attribContainer, attribName);
    // Check that type is not null.
    if (this.getType() == null) {
      this.isValid = false;
      msgLog.append(" ** DatasetNamer (1): invalid type =(" + typeName + ") for datasetNamer (" + name + ")");
    }
  }

  /**
   * Constructor
   *
   * @param parentDs
   * @param name
   * @param addLevel
   * @param type
   * @param matchPattern
   * @param substitutePattern
   * @param attribContainer
   * @param attribName
   */
  public DatasetNamer(InvDataset parentDs,
          String name, boolean addLevel, DatasetNamerType type,
          String matchPattern, String substitutePattern,
          String attribContainer, String attribName) {
    this.parentDataset = parentDs;
    this.name = name;
    this.addLevel = addLevel;
    this.type = type;
    this.matchPattern = matchPattern;
    this.substitutePattern = substitutePattern;
    this.attribContainer = attribContainer;
    this.attribName = attribName;
  }

  /**
   * Return the parent dataset of this DatasetNamer
   */
  public InvDataset getParentDataset() {
    return (this.parentDataset);
  }

  /**
   * Set the type of this DatasetNamer
   */
  public void setParentDataset(InvDataset parentDataset) {
    this.parentDataset = parentDataset;
  }

  /**
   * Return the name of this DatasetNamer.
   *
   * @return String name of this DatasetNamer
   */
  public String getName() {
    return (this.name);
  }

  /**
   * Set name attribute for this DatasetNamer
   *
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Return the value of the addLevel attribute of this DatasetNamer.
   *
   * @return boolean addLevel attribute value for this DatasetNamer
   */
  public boolean getAddLevel() {
    return (this.addLevel);
  }

  /**
   * Set the value of the addLevel attribute
   *
   * @param addLevel
   */
  public void setAddLevel(boolean addLevel) {
    this.addLevel = addLevel;
  }

  /**
   * Return the type attribute of this DatasetNamer.
   *
   * @return DatasetNamerType type value for this DatasetNamer
   */
  public DatasetNamerType getType() {
    return (this.type);
  }

  /**
   * Set the value of the type attribute
   *
   * @param typeName String
   */
  public void setType(String typeName) {
    this.type = DatasetNamerType.getType(typeName);
    // Check that type is not null.
    if (this.getType() == null) {
      this.isValid = false;
      msgLog.append(" ** DatasetNamer (2): invalid type =(" + typeName + ") for datasetNamer (" + name + ")");
    }
  }

  /**
   * Set the value of the type attribute
   *
   * @param type DatasetNamerType
   */
  public void setType(DatasetNamerType type) {
    this.type = type;
  }

  /**
   * Return the value of the matchPattern attribute of this DatasetNamer.
   *
   * @return String value of matchPattern
   */
  public String getMatchPattern() {
    return (this.matchPattern);
  }

  /**
   * Set the value of the matchPattern attribute
   *
   * @param mPat
   */
  public void setMatchPattern(String mPat) {
    this.matchPattern = mPat;
  }

  /**
   * Return the value of the substitutePattern attribute of this DatasetNamer.
   *
   * @return String - value of substitutePattern
   */
  public String getSubstitutePattern() {
    return (this.substitutePattern);
  }

  /**
   * Set the value of the substututePattern attribute
   *
   * @param sPat
   */
  public void setSubstitutePattern(String sPat) {
    this.substitutePattern = sPat;
  }

  /**
   * Return the value of the attribContainer attribute of this DatasetNamer.
   *
   * @return String - value of attribContainer
   */
  public String getAttribContainer() {
    return (this.attribContainer);
  }

  /**
   * Set the value of the attribContainer attribute
   *
   * @param attContainer
   */
  public void setAttribContainer(String attContainer) {
    this.attribContainer = attContainer;
  }

  /**
   * Return the value of the attribName attribute of this DatasetNamer.
   *
   * @return String - value of attribName
   */
  public String getAttribName() {
    return (this.attribName);
  }

  /**
   * Set the value of the attribute Name attribute
   *
   * @param attName
   */
  public void setAttribName(String attName) {
    this.attribName = attName;
  }

  /**
   * Validate this DatasetNamer object. Return true if valid, false if invalid.
   *
   * @param out StringBuffer with validation messages.
   * @return boolean true if valid, false if invalid
   */
  boolean validate(StringBuilder out) {
    this.isValid = true;

    // If log from construction has content, append to validation output msg.
    if (this.msgLog.length() > 0) {
      out.append(this.msgLog);
    }

    // Check that name is not null (it can be an empty string).
    if (this.getName() == null) {
      this.isValid = false;
      out.append(" ** DatasetNamer (1): null value for name is not valid.");
    }

    // Check that addLevel is not null.
    // boolean can't be null
    //if ( this.getAddLevel() == null)
    //{
    //  this.isValid = false;
    //  out.append(" ** DatasetNamer (2): null value for addLevel is not valid.");
    //}

    // Check that type is not null.
    if (this.getType() == null) {
      this.isValid = false;
      out.append(" ** DatasetNamer (3): null value for type is not valid (set with bad string?).");
    }

    if (this.getType() == DatasetNamerType.REGULAR_EXPRESSION
            && (this.getMatchPattern() == null || this.getSubstitutePattern() == null
            || this.getAttribContainer() != null || this.getAttribName() != null)) {
      this.isValid = false;
      out.append(" ** DatasetNamer (4): invalid datasetNamer <" + this.getName() + ">;" +
              " type is " + this.getType().toString() + ": matchPattern(" + this.getMatchPattern() + ") and substitutionPattern(" + this.getSubstitutePattern() + ") " +
              "must not be null and attriuteContainer(" + this.getAttribContainer() + ") and attributeName(" + this.getAttribName() + ") must be null.");
    }

    if (this.getType() == DatasetNamerType.DODS_ATTRIBUTE
            && (this.getMatchPattern() != null || this.getSubstitutePattern() != null
            || this.getAttribContainer() == null || this.getAttribName() == null)) {
      this.isValid = false;
      out.append(" ** DatasetNamer (5): invalid datasetNamer <" + this.getName() + ">;" +
              " type is " + this.getType().toString() + ": matchPattern(" + this.getMatchPattern() + ") and substitutionPattern(" + this.getSubstitutePattern() + ") " +
              "must be null and attriuteContainer(" + this.getAttribContainer() + ") and attributeName(" + this.getAttribName() + ") must not be null.");
    }

    return (this.isValid);
  }

  /**
   * string representation
   */
  public String toString() {
    StringBuffer tmp = new StringBuffer();
    tmp.append("DatasetNamer[name:<" + this.getName() + "> addLevel:<" +
            this.getAddLevel() + "> type:<" + this.getType() +
            "> matchPattern:<" + this.getMatchPattern() +
            "> substitutePatter:<" + this.getSubstitutePattern() +
            "> attribContainer:<" + this.getAttribContainer() +
            "> attribName:<" + this.getAttribName() + ">]");
    return (tmp.toString());
  }

  /**
   * Try to name the given dataset.
   */
  public boolean nameDataset(InvDatasetImpl dataset) {
    if (this.type == DatasetNamerType.REGULAR_EXPRESSION) {
      return (this.nameDatasetRegExp(dataset));
    } else if (this.type == DatasetNamerType.DODS_ATTRIBUTE) {
      return (this.nameDatasetDodsAttrib(dataset));
    } else {
      String tmpMsg = "This DatasetNamer <" + this.getName() + "> has unsupported type <" + this.type.toString() + ">.";
      logger.error("nameDataset(): " + tmpMsg);
      throw new IllegalStateException(tmpMsg);
    }

  }

  /**
   * Try to name the given dataset.
   */
  public boolean nameDatasetList(java.util.List datasetList)
          throws java.lang.Exception {
    boolean returnValue = false;

    InvDatasetImpl curDataset = null;
    for (int i = 0; i < datasetList.size(); i++) {
      curDataset = (InvDatasetImpl) datasetList.get(i);
      returnValue &= this.nameDataset(curDataset);
    }
    return (returnValue);
  }

  /**  */
  private boolean nameDatasetRegExp(InvDatasetImpl dataset) {
    // @todo Replace gnu.regexp with java.util.regexp???
    gnu.regexp.RE regExp = null;
    gnu.regexp.REMatch regExpMatch = null;

    // Setup the regular expression.
    try {
      regExp = new gnu.regexp.RE(this.matchPattern);
    }
    catch (gnu.regexp.REException e) {
      logger.debug("nameDatasetRegExp(): regular expression failed: {}", e.getMessage());
      return (false);
    }

    // Test for a match on the urlPath
    if (dataset.getUrlPath() != null) {
      logger.debug("nameDatasetRegExp(): try naming on urlPath <{}>", dataset.getUrlPath());
      regExpMatch = regExp.getMatch(dataset.getUrlPath());
    } else {
      regExpMatch = regExp.getMatch(dataset.getName());
    }

    if (regExpMatch != null) {
      // Test for substitution.
      String name = regExpMatch.substituteInto(substitutePattern);
      if (name != null) {
        logger.debug("nameDatasetRegExp(): Setting name to \"" + name + "\".");
        dataset.setName(name);
        return (true);
      } else {
        logger.debug("nameDatasetRegExp(): No name for regEx substitution.");
        return (false);
      }
    }
    if (logger.isDebugEnabled())
      logger.debug("nameDatasetRegExp(): Neither URL <" + dataset.getUrlPath() + "> or name <" +
              dataset.getName() + "> matched pattern <" + this.matchPattern + "> .");
    return (false);
  }

  /**  */
  private boolean nameDatasetDodsAttrib(InvDatasetImpl dataset)
//    throws java.lang.Exception
  {
    DConnect dodsConnection = null;
    DAS das = null;

    boolean acceptDeflate = true;

    String newDatasetName = null;

    //-----
    // Test that this dataset has a DODS type service.
    //-----
    InvAccess access = dataset.getAccess(ServiceType.DODS);

    if (access == null) {
      logger.warn("nameDatasetDodsAttrib(): dataset is not DODS accessible and so cannot be named using DODS attributes.");
      return (false);
    }

    //-----
    // Connect to DODS dataset.
    //-----
    String url = access.getStandardUrlName();

    try {
      dodsConnection = new DConnect(url, acceptDeflate);
    }
    catch (java.io.FileNotFoundException e) {
      logger.error("nameDatasetDodsAttrib(): URL <" + url + "> not found: " + e.getMessage());
      return (false);
    }
    catch (Exception e) // java.lang.NullPointerException
    {
      logger.error("nameDatasetDodsAttrib(): Failed DODS connect: " + e.getMessage());
      return (false);
    }

    logger.debug("nameDatasetDodsAttrib(): Got DODS Connect <url={}>", url);

    //-----
    // Get the DAS
    //-----
    try {
      das = dodsConnection.getDAS();
    }
    catch (DAP2Exception e) // DODSException and DASException
    {
      logger.error("nameDatasetDodsAttrib(): Failed to get DAS: " + e.getMessage());
      return (false);
    }
    catch (opendap.dap.parser.ParseException e) {
      logger.error("nameDatasetDodsAttrib(): Failed to get DAS: " + e.getMessage());
      return (false);
    }
    catch (Exception e) // java.net.MalformedURLException, java.io.IOException
    {
      logger.error("nameDatasetDodsAttrib(): Failed to get DAS: " + e.getMessage());
      return (false);
    }

    logger.debug("nameDatasetDodsAttrib(): Got DAS");

    //-----
    // Get attribute value from attribute container.
    //-----
    AttributeTable dodsAttTable = null;
    try {
      dodsAttTable = das.getAttributeTable(this.attribContainer);

      if (dodsAttTable != null) {
        Attribute desiredAtt =
                dodsAttTable.getAttribute(this.attribName);
        // Check that desired attribute is a string
        if (desiredAtt.getType() == opendap.dap.Attribute.STRING) {
          java.util.Enumeration enumValues = desiredAtt.getValues();
          if (enumValues.hasMoreElements()) {
            newDatasetName = (String) enumValues.nextElement();
            // Java DODS string attributes are enclosed in double quotes.
            // Remove them.
            newDatasetName = newDatasetName.substring(1, newDatasetName.length() - 1);
            if (enumValues.hasMoreElements()) {
              // If attribute has more than one value, return false.
              logger.warn("nameDatasetDodsAttrib(): attribute has multiple values, only using first value <" + newDatasetName + ">");
              dataset.setName(newDatasetName);
              return (true);
            }
            // Desired attribute is a string and contains only one value,
            // set the dataset name and return true.
            logger.debug("nameDatasetDodsAttrib(): setting dataset name to <{}>.", newDatasetName);
            dataset.setName(newDatasetName);
            return (true);
          } else {
            // If attribute has no values, return false.
            logger.debug("nameDatasetDodsAttrib(): attribute has no value");
            return (false);
          }
        } else {
          // Desired attribute is not a string, return false.
          logger.debug("nameDatasetDodsAttrib(): attribute value is not a string.");
          return (false);
        }
      } else {
        // No such attribute container, return false.
        logger.debug("nameDatasetDodsAttrib(): attribute container does not exist.");
        return (false);
      }

    } catch (NoSuchAttributeException e) {
        // No such attribute container, return false.
        logger.debug("nameDatasetDodsAttrib(): attribute container does not exist.");
        return (false);
    }
  }
}
