package de.fhopf.elasticsearch.singafood.springdata;

public class Favorite {

	private String location;
	private Double price;

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}
}
