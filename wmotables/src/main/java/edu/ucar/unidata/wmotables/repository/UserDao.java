package edu.ucar.unidata.wmotables.repository;

import java.util.List;

import edu.ucar.unidata.wmotables.domain.User;

/**
 * The data access object representing a User.  
 */

public interface UserDao {

    /*
     * Looks up and retrieves a user from the persistence mechanism using the user id.
     * 
     * @param userId  The id of the user we are trying to locate (will be unique for each user). 
     * @return  The user represented as a User object.   
     */
    public User lookupUser(int userId);

    /*
     * Requests a List of all registries from the persistence mechanism.
     * 
     * @return  A List of registries.   
     */
    public List<User> getUserList();

    /*
     * Queries the persistence mechanism and returns the number of registries.
     * 
     * @return  The total number of registries as an int.   
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
