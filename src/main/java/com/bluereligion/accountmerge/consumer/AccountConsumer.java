package com.bluereligion.accountmerge.consumer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.BlockingQueue;

import com.google.common.base.Strings;

import com.bluereligion.accountmerge.util.AccountMergeUtils;
import com.bluereligion.accountmerge.dto.Account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class writes the account information to a file in .csv format and reading from a queue.
 * This is implemented by implementing Callable and using a BlockingQueue.
 *
 * @see https://docs.oracle.com/javase/8/docs/api/index.html?java/util/concurrent/Callable.html
 * @see https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/BlockingQueue.html
 */
public class AccountConsumer
        implements Callable<Integer> {

    private String outboundFilePath;
    private Charset characterSet;
    private BlockingQueue<Account> accountsQueue;

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountConsumer.class);


    /**
     * Initializes the consumer.
     * @param accountsQueue - The queue to read from.
     * @param outboundFilePath - The file to write to.
     * @param characterSet - The characterset to apply.
     */
    public AccountConsumer(BlockingQueue<Account> accountsQueue, String outboundFilePath, Charset characterSet) {
        this.accountsQueue = accountsQueue;
        this.outboundFilePath = outboundFilePath;
        this.characterSet = characterSet;
    }

    /**
     * Runs the process.  Reads from the queue and writes to the outbound file until the queue is completed.
     * @return 0 = Sucess;
     * @throws InterruptedException
     * @throws IOException - Any issue arising from writing to the outbound file.
     */
    public Integer call() throws InterruptedException, IOException {
        LOGGER.debug("Inbound process beginning.");
        LOGGER.debug(String.format("Using outboundFilePath=%s and characterSet=%s", outboundFilePath, characterSet.toString()));

        Account account;
        try ( BufferedWriter writer = Files.newBufferedWriter(Paths.get(outboundFilePath), characterSet) ) {

            writer.write(AccountMergeUtils.getOutboundHeaderRecord());
            writer.newLine();

            while (true) {
                account = accountsQueue.take();
                LOGGER.debug(String.format("Next item taken from queue=%s",account));

                if ( account.equals(AccountMergeUtils.getPoisonAccount()) ) {
                    LOGGER.debug("End of account queue has been reached, returning.");
                    writer.close();
                    break;
                }

                String s = AccountMergeUtils.createDelimintaedRow(account);
                LOGGER.debug(String.format("Writing the following account information to outbound=%s",s));

                if ( !Strings.isNullOrEmpty(s) ) {
                    writer.write(s);
                    writer.newLine();
                }
            }
        }
        return 0; // success
    }

}
