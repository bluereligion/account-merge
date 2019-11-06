package com.bluereligion.accountmerge.service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.bluereligion.accountmerge.util.AccountMergeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is a wrapper around the AccountsServiceProvider and handles the configuration for thw service.
 * This implenetation applies the configuration patterns provided by the springframework.
 */
@Component("accountsService")
@Scope("singleton")
@Configuration
public class AccountsService {

    @Value("${application.encoding}")
    private String encoding;
    protected void setEncoding(final String encoding) { this.encoding = encoding; }

    @Value("${application.restStatusApi}")
    private String restStatusApi;
    protected void setRestStatusApi(final String restStatusApi) { this.restStatusApi = restStatusApi; }

    @Value("${application.maxInboundFileSizeMb}")
    private Integer maxInboundFileSizeMb;
    protected void setMaxInboundFileSizeMb(final Integer maxInboundFileSizeMb) { this.maxInboundFileSizeMb = maxInboundFileSizeMb; }

    @Value("${application.numOfRowConsumers}")
    private Integer numOfRowConsumers;
    protected void setNumOfRowConsumers(final Integer numOfRowConsumers) { this.numOfRowConsumers = numOfRowConsumers; }

    private Charset characterSet = StandardCharsets.UTF_8;
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountsService.class);


    /**
     * Kicks off the primary service.
     * @param inboundFilePath - Path to the inbound file.
     * @param outboundFilePath - Path to the outbound file.
     * @throws Exception
     */
    public void processAccounts(String inboundFilePath, String outboundFilePath) throws Exception {

        AccountMergeUtils.validateFile(inboundFilePath, maxInboundFileSizeMb);

        AccountsServiceProvider accountsServiceProvider = new AccountsServiceProvider.AccountsServiceProviderBuilder()
                .inboundFilePath(inboundFilePath)
                .outboundFilePath(outboundFilePath)
                .numOfRowConsumers(this.numOfRowConsumers)
                .characterSet(this.characterSet)
                .restStatusApi(this.restStatusApi)
                .build();

        accountsServiceProvider.processAccounts(inboundFilePath, outboundFilePath);

    }

    @PostConstruct
    protected void init() {
        this.characterSet = AccountMergeUtils.applyCharSet(encoding);
        LOGGER.debug("***************************************************");
        LOGGER.debug("AccountsService:");
        LOGGER.debug("   encoding="+encoding);
        LOGGER.debug("   restStatusApi="+restStatusApi);
        LOGGER.debug("   maxInboundFileSizeMb="+maxInboundFileSizeMb);
        LOGGER.debug("   numOfRowConsumers="+numOfRowConsumers);
        LOGGER.debug("***************************************************");
    }

}
