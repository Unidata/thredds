// $Id: Selector.java,v 1.3 2006/01/20 20:42:05 caron Exp $
package thredds.dqc;

/**
 * Represents a DQC selector element.
 *
 * User: edavis
 * Date: Jan 22, 2004
 * Time: 10:26:24 PM
 */
public abstract class Selector
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( Selector.class );

  protected String id = null;
  protected String title = null;
  protected String template = null;
  protected boolean required = true;
  protected boolean multiple = false;
  protected Description description = null;

  /** Null constructor. */
  protected Selector(){}

  /** Full constructor. */
  protected Selector( String id, String title, String template,
                      boolean required, boolean multiple, Description description)
  {
    this.id = id;
    this.title = title;
    this.template = template;
    this.required = required;
    this.multiple = multiple;
    this.description = description;
  }

  /** Full constructor. */
  protected Selector( String id, String title, String template, Description description)
  {
    this.id = id;
    this.title = title;
    this.template = template;
    this.description = description;
  }

  /** Get the id of this selector. */
  public String getId() { return id; }

  /** Get the title of this selector. */
  public String getTitle() { return title; }

  /** Get the template of this selector. */
  public String getTemplate() { return template; }

  /** Check if this selector is required for a valid query. */
  public boolean isRequired() { return required; }

  /** Check whether multiple selections can be made on this selector. */
  public boolean isMultiple() { return multiple; }

  /** Get the description for this selector. */
  public Description getDescription() { return description; }


  /** Set the id of this selector. */
  protected void setId( String id ) { this.id = id; }

  /** Set the title of this selector. */
  public void setTitle( String title ) { this.title = title; }

  /** Set the template of this selector. */
  protected void setTemplate( String template ) { this.template = template; }

  /** Set whether this selector is required for a valid query. */
  public void setRequired( boolean required ) { this.required = required; }

  /** Set whether multiple selections can be made on this selector. */
  public void setMultiple( boolean multiple ) { this.multiple = multiple; }

  /** */
  public void setDescription( String description ) { this.description = new Description( description); }

  /** */
  public abstract Selection validateSelection( Selection selection);

}

/*
 * $Log: Selector.java,v $
 * Revision 1.3  2006/01/20 20:42:05  caron
 * convert logging
 * use nj22 libs
 *
 * Revision 1.2  2005/04/05 22:37:04  edavis
 * Convert from Log4j to Jakarta Commons Logging.
 *
 * Revision 1.1  2004/03/05 06:33:40  edavis
 * Classes to handle DQC and user query information.
 *
 */