package edu.ucar.unidata.wmotables.controller;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.ui.Model;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;
import edu.ucar.unidata.wmotables.service.TableManager;
import edu.ucar.unidata.wmotables.service.UserManager;
// import edu.ucar.unidata.wmotables.service.UserValidator;

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
    //@Resource(name="userValidator")
    // private UserValidator userValidator;

    private String authName;


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
     * Only the user and application administrators are allowed to create tables for the user account.
     * 
     * @param userName  The 'userName' as provided by @PathVariable. 
     * @param model  The Model used by the view.
     * @return  The 'tableForm' path for the ViewResolver.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userName == authentication.name")
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
     * Only the user and application administrators are allowed to create tables for the user account.
     * 
     * @param table  The Table to persist. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to viewTable view (/table/{checksum})
     * @throws IOException  If an IO error occurs when writing the table to the file system.
     * @thows AccessDeniedException  If logged in user does not have permission to create the table.
     */
    @RequestMapping(value="/table/create", method=RequestMethod.POST)
    public ModelAndView createTable(Table table, BindingResult result, Model model) throws IOException, AccessDeniedException {   
        // The first thing we do is validate that the user has permission to create a table
        User user = userManager.lookupUser(table.getUserId()); 
        if (isAuthorized(user.getUserName())) {
           table.setVisibility(1);
           tableManager.createTable(table);
           model.addAttribute("table", table);
           model.addAttribute("user", user);     
           return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
        } else {   
           throw new AccessDeniedException ("User: " + getAuthName()  + " is attempting to create under the user account: "  + user.getUserName());
        }
    }

    /**
     * Accepts a GET request to update an existing Table object.
     * View is a web form to update an existing Table.
     * Only the user and application administrators are allowed to update tables for the user account.
     * 
     * @param checksum  The checksum as provided by @PathVariable. 
     * @param model  The Model used by the view.
     * @return  The 'tableForm' path for the ViewResolver.
     * @thows AccessDeniedException  If logged in user does not have permission to update the table.
     */
    @RequestMapping(value="/table/update/{checksum}", method=RequestMethod.GET)
    public String updateTable(@PathVariable String checksum, Model model) throws AccessDeniedException { 
        Table table = tableManager.lookupTable(checksum);
        // The first thing we do is validate that the user has permission to update a table
        User user = userManager.lookupUser(table.getUserId());   
        if (isAuthorized(user.getUserName())) {
           model.addAttribute("table", table);         
           model.addAttribute("user", user);       
           model.addAttribute("formAction", "update");  
           return "tableForm";
        } else {   
           throw new AccessDeniedException ("User: " + getAuthName()  + " is attempting to update a table that belongs to the user account: "  + user.getUserName());
        }
    }

    /**
     * Accepts a POST request to update an existing Table object. 
     * View is the updated Table object.
     * Only the user and application administrators are allowed to update tables for the user account.
     * 
     * @param table  The Table to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to 'viewTable' view (/table/{checksum})
     * @thows AccessDeniedException  If logged in user does not have permission to update the table.
     */
    @RequestMapping(value="/table/update", method=RequestMethod.POST)
    public ModelAndView updateTable(Table table, BindingResult result, Model model) throws AccessDeniedException {  
        // The first thing we do is validate that the user has permission to create a table
        User user = userManager.lookupUser(table.getUserId()); 
        if (isAuthorized(user.getUserName())) {
            tableManager.updateTable(table);
            table = tableManager.lookupTable(table.getTableId());
            model.addAttribute("table", table);         
            model.addAttribute("user", user);     
            return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
        } else {   
           throw new AccessDeniedException ("User: " + getAuthName()  + " is attempting to update a table that belongs to the user account: "  + user.getUserName());
        }
    }

    /**
     * Accepts a POST request to hide a new Table object. 
     * View is the hidden Table object.
     * Only the user and application administrators are allowed to hide tables for the user account.
     * 
     * @param table  The Table to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to 'viewTable' view (/table/{checksum})
     * @thows AccessDeniedException  If logged in user does not have permission to delete the table.
     */
    @RequestMapping(value="/table/hide", method=RequestMethod.POST)
    public ModelAndView hideTable(Table table, BindingResult result, Model model) throws AccessDeniedException {  
        int visibility = table.getVisibility();
        table = tableManager.lookupTable(table.getTableId());
        // The first thing we do is validate that the user has permission to create a table
        User user = userManager.lookupUser(table.getUserId()); 
        if (isAuthorized(user.getUserName())) {
            if (visibility == 1) {
                table.setVisibility(0);
            } else {
                table.setVisibility(1);
            }
            tableManager.toggleTableVisibility(table);
            model.addAttribute("table", table);         
            model.addAttribute("user", user);     
            return new ModelAndView(new RedirectView("/table/" + table.getChecksum(), true));
        } else {   
           throw new AccessDeniedException ("User: " + getAuthName()  + " is attempting to hide a table that belongs to the user account: "  + user.getUserName());
        }
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
        ModelAndView modelAndView = new ModelAndView();
        Map<String, Object> model = new HashMap<String, Object>();
        modelAndView.setViewName("fatalError"); 
        if (exception instanceof AccessDeniedException){ 
            message = exception.getMessage();
            modelAndView.setViewName("denied");
        } else if (exception instanceof MaxUploadSizeExceededException){ 
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
        model.put("message", message);
        modelAndView.addAllObjects(model);
        return modelAndView;
    }


    /**
     * Checks the authenticated user's credentials and roles to determine if the user
     * has permission to perform the required method action.  Returns true if authorized.
     * 
     * @param userName  The user name we will evaluate the logged in user against. 
     * @return  The boolean determining if the user is authorized to perform the method action.
     */
    public boolean isAuthorized(String userName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Get logged in username
        String authName = auth.getName(); 
        if (authName.equals(userName)) {
            return true;
        } else {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
            while (iterator.hasNext()) {
                GrantedAuthority grantedAuthority = iterator.next();
                String authority = grantedAuthority.getAuthority();
                if (authority.equals("ROLE_ADMIN")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Returns the authenticated user name.
     * 
     * @return  The authenticated user name.
     */
    public String getAuthName() {
        return authName;
    }

    /**
     * Sets the authenticated user name.
     * 
     * @param authName  The authenticated user name.
     */
    public void setAuthName(String authName) {
        this.authName = authName;
    }
}
