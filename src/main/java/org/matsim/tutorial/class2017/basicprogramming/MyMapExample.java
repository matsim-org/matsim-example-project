package org.matsim.tutorial.class2017.basicprogramming;

import java.util.HashMap;
import java.util.Map;


public class MyMapExample {

	public static void main(String[] args) {
		
		double a =1.;
		double b=2.;
		double c=3.;
		double d=4.;
		String color1="Blue";
		String color2="Green";
			
		
		Rectangle rectangle1= new Rectangle(a,b,color1);
		Rectangle rectangle2=new Rectangle (c,d,color2);
		
		Map<Integer,Rectangle> geometryStuff = new HashMap<>();
		
		geometryStuff.put(1, rectangle1);
		geometryStuff.put(2, rectangle2);
		
		System.out.println(geometryStuff.size());
		
		for (Rectangle testRectangle : geometryStuff.values()){
			System.out.println(testRectangle.toString());
			testRectangle.length=testRectangle.length*2;
			System.out.println(testRectangle.toString());

		}
	}

}
