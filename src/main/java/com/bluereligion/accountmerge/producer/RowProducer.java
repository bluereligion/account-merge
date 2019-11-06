package com.bluereligion.accountmerge.producer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import com.google.common.base.Strings;

import com.bluereligion.accountmerge.util.AccountMergeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Responsible for reading the raw lines from an inbound .csv file.
 * It suppresses null and empty rows and those that appear to be header rows.
 * Places the rows onto another queue to be processed into accounts.
 *
 * @see https://docs.oracle.com/javase/8/docs/api/index.html?java/util/concurrent/Callable.html
 * @see https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/BlockingQueue.html
 */
public class RowProducer
        implements Callable<Integer> {

    private String inboundFilePath;
    private BlockingQueue<String> rowssQueue;
    private Charset characterSet;
    private Integer numOfRowConsumers;

    private static final Logger LOGGER = LoggerFactory.getLogger(RowProducer.class);


    /**
     * Initializes the producer.
     * @param rowssQueue - The queue to place the inbound rows read from the file.
     * @param inboundFilePath - The path to the inbound file.
     * @param characterSet - The characterset to apply.
     * @param numOfRowConsumers - The number of row consumers that will be run. This is used in shutting down the queue gracefull when all rows have been read.
     */
    public RowProducer(BlockingQueue<String> rowssQueue, String inboundFilePath, Charset characterSet, Integer numOfRowConsumers) {
        this.rowssQueue = rowssQueue;
        this.inboundFilePath = inboundFilePath;
        this.characterSet = characterSet;
        this.numOfRowConsumers = numOfRowConsumers;
    }

    /**
     * Runs the process.  Reads from the inbound file and places them onto an account queue for further processing.
     * @return 0 = Sucess;
     * @throws InterruptedException
     * @throws IOException - Any issue arising from writing to the inbound file.
     */
    public Integer call() throws InterruptedException, IOException {
        LOGGER.debug("Inbound process beginning.");
        LOGGER.debug(String.format("Using inboundFilePath=%s and characterSet=%s", inboundFilePath, characterSet.toString()));

        String line;
        try ( BufferedReader br = Files.newBufferedReader(Paths.get(inboundFilePath), characterSet) ) {
            while ((line = br.readLine()) != null) {
                LOGGER.debug(String.format("Line read from file=%s",line));

                if ( !Strings.isNullOrEmpty(line) && !AccountMergeUtils.isInboundHeaderRecord(line)) //prevent any null/blank lines & header record
                    this.rowssQueue.put(line);
            }
            for (int i = 0; i < numOfRowConsumers; i++ ) {
                LOGGER.debug("EOF has been reached. Adding marker to the queue.");
                rowssQueue.put(AccountMergeUtils.EOF_MARKER);
            }
        }
        return 0; // success
    }

}
