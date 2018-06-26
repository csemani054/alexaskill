package com.providersearch.alexa;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.providersearch.alexa.exceptions.SearchClientException;
import com.providersearch.alexa.exceptions.UnauthorizedException;

public class SearchClient {

	private static final Logger log = LoggerFactory.getLogger(SearchClient.class);
	
	private static final String BASE_API_PATH = "https://my-json-server.typicode.com/csemani054/demo/providerDetails";
    
    public static List<ProviderSearch> getProviderSearch() throws SearchClientException, UnauthorizedException {
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();

        String requestUrl = BASE_API_PATH;
        log.info("Request will be made to the following URL: {}", requestUrl);

        HttpGet httpGet = new HttpGet(requestUrl);

//        httpGet.addHeader("Authorization", "Bearer " + apiAccessToken);
        
        httpGet.addHeader("Content-Type", "application/json");
        
        List<ProviderSearch> provSearch;
        try {
            HttpResponse searchResponse = closeableHttpClient.execute(httpGet);
            int statusCode = searchResponse.getStatusLine().getStatusCode();

            log.info("The Search Client API responded with a status code of {}", statusCode);

            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity httpEntity = searchResponse.getEntity();
                String responseBody = EntityUtils.toString(httpEntity);

                log.info("responseBody {}", responseBody);
                
                ObjectMapper objectMapper = new ObjectMapper();
                provSearch = objectMapper.readValue(responseBody, new TypeReference<List<ProviderSearch>>(){});
                
            } else if (statusCode == HttpStatus.SC_FORBIDDEN) {
                log.info("Failed to authorize with a status code of {}", statusCode);
                throw new UnauthorizedException("Failed to authorize.");
            } else {
                String errorMessage = "Search Client API query failed with status code of " + statusCode;
                log.info(errorMessage);
                throw new SearchClientException(errorMessage);
            }
        }  catch (IOException e) {
            throw new SearchClientException(e);
        } finally {
            log.info("Request to Address Device API completed.");
        }

        return provSearch;
    }
}
