package by.ralovets.epamcourse.dao.pool.resource;

/**
 * The class contains the names of the parameters for initializing the connection pool
 *
 * @author Anton Ralovets
 * @version 1.0
 */
public class DBParameter {

    private DBParameter() {
    }

    public static final String DB_DRIVER = "db.driver";
    public static final String DB_URL = "db.url";
    public static final String DB_USER = "db.user";
    public static final String DB_PASSWORD = "db.password";
    public static final String DB_POOLSIZE = "db.poolsize";
}
