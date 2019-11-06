package com.bluereligion.accountmerge.consumer;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.BlockingQueue;

import com.google.common.base.Strings;

import com.bluereligion.accountmerge.util.AccountMergeUtils;
import com.bluereligion.accountmerge.client.AccountStatusClient;
import com.bluereligion.accountmerge.dto.Account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Responsible for reading the raw lines from a queue and parsing them into accounts.
 * Will then call the Account REST API to request additional information.
 * Places the completed accounts into another queue to be writtent to a file.
 *
 * @see https://docs.oracle.com/javase/8/docs/api/index.html?java/util/concurrent/Callable.html
 * @see https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/BlockingQueue.html
 */
public class RowConsumer
        implements Callable<Integer> {

    private BlockingQueue<String> rowsQueue;
    private BlockingQueue<Account> accountsQueue;
    private AccountStatusClient accountStatusClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(RowConsumer.class);


    /**
     * Initializes the consumer.
     * @param rowsQueue - The queue to read from.
     * @param accountsQueue - The queue to write to.
     * @param restStatusApi - The url to the REST Api.
     */
    public RowConsumer(BlockingQueue<String> rowsQueue, BlockingQueue<Account> accountsQueue, String restStatusApi) {
        this.rowsQueue = rowsQueue;
        this.accountsQueue = accountsQueue;
        this.accountStatusClient = new AccountStatusClient(restStatusApi);
    }


    /**
     * Runs the process.  Reads from the queue and writes to the outbound file until the queue is completed.
     * @return 0 = Sucess;
     * @throws InterruptedException
     */
    public Integer call() throws InterruptedException {

        while ( true ) {
            String s = rowsQueue.take();
            LOGGER.debug(String.format("Next item taken from queue=%s",s));

            if ( !Strings.isNullOrEmpty(s) && s.equals(AccountMergeUtils.EOF_MARKER) ) {
                LOGGER.debug("End of rows queue has been reached, returning.");
                accountsQueue.put(AccountMergeUtils.getPoisonAccount());
                break;
            }

            final Account account = createAccount(s);

            LOGGER.debug(String.format("Adding account to queue=%s",account));
            if ( !Objects.isNull(account) ) accountsQueue.put(account);
            else LOGGER.debug("Account returned from createAccount was null.");
        }
        return 0; // success
    }


    /**
     * @param s - The comma delimited row from the inbound file.
     * @return - The fully loaded account with the available details.
     */
    private final Account createAccount(String s) {

        LOGGER.debug(String.format("Creating account using s=%s",s));
        if ( Strings.isNullOrEmpty(s) ) return null;

        Account account = AccountMergeUtils.parseAccount(s);
        if ( Objects.isNull(account) ) {
            LOGGER.debug(String.format("Account failed to parse and was returned as null using this string input=%s", s));
            return null;
        }

        if ( Objects.isNull(account.getId()) || account.getId() < 1 ) {
            String msg = String.format("Account does not have a valid ID=%s", account.getId());
            LOGGER.debug(msg);
            account.setMessage(msg);
            return account;
        }

        try {
            account = this.accountStatusClient.callService(account);
        }
        catch(Exception ex) {
            account.setMessage(ex.getMessage());
        }
        return account;
    }

}
