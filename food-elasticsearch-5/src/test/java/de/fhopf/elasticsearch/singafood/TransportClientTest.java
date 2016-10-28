package de.fhopf.elasticsearch.singafood;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.elasticsearch.action.DocWriteResponse.Result.CREATED;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransportClientTest {

	@Test
	public void indexAndSearchWithTransportClient() throws IOException {
		TransportAddress address = new InetSocketTransportAddress(new InetSocketAddress("localhost", 9300));
		Settings settings = Settings.builder().put("cluster.name", "my-test-cluster").build();
		Client transportClient = new PreBuiltTransportClient(settings)
				.addTransportAddress(address);
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

		IndexResponse response = client.prepareIndex("singafood", "dish")
				.setSource(builder)
				.execute()
				.actionGet();

		assertEquals(CREATED, response.getResult());

		GetResponse getResponse = client.prepareGet("singafood", "dish", response.getId()).execute().actionGet();

		assertTrue(getResponse.isExists());
		assertEquals("Roti Prata", getResponse.getSourceAsMap().get("food"));

		client.admin().indices().prepareRefresh("singafood").execute().actionGet();
	}

	private void search(Client client) {

		SearchResponse searchResponse = client.prepareSearch("singafood")
				.setTypes("dish")
				.setQuery(matchQuery("food", "roti"))
				.execute()
				.actionGet();

		SearchHits hits = searchResponse.getHits();
		assertEquals(1, hits.getTotalHits());
		assertEquals("Roti Prata", hits.getAt(0).getSource().get("food"));
	}



}
