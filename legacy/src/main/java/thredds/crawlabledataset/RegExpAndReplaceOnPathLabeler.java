/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
// $Id: RegExpAndReplaceOnPathLabeler.java 63 2006-07-12 21:50:51Z edavis $
package thredds.crawlabledataset;

/**
 * _more_
 *
 * @author edavis
 * @since Dec 28, 2005 9:31:33 PM
 */
public class RegExpAndReplaceOnPathLabeler implements CrawlableDatasetLabeler
{


  private String regExp;
  private java.util.regex.Pattern pattern;
  private String replaceString;

  public RegExpAndReplaceOnPathLabeler( String regExp, String replaceString )
  {
    this.regExp = regExp;
    this.pattern = java.util.regex.Pattern.compile( regExp );
    this.replaceString = replaceString;
  }

  public Object getConfigObject() { return null; }

  public String getRegExp() { return regExp; }
  public String getReplaceString() { return replaceString; }

  public String getLabel( CrawlableDataset dataset )
  {
    java.util.regex.Matcher matcher = this.pattern.matcher( dataset.getPath() );
    if ( ! matcher.find() ) return null;

    StringBuffer startTime = new StringBuffer();
    matcher.appendReplacement( startTime, this.replaceString );
    startTime.delete( 0, matcher.start() );

    if ( startTime.length() == 0 ) return null;

    return startTime.toString();
  }
}
