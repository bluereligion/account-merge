package com.bluereligion.accountmerge.service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.bluereligion.accountmerge.consumer.RowConsumer;
import com.bluereligion.accountmerge.consumer.AccountConsumer;
import com.bluereligion.accountmerge.dto.Account;
import com.bluereligion.accountmerge.producer.RowProducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class AccountsServiceProvider {

    private String inboundFilePath;
    protected void setInboundFilePath(final String inboundFilePath) {
        this.inboundFilePath = inboundFilePath;
    }

    private String outboundFilePath;
    protected void setOutboundFilePath(final String outboundFilePath) {
        this.outboundFilePath = outboundFilePath;
    }

    private Charset characterSet = StandardCharsets.UTF_8;
    protected void setCharacterSet(final Charset characterSet) {
        this.characterSet = characterSet;
    }

    private Integer numOfRowConsumers;
    protected void setNumOfRowConsumers(final Integer numOfRowConsumers) {
        this.numOfRowConsumers = numOfRowConsumers;
    }

    private String restStatusApi;
    protected void setRestStatusApi(final String restStatusApi) {
        this.restStatusApi = restStatusApi;
    }

    private BlockingQueue<String> rowQueue = new LinkedBlockingQueue<>(50);
    private BlockingQueue<Account> accountQueue = new LinkedBlockingQueue<>(50);

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountsServiceProvider.class);

    /**
     * The primary service entry point the kicks-off the processing of the inbound .csv file, calls the REST API for additional details than writes to an outbound .csv file.
     * @param inbound - Path to the inbound file.
     * @param outbound - Path to the outbound file.
     * @throws Exception
     */
    protected void processAccounts(String inbound, String outbound) throws Exception {
        ExecutorService singleExecutorService = Executors.newSingleThreadExecutor();
        ExecutorService multipleExecutorService = Executors.newFixedThreadPool(this.numOfRowConsumers);

        Integer rowProducerResult = new Integer(-99);
        Integer rowConsumerResult = new Integer(-99);
        Integer accountConsumerResult = new Integer(-99);

        try {
            LOGGER.debug("Initiating queues and processing Accounts.");

            // Reads lines from file.
            Future<Integer> rowProducerFuture = singleExecutorService.submit(new RowProducer(this.rowQueue, this.inboundFilePath, this.characterSet, this.numOfRowConsumers));
            rowProducerResult = rowProducerFuture.get();

            // Parses rows into account objects and calls REST API.
            for (int i = 0; i < this.numOfRowConsumers; i++) {
                Future<Integer> rowConsumerFuture = multipleExecutorService.submit(new RowConsumer(this.rowQueue, this.accountQueue, this.restStatusApi));
                rowConsumerResult = rowConsumerFuture.get();
            }

            // Writes to outbound .csv file.
            Future<Integer> accountConsumerFuture = singleExecutorService.submit(new AccountConsumer(this.accountQueue, this.outboundFilePath, this.characterSet));
            accountConsumerResult = accountConsumerFuture.get();

            LOGGER.debug(String.format("Result from rowProducerFuture=%d", rowProducerResult));
            LOGGER.debug(String.format("Result from rowConsumerFuture=%d", rowConsumerResult));
            LOGGER.debug(String.format("Result from accountConsumerFuture=%d", accountConsumerResult));

        }
        finally {

            LOGGER.debug("Closing executors.");

            if (singleExecutorService != null)
                singleExecutorService.shutdown();

            if (multipleExecutorService != null)
                multipleExecutorService.shutdown();

            LOGGER.debug("Executors successfully closed.");
        }
        LOGGER.debug("Account processing is completed.");
    }

    private AccountsServiceProvider(AccountsServiceProviderBuilder accountsServiceProviderBuilder) {
        this.inboundFilePath = accountsServiceProviderBuilder.inboundFilePath;
        this.outboundFilePath = accountsServiceProviderBuilder.outboundFilePath;
        this.characterSet = accountsServiceProviderBuilder.characterSet;
        this.numOfRowConsumers = accountsServiceProviderBuilder.numOfRowConsumers;
        this.restStatusApi = accountsServiceProviderBuilder.restStatusApi;
    }

    /**
     * The builder class.
     */
    public static class AccountsServiceProviderBuilder {

        private String inboundFilePath;
        private String outboundFilePath;
        private Charset characterSet;
        private Integer numOfRowConsumers;
        private String restStatusApi;

        public AccountsServiceProviderBuilder inboundFilePath(String inboundFilePath) {
            this.inboundFilePath = inboundFilePath;
            return this;
        }

        public AccountsServiceProviderBuilder outboundFilePath(String outboundFilePath) {
            this.outboundFilePath = outboundFilePath;
            return this;
        }

        public AccountsServiceProviderBuilder characterSet(Charset characterSet) {
            this.characterSet = characterSet;
            return this;
        }

        public AccountsServiceProviderBuilder numOfRowConsumers(Integer numOfRowConsumers) {
            this.numOfRowConsumers = numOfRowConsumers;
            return this;
        }

        public AccountsServiceProviderBuilder restStatusApi(String restStatusApi) {
            this.restStatusApi = restStatusApi;
            return this;
        }

        public AccountsServiceProvider build() {
            return new AccountsServiceProvider(this);
        }

    }

}
