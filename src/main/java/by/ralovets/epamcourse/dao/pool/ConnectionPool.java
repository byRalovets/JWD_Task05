package by.ralovets.epamcourse.dao.pool;

import by.ralovets.epamcourse.dao.pool.exception.ConnectionPoolException;
import by.ralovets.epamcourse.dao.pool.resource.DBParameter;
import by.ralovets.epamcourse.dao.pool.resource.DBResourceManager;

import java.sql.*;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

/**
 * @author Anton Ralovets
 * @version 1.0
 */
public class ConnectionPool {

    BlockingQueue<Connection> connectionQueue;
    BlockingQueue<Connection> usedConnectionQueue;

    private final String driverName;
    private final String url;
    private final String user;
    private final String password;
    private int poolSize;

    private static ConnectionPool instance;

    public static final int DEFAULT_CONNECTIONS_COUNT = 10;

    public static final String EXC_DRIVER_NOT_FOUND = "Driver class cannot be located";
    public static final String EXC_CONNECTION_FAILED = "The connection to the database failed";
    public static final String EXC_CLOSING_CONNECTIONS = "An error occurred while closing connections";
    public static final String EXC_ACQUIRING_CONNECTION = "An error occurred while acquiring the connection";

    private ConnectionPool() {
        DBResourceManager resourceManager = DBResourceManager.getInstance();

        driverName = resourceManager.getValue(DBParameter.DB_DRIVER);
        url = resourceManager.getValue(DBParameter.DB_URL);
        user = resourceManager.getValue(DBParameter.DB_USER);
        password = resourceManager.getValue(DBParameter.DB_PASSWORD);

        try {
            poolSize = Integer.parseInt(resourceManager.getValue(DBParameter.DB_POOLSIZE));
        } catch (NumberFormatException e) {
            poolSize = DEFAULT_CONNECTIONS_COUNT;
        }
    }

    public static ConnectionPool getInstance() throws ConnectionPoolException {
        if (instance == null) {
            instance = new ConnectionPool();
            instance.initPoolData();
        }

        return instance;
    }

    public void initPoolData() throws ConnectionPoolException {
        Locale.setDefault(Locale.ENGLISH);

        try {
            Class.forName(driverName);

            connectionQueue = new ArrayBlockingQueue<>(poolSize);
            usedConnectionQueue = new ArrayBlockingQueue<>(poolSize);

            for (int i = 0; i < poolSize; i++) {
                Connection connection = DriverManager.getConnection(url, user, password);

                PooledConnection pooledConnection = new PooledConnection(connection);
                connectionQueue.add(pooledConnection);
            }
        } catch (ClassNotFoundException e) {
            throw new ConnectionPoolException(EXC_DRIVER_NOT_FOUND);
        } catch (SQLException e) {
            throw new ConnectionPoolException(EXC_CONNECTION_FAILED);
        }
    }

    public void dispose() throws ConnectionPoolException {
        clearConnectionQueue();
    }

    private void clearConnectionQueue() throws ConnectionPoolException {
        try {
            closeConnectionQueue(connectionQueue);
            closeConnectionQueue(usedConnectionQueue);
        } catch (SQLException e) {
            throw new ConnectionPoolException(EXC_CLOSING_CONNECTIONS);
        }
    }

    public Connection acquireConnection() throws ConnectionPoolException {
        Connection connection;

        try {
            connection = connectionQueue.take();
            usedConnectionQueue.add(connection);
        } catch (InterruptedException e) {
            throw new ConnectionPoolException(EXC_ACQUIRING_CONNECTION);
        }

        return connection;
    }

    private void closeConnectionQueue(BlockingQueue<Connection> queue) throws SQLException {
        Connection connection;

        while ((connection = queue.poll()) != null) {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            ((PooledConnection) connection).reallyClose();
        }
    }

    /**
     * This class is used by connection pool. It has an overridden close()
     * method. When calling the close() method on the PooledConnection
     * object, the connection is not closed, but is actually returned to
     * the connection pool.
     *
     * @author Anton Ralovets
     * @version 1.0
     */
    private class PooledConnection implements Connection {

        private final Connection connection;

        public PooledConnection(Connection connection) throws SQLException {
            connection.setAutoCommit(true);
            this.connection = connection;
        }

        public void reallyClose() throws SQLException {
            connection.close();
        }

        @Override
        public Statement createStatement() throws SQLException {
            return connection.createStatement();
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return connection.prepareStatement(sql);
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return connection.prepareCall(sql);
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return connection.nativeSQL(sql);
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            connection.setAutoCommit(autoCommit);
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return connection.getAutoCommit();
        }

        @Override
        public void commit() throws SQLException {
            connection.commit();
        }

        @Override
        public void rollback() throws SQLException {
            connection.rollback();
        }

        @Override
        public void close() throws SQLException {
            if (connection.isClosed()) {
                throw new SQLException("dsf");
            }

            if (connection.isReadOnly()) {
                connection.setReadOnly(false);
            }

            if (!usedConnectionQueue.remove(this)) {
                throw new SQLException("");
            }

            if (!connectionQueue.offer(this)) {
                throw new SQLException("");
            }
        }

        @Override
        public boolean isClosed() throws SQLException {
            return connection.isClosed();
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return connection.getMetaData();
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            connection.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return connection.isReadOnly();
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
            connection.setCatalog(catalog);
        }

        @Override
        public String getCatalog() throws SQLException {
            return connection.getCatalog();
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
            connection.setTransactionIsolation(level);
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return connection.getTransactionIsolation();
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return connection.getWarnings();
        }

        @Override
        public void clearWarnings() throws SQLException {
            connection.clearWarnings();
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return connection.createStatement(resultSetType, resultSetConcurrency);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return connection.getTypeMap();
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            connection.setTypeMap(map);
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
            connection.setHoldability(holdability);
        }

        @Override
        public int getHoldability() throws SQLException {
            return connection.getHoldability();
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return connection.setSavepoint();
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return connection.setSavepoint(name);
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
            connection.rollback(savepoint);
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            connection.releaseSavepoint(savepoint);
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            return connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return connection.prepareStatement(sql, autoGeneratedKeys);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return connection.prepareStatement(sql, columnIndexes);
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return connection.prepareStatement(sql, columnNames);
        }

        @Override
        public Clob createClob() throws SQLException {
            return connection.createClob();
        }

        @Override
        public Blob createBlob() throws SQLException {
            return connection.createBlob();
        }

        @Override
        public NClob createNClob() throws SQLException {
            return connection.createNClob();
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return connection.createSQLXML();
        }

        @Override
        public boolean isValid(int timeout) throws SQLException {
            return connection.isValid(timeout);
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            connection.setClientInfo(name, value);
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            connection.setClientInfo(properties);
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return connection.getClientInfo(name);
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return connection.getClientInfo();
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return connection.createArrayOf(typeName, elements);
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return connection.createStruct(typeName, attributes);
        }

        @Override
        public void setSchema(String schema) throws SQLException {
            connection.setSchema(schema);
        }

        @Override
        public String getSchema() throws SQLException {
            return connection.getSchema();
        }

        @Override
        public void abort(Executor executor) throws SQLException {
            connection.abort(executor);
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
            connection.setNetworkTimeout(executor, milliseconds);
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return connection.getNetworkTimeout();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return connection.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return connection.isWrapperFor(iface);
        }
    }
}
