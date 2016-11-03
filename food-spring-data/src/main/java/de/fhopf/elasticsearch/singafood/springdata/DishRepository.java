package de.fhopf.elasticsearch.singafood.springdata;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;

import java.util.List;

public interface DishRepository extends ElasticsearchCrudRepository<Dish, String> {

	List<Dish> findByFood(String food);

	List<Dish> findByFoodAndTags(String food, String tag);

	List<Dish> findByFoodOrTags(String food, String tag);

	List<Dish> findByTagsAndFavoriteLocation(String tag, String location);

	List<Dish> findByFavoritePriceLessThan(Double price);

	@Query("{\"query\": {\"match_all\": {}}}")
	List<Dish> customFindAll();

}
