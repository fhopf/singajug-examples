package de.fhopf.elasticsearch.singafood.springdata;


import com.google.common.collect.Iterables;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JavaConfigTest.Config.class})
public class JavaConfigTest {

	@Autowired
	private DishRepository dishRepository;

	@Autowired
	private ElasticsearchOperations elasticsearchOperations;

	@Test
	public void indexAndSearch() {

		Dish blackCarrotCake = new Dish();
		blackCarrotCake.setId("black-carrot-cake");
		blackCarrotCake.setFood("Black Carrot Cake");
		blackCarrotCake.setTags(Arrays.asList("vegetarian"));

		Dish whiteCarrotCake = new Dish();
		whiteCarrotCake.setId("white-carrot-cake");
		whiteCarrotCake.setFood("White Carrot Cake");
		whiteCarrotCake.setTags(Arrays.asList("vegetarian"));

		Dish hokkienPrawnMie = new Dish();
		hokkienPrawnMie.setId("hokkien-prawn-mie");
		hokkienPrawnMie.setFood("Hokkien Prawn Mie");
		hokkienPrawnMie.setTags(Arrays.asList("noodles", "prawn"));

		dishRepository.save(Arrays.asList(blackCarrotCake, whiteCarrotCake, hokkienPrawnMie));

		elasticsearchOperations.refresh(Dish.class);

		Iterable<Dish> all = dishRepository.findAll();
		assertEquals(3, Iterables.size(all));

		List<Dish> result = dishRepository.findByFood("carrot cake");
		assertEquals(2, result.size());

		result = dishRepository.findByFoodOrTags("carrot cake", "noodles");
		assertEquals(3, result.size());

		result = dishRepository.customFindAll();
		assertEquals(3, result.size());
	}

	@Test
	public void basicElasticsearchOperations() {

		boolean created = elasticsearchOperations.createIndex("something-else");
		assertTrue(created);

		assertTrue(elasticsearchOperations.indexExists("something-else"));

		IndexQuery indexQuery = new IndexQuery();
		indexQuery.setId("some-id");
		indexQuery.setIndexName("something-else");
		indexQuery.setType("type");
		indexQuery.setSource("{ \"title\": \"my custom object\" }");

		String id = elasticsearchOperations.index(indexQuery);
		elasticsearchOperations.refresh("something-else");

		SearchQuery searchQuery = new NativeSearchQuery(matchQuery("title", "object"));
		List<Map> maps = elasticsearchOperations.query(searchQuery, new ResultsExtractor<List<Map>>() {
			@Override
			public List<Map> extract(SearchResponse response) {
				return Stream.of(response.getHits().getHits()).map(hit -> hit.getSource()).collect(Collectors.toList());
			}
		});

		assertEquals(1, maps.size());
		assertEquals("my custom object", maps.get(0).get("title"));

		elasticsearchOperations.delete("something-else", "type", id);

		elasticsearchOperations.deleteIndex("something-else");

		assertFalse(elasticsearchOperations.indexExists("something-else"));

	}

	@After
	public void deleteAll() {
		dishRepository.deleteAll();
	}



	@Configuration
	@EnableElasticsearchRepositories(basePackages = "de/fhopf/elasticsearch")
	static class Config {

		@Bean
		public ElasticsearchOperations elasticsearchTemplate() {
			Settings.Builder settings = Settings.settingsBuilder()
					.put("cluster.name", "my_test_cluster");
			TransportClient transportClient = new TransportClient.Builder()
					.settings(settings)
					.build();
			TransportAddress address = new InetSocketTransportAddress(
					new InetSocketAddress("localhost", 9300));
			transportClient.addTransportAddress(address);
			return new ElasticsearchTemplate(transportClient);
		}
	}




}
