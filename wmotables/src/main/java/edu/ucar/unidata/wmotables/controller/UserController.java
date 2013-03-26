package edu.ucar.unidata.wmotables.controller;

import org.apache.log4j.Logger;


import java.util.HashMap;
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
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.ui.Model;

import edu.ucar.unidata.wmotables.domain.Table;
import edu.ucar.unidata.wmotables.domain.User;
import edu.ucar.unidata.wmotables.service.TableManager;
import edu.ucar.unidata.wmotables.service.UserManager;

/**
 * Controller to handle and modify a User. 
 */

@Controller
public class UserController implements HandlerExceptionResolver {

    protected static Logger logger = Logger.getLogger(UserController.class);
    @Resource(name="userManager")
    private UserManager userManager;
    @Resource(name="tableManager")
    private TableManager tableManager;

    /**
     * Accepts a GET request for a List of User objects.
     * View is a list of all User objects in the persistence mechanism.
     * 
     * @param model  The Model used by the View.
     * @return  The 'listUsers' path for the ViewResolver.
     */
    @RequestMapping(value="/user", method=RequestMethod.GET)
    public String listUsers(Model model) { 
        List<User> users = userManager.getUserList();           
        model.addAttribute("users", users);    
        return "listUsers";
    }

    /**
     * Accepts a GET request for a specific User object.
     * View is the requested User object in the persistence mechanism.
     * 
     * @param userName  The userName as provided by @PathVariable.
     * @param model  The Model used by the View.
     * @return  The 'viewUser' path for the ViewResolver.
     */
    @RequestMapping(value="/user/{userName}", method=RequestMethod.GET)
    public String viewUser(@PathVariable String userName, Model model) { 
        User user = userManager.lookupUser(userName);           
        model.addAttribute("user", user);    
        List<Table> tables = tableManager.getTableList(user.getUserId());
        model.addAttribute("tables", tables);         
        return "viewUser";
    }

    /**
     * Accepts a GET request to create a new User object.
     * View is a web form to create the new User.
     * 
     * @param model  The Model used by the View.
     * @return  The 'userForm' path for the ViewResolver.
     */
    @RequestMapping(value="/user/create", method=RequestMethod.GET)
    public String createUser(Model model) {   
        model.addAttribute("formAction", "create");  
        return "userForm";
    }

    /**
     * Accepts a POST request to create a new User object and persist it. 
     * View is the newly created User object.
     * 
     * @param user  The User to persist. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to viewUser View (/user/{userName})
     */
    @RequestMapping(value="/user/create", method=RequestMethod.POST)
    public ModelAndView createUser(User user, BindingResult result, Model model) {   
        userManager.createUser(user);
        model.addAttribute("user", user);      
        return new ModelAndView(new RedirectView("/user/" + user.getUserName(), true));     
    }

    /**
     * Accepts a GET request to update an existing User object. 
     * View is a web form to update a few selected attributes of an existing User.
     * 
     * @param userName  The userName as provided by @PathVariable. 
     * @param model  The Model used by the view.
     * @return  The 'userForm' path for the ViewResolver.
     */
    @RequestMapping(value="/user/{userName}/update", method=RequestMethod.GET)
    public String updateUser(@PathVariable String userName, Model model) {        
        User user = userManager.lookupUser(userName);   
        model.addAttribute("user", user);   
        model.addAttribute("formAction", "update");      
        return "userForm";
    }

    /**
     * Accepts a POST request to update an existing User object.
     * View is the updated User object.
     * 
     * @param user  The User to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to viewUser View (/user/{userName})
     */
    @RequestMapping(value="/user/update", method=RequestMethod.POST)
    public ModelAndView updateUser(User user, BindingResult result, Model model) {   
        userManager.updateUser(user);
        model.addAttribute("user", user);          
        return new ModelAndView(new RedirectView("/user/" + user.getUserName(), true));   
    }

    /**
     * Accepts a POST request to delete an existing User object. 
     * View is a list of all User objects.
     * 
     * @param user  The User to delete. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to listUsers View (/user)
     */
    @RequestMapping(value="/user/delete", method=RequestMethod.POST)
    public ModelAndView deleteUser(User user, BindingResult result, Model model) {   
        userManager.deleteUser(user.getUserId());
        List<User> users = userManager.getUserList();           
        model.addAttribute("users", users);           
        return new ModelAndView(new RedirectView("/user", true));
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
        String message = "An error has occurred: " + exception.getClass().getName() + ": " + exception.getMessage();   
        logger.error(message);
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("message", message);
        ModelAndView modelAndView = new ModelAndView("fatalError", model);
        return modelAndView;
    }
  

}
