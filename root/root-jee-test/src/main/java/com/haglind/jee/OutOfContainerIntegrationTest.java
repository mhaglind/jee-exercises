package com.haglind.jee;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.junit.After;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * 
 */
@ContextConfiguration(locations = {"classpath:springTestContext.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class OutOfContainerIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(OutOfContainerIntegrationTest.class.getName());

    @Inject
    @Qualifier("dataSource")
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate template;

    private DatabaseConnection dbunitConnection;

    @Inject
    private DbUnitHelper dbUnitHelper;

    /**
     * @throws Exception e
     */
    @BeforeTransaction
    public final void outOfContainerIntegrationTestSetUp() throws Exception {
        LOG.trace("Entering outOfContainerIntegrationTestSetUp");
        dbunitConnection = dbUnitHelper.createConnection(dataSource);
        dbUnitHelper.purgeRecycleBin(dbunitConnection);
        DatabaseConfig config = dbunitConnection.getConfig();
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new OracleDataTypeFactory());
        LOG.trace("Exiting outOfContainerIntegrationTestSetUp");
    }

    @After
    public final void tearDownDbUnit() throws Exception {
        try {
            // close connection
            if (dbunitConnection != null) {
                dbunitConnection.close();
                dbunitConnection = null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Closing DbUnit connection failed", e);
        }
    }

    /**
     * 
     */
    @PostConstruct
    private void initJdbcTemplate() {
        LOG.trace("initJdbcTemplate");
        template = new JdbcTemplate(dataSource);
    }

    /**
     * 
     * @param tableName The db table name.
     */
    protected void flushAndDump(String tableName) {
        LOG.trace("Entering flushAndDump: " + tableName);
        entityManager.flush();
        List<Map<String, Object>> list = template.queryForList("SELECT * FROM " + tableName);
        for (Map<String, Object> rowMap : list) {
            StringBuffer sb = new StringBuffer(tableName).append(" ");
            for (String key : rowMap.keySet()) {
                Object data = rowMap.get(key);
                if (data != null) {
                    sb.append(key + ":" + rowMap.get(key) + " ");
                }
            }
            LOG.info(sb.toString());
        }
        LOG.trace("Exiting flushAndDump: " + tableName);
    }

    /**
     * 
     * @param tableName The db table name.
     * @return Total number of rows in the table.
     */
    protected int flushAndRows(String tableName) {
        LOG.trace("Entering flushAndRows: " + tableName);
        entityManager.flush();
        int rows = template.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        LOG.trace("Exiting flushAndRows: " + rows);
        return rows;
    }

    /**
     * 
     * @param sql SQL statement to execute.
     * @return result
     */
    protected Object flushAndQueryForObject(String sql) {
        LOG.trace("Entering flushAndQueryForObject: " + sql);
        entityManager.flush();
        Object object = template.queryForObject(sql, Object.class);
        LOG.trace("Exiting flushAndQueryForObject: " + object);
        return object;

    }

    /**
     * 
     * @param xmlDbUnitDataSet x
     * @throws DatabaseUnitException due
     * @throws SQLException se
     */
    protected void cleanInsert(String xmlDbUnitDataSet) throws DatabaseUnitException, SQLException {
        dbUnitHelper.cleanInsert(xmlDbUnitDataSet, dbunitConnection);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public JdbcTemplate getTemplate() {
        return template;
    }

    public DatabaseConnection getDbunitConnection() {
        return dbunitConnection;
    }
}
