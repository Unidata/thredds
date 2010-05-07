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

package thredds.cataloggen.config;

import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDataset;

import java.util.List;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Provides filtering capabilities for InvDatasets.
 *
 * The static method acceptDatasetByFilterGroup() is provided to allow
 * filtering with a group of filters. To make filtering out a set of
 * datasets as easy as allowing a set of datasets, DatasetFilter provides
 * a dataset reject mode as well as a dataset accept mode. [Notes:
 * 1) rejection of a dataset takes precedence over acceptance; and 2) all
 * datasets are accepted if the filter group is empty.]
 *
 * A DatasetFilter instance contains information on how to filter datasets
 * and can be set to either accept or reject datasets that match
 * the filter criteria (accept is the default). The accept() method
 * should only be called if the isAcceptMatchingDatasets() method returns
 * true. Similarly, the reject() method should only be called if the
 * isRejectMatchingDatasets() method returns true.
 *
 * @author Ethan Davis
 * @since 2002-12-11T15:27+0700
 */

public class DatasetFilter
{
  private DatasetSource parentDatasetSource = null;

  // attributes of the datasetFilter element
  private String name = null;
  private DatasetFilter.Type type = null;
  private String matchPattern = null;
  protected Pattern regExpPattern;

  /** Specifies the target of the matchPattern. */
  private String matchPatternTarget = null;

  /** If true this DatasetFilter applies to collection datasets, if false
   *  it does not (the default value is false). */
  private boolean applyToCollectionDatasets = false;
  /** If true this DatasetFilter applies to atomic datasets, if false it
   *  does not (the default value is true). */
  private boolean applyToAtomicDatasets = true;
  
  /** Indicates whether datasets that match the filter criteria are accepted
   *  or rejected (accept is the default). */
  private boolean rejectMatchingDatasets = false;

  // validation flag and log
  private boolean isValid = true;
  private StringBuffer log = new StringBuffer();

  /**
   * Constructor
   * @param parentDsSource
   * @param name
   * @param type
   * @param matchPattern
   */
  public DatasetFilter( DatasetSource parentDsSource, String name,
                        DatasetFilter.Type type,
                        String matchPattern)
  {
    // Check that given type is not null.
    if ( type == null)
    {
      isValid = false;
      log.append( " ** DatasetFilter (1): invalid type for datasetFilter (" + name + ")");
    }
    this.parentDatasetSource = parentDsSource;
    this.name = name;
    this.type = type;

    if ( matchPattern == null )
    {
      isValid = false;
      log.append( " ** DatasetFilter (2): null matchPattern not allowed." );
    }
    else
    {
      this.matchPattern = matchPattern;
      try
      {
        this.regExpPattern = java.util.regex.Pattern.compile( this.matchPattern );
      }
      catch ( PatternSyntaxException e )
      {
        isValid = false;
        log.append( " ** DatasetFilter (3): invalid matchPattern [" + this.matchPattern + "]." );
      }
    }
  }
  
  public DatasetFilter( DatasetSource parentDsSource, String name, DatasetFilter.Type type,
                        String matchPattern, boolean applyToCollectionDatasets,
                        boolean applyToAtomicDatasets, boolean rejectMatchingDatasets)
  {
    this( parentDsSource, name, type, matchPattern);
    this.applyToCollectionDatasets = applyToCollectionDatasets;
    this.applyToAtomicDatasets = applyToAtomicDatasets;
    this.rejectMatchingDatasets = rejectMatchingDatasets;
  }

  /** Return the parent DatasetSource of this DatasetFilter */
  public DatasetSource getParentDatasetSource() {
    return( this.parentDatasetSource);
  }

  /**
   * Return the name of this DatasetFilter
   * @return String the name of this.
   */
  public String getName() {
    return( this.name);
  }

  /**
   * Return the type of this DatasetFilter
   * @return DatasetFilter.Type the type of this.
   */
  public DatasetFilter.Type getType() {
    return( this.type);
  }

  /**
   * Return the matchPattern of this DatasetFilter
   * @return String the matchPattern of this.
   */
  public String getMatchPattern() {
    return( this.matchPattern);
  }

  public String getMatchPatternTarget() {
    return matchPatternTarget;
  }

  public void setMatchPatternTarget( String matchPatternTarget) {
    this.matchPatternTarget = matchPatternTarget;
  }

  public boolean isApplyToCollectionDatasets() {
    return( this.applyToCollectionDatasets);
  }
  public void setApplyToCollectionDatasets( boolean applyToCollectionDatasets) {
    this.applyToCollectionDatasets = applyToCollectionDatasets;
  }
  public boolean isApplyToAtomicDatasets() {
    return( this.applyToAtomicDatasets);
  }
  public void setApplyToAtomicDatasets( boolean applyToAtomicDatasets) {
    this.applyToAtomicDatasets = applyToAtomicDatasets;
  }
  public boolean isRejectMatchingDatasets() {
    return( this.rejectMatchingDatasets);
  }
  public boolean isAcceptMatchingDatasets() {
    return( ! this.rejectMatchingDatasets);
  }
  public void setRejectMatchingDatasets( boolean rejectMatchingDatasets) {
    this.rejectMatchingDatasets = rejectMatchingDatasets;
  }

  /**
   * Validate this DatasetFilter object. Return true if valid, false if invalid.
   *
   * @param out StringBuffer with validation messages.
   * @return boolean true if valid, false if invalid
   */
  boolean validate( StringBuilder out)
  {
    this.isValid = true;

    // If log from construction has content, append to validation output msg.
    if (this.log.length() > 0)
    {
      out.append( this.log);
    }

    // Validity check: 'name' cannot be null. (Though, 'name'
    // can be an empty string.)
    if (this.getName() == null)
    {
      isValid = false;
      out.append(" ** DatasetFilter (4): null value for name is not valid.");
    }

    // Check that type is not null.
    if ( this.getType() == null)
    {
      isValid = false;
      out.append( " ** DatasetFilter (5): null value for type is not valid (set with bad string?).");
    }

    // Validity check: 'matchPattern' must be null if 'type' value
    // is not 'RegExp'.
    if ( this.type == DatasetFilter.Type.REGULAR_EXPRESSION &&
         this.matchPattern == null)
    {
      isValid = false;
      out.append(" ** DatasetFilter (6): null value for matchPattern not valid when type is 'RegExp'.");
    }
    if ( this.type != DatasetFilter.Type.REGULAR_EXPRESSION &&
         this.type != null &&
         this.matchPattern != null)
    {
      isValid = false;
      out.append( " ** DatasetFilter (7): matchPattern value (" + this.matchPattern +
                  ") must be null if type is not 'RegExp'.");
    }

    return( this.isValid);
  }

  /** string representation */
  public String toString()
  {
    StringBuffer tmp = new StringBuffer();
    tmp.append( "DatasetFilter[name:<" + this.getName() +
                "> type:<" + this.getType() +
                "> matchPattern:<" + this.getMatchPattern() + ">");

    return( tmp.toString());
  }

  /**
   * Test whether the given dataset should be excluded from the dataset collection.
   * @param dataset - the dataset to be tested
   * @return true if and only if the dataset should be excluded
   *
   * @throws IllegalStateException if the filter accepts rather than rejects datasets.
   */
  public boolean reject( InvDataset dataset)
  {
    if ( this.isAcceptMatchingDatasets())
      throw new IllegalStateException( "Accept filter <" + this.getName() + "> does not allow call to reject().");
    return( this.match( dataset));
  }

  /**
   * Test whether the given dataset should be included in a dataset collection.
   * @param dataset - the dataset to be tested
   * @return true if and only if the dataset should be included
   *
   * @throws IllegalStateException if the filter rejects rather than accepts datasets.
   */
  public boolean accept( InvDataset dataset)
  {
    if ( this.isRejectMatchingDatasets())
      throw new IllegalStateException( "Reject filter <" + this.getName() + "> does not allow call to accept().");
    return( this.match( dataset));
  }

  protected boolean appliesToDataset( InvDataset dataset )
  {
    // Check whether this filter applies to the given dataset.
    if ( this.getParentDatasetSource().isCollection( dataset )
         && ! this.applyToCollectionDatasets )
      return ( false );
    if ( ( ! this.getParentDatasetSource().isCollection( dataset ) )
         && ! this.applyToAtomicDatasets )
      return ( false );
    return( true);
  }

  /**
   * Test whether the given dataset matches the filter criteria.
   * @param dataset - the dataset to be tested.
   * @return true if and only if the dataset matches the filter criteria.
   */
  private boolean match( InvDataset dataset)
  {
    // Check whether this filter applies to the given dataset.
    if ( this.getParentDatasetSource().isCollection( dataset ) && ! this.applyToCollectionDatasets)
      return( false);
    if ( (! this.getParentDatasetSource().isCollection( dataset)) && ! this.applyToAtomicDatasets)
      return( false);

    // Set the default matchPatternTarget so old versions still work.
    if ( this.matchPatternTarget == null)
    {
      if ( this.getParentDatasetSource().isCollection( dataset))
      {
        this.setMatchPatternTarget( "name");
      }
      else
      {
        this.setMatchPatternTarget( "urlPath");
      }
    }

    if ( this.type == DatasetFilter.Type.REGULAR_EXPRESSION)
    {
      boolean isMatch;
      if ( this.getMatchPatternTarget().equals( "name"))
      {
        java.util.regex.Matcher matcher = this.regExpPattern.matcher( dataset.getName() );
        isMatch = matcher.matches();
      }
      else if ( this.getMatchPatternTarget().equals( "urlPath"))
      {

        java.util.regex.Matcher matcher = this.regExpPattern.matcher( ( (InvDatasetImpl) dataset ).getUrlPath() );
        isMatch = matcher.matches();
      }
      else
      {
        // ToDo deal with any matchPatternTarget (XPath-ish)
        isMatch = false;
      }

//      // Invert the meaning of a match (accept things that don't match).
//      if ( this.isRejectMatchingDatasets())
//      {
//        // If match, return false.
//        return( regExpMatch == null ? true : false );
//      }
//      // Don't invert (a match is a match).
//      else
//      {
        // If match, return true.
        return( isMatch );
//      }
    }
    else
    {
      System.err.println( "WARNING -- DatasetFilter.accept(): unsupported type" +
        " <" + this.type.toString() + ">.");
        return( false);
      // @todo think about exceptions.
      //throw new java.lang.Exception( "DatasetFilter.accept():" +
      //  " unsupported type <" + this.type.toString() + ">.");
    }

  }

  /**
   * Given a dataset and a group of filters, return true if the group of
   * filters indicates that the dataset should be accepted, false if it
   * should be rejected.
   *
   * To make filtering out a set of datasets as easy as allowing a set of
   * datasets, DatasetFilter provides a dataset reject mode as well as a
   * dataset accept mode. Rejection of a dataset by any filter overrides
   * acceptance by any number of filters. Therefore, to be accepted by the
   * group a dataset needs to be accepted by at least one filter, however,
   * rejection by a single filter will cause rejection by the group.
   *
   * If the filter group is empty, the dataset will be accepted.
   *
   * @param filters - the group of filters to apply to the dataset.
   * @param dataset - the dataset on which to apply the filter group.
   * @param isCollectionDataset
   * @return true if the group of filters indicates that the dataset should be accepted, false otherwise.
   *
   * @throws NullPointerException if the filter list or the dataset is null.
   */
  public static boolean acceptDatasetByFilterGroup( List filters, InvDataset dataset, boolean isCollectionDataset )
  {
    if ( filters == null) throw new NullPointerException( "Given null list of filters.");
    if ( dataset == null) throw new NullPointerException( "Given null dataset.");

    // If not filters, accept all datasets.
    if ( filters.isEmpty())
      return( true);

    // Loop through DatasetFilter list to check if current dataset should be accepted.
    // @todo If none of the filters apply to directories, accept all directories.
    boolean accept = false;
    boolean anyApplyToAtomic = false;
    boolean anyApplyToCollection = false;
    for ( Iterator it = filters.iterator(); it.hasNext(); )
    {
      DatasetFilter curFilter = (DatasetFilter) it.next();
      anyApplyToAtomic |= curFilter.isApplyToAtomicDatasets();
      anyApplyToCollection |= curFilter.isApplyToCollectionDatasets();

      if ( curFilter.isAcceptMatchingDatasets())
      {
        if ( curFilter.accept( dataset))
        {
          accept = true; // Dataset accepted by current DatasetFilter.
        }
      }
      else // if ( ! curFilter.isAcceptMatchingDatasets())
      {
        if ( curFilter.reject( dataset))
        {
          return( false); // Rejection takes precedence over accpetance
        }
      }
    }

    // At least one filter accepted (and none rejected), so accept.
    if ( accept) return( true);

    // Check if any filters apply to dataset. If none apply, accept the dataset.
    if ( isCollectionDataset)
    {
      if ( ! anyApplyToCollection ) return( true); // Collection ds, no collection filters.
    }
    else
    {
      if ( ! anyApplyToAtomic ) return( true); // Atomic ds, no atomic filters.
    }

    // Dataset not accepted or rejected by any DatasetFilter (so reject).
    return( false);
  }

  enum Type
  {
    REGULAR_EXPRESSION( "RegExp");

    private String altId;
    Type( String altId) {
      this.altId = altId;
    }

    public String toString() {
      return this.altId;
    }

    public static Type getType( String altId )
    {
      if ( altId == null )
        return null;

      for ( Type curType : Type.values() ) {
        if ( curType.altId.equals( altId ) )
          return curType;
      }
      return null;
    }
  }
}
