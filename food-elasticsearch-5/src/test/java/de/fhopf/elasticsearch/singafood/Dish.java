package de.fhopf.elasticsearch.singafood;

import java.util.List;

public class Dish {

	private String food;
	private List<String> tags;
	private Favorite favorite;

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
}
