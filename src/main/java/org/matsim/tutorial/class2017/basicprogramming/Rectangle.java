package org.matsim.tutorial.class2017.basicprogramming;

public class Rectangle {

	double length;
	double width;
	String color;

	public Rectangle(double l, double width, String color) {
		this.length = l ;
		this.width = width;
		this.color = color;
	}
	
	
	@Override
	public String toString() {
			return ("Rectangle: l "+length+ " w "+width+ " col. "+color);
	}
}
