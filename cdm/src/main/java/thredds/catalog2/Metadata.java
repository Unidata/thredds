/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
