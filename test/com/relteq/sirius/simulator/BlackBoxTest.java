package com.relteq.sirius.simulator;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

public class BlackBoxTest {

	private static int numberA;
	private static double numberB;
	
	@BeforeClass
	public static void beforeClass() {
		System.out.println("Before Class");
		numberA = 5;
		numberB = 3;
	}

	@Before
	public void beforeMethod() {
		System.out.println("Before Method");
	}
	
	@Test
	public void testClassTwo() {
		System.out.println("test class two");
	}
	
	@Test
	public void testClassOne() {
		System.out.println("test class one");
	}

	@After
	public void afterMethod() {
		System.out.println("After method");
	}
	
	@AfterClass
	public static void afterClass() {
		System.out.println("After class");
	}

	
}
