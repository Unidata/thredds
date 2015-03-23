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
package thredds.crawlabledataset;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import thredds.util.DodsURLExtractor;

/**
 * A description
 * 
 * @author Ethan Davis
 * @author Bas Retsios
 * @since Jun 8, 2005 15:34:04 -0600
 */
public class CrawlableDatasetDods implements CrawlableDataset {
	static private org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(CrawlableDatasetDods.class);

	private static DodsURLExtractor urlExtractor = null;
	
	private static Map listDatasetsMap = null; // maintain an in-memory copy for performance reasons .. TODO: add a version-check
	
	private String path;
	
	private URLConnection pathUrlConnection = null; // store this, for performance reasons

	private String name;

	private Object configObj = null;
	
	protected CrawlableDatasetDods() {
	}

	protected CrawlableDatasetDods(String path, Object configObj)
	{

		if (urlExtractor == null)
			urlExtractor = new DodsURLExtractor();
		
		if (listDatasetsMap == null) // for performance
			listDatasetsMap = new HashMap();

		if (configObj != null) {
			log.debug("CrawlableDatasetDods(): config object not null, it will be ignored <"
							+ configObj.toString() + ">.");
			this.configObj = configObj;
		}
		
		if (path.startsWith("http:")) {
			this.path = path;

			try {
				new URI(path); // check syntax .. URISyntaxException if its not good
				name = getName(path);
			} catch (URISyntaxException e) {
		        String tmpMsg = "Bad URI syntax for path <" + path + ">: " + e.getMessage();
		        log.debug( "CrawlableDatasetDods(): " + tmpMsg);
		        throw new IllegalArgumentException( tmpMsg);
			}

		    // Check if this accessPoint URL is an OPeNDAP server URL.
			// For now commented-out because it takes far too long when expanding a directory:
			// all links would be tested, because a CrawlableDataset is new-ed too fast (when its parent is expanded).
			/*
		    String apVersionString = path + (path.endsWith("/") ? "version" : "/version");
		    String apVersionResultContent = null;
		    try
		    {
		      apVersionResultContent = urlExtractor.getTextContent( apVersionString);
		    }
		    catch (java.io.IOException e)
		    {
		      String tmpMsg = "The accessPoint URL is not an OPeNDAP server URL (no version info) <" + apVersionString + ">";
		      log.error( "CrawlableDatasetDods(): " + tmpMsg, e);
		    }
		    if ( apVersionResultContent == null ||
		    	 (apVersionResultContent.indexOf( "DODS") == -1 &&
		         apVersionResultContent.indexOf( "OPeNDAP") == -1 &&
		         apVersionResultContent.indexOf( "DAP") == -1))
		    {
		      String tmpMsg = "The accessPoint URL version info is not valid <" + apVersionResultContent + ">";
		      log.error(  "CrawlableDatasetDods(): " + tmpMsg);
		    }
		    */
		} else {
			String tmpMsg = "Invalid url <" + path + ">.";
			log.debug("CrawlableDatasetDods(): " + tmpMsg);
			throw new IllegalArgumentException(tmpMsg);
		}
	}
	
	private CrawlableDatasetDods(CrawlableDatasetDods parent, String childPath)
	{
		String normalChildPath = childPath.startsWith("/")?childPath.substring(1):childPath;
		this.path = parent.getPath();
		this.path += this.path.endsWith("/") ? normalChildPath : "/" + normalChildPath;
		this.name = getName(path);
		this.configObj = null;
	}

	private String getName(String path) {
		// Attempt to return the last name in the path name sequence.
		if (!path.equals("/")) {
			String tmpName = path.endsWith("/")
                       ? path.substring(0, path.length() - 1) : path;
      int index = tmpName.lastIndexOf("/");
			if (index != -1)
				tmpName = tmpName.substring(index + 1);
			return tmpName;
		} else
			return path;
	}

  /**
   * Provide access to the java.net.URI that this CrawlableDataset represents.
   *
   * @return the java.net.URI that this CrawlableDataset represents.
   */
  public URI getUri()
  {
    try
    {
      return new URI( this.path);
    }
    catch ( URISyntaxException e )
    {
      return null;
    }
  }

  public Object getConfigObject() {
		return configObj;
	}

	public String getPath() {
		return (this.path);
	}

	public String getName() {
		return (this.name);
	}

	public boolean isCollection() {
		return isCollection(path);
	}

	public CrawlableDataset getDescendant( String relativePath )
	{
    if ( relativePath.startsWith( "/"))
      throw new IllegalArgumentException( "Path must be relative <" + relativePath + ">.");
    return new CrawlableDatasetDods(this, relativePath);
	}

	// how do we determine if a url is a collection?
	// we can't count on a trailing backslash, as this was removed by CrawlableDatasetFactory
	// for now, assume collection unless a known file extension is encountered
	private static String [] knownFileExtensions = {".hdf", ".xml", ".nc", ".bz2", ".cdp", ".jpg"};
	
	private static boolean isCollection(String path)
	{
		String testPath = path.toLowerCase(); // otherwise our matches may fail
		if (isDodsDataset(testPath))
			return false;
		else
		{
			int i = 0;
			while ((i < (knownFileExtensions.length)) && !testPath.endsWith(knownFileExtensions[i]))
					++i;
			return (i >= knownFileExtensions.length); // i < length means we deal with a known file ==> no collection 
		}
	}

	private static String [] dodsExtensions = {".html", ".htm", ".das", ".dds", ".info"};

	private static String getDodsExtension(String path)
	{
		String extension = "";
		String testPath = path.toLowerCase(); // otherwise our matches may fail

		int i = 0;
		while ((i < (dodsExtensions.length)) && !testPath.endsWith(dodsExtensions[i]))
			++i;
		if (i < dodsExtensions.length)
			extension = dodsExtensions[i];
		return extension;
	}

	private static boolean isDodsDataset(String path)
	{
		return getDodsExtension(path).length() > 0;
	}
	
	private static String removeDodsExtension(String path)
	{
		String dodsExtension = getDodsExtension(path);
		if (dodsExtension.length() > 0)
			path = path.substring(0, path.length() - dodsExtension.length());

		return path;
	}
	
	// This function shouldn't be here !!!
	// It is a workaround for many OPeNDAP servers that crop part of their urls (the /opendap-bin/nph-dods/ part)
	// e.g. of server with problem (2-Nov-2006): http://acdisc.sci.gsfc.nasa.gov/opendap-bin/nph-dods/OPENDAP/Giovanni/
	private String forceChild(String url)
	{
		String prefix = path;
		if (prefix.endsWith("/"))
			prefix = path.substring(0, path.length() - 1); // because the url also contains a '/' that we will use			
		int j = url.substring(0, url.length() - 1).lastIndexOf('/'); // url.length() - 1 was intentional .. if the last char is a '/', we're interested in the previous one.
		if (j >= 0)
		{
			String ret = prefix + url.substring(j);
			return ret;
		}
		else // relative paths .. leave intact
			return url;
	}

	public List listDatasets() throws IOException {

		if (!this.isCollection()) {
			String tmpMsg = "This dataset <" + this.getPath()
					+ "> is not a collection dataset.";
			log.error("listDatasets(): " + tmpMsg);
			throw new IllegalStateException(tmpMsg);
		}
		
		if (listDatasetsMap.containsKey(path)) // shortcut .. for performance
			return (List)listDatasetsMap.get(path);
		else
		{
			List list = new ArrayList();
			List pathList = new ArrayList(); // only for detecting duplicates (after removing the extension, sometimes we end up with duplicates)
	
			// Get list of possible datasets from current URL.
			List possibleDsList = null;
			try {
				String openPath = path;
				if (!openPath.endsWith("/")) // if you skip this, you will find that relative URLs don't work (fails in "extract", and in particular in URL u = new URL(baseURL, value))
					openPath += "/";
				possibleDsList = urlExtractor.extract(openPath);
			} catch (java.io.IOException e) {
				log.warn("listDatasets(): IOException while extracting dataset info from given OPeNDAP directory <"
								+ path + ">, return empty list: " + e.getMessage());
				return (list);
			}
	
			// Handle each link in the current access path.
			String curDsUrlString = null;
			for (Iterator it = possibleDsList.iterator(); it.hasNext(); ) {
				curDsUrlString = (String) it.next();
				// Perform some tests on curDsUrlString
				// Skip datasets that aren't OPeNDAP datasets (".html") or
				// collection datasets ("/").
				if ((!isDodsDataset(curDsUrlString)) && (!isCollection(curDsUrlString))) {
					log.warn("expandThisLevel(): Dataset isn't an OPeNDAP dataset or collection dataset, skip <"
									+ path + ">.");
					continue;
				}
	
				curDsUrlString = removeDodsExtension(curDsUrlString);

        // This function goes a bit too far trying to recover from servers that drop part of URL path.
        // However, it also converts URLs that point to external servers to be subdirectories of this CrDS.
        //curDsUrlString = forceChild(curDsUrlString);

        // Skip any URLs that aren't children of this CrDs
        if ( !curDsUrlString.startsWith( path ) )
        {
          log.debug( "listDatasets(): skipping URL <" + curDsUrlString + ">, not child of this CrDs <" + path + ">." );
          continue;
        }

        if (pathList.contains(curDsUrlString))
					continue; // duplicate
				else
					pathList.add(curDsUrlString);
				
	
				// Avoid links back down the path hierarchy (i.e., parent directory links).
				// Comment: this call was taken over from CrawlableDatasetFile. Since we use forceChild, this call is currently useless.
				if (!curDsUrlString.startsWith(path)) {
					log.debug("listDatasets(): current path <" + curDsUrlString
							+ "> not child of given" + " location <" + path
							+ ">, skip.");
					continue;
				}
	
				try {
					new URI(curDsUrlString); // syntax check
				} catch (URISyntaxException e) {
					log.error("listDatasets(): Skipping dataset  <"
							+ curDsUrlString + "> due to URISyntaxException: "
							+ e.getMessage());
					continue;
				}
	
				log.debug("listDatasets(): handle dataset (" + curDsUrlString
								+ ")");
	
				// So far so good .. curDsUrlString passed all tests, thus add it to the list
        try {
          list.add(CrawlableDatasetFactory.createCrawlableDataset(
							curDsUrlString, this.getClass().getName(), null));
				} catch (ClassNotFoundException e) {
					log.warn("listDatasets(): Can't make CrawlableDataset for child url <"
									+ curDsUrlString + ">: " + e.getMessage());
				} catch (NoSuchMethodException e) {
					log.warn("listDatasets(): Can't make CrawlableDataset for child url <"
									+ curDsUrlString + ">: " + e.getMessage());
				} catch (IllegalAccessException e) {
					log.warn("listDatasets(): Can't make CrawlableDataset for child url <"
									+ curDsUrlString + ">: " + e.getMessage());
				} catch (InvocationTargetException e) {
					log.warn("listDatasets(): Can't make CrawlableDataset for child url <"
									+ curDsUrlString + ">: " + e.getMessage());
				} catch (InstantiationException e) {
					log.warn("listDatasets(): Can't make CrawlableDataset for child url <"
									+ curDsUrlString + ">: " + e.getMessage());
				}
			}
			
			listDatasetsMap.put(path, list); // remember it next time, for performance
	
			return list;
		}
	}

	public List listDatasets(CrawlableDatasetFilter filter) throws IOException {
		List list = this.listDatasets();
		if (filter == null)
			return list;
		List retList = new ArrayList();
		for (Iterator it = list.iterator(); it.hasNext();) {
			CrawlableDataset curDs = (CrawlableDataset) it.next();
			if (filter.accept(curDs)) {
				retList.add(curDs);
			}
		}
		return (retList);
	}

	public CrawlableDataset getParentDataset() {
		if (!path.equals("/")) {
      String parentPath = path;
      int index = parentPath.lastIndexOf( "/", parentPath.endsWith( "/") ? parentPath.length() - 2 : parentPath.length() - 1 );
      if ( index != -1 )
        parentPath = parentPath.substring( 0, index + 1 );
      
			return new CrawlableDatasetDods( parentPath, null);
		} else
			return null;
	}

  public boolean exists() {
	if (pathUrlConnection == null)
	try {
		URL u = new URL(path);
		pathUrlConnection = u.openConnection();
	} catch (MalformedURLException e) {
	} catch (IOException e) {
	}
  if ( pathUrlConnection != null )
  try {
		int responseCode = ((HttpURLConnection)pathUrlConnection).getResponseCode();
		if (responseCode >= 200 && responseCode < 300) // Successful
			return true;
	} catch (IOException e) {
	}
	return false;
  }

  public long length() {
    if (this.isCollection())
      return (0);
    if (pathUrlConnection == null)
    {
    	try {
	        URL u = new URL(path);
	        pathUrlConnection = u.openConnection();
	    } catch (MalformedURLException e) {
	    } catch (IOException e) {
	    }
    }
    if (pathUrlConnection != null)
    	return pathUrlConnection.getContentLength();
    else
    	return (-1);
  }

	public Date lastModified() {
		if (pathUrlConnection == null)
		{
			try {
				URL u = new URL(path);
				pathUrlConnection = u.openConnection();
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
		}

		if (pathUrlConnection != null)
		{
			long lastModified = pathUrlConnection.getLastModified();
			if (lastModified != 0)
			{
				Calendar cal = Calendar.getInstance();
				cal.clear();
				cal.setTimeInMillis(lastModified);
				return (cal.getTime());
			}
			else
				return null;
		}
		else
			return null;
	}

  public String toString()
  {
    return this.path;
  }
}
