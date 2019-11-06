package com.bluereligion.accountmerge.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.*;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.bluereligion.accountmerge.dto.Account;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class AccountStatusClientTest {

    @Mock
    protected RestTemplate restTemplate;

    @InjectMocks
    private AccountStatusClient asc = new AccountStatusClient("http://localhost:8080");
    private static final String serviceUrl = "/v1/accounts/";


    @Test
    public void testFormulateUrlClean() throws Exception {
        String expected = "http://localhost:8080/v1/accounts/12345";
        assertTrue(expected.equals(asc.formulateUrl(12345l)));
    }

    @Test
    public void testprocessResponseClean() throws Exception {

        String node = "{\"account_id\":23232,\"status\":\"poor\",\"created_on\":\"2015-06-07\"}";
        Account expected = new Account.AccountBuilder()
                .id(23232l)
                .accountName("stark industries")
                .firstName("Tony")
                .createdOn("5-12-2015")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(node);

        Account actual = asc.processResponse(expected, json);
        expected.setStatus("poor");
        expected.setStatusSetOn("2015-06-07");

        assertTrue(actual.equals(expected));
    }

    @Test
    public void testprocessResponseWithNullStatus() throws Exception {

        String node = "{\"account_id\":23232,\"created_on\":\"2015-06-07\"}";
        Account expected = new Account.AccountBuilder()
                .id(23232l)
                .accountName("stark industries")
                .firstName("Tony")
                .createdOn("5-12-2015")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(node);

        Account actual = asc.processResponse(expected, json);
        expected.setStatusSetOn("2015-06-07");

        assertTrue(actual.equals(expected));
    }

    @Test
    public void testprocessResponseWithNullStatusCreadtedOn() throws Exception {

        String node = "{\"account_id\":23232,\"status\":\"poor\"}";
        Account expected = new Account.AccountBuilder()
                .id(23232l)
                .accountName("stark industries")
                .firstName("Tony")
                .createdOn("5-12-2015")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(node);

        Account actual = asc.processResponse(expected, json);
        expected.setStatus("poor");

        assertTrue(actual.equals(expected));
    }

    @Test
    public void testCallServiceClean() throws Exception {

        String node = "{\"account_id\":23232,\"status\":\"poor\",\"created_on\":\"2015-06-07\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(node);

        Account expected = new Account.AccountBuilder()
                .id(23232l)
                .accountName("stark industries")
                .firstName("Tony")
                .createdOn("5-12-2015")
                .status("poor")
                .statusSetOn("2015-06-07")
                .build();

        ResponseEntity<JsonNode> responseEntity = new ResponseEntity<JsonNode>(json, HttpStatus.OK);

        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), ArgumentMatchers.any(Class.class)))
                .thenReturn(responseEntity);

        Account actual = asc.callService(expected);
        assertTrue(actual.equals(expected));

    }

    @Test
    public void testCallServiceWithHttpServerErrorException() throws Exception {

        String node = "{\"account_id\":23232,\"status\":\"poor\",\"created_on\":\"2015-06-07\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(node);

        Account expected = new Account.AccountBuilder()
                .id(23232l)
                .accountName("stark industries")
                .firstName("Tony")
                .createdOn("5-12-2015")
                .status("poor")
                .statusSetOn("2015-06-07")
                .build();

        ResponseEntity<JsonNode> responseEntity = new ResponseEntity<JsonNode>(json, HttpStatus.SERVICE_UNAVAILABLE);

        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), ArgumentMatchers.any(Class.class)))
                .thenReturn(responseEntity);

        String errorMsg = new String();
        boolean classMatch = false;

        try {
            Account actual = asc.callService(expected);
        } catch (Exception ex) {
            if (ex instanceof HttpServerErrorException) classMatch = true;
            errorMsg = ex.getMessage();
        }
        System.out.println("errorMsg="+errorMsg);
        assertTrue(errorMsg.equals("503 {\"account_id\":23232,\"status\":\"poor\",\"created_on\":\"2015-06-07\"}"));
        assertTrue(classMatch);
    }

    @Test
    public void testCallServiceWithIllegalArgumentException() throws Exception {

        String errorMsg = new String();
        boolean classMatch = false;
        try {
            Account actual = asc.callService(null);
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException) classMatch = true;
            errorMsg = ex.getMessage();
        }
        assertTrue(errorMsg.equals("Account parameter passed to callService is either null or has a null id."));
        assertTrue(classMatch);
    }

    @Test
    public void testCallServiceWithNoResponse() throws Exception {

        Account expected = new Account.AccountBuilder()
                .id(23232l)
                .accountName("stark industries")
                .firstName("Tony")
                .createdOn("5-12-2015")
                .status("poor")
                .statusSetOn("2015-06-07")
                .build();

        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), ArgumentMatchers.any(Class.class)))
                .thenReturn(null);

        String errorMsg = new String();
        boolean classMatch = false;

        try {
            Account actual = asc.callService(expected);
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException) classMatch = true;
            errorMsg = ex.getMessage();
        }
        assertTrue(errorMsg.equals("No body from API request received for account=Account{id=23232, accountName='stark industries', firstName='Tony', createdOn='5-12-2015', status='poor', statusSetOn='2015-06-07', message='null'}"));
        assertTrue(classMatch);
    }

}
