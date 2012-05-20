package com.relteq.sirius;

/**
 * Implements "Sirius: Concept of Operations"
 */
public class Runner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (0 == args.length) {
			printUsage();
			System.exit(-1);
		}
		switch (args[0]) {
		case "import":
		case "i":
			break;
		case "update":
		case "u":
			break;
		case "export":
		case "e":
			break;
		case "calibrate":
		case "c":
			break;
		case "simulate":
		case "s":
			break;
		case "simulate_output":
		case "so":
			break;
		case "simulate_process":
		case "sp":
			break;
		case "list_scenarios":
		case "ls":
			break;
		case "list_runs":
		case "lr":
			break;
		case "load":
		case "l":
			break;
		}
	}
	
	private static void printUsage() {
		System.out.println("Usage: command [parameters]");
	}

}
