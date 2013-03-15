package edu.ucar.unidata.wmotables.service;

import java.util.List;

import edu.ucar.unidata.wmotables.domain.User;


/**
 * Service for processing User objects. 
 */
public interface UserManager {

    /*
     * Looks up and retrieves a user from the persistence mechanism using the user id.
     * 
     * @param userId  The id of the user we are trying to locate (will be unique for each user).
     * @return  The user represented as a User object.   
     */
    public User lookupUser(int userId);
   
    /*
     * Requests a List of all users from the persistence mechanism.
     * 
     * @return  A List of users.   
     */
    public List<User> getUserList();

    /*
     * Queries the persistence mechanism and returns the number of users.
     * 
     * @return  The total number of users as an int.   
     */
    public int getUserCount();

    /*
     * Finds and removes the user from the persistence mechanism.
     * 
     * @param userId  The user id in the persistence mechanism.  
     */
    public void deleteUser(int userId);

    /*
     * Creates a new user.
     * 
     * @param user  The user to be created. 
     */
    public void createUser(User user);

    /*
     * Saves changes made to an existing user. 
     * 
     * @param user   The existing user with changes that needs to be saved. 
     */
    public void updateUser(User user);
}
