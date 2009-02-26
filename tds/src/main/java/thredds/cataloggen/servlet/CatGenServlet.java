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
package thredds.cataloggen.servlet;

import thredds.servlet.ServletUtil;
import thredds.servlet.AbstractServlet;
import thredds.servlet.DataRootHandler;
import thredds.servlet.UsageLog;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The Catalog Generation servlet provides a web interface for scheduling
 * catalog generation tasks.
 *
 * In the TDS, this is mapped to "/thredds/cataloggen/*" and access to these
 * URLs requires user authentication.
 *
 * @author Ethan Davis
 */
public class CatGenServlet extends AbstractServlet
{
  /* Requests that will be handled (for the URL, prepend path with
   * http://<myserver>/thredds/cataloggen):
   *
   *  GET, PUT,
   *   OR POST       Path          Location
   *  ---------      ----          --------
   *    GET        /doc/README    rootPath/cataloggen/README
   *    GET        /doc/NEWS      rootPath/cataloggen/NEWS
   *    GET        /doc/*.html    rootPath/cataloggen/docs/*.html
   *    GET        ""             redirect to "/"
   *    GET        /              Create HTML doc on fly that points to /admin/
   *    GET        /admin/        Create HTML form that represents config.xml
   *    GET        /admin/addTask
   *                              Create HTML form for entering task info
   *    GET        /admin/editTask-<taskId>.html
   *                              Create HTML form for editing an existing task
   *    GET        /admin/deleteTask-<taskId>.html
   *                              Create HTML form for deleteing an existing task
   *    GET/PUT    /config/config.xml
   *                              contentPath/cataloggen/config/config.xml
   *    GET/PUT    /config/configCatGenExample.xml
   *                              contentPath/cataloggen/config/configCatGenExample.xml
   *    GET        /catalogs/<xmlFileName>
   *                              contentPath/cataloggen/catalogs/<xmlFileName>
   *    POST       /admin/doAddTask
   *                              Action performed when /admin/addTask form is submitted
   *    POST       /admin/doEditTask-<taskId>
   *                              Action performed when /admin/editTask-<>.html form is submitted
   *    POST       /admin/doDeleteTask-<taskId>
   *                              Action performed when /admin/deleteTask-<>.html form is submitted
   */
  private String configFileName;
  private File catGenContentPath, catGenConfigPath, catGenResultPath;
  private File catGenStaticContentPath;

  private String servletName = "cataloggen";
  private String catGenDocDirName = "doc";
  private String catGenConfigDirName = "config";
  private String catGenResultCatalogsDirName = "catalogs";


  private String adminPath = "/admin";

  private String adminAddTaskPath = "/admin/addTask";
  private String adminEditTaskPath = "/admin/editTask-";
  private String adminDeleteTaskPath = "/admin/deleteTask-";

  private String adminDoAddTaskPath = "/admin/doAddTask";
  private String adminDoEditTaskPath = "/admin/doEditTask-";
  private String adminDoDeleteTaskPath = "/admin/doDeleteTask-";


  private CatGenServletConfig mainConfig = null;

  protected String getPath() { return( servletName + "/");}
  protected void makeDebugActions() { }

  public void init() throws ServletException
  {
    super.init();

    // Get various paths and file names.
    this.catGenStaticContentPath = new File( ServletUtil.getRootPath(), this.servletName ); // cataloggen
    // this.catGenDocPath = new File( this.catGenStaticContentPath, this.catGenDocDirName ); // cataloggen/doc

    this.catGenContentPath = new File( this.contentPath );
    this.catGenConfigPath = new File( this.catGenContentPath, this.catGenConfigDirName ); // cataloggen/config
    this.catGenResultPath = new File( this.catGenContentPath, this.catGenResultCatalogsDirName ); // cataloggen/catalogs

    this.configFileName = this.getInitParameter( "configFile");

    // Some debug info.
    log.debug( "init(): CatGen static content path = " + this.catGenStaticContentPath.toString() );
    log.debug( "init(): CatGen content path = " + this.catGenContentPath.toString() );
    log.debug( "init(): CatGen config path = " + this.catGenConfigPath.toString() );
    log.debug( "init(): CatGenServlet config file = " + this.configFileName );
    log.debug( "init(): CatGen result path = " + this.catGenResultPath.toString() );

    // Setup the configuration information.
    try
    {
      this.mainConfig = new CatGenServletConfig( this.catGenResultPath, this.catGenConfigPath, this.configFileName);
    }
    catch ( IOException e )
    {
      String tmpMsg = "Reading config file failed";
      log.error( "init(): " + tmpMsg, e );
      throw new ServletException( tmpMsg + ": " + e.getMessage(), e );
    }
  }

  public void destroy()
  {
    log.debug( "destroy()");
    //this.mainConfig.writeConfig();
    this.mainConfig.cancelTimer();
    super.destroy();
  }

  /**
   * Handle all GET requests, includes requests for: documentation files,
   * configuration files, the resulting catalogs, and the HTML admin pages.
   *
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @throws ServletException if the request could not be handled for some reason.
   * @throws IOException if an I/O error is detected (when communicating with client not for servlet internal IO problems?).
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    log.info( UsageLog.setupRequestContext( req ) );

    String path = req.getPathInfo();
    log.info( "doGet(): path = " + path );

    PrintWriter out = null;

    // Redirect root request to the HTML admin interface.
    if ( path == null )
    {
      res.sendRedirect( res.encodeRedirectURL( req.getContextPath() + req.getServletPath() + "/" ) );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_MOVED_PERMANENTLY, 0 ));
      return;
    }
    else if ( path.equals( "/" ) )
    {
      out = res.getWriter();
      res.setContentType( "text/html" );
      res.setStatus( HttpServletResponse.SC_OK );
      String resString = this.getHtmlRootRequest( req);
      out.print( resString );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resString.length() ));
      return;
    }
    // Handle requests for documentation files, configuration files, or results catalogs.
    else if ( path.startsWith( "/" + this.catGenDocDirName + "/" ) ||
              path.startsWith( "/" + this.catGenConfigDirName + "/" ) ||
              path.startsWith( "/" + this.catGenResultCatalogsDirName + "/" ) )
    {

      // see if its a static catalog
      if (DataRootHandler.getInstance().processReqForCatalog(req, res))
        return;

      this.doGetFiles( path, req, res );
      return;
    }
    // Handle all HTML interface requests.
    else if ( path.startsWith( this.adminPath ) )
    {
      this.doGetHtmlUI( path, req, res );
      return;
    }
    else
    {
      String tmpMsg = "Request not understood(" + path + ").";
      log.debug( "doGet(): " + tmpMsg );
      out = res.getWriter();
      res.setContentType( "text/html" );
      res.setStatus( HttpServletResponse.SC_OK );
      String responseString = this.getHtmlReturnMessage( req, tmpMsg);
      out.print( responseString );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, responseString.length() ));
      return;
    }
  }

  /**
   * Handle PUT requests, only PUT requests for the CatGenServlet config
   * files are allowed (authentication required).
   *
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse
   * @throws ServletException if the request could not be handled for some reason.
   * @throws IOException if an I/O error is detected (when communicating with client not for servlet internal IO problems?).
   */
  public void doPut(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException

  {
    log.info( UsageLog.setupRequestContext( req ) );

    File tmpFile = null;
    String tmpMsg = null;

    String path = req.getPathInfo();

    if (path != null)
    {
      // Handle all requests to PUT config files.
      if ( path.startsWith( "/" + this.catGenConfigDirName + "/" ))
      {
        tmpFile = new File( this.catGenContentPath, path );

        log.debug( "doPut(): putting CatGenConfig file - " + path);

        // Handle PUT of main config file.
        if ( path.equals( "/" + this.catGenConfigDirName + "/" + this.configFileName))
        {
          if ( ServletUtil.saveFile( this, this.catGenContentPath.getAbsolutePath(),
                                     path, req, res))
          {
            // @todo Check that new config file is valid before write.
            // Create a new servlet config with the newly PUT config file.
            this.mainConfig.cancelTimer();
            try
            {
              this.mainConfig = new CatGenServletConfig( this.catGenResultPath, this.catGenConfigPath, this.configFileName );
              res.setStatus( HttpServletResponse.SC_OK);
              log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, 0 ));
              return;
            }
            catch ( IOException e )
            {
              // @todo If old version exists, reinstate and backup new file.
              tmpMsg = "Reading config file failed";
              String tmpMsg2 = tmpMsg + ": " + e.getMessage();
              log.error( "doPut(): " + tmpMsg, e );
              res.sendError( HttpServletResponse.SC_ACCEPTED, tmpMsg2 );
              log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_ACCEPTED, tmpMsg2.length() ));
              return;
            }
          }
          else
          {
            tmpMsg = "Failed to save file <" + path + ">";
            log.error( "doPut(): " + tmpMsg );
            res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
            log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg.length() ));
            return;
          }
        }
        // Handle PUT of all other config files (CatGenConfig files).
        else
        {
          boolean creatingNewFile = true;
          if ( tmpFile.exists() ) creatingNewFile = false;
          if ( ServletUtil.saveFile( this, this.catGenContentPath.getAbsolutePath(),
                                     path, req, res))
          {
            log.debug( "doPut(): file saved <" + path + ">.");

            // Tell the servlet config that a CatGen config doc was written.
            this.mainConfig.notifyNewConfigDoc( tmpFile.getName() );
            if ( creatingNewFile )
            {
              res.setStatus( HttpServletResponse.SC_CREATED );
              log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_CREATED, 0 ));
              return;
            }
            else
            {
              res.setStatus( HttpServletResponse.SC_OK);
              log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, 0 ));
              return;
            }
          }
          else
          {
            tmpMsg = "Failed to save file <" + path + ">";
            log.error( "doPut(): " + tmpMsg);
            res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg );
            log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, tmpMsg.length() ));
            return;
          }
        }
      }
      else
      {
        tmpMsg = "Cannot PUT a document outside the " + this.catGenConfigDirName + "directory";
        log.warn( "doPut(): " + tmpMsg);
        res.sendError( HttpServletResponse.SC_NOT_FOUND, tmpMsg );
        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, tmpMsg.length() ));
        return;
      }
    }
    else
    {
      tmpMsg = "Cannot PUT a document here (empty request path)";
      log.warn( "doPut(): " + tmpMsg);
      res.sendError( HttpServletResponse.SC_NOT_FOUND, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, tmpMsg.length() ));
      return;
    }
  }

  public void doPost(HttpServletRequest req, HttpServletResponse res)
    throws ServletException, IOException
  {
    log.info( UsageLog.setupRequestContext( req ) );

    String path = req.getPathInfo();
    PrintWriter out = null;

    if (path != null)
    {
      // Create a new task with the information provided.
      if ( path.equals( this.adminDoAddTaskPath))
      {
        log.debug( "doPost(): adding Task.");

        // Create new task with given values.
        CatGenTimerTask newTask = new CatGenTimerTask( req.getParameter( "taskName"),
                                                       req.getParameter( "fileName"),
                                                       req.getParameter( "resultFileName"),
                                                       Integer.parseInt( req.getParameter( "period")),
                                                       Integer.parseInt( req.getParameter( "delay")));
        newTask.init( this.catGenResultPath, this.catGenConfigPath);

        // Check validity of the new task.
        StringBuilder messages = new StringBuilder();
        if ( newTask.isValid( messages))
        {
          log.debug( "doPost():     task is valid - " + messages.toString());
          // Add task to servlet config.
          if ( this.mainConfig.addTask( newTask))
          {
            log.debug( "doPost():     task added.");
            out = res.getWriter();
            res.setContentType( "text/html" );
            String resMsg = this.getHtmlAddTaskResultSuccess( req, newTask, messages );
            res.setStatus( HttpServletResponse.SC_OK );
            out.print( resMsg );
            log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
            return;
          }
          else
          {
            log.debug( "doPost():     duplicate task.");
            out = res.getWriter();
            res.setContentType( "text/html" );
            String resMsg = this.getHtmlAddTaskResultDuplicate( req, newTask, messages );
            res.setStatus( HttpServletResponse.SC_OK );
            out.print( resMsg );
            log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
            return;
          }
        }
        else
        {
          log.debug( "doPost():     task is invalid - " + messages.toString());
          // Set period to zero and add task back into config?
          out = res.getWriter();
          res.setContentType( "text/html" );
          String resMsg = this.getHtmlAddTaskResultInvalid( req, newTask, messages );
          res.setStatus( HttpServletResponse.SC_OK );
          out.print( resMsg );
          log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
          return;
        }

//        log.debug( "CatGenServlet.doPost(): done adding Task - " + fileName);
      }
      // Edit the specified task with the information provided.
      else if ( path.startsWith( this.adminDoEditTaskPath))
      {
        // Determine the task to be edited.
        String taskIdConfigDocName = path.substring(
                                         path.indexOf( this.adminDoEditTaskPath ) +
                                         this.adminDoEditTaskPath.length());
        log.debug( "doPost(): editing Task - " +
                            taskIdConfigDocName);

        CatGenTimerTask oldTask =
          mainConfig.findTaskByConfigDocName( taskIdConfigDocName);

        // Remove task from servlet config while editing task.
        this.mainConfig.removeTask( oldTask);

        // Create new task with given values.
        CatGenTimerTask newTask = new CatGenTimerTask( req.getParameter( "taskName"),
                                                       req.getParameter( "fileName"),
                                                       req.getParameter( "resultFileName"),
                                                       Integer.parseInt( req.getParameter( "period")),
                                                       Integer.parseInt( req.getParameter( "delay")));
        newTask.init( this.catGenResultPath, this.catGenConfigPath);

        // Check validity of the new task.
        StringBuilder messages = new StringBuilder();
        if ( newTask.isValid( messages))
        {
          // Add task to servlet config.
          if ( this.mainConfig.addTask( newTask))
          {
            log.debug( "doPost(): task added (" + newTask.getName() + ").");
            out = res.getWriter();
            res.setContentType( "text/html" );
            String resMsg = this.getHtmlEditTaskResultSuccess( req, oldTask, newTask, messages );
            res.setStatus( HttpServletResponse.SC_OK );
            out.print( resMsg );
            log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
            return;
          }
          else
          {
            log.debug( "doPost(): no task added, duplicate (" + newTask.getName() + ").");
            out = res.getWriter();
            res.setContentType( "text/html" );
            String resMsg = this.getHtmlEditTaskResultDuplicate( req, oldTask, newTask, messages );
            res.setStatus( HttpServletResponse.SC_OK );
            out.print( resMsg );
            log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
            return;
          }
        }
        else
        {
          // @todo Set period to zero and add task back into config?
          log.debug( "doPost(): no task added, invalid (" + newTask.getName() + ").");
          out = res.getWriter();
          res.setContentType( "text/html" );
          String resMsg = this.getHtmlEditTaskResultInvalid( req, oldTask, newTask, messages );
          res.setStatus( HttpServletResponse.SC_OK );
          out.print( resMsg );
          log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
          return;
        }
      }
      // Delete the specified task.
      else if ( path.startsWith( this.adminDoDeleteTaskPath))
      {
        String fileName = path.substring( path.indexOf( this.adminDoDeleteTaskPath ) +
                                          this.adminDoDeleteTaskPath.length());
        log.debug( "doPost(): deleteing Task - " + fileName);

        CatGenTimerTask oldTask =
          mainConfig.findTaskByConfigDocName( fileName);
        if ( oldTask == null)
        {
          log.debug( "doPost(): task to delete not in list (" + fileName + ").");
          out = res.getWriter();
          res.setContentType( "text/html" );
          String resMsg = this.getHtmlReturnMessage( req, "Task to delete not in list (" + fileName + ")" );
          res.setStatus( HttpServletResponse.SC_OK );
          out.print( resMsg );
          log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
          return;
        }

        // Remove task from servlet config while editing task.
        if ( this.mainConfig.removeTask( oldTask))
        {
          log.debug( "doPost(): task deleted (" + oldTask.getName() + ").");
          out = res.getWriter();
          res.setContentType( "text/html" );
          String resMsg = this.getHtmlDeleteTaskResultSuccess( req, oldTask );
          res.setStatus( HttpServletResponse.SC_OK );
          out.print( resMsg );
          log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
          return;
        }
        else
        {
          log.debug( "doPost(): failed to delete task (" + oldTask.getName() + ").");
          out = res.getWriter();
          res.setContentType( "text/html" );
          String resMsg = this.getHtmlDeleteTaskResultFail( req, oldTask );
          res.setStatus( HttpServletResponse.SC_OK );
          out.print( resMsg );
          log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
          return;
        }

        //log.debug( "CatGenServlet.doPost(): deleteing task");
        //res.setStatus( HttpServletResponse.SC_OK);
      }
    }
    log.debug( "doPost(): no path given for POST.");
    out = res.getWriter();
    res.setContentType( "text/html" );
    String resMsg = this.getHtmlReturnMessage( req, "No path given for POST action." );
    res.setStatus( HttpServletResponse.SC_OK );
    out.print( resMsg );
    log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, resMsg.length() ));
    return;
  }

//  public void doQuery(HttpServletRequest req, HttpServletResponse res)
//    throws ServletException, IOException
//  {
//  }
//
//  public void doDebug(HttpServletRequest req, HttpServletResponse res)
//    throws ServletException, IOException
//  {
//  }

  /**
   * Handle any requests to GET a documentation file, a configuration file,
   * or a results catalog.
   *
   * @param path - the requested path
   * @param req
   * @param res  - the servlet response
   * @throws IllegalArgumentException when path is not into the correct directory.
   * @throws RuntimeException         if the file is not found or cannot be returned for some other reason.
   * @throws IOException if HttpServletResponse.sendError() encounters problems.
   */
  private void doGetFiles( String path, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    String tmpMsg = null;
    File tmpFile = null;
    String fileType = null;

    // Documentation files are in the static content directory.
    if ( path.startsWith( "/" + this.catGenDocDirName + "/" ) )
    {
      tmpFile = new File( this.catGenStaticContentPath, path );
    }
    // Configuration files are in the regular content directory.
    else if ( path.startsWith( "/" + this.catGenConfigDirName + "/" ) )
    {
      tmpFile = new File( this.catGenContentPath, path );
    }
    // Resulting catalog files are in the regular content directory.
    else if ( path.startsWith( "/" + this.catGenResultCatalogsDirName + "/" ) )
    {
      tmpFile = new File( this.catGenContentPath, path );
    }
    // Request isn't for documentation, configuration, or catalog files.
    else
    {
      tmpMsg = "Illegal request <" + path + "> (doGet() should not have passed in the request).";
      log.debug( "doGetFiles(): " + tmpMsg );
      res.sendError( HttpServletResponse.SC_BAD_REQUEST , tmpMsg);
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_BAD_REQUEST, tmpMsg.length() ));
      return;
    }

    // Find desired file.
    if ( tmpFile.exists() && tmpFile.isFile() )
    {
      // Determine type of file (if null, determined by returnFile()).
      /* @todo Let returnFile() determine type for all files.
      if ( path.endsWith( ".xml" ) )
      {
        fileType = "text/xml";
      }
      else if ( path.endsWith( ".html" ) )
      {
        fileType = "text/html";
      } */

      // Return the file.
      log.debug( "doGetFiles(): sending file <" + tmpFile.getName() + "> in directory <" + tmpFile.getParent() + "> to returnFile()");
      ServletUtil.returnFile( this, tmpFile.getParent(), tmpFile.getName(), req, res, fileType );
      return;
    }
    // File does not exist (or is a directory).
    else
    {
      tmpMsg = "doGetFiles(): Requested file does not exist or is a directory <" + path + ">";
      log.debug( tmpMsg );
      res.sendError( HttpServletResponse.SC_NOT_FOUND, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, tmpMsg.length() ));
      // @todo Consider instead creating empty file and returning?
      return;
    }
  }

  private void doGetHtmlUI( String path, HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    PrintWriter out = null;
    // Return the main admin page, listing all current tasks.
    if ( path.equals( this.adminPath + "/" ) )
    {
      log.debug( "doGetHtmlUI(): listing all tasks" );

      out = res.getWriter();
      res.setContentType( "text/html" );
      String responseString = this.getHtmlListTasks( req, this.mainConfig );
      res.setStatus( HttpServletResponse.SC_OK );
      out.print( responseString );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, responseString.length() ));
      return;
    }
    // Redirect to main admin page (above).
    else if ( path.equals( this.adminPath ) )
    {
      res.sendRedirect( res.encodeRedirectURL( req.getContextPath() + req.getServletPath() +
                                               this.adminPath + "/" ) );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_MOVED_PERMANENTLY, 0 ));
    }
    // Return form for editing specified task.
    else if ( path.startsWith( this.adminEditTaskPath ) )
    {
      String fileName = path.substring( path.indexOf( this.adminEditTaskPath ) +
                                        this.adminEditTaskPath.length() );
      log.debug( "doGetHtmlUI(): edit task - " + fileName );
      CatGenTimerTask task = this.mainConfig.findTaskByConfigDocName( fileName );

      out = res.getWriter();
      res.setContentType( "text/html" );
      String responseString = this.getHtmlEditTask( req, task );
      res.setStatus( HttpServletResponse.SC_OK );
      out.print( responseString );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, responseString.length() ));
      return;
    }
    // Return form for adding a task.
    else if ( path.equals( this.adminAddTaskPath ) )
    {
      log.debug( "doGetHtmlUI(): add task" );

      out = res.getWriter();
      res.setContentType( "text/html" );
      String responseString = this.getHtmlAddTask( req );
      res.setStatus( HttpServletResponse.SC_OK );
      out.print( responseString );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, responseString.length() ));
      return;
    }
    // Return form for deleting the specified task.
    else if ( path.startsWith( this.adminDeleteTaskPath ) )
    {
      String fileName = path.substring( path.indexOf( this.adminDeleteTaskPath ) +
                                        this.adminDeleteTaskPath.length() );
      log.debug( "doGetHtmlUI(): delete task - " + fileName );
      CatGenTimerTask task = this.mainConfig.findTaskByConfigDocName( fileName );
      log.debug( "doGetHtmlUI(): return form for editing task - \""
                    + task.getName() + "\"." );

      out = res.getWriter();
      res.setContentType( "text/html" );
      String responseString = this.getHtmlDeleteTask( req, task );
      res.setStatus( HttpServletResponse.SC_OK );
      out.print( responseString );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, responseString.length() ));
      return;
    }
    // Otherwise, not a valid request.
    else
    {
      String tmpMsg = "Request <" + path + "> not understood";
      log.error( "doGetHtmlUI(): " + tmpMsg );
      res.sendError( HttpServletResponse.SC_NOT_FOUND, tmpMsg );
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, tmpMsg.length() ));
      return;

    }

  }

  public String getHtmlDoctypeAndOpenTag()
  {
    return new StringBuilder()
            .append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" )
            .append( "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" )
            .append( "<html>\n" )
            .toString();
  }

  public String getXHtmlDoctypeAndOpenTag()
  {
    return new StringBuilder()
            // .append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            .append( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n" )
            .append( "        \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" )
            .append( "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">" )
            .toString();
  }

  /**
     * HTML response when request path equals "/".
     *
     * @param req - the incoming servlet request
     */
    private String getHtmlRootRequest( HttpServletRequest req)
    {
      log.debug( "htmlRootRequest(): start" );
      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet</h1>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "<a href=\"" ).append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath ).append( "/" + "\">List Current Tasks</a>\n" );
      retValue.append( "</p>\n" );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return ( retValue.toString() );
    }

    /**
     * Build an HTML document that contains the given message.
     *
     * @param req
     * @param message
     */
    private String getHtmlReturnMessage( HttpServletRequest req, String message )
    {
      log.debug( "getHtmlReturnMessage(): start (" + message + ")" );
      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet</h1>\n" );

      retValue.append( "<p>\n" );
      retValue.append( message );
      retValue.append( "</p>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "<a href=\"" ).append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath ).append( "/" + "\">List Current Tasks</a>\n" );
      retValue.append( "</p>\n" );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build response listing all tasks in given CatGenServletConfig.
     */
    private String getHtmlListTasks( HttpServletRequest req, CatGenServletConfig config )
    {
      log.debug( "htmlListTasks(): starting" );

      StringBuilder retVal = new StringBuilder();

      retVal.append( getHtmlDoctypeAndOpenTag() );
      retVal.append( "<head><title>Catalog Generator Servlet Config</title></head>\n" );
      retVal.append( "<body>\n" );
      retVal.append( "<h1>Catalog Generator Servlet Config</h1>\n" );
      retVal.append( "<hr>\n" );

      retVal.append( "<h2>Currently Scheduled Tasks</h2>\n" );
      retVal.append( "<table border=\"1\">\n" );
      retVal.append( "<tr>\n" );
      retVal.append( "<th> Task Name</th>\n" );
      retVal.append( "<th> Configuration Document</th>\n" );
      retVal.append( "<th>\n" );
      retVal.append( "Resulting Catalog\n" );
      retVal.append( "</th>\n" );
      retVal.append( "<th> Period (minutes)</th>\n" );
      retVal.append( "<th> Initial Delay (minutes)</th>\n" );
      retVal.append( "<th> Edit/Delete Task</th>\n" );
      retVal.append( "</tr>\n" );

      CatGenTimerTask curTask = null;
      java.util.Iterator iter = config.getUnmodTaskIterator();
      while ( iter.hasNext() )
      {
        curTask = (CatGenTimerTask) iter.next();
        retVal.append( "<tr>\n" );
        retVal.append( "<td>" ).append( curTask.getName() ).append( "</td>\n" );
        retVal.append( "<td> <a href=\"" ).append( req.getContextPath() ).append( req.getServletPath() ).append( "/" ).append( this.catGenConfigDirName ).append( "/" ).append( curTask.getConfigDocName() ).append( "\">" ).append( curTask.getConfigDocName() ).append( "</a></td>\n" );
        retVal.append( "<td> <a href=\"" ).append( req.getContextPath() ).append( req.getServletPath() ).append( "/" ).append( this.catGenResultCatalogsDirName ).append( "/" ).append( curTask.getResultFileName() ).append( "\">" ).append( curTask.getResultFileName() ).append( "</a></td>\n" );
        retVal.append( "</td>\n" );
        retVal.append( "<td>" ).append( curTask.getPeriodInMinutes() ).append( "</td>\n" );
        retVal.append( "<td>" ).append( curTask.getDelayInMinutes() ).append( "</td>\n" );
        retVal.append( "<td>\n" );
        retVal.append( "[<a href=\"" ).append( req.getContextPath() ).append( req.getServletPath() ).append( this.adminEditTaskPath ).append( curTask.getConfigDocName() ).append( "\">Edit</a>]" + "[<a href=\"" ).append( req.getContextPath() ).append( req.getServletPath() ).append( this.adminDeleteTaskPath ).append( curTask.getConfigDocName() ).append( "\">Delete</a>]\n" );
        retVal.append( "</td>\n" );
        retVal.append( "</tr>\n" );
      }
      retVal.append( "</table>\n" );
      retVal.append( "<a href=\"" ).append( req.getContextPath() ).append( req.getServletPath() ).append( this.adminAddTaskPath ).append( "\">Add a new task</a>\n" );
      retVal.append( "<hr>\n" );
      retVal.append( "<p>Note: If period is zero (0), the task will not be scheduled to run.</p>\n" );
      retVal.append( "<hr>\n" );
      retVal.append( "</body>\n" );
      retVal.append( "</html>" );

      return( retVal.toString());
    }

    /**
     * Build response for editing the given CatGenTimerTask.
     */
    private String getHtmlEditTask( HttpServletRequest req, CatGenTimerTask task )
    {
      log.debug( "htmlEditTask(): start" );

      StringBuilder retValue = new StringBuilder( );

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Edit Task</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Edit Task</h1>\n" );
      retValue.append( "<hr>\n" );

      retValue.append( this.getHtmlEditForm( req, task ) );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build response for adding a new CatGenTimerTask.
     */
    private String getHtmlAddTask( HttpServletRequest req )
    {
      log.debug( "htmlAddTask(): start" );

      StringBuilder retValue = new StringBuilder( );

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Add Task</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Add Task</h1>\n" );
      retValue.append( "<hr>\n" );

      retValue.append( this.getHtmlAddForm( req, null ) );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build response for deleting an existing CatGenTimerTask.
     */
    private String getHtmlDeleteTask( HttpServletRequest req, CatGenTimerTask task )
    {
      log.debug( "getHtmlDeleteTask(): start" );

      StringBuilder retValue = new StringBuilder( );

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Delete Task</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Delete Task</h1>\n" );
      retValue.append( "<hr>\n" );

      retValue.append( "<form method=\"POST\" action=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminDoDeleteTaskPath ).append( task.getConfigDocName() )
              .append( "\">\n" );

      retValue.append( "<h2>Task to Delete</h2>\n" );

      retValue.append( this.getHtmlListTable( task ) );

      retValue.append( "<p>\n" );
      retValue.append( "To delete this task, click on the \"Submit\" button.\n" );
      retValue.append( "To stop this task but not delete it, <a href=\"" )
              .append( req.getContextPath()).append( req.getServletPath() )
              .append( this.adminEditTaskPath).append( task.getConfigDocName() )
              .append( "\">edit the task</a> and set \n" );
      retValue.append( "the value of \"Period\" to zero.\n" );
      retValue.append( "</p>\n" );

      retValue.append( "<input type=\"submit\" value=\"Submit\">\n" );
      retValue.append( "</form>\n" );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build HTML response to deal with a duplicate task resulting
     * from editing a CatGenTimerTask.
     */
    private String getHtmlEditTaskResultDuplicate( HttpServletRequest req,
                                                   CatGenTimerTask oldTask,
                                                   CatGenTimerTask newTask,
                                                   StringBuilder messages )
    {
      log.debug( "htmlEditTaskResultDuplicate(): start" );

      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Task Edit Results</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Task Edit Results</h1>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "Resulting task is duplicate (in name and/or config doc name)\n" );
      retValue.append( "of a different existing task. Change the task below and\n" );
      retValue.append( "try adding again, or go back to the\n" );
      retValue.append( "<a href=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath ).append( "/" )
              .append( "\">list of tasks (minus this task)</a>.\n" );
      retValue.append( "</p>\n" );

      retValue.append( this.getHtmlAddForm( req, newTask ) );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build an HTML response to deal with an invalid task resulting
     * from editing a CatGenTimerTask.
     */
    private String getHtmlEditTaskResultInvalid( HttpServletRequest req,
                                                 CatGenTimerTask oldTask,
                                                 CatGenTimerTask newTask,
                                                 StringBuilder messages )
    {
      log.debug( "htmlEditTaskResultInvalid(): start" );

      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Task Edit Results</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Task Edit Results</h1>\n" );

      retValue.append( "<p>Resulting task is invalid.</p>\n" );
      retValue.append( "<p>Message: " ).append( messages.toString() ).append( "</p>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "Change the task below and" );
      retValue.append( "try adding it again, or go back to the" );
      retValue.append( "<a href=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath ).append( "/" )
              .append( "\">list of tasks (minus this task)</a>.\n" );
      retValue.append( "To keep this task and edit the configuration, change the period to zero (0).\n" );
      retValue.append( "</p>\n" );

      retValue.append( this.getHtmlAddForm( req, newTask ) );

      retValue.append( "</body>\n" );
      retValue.append( "</html>\n" );

      return( retValue.toString() );
    }

    /**
     * Build an HTML response for when editing a CatGenTimerTask is successful.
     */
    private String getHtmlEditTaskResultSuccess( HttpServletRequest req,
                                                 CatGenTimerTask oldTask,
                                                 CatGenTimerTask newTask,
                                                 StringBuilder messages )
    {
      log.debug( "htmlEditTaskResultSuccess(): start" );

      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Task Edit Results</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Task Edit Results</h1>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "Task Successfully edited.\n" );
      retValue.append( "</p>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "<a href=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath ).append( "/" )
              .append(  "\">List Current Tasks</a>\n" );
      retValue.append( "</p>\n" );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build an HTML response for when adding a CatGenTimerTask results in a
     * duplicate task.
     */
    private String getHtmlAddTaskResultDuplicate( HttpServletRequest req,
                                                  CatGenTimerTask newTask,
                                                  StringBuilder messages )
    {
      log.debug( "htmlAddTaskResultDuplicate(): start" );

      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Task Add Results</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Task Add Results</h1>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "Resulting task is a duplicate (in name and/or config doc name)\n" );
      retValue.append( "of a different existing task. Change the task below and\n" );
      retValue.append( "try adding it again, or go back to the\n" );
      retValue.append( "<a href=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath ).append( "/\">list of current tasks</a>.\n" );
      retValue.append( "</p>\n" );

      retValue.append( this.getHtmlAddForm( req, newTask ) );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build an HTML response for when adding a CatGenTimerTask results in an
     * invalid task.
     */
    private String getHtmlAddTaskResultInvalid( HttpServletRequest req,
                                                CatGenTimerTask newTask,
                                                StringBuilder messages )
    {
      log.debug( "htmlAddTaskResultInvalid(): start" );

      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Task Add Results</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Task Add Results</h1>\n" );

      retValue.append( "<p>Resulting task is invalid.</p>\n" );
      retValue.append( "<p>Message: " ).append( messages.toString() ).append( "</p>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "Change the task below and\n" );
      retValue.append( "try adding it again, or go back to the\n" );
      retValue.append( "<a href=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath )
              .append( "/\">list of tasks (minus this task)</a>.\n" );
      retValue.append( "To keep this task and edit the configuration, change the period to zero (0).\n" );
      retValue.append( "</p>\n" );

      retValue.append( this.getHtmlAddForm( req, newTask ) );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build an HTML response for when adding a CatGenTimerTask is successful.
     */
    private String getHtmlAddTaskResultSuccess( HttpServletRequest req,
                                                CatGenTimerTask newTask,
                                                StringBuilder messages )
    {
      log.debug( "htmlAddTaskResultSuccess(): start" );

      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Task Add Results</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Task Add Results</h1>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "Task Successfully added.\n" );
      retValue.append( "</p>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "<a href=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath ).append( "/\">List Current Tasks</a>\n" );
      retValue.append( "</p>\n" );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build an HTML response for when deleteing a CatGenTimerTask fails.
     */
    private String getHtmlDeleteTaskResultFail( HttpServletRequest req,
                                                CatGenTimerTask oldTask )
    {
      log.debug( "htmlDeleteTaskResultFail(): start" );

      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Task Delete Results</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Task Delete Results</h1>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "Deletion of task failed. (This shouldn't happen! We found\n" );
      retValue.append( "the task in the list so we should be able to remove it)\n" );
      retValue.append( "</p>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "<a href=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath ).append( "/\">List Current Tasks</a>\n" );
      retValue.append( "</p>\n" );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Build an HTML response for when deleteing a CatGenTimerTask is successful.
     */
    private String getHtmlDeleteTaskResultSuccess( HttpServletRequest req,
                                                   CatGenTimerTask oldTask )
    {
      log.debug( "htmlDeleteTaskResultSuccess(): start" );

      StringBuilder retValue = new StringBuilder();

      retValue.append( getHtmlDoctypeAndOpenTag() );
      retValue.append( "<head><title>Catalog Generator Servlet - Task Delete Results</title></head>\n" );
      retValue.append( "<body>\n" );
      retValue.append( "<h1>Catalog Generator Servlet - Task Delete Results</h1>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "Task Successfully deleted.\n" );
      retValue.append( "</p>\n" );

      retValue.append( "<p>\n" );
      retValue.append( "<a href=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminPath ).append( "/\">List Current Tasks</a>\n" );
      retValue.append( "</p>\n" );

      retValue.append( "</body>\n" );
      retValue.append( "</html>" );

      return( retValue.toString() );
    }

    /**
     * Helper method for producing an HTML form for adding a
     * new or the given task.
     */
    private String getHtmlAddForm( HttpServletRequest req, CatGenTimerTask task )
    {
      StringBuilder buf = new StringBuilder();
      log.debug( "getHtmlAddForm(): start" );

      String taskName = "";
      String taskConfigDocName = "";
      String taskResultFileName = "";
      String taskPeriodInMinutes = "";
      String taskDelayInMinutes = "";

      if ( task != null )
      {
        taskName = task.getName();
        taskConfigDocName = task.getConfigDocName();
        taskResultFileName = task.getResultFileName();
        taskPeriodInMinutes = Integer.toString( task.getPeriodInMinutes() );
        taskDelayInMinutes = Integer.toString( task.getDelayInMinutes() );
      }

      buf.append( "<form method=\"POST\" action=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminDoAddTaskPath ).append( "\">" ).append( "\n" );

      buf.append( "<h2>Task to Add</h2>" ).append( "\n" );
      buf.append( "<table border=\"1\">" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th> Task Name</th>" ).append( "\n" );
      buf.append( "<td> <input name=\"taskName\" size=\"40\" value=\"" )
              .append( taskName ).append( "\"></td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th> Configuration Document</th>" ).append( "\n" );
      buf.append( "<td> <input name=\"fileName\" size=\"40\" value=\"" ).append( taskConfigDocName ).append( "\"></td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th>Resulting Catalog</th>" ).append( "\n" );
      buf.append( "<td> <input name=\"resultFileName\" size=\"80\" value=\"" ).append( taskResultFileName ).append( "\"></td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th> Period (minutes)</th>" ).append( "\n" );
      buf.append( "<td> <input name=\"period\" size=\"10\" value=\"" ).append( taskPeriodInMinutes ).append( "\"></td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th> Initial Delay (minutes)</th>" ).append( "\n" );
      buf.append( "<td> <input name=\"delay\" size=\"10\" value=\"" ).append( taskDelayInMinutes ).append( "\"></td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "</table>" ).append( "\n" );

      buf.append( "<input type=\"submit\" value=\"Submit\">" ).append( "\n" );
      buf.append( "<input type=\"reset\" value=\"Reset Values\">" ).append( "\n" );
      buf.append( "</form>" ).append( "\n" );

      return ( buf.toString() );
    }

    /**
     * Helper method for producing an HTML form for editing an existing task.
     */
    private String getHtmlEditForm( HttpServletRequest req, CatGenTimerTask task )
    {
      StringBuilder buf = new StringBuilder();
      log.debug( "getHtmlEditForm(): start" );

      buf.append( "<form method=\"POST\" action=\"" )
              .append( req.getContextPath() ).append( req.getServletPath() )
              .append( this.adminDoEditTaskPath ).append( task.getConfigDocName() )
              .append( "\">\n" );

      buf.append( "<h2>Task to Edit</h2>\n" );
      buf.append( "<table border=\"1\">\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th> Task Name</th>\n" );
      buf.append( "<td> <input name=\"taskName\" size=\"40\" value=\"" )
              .append( task.getName() ).append( "\"></td>\n" );
      buf.append( "</tr>\n" );
      buf.append( "<tr>\n" );
      buf.append( "<th> Configuration Document</th>\n" );
      buf.append( "<td> <input name=\"fileName\" size=\"40\" value=\"" )
              .append( task.getConfigDocName() ).append( "\"></td>\n" );
      buf.append( "</tr>\n" );
      buf.append( "<tr>\n" );
      buf.append( "<th>Resultin Catalog</th>\n" );
      buf.append( "<td> <input name=\"resultFileName\" size=\"80\" value=\"" )
              .append( task.getResultFileName() ).append( "\"></td>\n" );
      buf.append( "</tr>\n" );
      buf.append( "<tr>\n" );
      buf.append( "<th> Period (minutes)</th>\n" );
      buf.append( "<td> <input name=\"period\" size=\"10\" value=\"" )
              .append( task.getPeriodInMinutes() ).append( "\"></td>\n" );
      buf.append( "</tr>\n" );
      buf.append( "<tr>\n" );
      buf.append( "<th> Initial Delay (minutes)</th>\n" );
      buf.append( "<td> <input name=\"delay\" size=\"10\" value=\"" )
              .append( task.getDelayInMinutes() ).append( "\"></td>\n" );
      buf.append( "</tr>\n" );
      buf.append( "</table>\n" );

      buf.append( "<input type=\"submit\" value=\"Submit\">\n" );
      buf.append( "<input type=\"reset\" value=\"Reset Values\">\n" );
      buf.append( "</form>\n" );

      return ( buf.toString() );
    }


    /**
     * Helper method for producing an HTML table that lists a task.
     */
    private String getHtmlListTable( CatGenTimerTask task )
    {
      StringBuilder buf = new StringBuilder();
      log.debug( "getHtmlListTable(): start" );

      buf.append( "<table border=\"1\">" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th> Task Name</th>" ).append( "\n" );
      buf.append( "<td>" ).append( task.getName() ).append( "</td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th> Configuration Document</th>" ).append( "\n" );
      buf.append( "<td>" ).append( task.getConfigDocName() ).append( "</td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th>Resulting Catalog</th>" ).append( "\n" );
      buf.append( "<td>" ).append( task.getResultFileName() ).append( "</td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th> Period (minutes)</th>" ).append( "\n" );
      buf.append( "<td>" ).append( task.getPeriodInMinutes() ).append( "</td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "<tr>" ).append( "\n" );
      buf.append( "<th> Initial Delay (minutes)</th>" ).append( "\n" );
      buf.append( "<td>" ).append( task.getDelayInMinutes() ).append( "</td>" ).append( "\n" );
      buf.append( "</tr>" ).append( "\n" );
      buf.append( "</table>" ).append( "\n" );

      return ( buf.toString() );
    }

}
