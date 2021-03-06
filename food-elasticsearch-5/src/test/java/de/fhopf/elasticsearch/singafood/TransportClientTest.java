package de.fhopf.elasticsearch.singafood;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.elasticsearch.action.DocWriteResponse.Result.CREATED;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransportClientTest {

	private final String indexName = "singafood-" + System.currentTimeMillis();

	Settings settings = Settings.builder()
			.put("client.transport.sniff", true)
			.build();

	@After
	public void deleteIndex() {
		createClient().admin().indices().prepareDelete(indexName);
	}

	private Client createClient() {
		TransportAddress address = new InetSocketTransportAddress(new InetSocketAddress("localhost", 9300));
		Settings settings = Settings.builder().put("cluster.name", "my-test-cluster").build();
		Client transportClient = new PreBuiltTransportClient(settings)
				.addTransportAddress(address);
		return transportClient;
	}

	@Test
	public void indexAndSearchWithTransportClient() throws IOException {
		Client transportClient = createClient();
		index(transportClient);
		search(transportClient);
	}

	private void index(Client client) throws IOException {
		XContentBuilder builder = jsonBuilder()
				.startObject()
				.field("food", "Roti Prata")
				.array("tags", new String [] {"vegetarian", "curry"})
				.startObject("favorite")
				.field("location", "Tiong Bahru")
				.field("price", 2.00)
				.endObject()
				.endObject();

		IndexResponse response = client.prepareIndex(indexName, "dish")
				.setSource(builder)
				.execute()
				.actionGet();

		assertEquals(CREATED, response.getResult());

		GetResponse getResponse = client.prepareGet(indexName, "dish", response.getId()).execute().actionGet();

		assertTrue(getResponse.isExists());
		assertEquals("Roti Prata", getResponse.getSourceAsMap().get("food"));

		client.admin().indices().prepareRefresh(indexName).execute().actionGet();
	}

	private void search(Client client) {

		SearchResponse searchResponse = client.prepareSearch(indexName)
				.setTypes("dish")
				.setQuery(matchQuery("food", "roti"))
				.execute()
				.actionGet();

		SearchHits hits = searchResponse.getHits();
		assertEquals(1, hits.getTotalHits());
		assertEquals("Roti Prata", hits.getAt(0).getSource().get("food"));
	}



}
