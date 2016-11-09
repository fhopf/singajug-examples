package de.fhopf.elasticsearch.singafood.springdata;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test-config.xml")
public class XMLConfigTest {

	@Autowired
	private ElasticsearchOperations elasticsearchTemplate;

	@Autowired
	private DishRepository dishRepository;

	@Test
	public void canConnectToCluster() {
		assertTrue(elasticsearchTemplate.indexExists(Dish.class));
	}

	@Test
	public void indexDishes() {
		Dish dish = new Dish();
		dish.setId("hainanese-chicken-rice");
		dish.setFood("Hainanese Chicken Rice");
		dish.setTags(Arrays.asList("chicken", "rice"));
		Favorite favorite = new Favorite();
		favorite.setLocation("Tian Tian");
		favorite.setPrice(4.0);
		dish.setFavorite(favorite);

		dishRepository.save(dish);

		Dish dishById = dishRepository.findOne("hainanese-chicken-rice");
		assertEquals("Hainanese Chicken Rice", dishById.getFood());
		assertEquals("Tian Tian", dishById.getFavorite().getLocation());

		// refresh to make available for search
		elasticsearchTemplate.refresh(Dish.class);

		List<Dish> result = dishRepository.findByTagsAndFavoriteLocation("chicken", "Tian Tian");
		assertEquals(1, result.size());

		result = dishRepository.findByFavoritePriceLessThan(5.0);
		assertEquals(1, result.size());

		result = dishRepository.findByFavoritePriceLessThan(4.0);
		assertEquals(0, result.size());

	}

	@After
	public void deleteAll() {
		dishRepository.deleteAll();
	}

}
