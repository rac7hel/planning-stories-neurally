package r7.domaintext;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import edu.uky.cs.nil.sabre.Problem;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.comp.CompiledProblem;
import edu.uky.cs.nil.sabre.comp.Grounder;
import edu.uky.cs.nil.sabre.io.DefaultParser;
import edu.uky.cs.nil.sabre.io.Parser;
import edu.uky.cs.nil.sabre.util.Worker.Status;

import r7.llmsearch.PromptText;

public class TestDomainText {
	
	public static final LinkedHashMap<String, int[]> DOMAINS = new LinkedHashMap<>(); 
	public static DomainText text;
	public static CompiledProblem compiledProblem;
	
	public static void main(String[] args) throws Exception {
		
		DOMAINS.put("bribery", new int[] {1});
		DOMAINS.put("deerhunter", new int[] {1, 2});
		DOMAINS.put("secretagent", new int[] {1});
		DOMAINS.put("aladdin", new int[] {2});
		DOMAINS.put("hospital", new int[] {1, 2});
		DOMAINS.put("basketball", new int[] {1, 2});
		DOMAINS.put("western", new int[] {1});
		DOMAINS.put("fantasy", new int[] {1, 2, 3});
		DOMAINS.put("space", new int[] {1, 4});
		DOMAINS.put("raiders", new int[] {1});
		DOMAINS.put("treasure", new int[] {1});
		DOMAINS.put("gramma", new int[] {1, 2});
		DOMAINS.put("jailbreak", new int[] {1, 2});
		
		Parser parser = new DefaultParser();
		for(String domain : DOMAINS.keySet()) {
			for(int goal : DOMAINS.get(domain)) {
				String URL = "../../sabre-benchmarks/problems/" + domain + ".txt";
				Problem problem = parser.parse(new File(URL), Problem.class);
				compiledProblem = Grounder.compile(problem, new Status());
				text = DomainText.get(compiledProblem, goal);
				if(text != null) {
					String str = testHandwrittenInitial() + 
							 testAutomaticInitial() + 
							 testCharacterGoals() + 
							 testActionTypes() + 
							 testGoal() + 
							 testActions();
					BufferedWriter writer = new BufferedWriter(new FileWriter(new File("../out/text-" + domain + "-" + goal + ".txt")));
					writer.write(str);
					writer.newLine();
					writer.close();				
				} else {
					System.out.println("Domain not implemented: " + domain);
				}				
			}
		}
	}

	private static String testAutomaticInitial() { 
		return "--- Automatic Initial State ---\n" + 
				text.initialState() + "\n\n";
	}
	
	private static String testHandwrittenInitial() {
		return "--- Handwritten Initial State ---\n" + 
				text.handwrittenInitial + "\n\n";
	}

	private static String testCharacterGoals() {
		return "--- Character Goals ---\n" + 
				text.characterGoals() + "\n\n";
	}
	
	private static String testActionTypes() {
		return "--- Action Types ---\n" + 
				text.actionTypes() + "\n\n";
	}
	
	private static String testGoal() {
		return "--- Goal ---\n" + 
				text.goal() + "\n\n";
	}
	
	private static String testActions() {
		String s = "--- Actions ---\n";
		for(CompiledAction action : compiledProblem.actions) 
			s += text.action(action) + "\n";
		return s + "\n\n";
	}
		
	private static String testPrompt(ArrayList<CompiledAction> plan) {
		String prompt = "--- Example Prompt ---\n" + 
						text.initialState() + 
						text.actionTypes() + 
						text.characterGoals();
		if(plan.size() > 0) {
			prompt += PromptText.STORY_BEGINS_WITH;
			for(CompiledAction action : plan) 
				prompt += text.action(action) + "\\n";
			prompt += PromptText.TWO_THINGS_NEXT;
		} else {
			prompt += PromptText.TWO_THINGS_FIRST;
		}
		return prompt + "\n\n";
	}
}
