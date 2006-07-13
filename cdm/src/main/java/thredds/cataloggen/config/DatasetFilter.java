// $Id: DatasetFilter.java 63 2006-07-12 21:50:51Z edavis $

package thredds.cataloggen.config;

import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDataset;

import java.util.List;
import java.util.Iterator;

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
    this.matchPattern = matchPattern;
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
  public DatasetSource getParentDatasetSource()
  { return( this.parentDatasetSource); }
  /** Set the parent DatasetSource of this DatasetFilter */
  public void setParentDatasetSource( DatasetSource parentDatasetSource)
  { this.parentDatasetSource = parentDatasetSource; }

  /**
   * Return the name of this DatasetFilter
   * @return String the name of this.
   */
  public String getName() { return( this.name); }

  /**
   * Set the value of the name for this DatasetFilters.
   * @param name
   */
  public void setName( String name) { this.name = name; }

  /**
   * Return the type of this DatasetFilter
   * @return DatasetFilter.Type the type of this.
   */
  public DatasetFilter.Type getType() { return( this.type); }

  /**
   * Set the value of the type for this DatasetFilter
   * @param type
   */
  public void setType( DatasetFilter.Type type)
  {
    this.type = type;

    if (this.getType() == null)
    {
      isValid = false;
      log.append(" ** DatasetFilter (2): null value for type is not valid.");
    }
  }

  /**
   * Return the matchPattern of this DatasetFilter
   * @return String the matchPattern of this.
   */
  public String getMatchPattern() { return( this.matchPattern); }

  /**
   * Set the value of the matchPattern for this DatasetFilter
   * @param newMatchPattern
   *
   */
  // @todo should check that type is "RegExp" before setting matchPattern.
  public void setMatchPattern( String newMatchPattern)
  {
    this.matchPattern = newMatchPattern;
  }

  public String getMatchPatternTarget()
  {
    return matchPatternTarget;
  }

  public void setMatchPatternTarget( String matchPatternTarget)
  {
    this.matchPatternTarget = matchPatternTarget;
  }

  public boolean isApplyToCollectionDatasets()
  {
    return( this.applyToCollectionDatasets);
  }
  public void setApplyToCollectionDatasets( boolean applyToCollectionDatasets)
  {
    this.applyToCollectionDatasets = applyToCollectionDatasets;
  }
  public boolean isApplyToAtomicDatasets()
  {
    return( this.applyToAtomicDatasets);
  }
  public void setApplyToAtomicDatasets( boolean applyToAtomicDatasets)
  {
    this.applyToAtomicDatasets = applyToAtomicDatasets;
  }
  public boolean isRejectMatchingDatasets()
  {
    return( this.rejectMatchingDatasets);
  }
  public boolean isAcceptMatchingDatasets()
  {
    return( ! this.rejectMatchingDatasets);
  }
  public void setRejectMatchingDatasets( boolean rejectMatchingDatasets)
  {
    this.rejectMatchingDatasets = rejectMatchingDatasets;
  }

  /**
   * Validate this DatasetFilter object. Return true if valid, false if invalid.
   *
   * @param out StringBuffer with validation messages.
   * @return boolean true if valid, false if invalid
   */
  boolean validate( StringBuffer out)
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
      out.append(" ** DatasetFilter (3): null value for name is not valid.");
    }

    // Check that type is not null.
    if ( this.getType() == null)
    {
      isValid = false;
      out.append( " ** DatasetFilter (4): null value for type is not valid (set with bad string?).");
    }

    // Validity check: 'matchPattern' must be null if 'type' value
    // is not 'RegExp'.
    if ( this.type == DatasetFilter.Type.REGULAR_EXPRESSION &&
         this.matchPattern == null)
    {
      isValid = false;
      out.append(" ** DatasetFilter (5): null value for matchPattern not valid when type is 'RegExp'.");
    }
    if ( this.type != DatasetFilter.Type.REGULAR_EXPRESSION &&
         this.type != null &&
         this.matchPattern != null)
    {
      isValid = false;
      out.append( " ** DatasetFilter (6): matchPattern value (" + this.matchPattern +
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
      // @todo Replace gnu.regexp with java.util.regexp???
      gnu.regexp.RE regExp = null;

      // Setup the regular expression.
      try
      {
        regExp = new gnu.regexp.RE( this.matchPattern);
      }
      catch( gnu.regexp.REException e)
      {
        System.err.println("Error: exception on reg exp");
        System.err.println( e.getMessage());
        e.printStackTrace();
        return( false); //System.exit( 1);
        // Or just do a
        // return( null);
      }

      gnu.regexp.REMatch regExpMatch = null;
      if ( this.getMatchPatternTarget().equals( "name"))
      {
        regExpMatch = regExp.getMatch( dataset.getName());
      }
      else if ( this.getMatchPatternTarget().equals( "urlPath"))
      {
        // @todo Should use isMatch() instead?
        regExpMatch = regExp.getMatch( ((InvDatasetImpl) dataset).getUrlPath());
      }
      else
      {
        // @todo deal with any matchPatternTarget (XPath-ish)
        return( false);
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
        return( regExpMatch != null );
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

  /**
   * Type-safe enumeration of the types of DatasetFilter.
   *
   * @author Ethan Davis (from John Caron's thredds.catalog.ServiceType)
   */
  public static class Type
  {
    private static java.util.HashMap hash = new java.util.HashMap(20);

    public final static Type REGULAR_EXPRESSION = new Type( "RegExp");

    private String name;
    private Type( String name)
    {
      this.name = name;
      hash.put( name, this);
    }

    /**
     * Find the Type that matches this name.
     * @param name
     * @return Type or null if no match.
     */
    public static Type getType( String name)
    {
      if ( name == null) return null;
      return (Type) hash.get( name);
    }

    /**
     * Return the string name.
     */
    public String toString()
    {
      return name;
    }

  }
}
