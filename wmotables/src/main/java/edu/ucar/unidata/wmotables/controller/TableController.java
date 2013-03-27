package edu.ucar.unidata.wmotables.controller;

import org.apache.log4j.Logger;

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

    /**
     * Accepts a GET request for a main home page.
     * View is main index page.
     * 
     * @return  The 'index' path for the ViewResolver.
     */
    @RequestMapping(value="/", method=RequestMethod.GET)
    public String listAllTables() { 
        return "index";
    }



    /**
     * Accepts a GET request for a List of Table objects.
     * View is a list of ALL Tables in the persistence mechanism.
     * 
     * @param model  The Model used by the View.
     * @return  The 'listTables' path for the ViewResolver.
     */
    @RequestMapping(value="/table", method=RequestMethod.GET)
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

    /**
     * Accepts a GET request for a specific Table object.
     * View is a requested Table object.
     * 
     * @param checksum  The 'checksum' as provided by @PathVariable.
     * @param model  The Model used by the View.
     * @return  The 'viewTable' path for the ViewResolver.
     */
    @RequestMapping(value="/table/{checksum}", method=RequestMethod.GET)
    public String viewTable(@PathVariable String checksum, Model model) { 
        Table table = tableManager.lookupTable(checksum);
        model.addAttribute("table", table);         
        User user = userManager.lookupUser(table.getUserId());   
        model.addAttribute("user", user);    
        return "viewTable";
    }

    /**
     * Accepts a GET request to download a specific Table.
     * No view is produced.  The file is streamed to the client for download.
     * 
     * @param checksum  The 'checksum' as provided by @PathVariable.
     * @param response  The HttpServletResponse response.
     */
    @RequestMapping(value="/table/download/{checksum}", method=RequestMethod.GET)
    public void downloadTable(@PathVariable String checksum, HttpServletResponse response) { 
        Table table = tableManager.lookupTable(checksum);
        tableManager.downloadTableFile(table, response);
    }


    /**
     * Accepts a GET request to create a new Table object. 
     * View is a web form to upload a new Table.
     * 
     * @param userName  The 'userName' as provided by @PathVariable. 
     * @param model  The Model used by the view.
     * @return  The 'tableForm' path for the ViewResolver.
     */
    @RequestMapping(value="/table/create/{userName}", method=RequestMethod.GET)
    public String createTable(@PathVariable String userName, Model model) { 
        User user = userManager.lookupUser(userName);  
        model.addAttribute("user", user);    
        model.addAttribute("formAction", "create");     
        return "tableForm";
    }

    /**
     * Accepts a POST request to create a new Table object. 
     * View is the newly created Table object.
     * 
     * @param table  The Table to persist. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to viewTable view (/table/{checksum})
     * @throws IOException  If an IO error occurs when writing the table to the file system.
     */
    @RequestMapping(value="/table/create", method=RequestMethod.POST)
    public ModelAndView createTable(Table table, BindingResult result, Model model) throws IOException {   
        table.setVisibility(1);
        tableManager.createTable(table);
        model.addAttribute("table", table);         
        User user = userManager.lookupUser(table.getUserId());   
        model.addAttribute("user", user);     
        return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
    }

    /**
     * Accepts a GET request to update an existing Table object.
     * View is a web form to update an existing Table.
     * 
     * @param checksum  The checksum as provided by @PathVariable. 
     * @param model  The Model used by the view.
     * @return  The 'tableForm' path for the ViewResolver.
     */
    @RequestMapping(value="/table/update/{checksum}", method=RequestMethod.GET)
    public String updateTable(@PathVariable String checksum, Model model) { 
        Table table = tableManager.lookupTable(checksum);
        model.addAttribute("table", table);         
        User user = userManager.lookupUser(table.getUserId());   
        model.addAttribute("user", user);       
        model.addAttribute("formAction", "update");  
        return "tableForm";
    }

    /**
     * Accepts a POST request to update an existing Table object. 
     * View is the updated Table object.
     * 
     * @param table  The Table to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to 'viewTable' view (/table/{checksum})
     */
    @RequestMapping(value="/table/update", method=RequestMethod.POST)
    public ModelAndView updateTable(Table table, BindingResult result, Model model) {  
        tableManager.updateTable(table);
        table = tableManager.lookupTable(table.getTableId());
        model.addAttribute("table", table);         
        User user = userManager.lookupUser(table.getUserId());   
        model.addAttribute("user", user);     
        return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
    }

    /**
     * Accepts a POST request to hide a new Table object. 
     * View is the hidden Table object.
     * 
     * @param table  The Table to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to 'viewTable' view (/table/{checksum})
     */
    @RequestMapping(value="/table/hide", method=RequestMethod.POST)
    public ModelAndView hideTable(Table table, BindingResult result, Model model) {  
        logger.warn(table.getVisibility());
        if (table.getVisibility() == 1) {
            table.setVisibility(0);
        } else {
            table.setVisibility(1);
        }
        tableManager.toggleTableVisibility(table);
        table = tableManager.lookupTable(table.getTableId());
        model.addAttribute("table", table);         
        User user = userManager.lookupUser(table.getUserId());   
        model.addAttribute("user", user);     
        return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
    }



    /**
     * This method gracefully handles any uncaught exception that are fatal 
     * in nature and unresolvable by the user.
     * 
     * @param request   The current HttpServletRequest request.
     * @param response  The current HttpServletRequest response.
     * @param handler  The executed handler, or null if none chosen at the time of the exception.  
     * @param exception  The  exception that got thrown during handler execution.
     * @return  The error page containing the appropriate message to the user. 
     */
    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        String message = "";
        if (exception instanceof MaxUploadSizeExceededException){ 
            message = "File size should be less then "+ ((MaxUploadSizeExceededException)exception).getMaxUploadSize()+" byte.";
        } else if (exception instanceof NullPointerException) {
            message = "Problem with the tableStashDir argument during File creation.  Verify the wmotables.home value in the wmotables.properties file is correct: " + exception.getMessage();
        } else if (exception instanceof FileNotFoundException) {
            message = "Unable to create FileOutputStream for File: " + exception.getMessage();
        } else if (exception instanceof IOException) {
            message = "An IO error occured with the FileOutputStream for File: " + exception.getMessage();
        } else if (exception instanceof SecurityException) {
            message = "JVM security manager configuration conflict.  Unable to write File: " + exception.getMessage();
        } else {
            message = "An error has occurred: " + exception.getClass().getName() + ": " + exception.getMessage();  
        }        

        logger.error(message);
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("message", message);
        ModelAndView modelAndView = new ModelAndView("fatalError", model);
        return modelAndView;
    }
}
