package com.bluereligion.accountmerge.client;

import java.util.Objects;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import com.bluereligion.accountmerge.dto.Account;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The AccountStatusClient is responsible for handling the interaction with the Account REST
 * api service.
 *
 */
public class AccountStatusClient {

    private String serviceUrl;
    private RestTemplate restTemplate = new RestTemplate();

    private static final String STATUS_KEY = "status";
    private static final String CREATED_ON_KEY = "created_on";

    private static Logger LOGGER = LoggerFactory.getLogger(AccountStatusClient.class);
    private static String GET_ACCOUNT_STATUS_PATH = "%s/v1/accounts/%d";


    /**
     * Constructor taking the url to the service - ie "http://localhost:8080".
     * @param serviceUrl
     */
    public AccountStatusClient(String serviceUrl) {
        super();
        this.serviceUrl = serviceUrl;
    }


    /**
     * Calls the service using the account provided.
     * @param account
     * @return - The account with the additional details added.
     * @throws HttpServerErrorException - Thrown if other than a successful response returned.
     * @throws IllegalArgumentException - Thrown if the account isn't found or is missing the information needed to complete the request.
     */
    public Account callService(Account account) throws HttpServerErrorException, IllegalArgumentException {

        if ( Objects.isNull(account) || Objects.isNull(account.getId()) ) throw new IllegalArgumentException("Account parameter passed to callService is either null or has a null id.");

        String uri = formulateUrl(account.getId());
        LOGGER.debug("uri="+uri);

        ResponseEntity<JsonNode> response = this.restTemplate.getForEntity(uri, JsonNode.class);
        LOGGER.debug("Response="+response);

        if ( Objects.isNull(response) ||  Objects.isNull(response.getBody()) ) {
            String msg = String.format("No body from API request received for account=%s", account.toString());
            LOGGER.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if ( !response.getStatusCode().is2xxSuccessful() ) {
            throw new HttpServerErrorException(response.getStatusCode(), ( response.getBody() != null ) ? response.getBody().toString() : "");
        }

        return processResponse(account, response.getBody());

    }

    /**
     * Processes the response returned by a call to the REST service
     * @param account - The account requiring additional information.
     * @param body - The response body returned from the service.
     * @return - The account with the additional details added.
     */
    protected Account processResponse(Account account, JsonNode body) {
        LOGGER.debug(String.format("Processing rest response. Body=%s",body.toString()));
        LOGGER.debug(String.format("Processing rest response. Account=%s",account.toString()));

        if ( body.hasNonNull(STATUS_KEY) )
            account.setStatus(body.get(STATUS_KEY).asText());

        if ( body.hasNonNull(CREATED_ON_KEY) )
            account.setStatusSetOn(body.get(CREATED_ON_KEY).asText());

        return account;
    }



    protected String formulateUrl(Long accountId) {
        return String.format(GET_ACCOUNT_STATUS_PATH, serviceUrl, accountId);
    }

    protected void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

}
