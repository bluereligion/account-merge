package com.bluereligion.accountmerge.util;

import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.bluereligion.accountmerge.dto.Account;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class AccountMergeUtilsTest {

    private final static Path resourceDirectory = Paths.get("src","test","resources");

    @Test
    public void testAapplyCharSetWithValidParams() throws Exception {
        Charset x = AccountMergeUtils.applyCharSet("UTF-16LE");
        assertTrue(x.toString().equals("UTF-16LE"));

        x = AccountMergeUtils.applyCharSet("US-ASCII");
        assertTrue(x.toString().equals("US-ASCII"));
    }

    @Test
    public void testAapplyCharSetWithInvalidParams() throws Exception {
        Charset x = AccountMergeUtils.applyCharSet("UNKNOWN");
        assertTrue(x.toString().equals("UTF-8"));
    }

    @Test
    public void testPoisonAccount() throws Exception {
        Account p = AccountMergeUtils.getPoisonAccount();
        assertTrue(p.getId().equals(-99999l));
        assertTrue(p.getAccountName().equals("PoisonAccount"));
    }

    @Test
    public void testValidateFileWithNullFileName() throws Exception {
        String errorMsg = new String();
        try {
            AccountMergeUtils.validateFile(null, new Integer(10));
        }
        catch(Exception ex) {
            errorMsg = ex.getMessage();
        }
        assertTrue(errorMsg.equals("Filename is null. Please provide filename."));
    }

    @Test
    public void testValidateFileWithNullMaxFileSize() throws Exception {
        String errorMsg = new String();
        try {
            AccountMergeUtils.validateFile("TEST", null);
        }
        catch(Exception ex) {
            errorMsg = ex.getMessage();
        }
        assertTrue(errorMsg.equals("MaxInboundFileSizeMb has not been set in the config. Please provide a valid configuration value."));
    }

    @Test
    public void testValidateFileWithZeroMaxFileSize() throws Exception {
        String errorMsg = new String();
        try {
            AccountMergeUtils.validateFile("TEST", new Integer(0));
        }
        catch(Exception ex) {
            errorMsg = ex.getMessage();
        }
        assertTrue(errorMsg.equals("MaxInboundFileSizeMb has not been set in the config. Please provide a valid configuration value."));
    }

    @Test
    public void testValidateFileEmpty() throws Exception {
        String errorMsg = new String();
        try {
            AccountMergeUtils.validateFile(Paths.get(resourceDirectory + "/TestEmptyFile.csv").toString(), new Integer(5));
        }
        catch(Exception ex) {
            errorMsg = ex.getMessage();
        }
        assertTrue(errorMsg.equals("Inbound file exists but is empty. Please check the file and verify that it is complete."));
    }

    @Test
    public void testValidateFileLarge() throws Exception {
        String errorMsg = new String();
        try {
            AccountMergeUtils.validateFile(Paths.get(resourceDirectory + "/TestLargeFile.csv").toString(), new Integer(1));
        }
        catch(Exception ex) {
            errorMsg = ex.getMessage();
        }
        assertTrue(errorMsg.equals("Inbound file exceeds size limit of 1048576 MBs.  Please use a smaller file."));
    }

    @Test
    public void testCreateDelimintaedRow() throws Exception {
        Account a = new Account.AccountBuilder()
                .id(100l)
                .accountName("Avengers")
                .firstName("Tony")
                .createdOn("2019-03-07")
                .status("dead")
                .statusSetOn("2019-04-26")
                .build();

        String expected = ("100,Tony,2019-03-07,dead,2019-04-26");
        assertTrue(AccountMergeUtils.createDelimintaedRow(a).equals(expected));
    }

    @Test
    public void testCreateDelimintaedRowWithNullField() throws Exception {
        Account a = new Account.AccountBuilder()
                .id(100l)
                .accountName("Avengers")
                .firstName("Tony")
                .status("dead")
                .statusSetOn("2019-04-26")
                .build();

        String expected = ("100,Tony,,dead,2019-04-26");
        assertTrue(AccountMergeUtils.createDelimintaedRow(a).equals(expected));
    }

    @Test
    public void testCreateDelimintaedRowWitMessage() throws Exception {
        Account a = new Account.AccountBuilder()
                .id(100l)
                .accountName("Avengers")
                .firstName("Tony")
                .status("dead")
                .statusSetOn("2019-04-26")
                .message("Test Message")
                .build();

        String expected = ("100,Tony,,dead,2019-04-26,Test Message");
        assertTrue(AccountMergeUtils.createDelimintaedRow(a).equals(expected));
    }

    @Test
    public void testCreateDelimintaedRowWithNullAccount() throws Exception {
        assertNull(AccountMergeUtils.createDelimintaedRow(null));
    }

    @Test
    public void testIsInboundHeaderRecord() throws Exception {
        String header = ("account id,account name,first name,created on");
        assertTrue(AccountMergeUtils.isInboundHeaderRecord(header));
    }

    @Test
    public void testIsInboundHeaderRecordMixedCase() throws Exception {
        String header = ("acCoUNt ID,account Name,First name,CREATED on");
        assertTrue(AccountMergeUtils.isInboundHeaderRecord(header));

    }

    @Test
    public void testIsInboundHeaderRecordFalse() throws Exception {
        String header = ("acCoUNt IDs,accounts Name,First name,CREATED on");
        assertFalse(AccountMergeUtils.isInboundHeaderRecord(header));

    }

    @Test
    public void testGetInboundHeaderRecord() throws Exception {
        assertTrue(AccountMergeUtils.getOutboundHeaderRecord().equals("Account ID,First Name,Created On,Status,Status Set On"));
    }

    @Test
    public void testScrubLine() throws Exception {
        String s1 = "I am inevitable. I am Iron Man";
        String s2 = ",,,,,,I am inevitable. I am Iron Man";
        String s3 = "I am inevitable. I am Iron Man,,,,,,,,,";
        String s4 = ",,,,,,,I am inevitable. I am Iron Man,,,,,,";

        assertTrue(AccountMergeUtils.scrubLine(s1).equals(s1));
        assertTrue(AccountMergeUtils.scrubLine(s2).equals(s1));
        assertTrue(AccountMergeUtils.scrubLine(s3).equals(s1));
        assertTrue(AccountMergeUtils.scrubLine(s4).equals(s1));
    }

    /*
        Parse Line Tests
        ----------------

        Input: 23232,stark industries,Tony,5-12-2015
        Expected Output: 23232,stark industries,Tony,5-12-2015

        Input: "23232",stark industries,Tony,5-12-2015
        Expected Output: 23232,stark industries,Tony,5-12-2015

        Input: 23232,"stark industries",Tony,5-12-2015
        Expected Output: 23232,stark industries,Tony,5-12-2015

        Input: 23232,stark industries,"Tony",5-12-2015
        Expected Output: 23232,stark industries,Tony,5-12-2015

        Input: 23232,stark industries,Tony,"5-12-2015"
        Expected Output: 23232,stark industries,Tony,5-12-2015

        Input: 23232,stark""industries,Tony,5-12-2015
        Expected Output: 23232,stark industries,Tony,5-12-2015

        Input: 23232,"stark,industries",Tony,5-12-2015
        Expected Output: 23232,stark industries,Tony,5-12-2015
 */

    @Test
    public void testParseLineClean() throws Exception {
        String s1 = "23232,stark industries,Tony,5-12-2015";
        String[] test = AccountMergeUtils.parseLine(s1);

        assertTrue(test.length == 4);
        assertTrue(test[0].equals("23232"));
        assertTrue(test[1].equals("stark industries"));
        assertTrue(test[2].equals("Tony"));
        assertTrue(test[3].equals("5-12-2015"));
    }

    @Test
    public void testParseLineFieldInDoubleQuotes() throws Exception {
        String s1 = "\"23232\",stark industries,Tony,5-12-2015";
        //System.out.println("testParseLineFieldInDoubleQuotes");
        //System.out.println(s1);

        String[] test = (AccountMergeUtils.parseLine(s1));
        //for ( String r : test ) {
        //    System.out.println("r="+r);
        //}

        assertTrue(test.length == 4);
        assertTrue(test[0].equals("23232"));
        assertTrue(test[1].equals("stark industries"));
        assertTrue(test[2].equals("Tony"));
        assertTrue(test[3].equals("5-12-2015"));

        //

        s1 = "23232,\"stark industries\",Tony,5-12-2015";
        //System.out.println("testParseLineFieldInDoubleQuotes");
        //System.out.println(s1);

        test = (AccountMergeUtils.parseLine(s1));
        //for ( String r : test ) {
        //    System.out.println("r="+r);
        //}

        assertTrue(test.length == 4);
        assertTrue(test[0].equals("23232"));
        assertTrue(test[1].equals("stark industries"));
        assertTrue(test[2].equals("Tony"));
        assertTrue(test[3].equals("5-12-2015"));

        //

        s1 = "23232,stark industries,\"Tony\",5-12-2015";
        //System.out.println("testParseLineFieldInDoubleQuotes");
        //System.out.println(s1);

        test = (AccountMergeUtils.parseLine(s1));
        //for ( String r : test ) {
        //    System.out.println("r="+r);
        //}

        assertTrue(test.length == 4);
        assertTrue(test[0].equals("23232"));
        assertTrue(test[1].equals("stark industries"));
        assertTrue(test[2].equals("Tony"));
        assertTrue(test[3].equals("5-12-2015"));


        //

        s1 = "23232,stark industries,Tony,\"5-12-2015\"";
        //System.out.println("testParseLineFieldInDoubleQuotes");
        //System.out.println(s1);

        test = (AccountMergeUtils.parseLine(s1));
        //for ( String r : test ) {
        //    System.out.println("r="+r);
        //}

        assertTrue(test.length == 4);
        assertTrue(test[0].equals("23232"));
        assertTrue(test[1].equals("stark industries"));
        assertTrue(test[2].equals("Tony"));
        assertTrue(test[3].equals("5-12-2015"));

    }

    @Test
    public void testParseLineEscapedDoubleQuote() throws Exception {
        String s1 = "23232,stark\"\"industries,Tony,5-12-2015";
        //System.out.println("testParseLineEscapedDoubleQuote");
        //System.out.println(s1);

        String[] test = (AccountMergeUtils.parseLine(s1));
        //for ( String r : test ) {
        //    System.out.println("r="+r);
        //}

        assertTrue(test.length == 4);
        assertTrue(test[0].equals("23232"));
        assertTrue(test[1].equals("stark\"industries"));
        assertTrue(test[2].equals("Tony"));
        assertTrue(test[3].equals("5-12-2015"));

    }

    @Test
    public void testParseLineDelimiterInsideDoubleQuote() throws Exception {
        String s1 = "23232,\"stark,industries\",Tony,5-12-2015";
        //System.out.println("testParseLineDelimiterInsideDoubleQuote");
        //System.out.println(s1);
        String[] test = (AccountMergeUtils.parseLine(s1));

        //for ( String r : test ) {
        //    System.out.println("r="+r);
        //}

        assertTrue(test.length == 4);
        assertTrue(test[0].equals("23232"));
        assertTrue(test[1].equals("stark,industries"));
        assertTrue(test[2].equals("Tony"));
        assertTrue(test[3].equals("5-12-2015"));

    }

}
