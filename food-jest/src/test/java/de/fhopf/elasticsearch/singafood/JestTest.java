package de.fhopf.elasticsearch.singafood;

import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.cluster.NodesInfo;
import io.searchbox.core.*;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.Refresh;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Indexes and searches content using the REST Client.
 */
public class JestTest {

	public static final String FOOD = "Bak Kut Teh";
	public static final String LOCATION = "Founder";
	private String indexName = "singafood-" + System.currentTimeMillis();

	private static final String MATCH_ALL_QUERY = "{ \"query\": { \"match_all\": {} } }";
	private static final String MATCH_QUERY = "{ \"query\": { \"match\": { \"food\": \"bak kut teh\"} } }";

	private static final Logger LOG = LoggerFactory.getLogger(JestTest.class);

	private JestClient createClient() {
		JestClientFactory factory = new JestClientFactory();
		factory.setHttpClientConfig(new HttpClientConfig
				.Builder("http://localhost:9200")
				.multiThreaded(true)
				.build());
		JestClient client = factory.getObject();
		return client;
	}

	@After
	public void deleteIndex() throws IOException {
		createClient().execute(new DeleteIndex.Builder(indexName).build());
	}

	@Test
	public void indexAndSearch() throws IOException, InterruptedException {
		JestClient client = createClient();

		assertTestCluster(client);
		index(client);
		searchAsynchronously(client);
		search(client);
	}

	private void assertTestCluster(JestClient client) throws IOException {

		NodesInfo nodesInfo = new NodesInfo.Builder().build();

		JsonObject json = client.execute(nodesInfo).getJsonObject();

		String clusterName = json.getAsJsonPrimitive("cluster_name").getAsString();
		assertEquals("my-test-cluster", clusterName);
	}

	private void index(JestClient client) throws IOException {
		Dish dish = new Dish();
		dish.setFood(FOOD);
		dish.setTags(Arrays.asList("pork", "soup"));
		Favorite favorite = new Favorite();
		favorite.setLocation(LOCATION);
		favorite.setPrice(8.0);
		dish.setFavorite(favorite);

		DocumentResult indexResult = client.execute(new Index.Builder(dish).index(indexName).type("dish").build());
		assertTrue(indexResult.isSucceeded());
		String id = indexResult.getId();

		DocumentResult getResult = client.execute(new Get.Builder(indexName, id).build());
		Dish dishResult = getResult.getSourceAsObject(Dish.class);

		assertEquals(id, dishResult.getId());
		assertEquals(dish.getFood(), dishResult.getFood());
		assertEquals(dish.getFavorite().getPrice(), dishResult.getFavorite().getPrice());

		client.execute(new Refresh.Builder().addIndex(indexName).build());
	}

	private void search(JestClient client) throws IOException {

		Search searchRequest = new Search.Builder(MATCH_ALL_QUERY).addIndex(indexName).build();
		SearchResult searchResult = client.execute(searchRequest);

		assertEquals(Integer.valueOf(1), searchResult.getTotal());

		SearchResult.Hit<Dish, Void> firstHit = searchResult.getFirstHit(Dish.class);
		Dish dish = firstHit.source;

		assertNotNull(dish.getId());
		assertEquals(FOOD, dish.getFood());
		assertEquals(LOCATION, dish.getFavorite().getLocation());

	}

	private void searchAsynchronously(JestClient client) throws InterruptedException {

		Search searchRequest = new Search.Builder(MATCH_QUERY)
				.addIndex(indexName)
				.build();

		// asynchronous version doesn't wait obviously
		final CountDownLatch latch = new CountDownLatch(1);

		client.executeAsync(searchRequest, new JestResultHandler<SearchResult>() {
			@Override
			public void completed(SearchResult result) {
				LOG.info("Got result: " + result.getJsonString());
				latch.countDown();
			}

			@Override
			public void failed(Exception ex) {
				LOG.error(ex.getMessage(), ex);
				latch.countDown();
			}
		});

		latch.await(2000, TimeUnit.MILLISECONDS);
	}

}
