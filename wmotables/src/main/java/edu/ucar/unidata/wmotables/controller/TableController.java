package edu.ucar.unidata.wmotables.controller;

import org.apache.log4j.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.ui.Model;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;
import edu.ucar.unidata.wmotables.service.TableManager;
import edu.ucar.unidata.wmotables.service.UserManager;

/**
 * Controller to handle and modify a WMO table. 
 */

@Controller
public class TableController implements HandlerExceptionResolver {

    protected static Logger logger = Logger.getLogger(TableController.class);
    @Resource(name="userManager")
    private UserManager userManager;
    @Resource(name="tableManager")
    private TableManager tableManager;

    /*
     * Accepts a GET request for a List of Table objects, retrieves the List from 
     * the TableManager, and passes the List<Table> to the View for display. Also
     * gets a list of available Users from the UserManager and creates a 
     * Map<String, User> to display corresponding User information with each Table.
     * View is a list of ALL Table objects in the persistence mechanism.
     * 
     * @param model  The Model used by the View.
     * @return  The 'listTables' path for the ViewResolver.
     */
    @RequestMapping(value="/", method=RequestMethod.GET)
    public String listAllTables(Model model) { 
        List<Table> tables = tableManager.getTableList();
        model.addAttribute("tables", tables);   

        Map<String, User> users = new HashMap<String, User>();
        Iterator<Table> iterator = tables.iterator();
	    while (iterator.hasNext()) {
		    Table table =  iterator.next();
            User user = userManager.lookupUser(table.getUserId());
            users.put(new Integer(table.getUserId()).toString(), user);
	    }   
        model.addAttribute("users", users); 
        return "listTables";
    }

    /*
     * Accepts a GET request for a List of Table objects, retrieves the List
     * from the TableManager based on the userId URI Template patten matching, 
     * and passes the List<Table> to the View for display. The User object is 
     * also added to the model for use in the view.
     * View is a list of Table objects owned by a particular User.
     * 
     * @param userId  The userId as provided by @PathVariable.
     * @param model  The Model used by the View.
     * @return  The 'listTables' path for the ViewResolver.
     */
    @RequestMapping(value="/{userId}", method=RequestMethod.GET)
    public String listUserTables(@PathVariable int userId, Model model) { 
        List<Table> tables = tableManager.getTableList(userId);
        model.addAttribute("tables", tables);         
        User user = userManager.lookupUser(userId);   
        model.addAttribute("user", user);    
        return "listTables";
    }

    /*
     * Accepts a GET request to create a new Table object, retrieves the 
     * corresponding User object from the UserManager based on the userId 
     * URI Template patten matching, and passes it to the View for display. 
     * View is a web form to upload a new Table.
     * 
     * @param userId  The userId as provided by @PathVariable. 
     * @param model  The Model used by the view.
     * @return  The 'createTable' path for the ViewResolver.
     */
    @RequestMapping(value="/{userId}/create", method=RequestMethod.GET)
    public String createTable(@PathVariable int userId, Model model) { 
        User user = userManager.lookupUser(userId);   
        model.addAttribute("user", user);       
        return "createTable";
    }

    /*
     * Accepts a POST request to create a new Table object, sets the visibility 
     * of the Table object and persists it.  The corresponding User object is
     * retrieved from the UserManager and passes it to the View for display. 
     * View is a list of Table objects owned by a particular User.
     *
     * NOTE: exceptions surrounding File creation are being thrown instead of 
     * handled in this method.  The resolveException method gracefully handles 
     * any uncaught exceptions.
     * 
     * @param table  The Table to persist. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to /{userId}
     * @throws IOException  If an IO error occurs when writing the table to the file system.
     */
    @RequestMapping(value="/create", method=RequestMethod.POST)
    public ModelAndView createTable(Table table, BindingResult result, Model model) throws IOException {   
        table.setVisibility(1);
        tableManager.createTable(table);
        List<Table> tables = tableManager.getTableList(table.getUserId());
        model.addAttribute("tables", tables);
        User user = userManager.lookupUser(table.getUserId());   
        model.addAttribute("user", user);          
        return new ModelAndView(new RedirectView("/" + new Integer(table.getUserId()).toString(), true));
    }

    /*
     * Accepts a GET request to update an existing Table object, retrieves the 
     * requested Table object from the TableManager based on the tableId URI 
     * Template patten matching, and passes it to the View for display. The 
     * User object is also added to the model for use in the view.
     * View is a web form to update a few selected attributes of an existing Table.
     * 
     * @param userId  The userId as provided by @PathVariable. 
     * @param tableId  The tableId as provided by @PathVariable.
     * @param model  The Model used by the view.
     * @return  The 'updateTable' path for the ViewResolver.
     */
    @RequestMapping(value="/{userId}/{tableId}/update", method=RequestMethod.GET)
    public String updateTable(@PathVariable int userId, @PathVariable int tableId, Model model) { 
        Table table = tableManager.lookupTable(tableId);
        model.addAttribute("table", table);         
        User user = userManager.lookupUser(userId);   
        model.addAttribute("user", user);       
        return "updateTable";
    }

    /*
     * Accepts a POST request to create a new Table object, sets the visibility 
     * of the Table object and persists it.  The corresponding User object is
     * retrieved from the UserManager and passes it to the View for display. 
     * View is a list of Table objects owned by a particular User.
     *
     * NOTE: exceptions surrounding File creation are being thrown instead of 
     * handled in this method.  The resolveException method gracefully handles 
     * any uncaught exceptions.
     * 
     * @param table  The Table to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to /{userId}
     */
    @RequestMapping(value="/update", method=RequestMethod.POST)
    public ModelAndView updateTable(Table table, BindingResult result, Model model) {  
        tableManager.updateTable(table);
        List<Table> tables = tableManager.getTableList(table.getUserId());
        model.addAttribute("tables", tables); 
        User user = userManager.lookupUser(table.getUserId());   
        model.addAttribute("user", user);          
        return new ModelAndView(new RedirectView("/" + new Integer(table.getUserId()).toString(), true));
    }

    /*
     * Accepts a POST request to create a new Table object, sets the visibility 
     * of the Table object and persists it.  The corresponding User object is
     * retrieved from the UserManager and passes it to the View for display. 
     * View is a list of Table objects owned by a particular User.
     *
     * NOTE: exceptions surrounding File creation are being thrown instead of 
     * handled in this method.  The resolveException method gracefully handles 
     * any uncaught exceptions.
     * 
     * @param table  The Table to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to /{userId}
     */
    @RequestMapping(value="/hide", method=RequestMethod.POST)
    public ModelAndView hideTable(Table table, BindingResult result, Model model) {  
        tableManager.toggleTableVisibility(table); 
        List<Table> tables = tableManager.getTableList(table.getUserId());
        model.addAttribute("tables", tables); 
        User user = userManager.lookupUser(table.getUserId());   
        model.addAttribute("user", user);          
        return new ModelAndView(new RedirectView("/" + new Integer(table.getUserId()).toString(), true));
    }



    /*
     * This method gracefully handles any uncaught exception that are fatal 
     * in nature and unresolvable by the user.
     * 
     * @param request   The current HttpServletRequest request.
     * @param response  The current HttpServletRequest response.
     * @param handler  The executed handler, or null if none chosen at the time of the exception.  
     * @param handler  The  exception that got thrown during handler execution.
     * @return  The error page containing the appropriate message to the user. 
     */
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        Map<Object, Object> model = new HashMap<Object, Object>();
        if (exception instanceof MaxUploadSizeExceededException){ 
            logger.error("File size should be less then "+ ((MaxUploadSizeExceededException)exception).getMaxUploadSize()+" byte.");
        } else if (exception instanceof NullPointerException) {
            logger.error("Problem with the tableStashDir argument during File creation.  Verify the wmotables.home value in the wmotables.properties file is correct: " + exception.getMessage());
        } else if (exception instanceof FileNotFoundException) {
            logger.error("Unable to create FileOutputStream for File: " + exception.getMessage());
        } else if (exception instanceof IOException) {
            logger.error("An IO error occured with the FileOutputStream for File: " + exception.getMessage());
        } else if (exception instanceof SecurityException) {
            logger.error("JVM security manager configuration conflict.  Unable to write File: " + exception.getMessage());
        } else {
           logger.error("An error has occurred: " + exception.getClass().getName());
           logger.error(exception.getMessage());
        }        
        return new ModelAndView("fatalError");
    }
}
