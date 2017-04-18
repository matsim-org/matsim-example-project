package org.matsim.tutorial.class2017.basicprogramming;

public class IntsDoublesAndObjects {
	public static void main(String[] args) {
		int number = 5;
		
		Integer numberObject = 5;
		//only use this if you need to, e.g. for comparing objects.
		
		long numberlong = 5;
		
		
		float numberfloat = 5;
		double numberdouble = 5;
		//if in doubt, use double
		
		boolean isItTrue = true;
		
		System.out.println(number);		
		System.out.println(numberlong);		
		System.out.println(numberfloat);		
		System.out.println(numberdouble);		
		
		String numberAsAString = "five";
		System.out.println(numberAsAString);
		
		Rectangle rectangle = new Rectangle(2,5,"blue");
		System.out.println(rectangle.toString());
		
	}

}
