package thredds.catalog2;

import thredds.catalog.MetadataType;

import java.net.URI;
import java.util.List;

/**
 * Represents non-THREDDS metadata about the containing dataset. This Metadata object
 * may either directly contain content or contain an reference to an external resource.
 *
 * @author edavis
 * @since 4.0
 */
public interface Metadata
{
  /**
   * Return true if this Metadata object directly contains content and return false if,
   * this Metadata object instead contains a title for and a reference to an external resource.
   *
   * @return true if this Metadata object directly contains content, false if it instead contains a referernce to an external resource.
   */
  public boolean isContainedContent();

  /**
   * Return the title for the external resource this Metadata object references.
   *
   * @return the title for the external resource.
   * @throws IllegalStateException if this Metadata object contains content internally.
   */
  public String getTitle();

  /**
   * Return the reference to the external resource this Metadata object references.
   *
   * @return the reference for the external resource.
   * @throws IllegalStateException if this Metadata object contains content internally.
   */
  public URI getExternalReference();

  /**
   * Return the content contained in this Metadata object.
   *
   * @return the content contained in this Metadata object.
   * @throws IllegalStateException if this Metadata object references an external resource.
   */
  public String getContent();
}
