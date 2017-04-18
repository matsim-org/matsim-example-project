package org.matsim.tutorial.class2017.basicprogramming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class MyCollectionsExample {
	public static void main(String[] args) {
		Collection<Rectangle> myRectanglesSet = new HashSet<>();
		Rectangle rec = new Rectangle(1,2,"blue");
		//try to understand the difference between sets and List using this example class
		
		myRectanglesSet.add(rec);
		myRectanglesSet.add(rec);
		myRectanglesSet.add(rec);
		myRectanglesSet.add(rec);

		for (Rectangle r : myRectanglesSet){
			System.out.println(r.toString());
		}
			
		System.out.println(myRectanglesSet.size());
		
		Collection<Rectangle> myRectanglesList = new ArrayList<>();
		Rectangle rec2 = new Rectangle(1,2,"blue");
		Rectangle rec3 = new Rectangle(3,4,"black");
		
		
		myRectanglesList.add(rec2);
		myRectanglesList.add(rec3);
		myRectanglesList.add(rec2);
		myRectanglesList.add(rec2);

		for (Rectangle r : myRectanglesList){
			System.out.println(r.toString());
		}
		System.out.println(myRectanglesList.size());
		
	}
	

}
