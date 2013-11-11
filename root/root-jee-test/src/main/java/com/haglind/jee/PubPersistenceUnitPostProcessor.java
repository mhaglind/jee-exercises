package com.haglind.jee;

import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

/**
 * 
 */
public class PubPersistenceUnitPostProcessor implements PersistenceUnitPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(PubPersistenceUnitPostProcessor.class.getName());

    private DataSource jtaDataSource;

    private DataSource nonJtaDataSource;

    @Override
    public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo puInfo) {
        LOG.trace("Entering postProcessPersistenceUnitInfo");
        puInfo.setTransactionType(PersistenceUnitTransactionType.JTA);
        puInfo.setJtaDataSource(jtaDataSource);
        puInfo.setNonJtaDataSource(nonJtaDataSource);
        LOG.trace("Exiting postProcessPersistenceUnitInfo: " + puInfo);
    }

    public void setJtaDataSource(DataSource jtaDataSource) {
        this.jtaDataSource = jtaDataSource;
    }

    public void setNonJtaDataSource(DataSource nonJtaDataSource) {
        this.nonJtaDataSource = nonJtaDataSource;
    }
}
