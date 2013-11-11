package com.haglind.jee;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.dataset.xml.XmlDataSetWriter;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * DBUnit helper class.
 */
@Named("net.volvocars.pc.test.DbUnitHelper")
public final class DbUnitHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DbUnitHelper.class.getName());
    private static final String ORACLE_PURGE_RECYCLEBIN_QUERY = "PURGE RECYCLEBIN";

    @Inject
    private DataSource dataSource;

    @Inject
    @Qualifier("dataBaseSchema")
    private String databaseSchema;

    /**
     * 
     */
    protected DbUnitHelper() {
    }

    /**
     * 
     * @param dataSource d
     * @return DatabaseConnection
     * @throws java.sql.SQLException se
     * @throws DatabaseUnitException due
     */
    public DatabaseConnection createConnection(DataSource dataSource) throws java.sql.SQLException, DatabaseUnitException {
        if (databaseSchema != null) {
            LOG.info(String.format("Configuring databaseSchema %s", databaseSchema));
            DatabaseConnection connection = new DatabaseConnection(dataSource.getConnection(), databaseSchema);
            connection.getConfig().setProperty(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, true);
            return connection;
        }

        LOG.info("Configuring without explicit databaseSchema");
        return new DatabaseConnection(dataSource.getConnection());
    }

    /**
     * 
     * @param xmlDbUnitDataSet x
     * @param dbUnitConnection d
     * @throws DatabaseUnitException due
     * @throws SQLException se
     */
    public void cleanInsert(String xmlDbUnitDataSet, DatabaseConnection dbUnitConnection) throws DatabaseUnitException, SQLException {
        LOG.trace("Entering cleanInsert: " + xmlDbUnitDataSet);
        DatabaseOperation.CLEAN_INSERT.execute(dbUnitConnection, getFilteredDataSet(xmlDbUnitDataSet));
        LOG.trace("Exiting cleanInsert");
    }

    private ReplacementDataSet getFilteredDataSet(String xmlDbUnitDataSet) throws DataSetException {
        ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSetBuilder().build(new FilteredSchemaReader(new StringReader(
            xmlDbUnitDataSet), databaseSchema)));
        dataSet.addReplacementObject("[NULL]", null);
        return dataSet;
    }

    /**
     * Convenience method for retriving a dataset from the live database.
     * 
     * @param actualDataSQL
     *            the SQL query to use
     * @param dbunitConnection
     *            the dbUnit connection
     * @return an ITable representing the Query result
     * @throws SQLException
     *             on any SQL-related failures
     * @throws DataSetException
     *             on DbUnit-related failures
     */
    public ITable getActualData(String actualDataSQL, DatabaseConnection dbunitConnection) throws SQLException, DataSetException {
        ITable actualTable = dbunitConnection.createQueryTable("test", actualDataSQL);
        return actualTable;
    }

    /**
     * Convenience method for asserting equality between two tables, only considering the columns in the expected
     * dataset.
     * 
     * @param expected
     *            the expected dataset
     * @param actual
     *            the actual dataset
     * @throws DatabaseUnitException
     *             on DbUnit exceptions
     */
    public void assertEquals(ITable expected, ITable actual) throws DatabaseUnitException {
        ITable actualFiltered = DefaultColumnFilter.includedColumnsTable(actual, expected.getTableMetaData().getColumns());
        Assertion.assertEquals(expected, actualFiltered);
    }

    /**
     * Convenience method for asserting equality between two tables when only considering a specified number of columns.
     * 
     * @param expected
     *            the expected dataset
     * @param actual
     *            the actual dataset
     * @param columns
     *            the columns to consider
     * @throws DatabaseUnitException
     *             on DbUnit exceptions
     */
    public void assertEquals(ITable expected, ITable actual, String[] columns) throws DatabaseUnitException {
        ITable expectedFiltered = DefaultColumnFilter.includedColumnsTable(expected, columns);
        ITable actualFiltered = DefaultColumnFilter.includedColumnsTable(actual, columns);
        Assertion.assertEquals(expectedFiltered, actualFiltered);
    }

    /**
     * Since Oracle version 10 all tables that are droped are not immediately removed but instead renamed using a system
     * defined, case sensitive, naming convention (works as a recyclebin). DbUnit uses a case insensitive approach when
     * searching for duplicate method names and this causes an AmbigousTableNameException. Purging the recyclebin before
     * returning a connection solves this problem. A defect on DbUnit has been submitted:
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1459205&group_id=47439&atid=449491
     * 
     * @param dataBaseConnection
     *            The connection to use when purging.
     */
    public void purgeRecycleBin(DatabaseConnection dataBaseConnection) {

        String vendor = null;
        int majorVersion = 0;
        try {
            Connection connection = dataBaseConnection.getConnection();
            vendor = connection.getMetaData().getDatabaseProductName();
            majorVersion = connection.getMetaData().getDriverMajorVersion();
            // Only purge if vendor is oracle and major version is 10 or later.
            if (vendor == null || vendor.toLowerCase().indexOf("oracle") < 0 || majorVersion < 10) {
                return;
            }
            Statement statement = connection.createStatement();
            statement.executeUpdate(ORACLE_PURGE_RECYCLEBIN_QUERY);
            LOG.info("Recyclebin was purged");
        } catch (SQLException sqlException) {
            // ORA-00900: invalid SQL statement. Most likely running Oracle 9 using Oracle 10 drivers. Ignore.
            if (sqlException.getErrorCode() != 900) {
                LOG.error("SQLException caught while trying to purge recyclebin. Vendor: " + vendor + ", major version : " + majorVersion,
                    sqlException);
            }
        } catch (Exception e) {
            LOG.error("Exception caught while trying to purge recyclebin. Vendor: " + vendor + ", major version : " + majorVersion, e);
        }
    }

    public String getSchema() {
        return databaseSchema;
    }

    /**
     * Convenience method. Returns a XML representation of an IDataSet insstance.
     * 
     * @param dataSet
     *            The data set.
     * @return The dataset as a XML string.
     * @throws DataSetException dse
     */
    public String toString(IDataSet dataSet) throws DataSetException {
        StringWriter sw = new StringWriter();
        XmlDataSetWriter writer = new XmlDataSetWriter(sw);
        writer.write(dataSet);
        return sw.toString();
    }
}
