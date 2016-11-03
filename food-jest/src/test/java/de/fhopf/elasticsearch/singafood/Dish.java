package de.fhopf.elasticsearch.singafood;

import io.searchbox.annotations.JestId;

import java.util.List;

public class Dish {

	private String food;
	private List<String> tags;
	private Favorite favorite;

	@JestId
	private String id;

	public String getFood() {
		return food;
	}

	public void setFood(String food) {
		this.food = food;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public Favorite getFavorite() {
		return favorite;
	}

	public void setFavorite(Favorite favorite) {
		this.favorite = favorite;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
