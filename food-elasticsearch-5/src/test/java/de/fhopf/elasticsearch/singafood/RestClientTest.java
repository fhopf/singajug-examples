package de.fhopf.elasticsearch.singafood;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.emptyMap;
import static org.apache.logging.log4j.core.layout.PatternLayout.*;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.junit.Assert.assertEquals;

/**
 * Indexes and searches content using the REST Client.
 */
public class RestClientTest {

	private String indexName = "singafood-" + System.currentTimeMillis();

	private ObjectMapper mapper = new ObjectMapper();

	private static final String QUERY = "{ \"query\": { \"match_all\": {} } }";

	private RestClient createClient() {
		return RestClient.builder(new HttpHost("localhost", 9200)).build();
	}

	@After
	public void deleteIndex() throws IOException {
		createClient().performRequest("DELETE", "/" + indexName);
	}

	@Test
	public void indexAndSearch() throws IOException {
		RestClient client = createClient();

		assertTestCluster(client);
		index(client);
		search(client);
	}

	private void assertTestCluster(RestClient client) throws IOException {

		Response response = client.performRequest("GET", "/");
		assertEquals(200, response.getStatusLine().getStatusCode());
		JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity()));

		String clusterName = json.getString("cluster_name");
		assertEquals("my-test-cluster", clusterName);
	}

	private void index(RestClient client) throws IOException {
		Dish dish = new Dish();
		dish.setFood("Roti Prata");
		dish.setTags(Arrays.asList("vegetarian", "curry"));
		Favorite favorite = new Favorite();
		favorite.setLocation("Tiong Bahru");
		favorite.setPrice(3.0);
		dish.setFavorite(favorite);

		String json = mapper.writeValueAsString(dish);
		NStringEntity entity = new NStringEntity(json);

		Response response = client.performRequest("POST", "/" + indexName + "/food/", emptyMap(), entity);
		assertEquals(201, response.getStatusLine().getStatusCode());

		client.performRequest("POST", "/" + indexName + "/_refresh");
	}

	private void search(RestClient client) throws IOException {

		// query using the existing String
		NStringEntity entity = new NStringEntity(QUERY);
		Response response = client.performRequest("POST", "/" + indexName + "/food/_search", emptyMap(), entity);
		assertContainsHit(response);

		// build query using Elasticsearch classes
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(matchQuery("food", "roti"));
		entity = new NStringEntity(searchSourceBuilder.toString());
		response = client.performRequest("POST", "/" + indexName + "/food/_search", emptyMap(), entity);
		assertContainsHit(response);
	}

	private void assertContainsHit(Response response) throws IOException {
		JSONObject jsonResponse = new JSONObject(EntityUtils.toString(response.getEntity()));
		JSONArray array = jsonResponse.getJSONObject("hits").getJSONArray("hits");
		assertEquals(1, array.length());
		JSONObject source = array.getJSONObject(0).getJSONObject("_source");
		assertEquals("Roti Prata", source.getString("food"));

	}

}
