package com.bluereligion.accountmerge.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Strings;
import com.google.common.base.CharMatcher;

import com.bluereligion.accountmerge.dto.Account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A utilities class that fulfills many of the low-level details to support the service and parsing .csv files.
 */
public class AccountMergeUtils {

    public static final String EOF_MARKER = "--[EOF MARKER]--";
    private static final String DELIMITER = ",";
    private static final char QUOTE = '"';
    private static final int KILOBYTES = 1024;
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountMergeUtils.class);

    /**
     * Apply the desired Character encoding permissible in Java.
     * @param encoding
     * @return
     */
    public static Charset applyCharSet(String encoding) {

        if (Objects.isNull(encoding)) return StandardCharsets.UTF_8;

        switch (encoding) {
            case "UTF-8":
                return StandardCharsets.UTF_8;
            case "UTF-16LE":
                return StandardCharsets.UTF_16LE;
            case "UTF-16BE":
                return StandardCharsets.UTF_16BE;
            case "UTF-16":
                return StandardCharsets.UTF_16;
            case "US-ASCII":
                return StandardCharsets.US_ASCII;
            case "ISO-8859-1":
                return StandardCharsets.ISO_8859_1;
            default:
                return StandardCharsets.UTF_8;
        }

    }

    /**
     * Formulates the poison account used by the queues in determining when the data processing has completed.
     * @return - the poison account.
     */
    public static Account getPoisonAccount() {
        return new Account.AccountBuilder()
                .id(-99999l)
                .accountName("PoisonAccount")
                .build();
    }

    /**
     * A file is validated:
     *      1. The fileName param isn't null or empty.
     *      2. Is under the allowable filesize limit.
     *      3. The file actually exists.
     *      4. The file is not empty.
     *      5. The file is readble by the application.
     *
     * @param fileName - File to validate
     * @param maxInboundFileSizeMb - Allowable filesize in megabytes,
     * @return - If the file is deemed valid.
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public static boolean validateFile(String fileName, Integer maxInboundFileSizeMb) throws IllegalArgumentException, IOException {

        if (Strings.isNullOrEmpty(fileName))
            throw new IllegalArgumentException("Filename is null. Please provide filename.");

        if (Objects.isNull(maxInboundFileSizeMb) || maxInboundFileSizeMb == 0)
            throw new IllegalArgumentException("MaxInboundFileSizeMb has not been set in the config. Please provide a valid configuration value.");

        if (!Files.exists(Paths.get(fileName))) {
            throw new IllegalArgumentException(String.format("Inbound file %s could not be located. Please ensure that the file exists and is correctly named.", fileName));
        }

        if (FileChannel.open(Paths.get(fileName)).size() == 0) {
            throw new IllegalArgumentException("Inbound file exists but is empty. Please check the file and verify that it is complete.");
        }

        long limit = Long.valueOf(maxInboundFileSizeMb * KILOBYTES * KILOBYTES);
        if (FileChannel.open(Paths.get(fileName)).size() > limit) {
            throw new IllegalArgumentException(String.format("Inbound file exceeds size limit of %s MBs.  Please use a smaller file.", limit));
        }

        if (!Files.isReadable(Paths.get(fileName))) {
            throw new IllegalArgumentException(String.format("Inbound file %s was found but is not readable. Please ensure that the file has correct permission.", fileName));
        }

        return true;
    }


    /**
     * Returns a comma delimited string representing the account.
     * @param account - Account to convert to a comma-delimited string.
     * @return - The delimited string.
     */
    public static String createDelimintaedRow(Account account) {
        if (Objects.isNull(account)) return null;

        StringBuilder sb = new StringBuilder();

        if (!Objects.isNull(account.getId())) sb.append(account.getId());
        sb.append(',');

        if (!Strings.isNullOrEmpty(account.getFirstName())) sb.append(account.getFirstName());
        sb.append(',');

        if (!Strings.isNullOrEmpty(account.getCreatedOn())) sb.append(account.getCreatedOn());
        sb.append(',');

        if (!Strings.isNullOrEmpty(account.getStatus())) sb.append(account.getStatus());
        sb.append(',');

        if (!Strings.isNullOrEmpty(account.getStatusSetOn())) sb.append(account.getStatusSetOn());

        if (!Strings.isNullOrEmpty(account.getMessage())) {
            sb.append(',').append(account.getMessage());
        }
        return sb.toString();
    }


    /**
     * Determines if the row is a header record.
     * @param s - The string to test.
     * @return - If s is a header record.
     */
    public static boolean isInboundHeaderRecord(String s) {
        s = s.trim().toLowerCase();
        if (s.startsWith("account id,account name,first name,created on")) return true;
        else return false;
    }

    /**
     * Returns a comma-delimited header row.
     * @return
     */
    public static String getOutboundHeaderRecord() {
        return "Account ID,First Name,Created On,Status,Status Set On";
    }


    /**
     * Parses a comma delimited row.
     * @param s - The string to parse.
     * @return - The parsed account represented by s.
     */
    public static Account parseAccount(String s) {

        LOGGER.debug(String.format("Parsing Account using%s", s));
        if ( Strings.isNullOrEmpty(s) || !s.contains(DELIMITER )) return null;

        LOGGER.debug("Delimiter found and removing extra spaces and carriage returns");
        s = scrubLine(s);
        LOGGER.debug(String.format("s after removing extra spaces and crria returns%s",s));

        String[] accountInfo = parseLine(s);

        return new Account.AccountBuilder()
                .id(Long.parseLong(accountInfo[0]))
                .accountName(accountInfo[1])
                .firstName(accountInfo[2])
                .createdOn(accountInfo[3])
                .build();

    }

    /**
     * Cleans up line by removing extraneous line breaks and leading and trailing commas.
     * @param data - String to clean.
     * @return - The cleaned string
     */
    protected static String scrubLine(String data) {
        String escapedData = data.replaceAll("\\R", ""); // Any Unicode linebreak sequence
        escapedData = CharMatcher.is(',').trimLeadingFrom(escapedData);  // Remove leading ,
        escapedData = CharMatcher.is(',').trimTrailingFrom(escapedData); // Remove trailing ,
        return escapedData;
    }


    /**
     * Remedies many of the common issues associated with .csv processing.
     *
     *  Clean
     *  -----
     *  Input: 23232,stark industries,Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Uneeded Quotatations
     *  --------------------
     *  Input: "23232",stark industries,Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Input: 23232,"stark industries",Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Input: 23232,stark industries,"Tony",5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Input: 23232,stark industries,Tony,"5-12-2015"
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *
     *  Escaped Comma
     *  -------------
     *  Input: 23232,stark""industries,Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *  Escaped Delimiter
     *  -----------------
     *  Input: 23232,"stark,industries",Tony,5-12-2015
     *  Expected Output: 23232,stark industries,Tony,5-12-2015
     *
     *
     * @param s - The string to parse.
     * @return An array representing the account's data elements.
     */
    protected static String[] parseLine(String s) {
        LOGGER.debug(String.format("Begin parsing lines=%s",s));
        List<String> values = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        char[] chars = s.toCharArray();

        boolean insideQuote = false;
        int charsReadSinceOpenQuote = 0;

        for ( char c : chars ) {
            LOGGER.debug(String.format("Walking chars-c=%s, insideQuote=%s",c,insideQuote));
            if ( c == QUOTE ) {
                LOGGER.debug("Found a quote - insideQuote="+insideQuote+", charsReadSinceOpenQuote="+charsReadSinceOpenQuote);
                if ( insideQuote ) { // Inside an open quote
                    LOGGER.debug("Inside Open Quote.");
                    if ( charsReadSinceOpenQuote == 0 ) { // Quote escaped by open quote - """
                        LOGGER.debug("Escaped Quote found.");
                        sb.append(c);
                        charsReadSinceOpenQuote++;
                        insideQuote = false;
                    }
                    else {
                        LOGGER.debug("End Quote found.");
                        insideQuote = false;
                    }
                }
                else {
                    insideQuote = true;
                    LOGGER.debug("Open Quote Found");}
            }
            else {
                if ( c == DELIMITER.charAt(0) && !insideQuote ) { // To handle a comma inside an open quote
                    LOGGER.debug("Handling a comma inside an open quote.");
                    values.add(sb.toString());
                    sb.setLength(0);
                    insideQuote = false;
                    charsReadSinceOpenQuote = 0;
                }
                else {
                    sb.append(c);
                    if ( insideQuote ) charsReadSinceOpenQuote++;
                }
            }

        }
        values.add(sb.toString());
        return values.stream().toArray(String[]::new);
    }

}
