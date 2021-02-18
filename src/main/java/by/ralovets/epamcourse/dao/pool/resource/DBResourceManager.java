package by.ralovets.epamcourse.dao.pool.resource;

import java.util.ResourceBundle;

/**
 * The class provides parameters for initializing the connection pool
 *
 * @author Anton Ralovets
 * @version 1.0
 */
public class DBResourceManager {

    private static final String RESOURCE_BUNDLE_NAME = "db.properties";

    private static final DBResourceManager instance = new DBResourceManager();
    private final ResourceBundle bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);

    public static DBResourceManager getInstance() {
        return instance;
    }

    public String getValue(String key) {
        return bundle.getString(key);
    }
}
