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

import edu.ucar.unidata.wmotables.domain.User;
import edu.ucar.unidata.wmotables.service.UserManager;

/**
 * Controller to handle and modify a User. 
 */

@Controller
public class UserController implements HandlerExceptionResolver {

    protected static Logger logger = Logger.getLogger(UserController.class);

    @Resource(name="userManager")
    private UserManager userManager;

    /**
     * Accepts a GET request for a List of User objects, retrieves the List from 
     * the UserManager, and passes the List<User> to the View for display.
     * View is a list of all User objects in the persistence mechanism.
     * 
     * @param model  The Model used by the View.
     * @return  The 'listUsers' path for the ViewResolver.
     */
    @RequestMapping(value="/users", method=RequestMethod.GET)
    public String listUsers(Model model) { 
        List<User> users = userManager.getUserList();           
        model.addAttribute("users", users);    
        return "listUsers";
    }

    /**
     * Accepts a GET request to create a new User object, retrieves the 
     * View is a web form to create the new User.
     * 
     * @return  The 'createTable' path for the ViewResolver.
     */
    @RequestMapping(value="/users/create", method=RequestMethod.GET)
    public String createUser() {   
        return "createUser";
    }

    /**
     * Accepts a POST request to create a new User object and persist it. 
     * A List of all User objects is retrieved from the UserManager, and 
     * passed the View for display.
     * View is a list of all User objects, including the newly created User.
     *
     * NOTE: exceptions surrounding File creation are being thrown instead of 
     * handled in this method.  The resolveException method gracefully handles 
     * any uncaught exceptions.
     * 
     * @param user  The User to persist. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The 'listUsers' path for the ViewResolver
     */
    @RequestMapping(value="/users/create", method=RequestMethod.POST)
    public String createUser(User user, BindingResult result, Model model) {   
        userManager.createUser(user);
        List<User> users = userManager.getUserList();           
        model.addAttribute("users", users);           
        return "listUsers";
    }

    /**
     * Accepts a GET request to update an existing User object, retrieves the 
     * requested User object from the UserManager based on the userId URI 
     * Template patten matching, and passes it to the View for display. 
     * View is a web form to update a few selected attributes of an existing User.
     * 
     * @param userId  The userId as provided by @PathVariable. 
     * @param model  The Model used by the view.
     * @return  The 'updateUser' path for the ViewResolver.
     */
    @RequestMapping(value="/users/{userId}/update", method=RequestMethod.GET)
    public String updateUser(@PathVariable int userId, Model model) {        
        User user = userManager.lookupUser(userId);   
        model.addAttribute("user", user);       
        return "updateUser";
    }

    /**
     * Accepts a POST request to update an existing User object.  A List of all User
     * objects is retrieved from the UserManager, and passed the View for display.
     * View is a list of all User objects, including the updated User.
     *
     * NOTE: exceptions surrounding File creation are being thrown instead of 
     * handled in this method.  The resolveException method gracefully handles 
     * any uncaught exceptions.
     * 
     * @param user  The User to update. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The 'listUsers' path for the ViewResolver
     */
    @RequestMapping(value="/users/update", method=RequestMethod.POST)
    public String updateUser(User user, BindingResult result, Model model) {   
        userManager.updateUser(user);
        List<User> users = userManager.getUserList();           
        model.addAttribute("users", users);           
        return "listUsers";
    }

    /**
     * Accepts a POST request to delete an existing User object from the persistence
     * mechanism.  The UserManager deletes the User, retrieves a List of remaining
     * User objects, and then passed the List to the View for display.
     * View is a list of all User objects.
     *
     * NOTE: exceptions surrounding File creation are being thrown instead of 
     * handled in this method.  The resolveException method gracefully handles 
     * any uncaught exceptions.
     * 
     * @param user  The User to delete. 
     * @param result  The BindingResult for error handling.
     * @param model  The Model used by the view.
     * @return  The redirect to listUsers View (/users)
     */
    @RequestMapping(value="/users/delete", method=RequestMethod.POST)
    public ModelAndView deleteUser(User user, BindingResult result, Model model) {   
        userManager.deleteUser(user.getUserId());
        List<User> users = userManager.getUserList();           
        model.addAttribute("users", users);           
        return new ModelAndView(new RedirectView("/users", true));
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
