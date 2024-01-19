package r7.llmsearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.ToLongFunction;

import edu.uky.cs.nil.sabre.prog.*;
import edu.uky.cs.nil.sabre.ptree.ProgressionTreeSpace;
import edu.uky.cs.nil.sabre.Number;
import edu.uky.cs.nil.sabre.HeadPlan;
import edu.uky.cs.nil.sabre.State;
import edu.uky.cs.nil.sabre.Utilities;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.comp.CompiledProblem;
import edu.uky.cs.nil.sabre.etree.EventTree;
import edu.uky.cs.nil.sabre.util.BigArrayLong;

import r7.llmsearch.PromptText;
import r7.nl.DomainText;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LLMSearch extends GoalFirstSearch {

	private static final int MAX_NODES = 5000; // <---------- node limit (-1 for unlimited)
	private static final int MAX_TOKENS = 25; // tokens per text response (need 10-15 per sentence) 25/2, 35/3
	private static final int SLEEP_SECONDS = 10; // increase when hit rate limit
	private static final float TEMPERATURE = 0.3f; // 
	private static final double SCALE_FACTOR = 100.0; // decimal places to preserve from vector similarity values	
	private static final boolean HANDWRITTEN_INITIAL_STATE = false;

	private static final String OUT_DIR = "out/";
	private String search_out;
	private String walkthrough_out;
	private String explainedNodes_in;
	private String stringEmbeddings_io; 	

	private final OpenAI openAi = new OpenAI();
	private final String domain;
	private DomainText text; 
	protected final CostTable cost;
	private BufferedWriter searchWriter; 
	private BufferedWriter walkthroughWriter;
	private final HashMap<String, double[]> stringEmbeddings;
	private final HashSet<String> explainedNodes;
	private int counter = 0;

	public void setText(DomainText text) {
		this.text = text;
	}
	
	public LLMSearch(
		CompiledProblem problem,
		long searchLimit,
		long spaceLimit,
		long timeLimit,
		int authorTemporalLimit,
		int characterTemporalLimit,
		int epistemicLimit,
		ProgressionTreeSpace space,
		String domain
	) throws Exception {
		super(
			problem,
			new CostTable(),
			ProgressionCost.ZERO,
			new EventTree<CompiledAction>(problem.actions),
			space,
			searchLimit,
			spaceLimit,
			timeLimit,			
			authorTemporalLimit,
			characterTemporalLimit,
			epistemicLimit,
			true
		);
		this.cost = (CostTable) super.cost;
		this.domain = domain;
		this.stringEmbeddings_io = "embeddings/" + domain + "-string-embeddings.csv";
		this.explainedNodes_in = "explained/" + domain + "-explained.txt";
		this.stringEmbeddings = new LinkedHashMap<>();
		this.explainedNodes = new HashSet<>();
		getStringEmbeddings();
		getExplainedNodes();
		setRun();
		this.searchWriter = new BufferedWriter(new FileWriter(new File(search_out)));
		this.walkthroughWriter = new BufferedWriter(new FileWriter(new File(walkthrough_out)));
	}
	
	@Override
	public void expand(SearchNode<?> node, EventTree<CompiledAction> actions) {
		if(counter == MAX_NODES)
			System.exit(1);
		HeadPlan<CompiledAction> plan = ((ProgressionTreeSpace)space).getTree().getPlan((long)node.node);
		System.out.println("Expanding node " + node.node + ": " + plan + " (" + ((node.getCharacter()==null) ? "author" : node.getCharacter()) + ")");
		counter++;

		/** If this is not an author node, prune all its children **/
		if(node.getCharacter() != null)
			return; 

		/** Record having visited this node **/
		try {
			searchWriter.write(this.cost.getCost((long)node.node) + ": " + plan.toString());
			searchWriter.newLine();
			searchWriter.flush();
		} catch(Exception e) { e.printStackTrace(); }

		/** If it's an author node that improves utility... **/
		if(node.getCharacter()==null && node.getUtility(null).compareTo(this.getGoal()) >= 0) {

			/** Check if it's a solution using pre-computed list of explained nodes **/
			String planStr = "";
			boolean solution = true;
			for(CompiledAction step : plan) {
				if(planStr != "")
					planStr += " ";
				planStr += step.toString();
				if(planStr != "" && !explainedNodes.contains(planStr)) {
					solution = false;
					break;
				}
			}
			if(solution) {
				System.out.println("Found a solution:\n" + planStr);
				System.exit(1); 
			}
			
			/** It's an ending regardless, so prune its children. **/
			return; 
		}  

		/** Store action costs **/
		HashMap<CompiledAction, Integer> rankings = new HashMap<>();

		/** Describe the current state to GPT and ask what to do next **/
		State state = (fluent) -> node.getValue(fluent);
		CompiledAction[] possibleActions = Utilities.toArray(problem.actions.getEvery(state), CompiledAction.class);
		String prompt = makePrompt((long)node.node);

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode;
		
		try {

			TimeUnit.SECONDS.sleep(SLEEP_SECONDS); // <-------------- wait for rate limit

			jsonNode = objectMapper.readTree(openAi.completeChat(new String[] {prompt}, MAX_TOKENS)
					.block());
			
			String completionText = jsonNode.path("choices").path(0).path("message").path("content").asText();

			// Embed GPT's suggestions
			ArrayList<double[]> answerEmbeddings = new ArrayList<>();		
			String[] answers = completionText.split("\n");
			
			for(int i=0; i<answers.length; i++) {
				if(!answers[i].replaceAll("\\s", "").equals("")) {
					System.out.println("-> " + answers[i]);
					answerEmbeddings.add(embed(answers[i]));
				}
			}
			
			// Rank the possible next actions by their proximity to the nearest answer embedding 

			for(CompiledAction a : possibleActions) {
				double[] actionEmbedding = embed(text.action(a));
				int minRank = Integer.MAX_VALUE;
				for(int i=0; i<answerEmbeddings.size(); i++) {
					double similarity = cosineSimilarity(answerEmbeddings.get(i), actionEmbedding); 
					// ^ similarity is supposed to range from -1 to 1, but sometimes doesn't, so fix rounding errors:
					if(similarity < -1.0)
						similarity = -1.0;
					else if(similarity > 1.0)
						similarity = 1.0;
					double scaled = (similarity + 1.0) / 2.0; // scale between 0 and 1
					double dist = 1.0 - scaled; // convert to distance
					int rank = (int) Math.round(SCALE_FACTOR * dist); // preserve decimal places when rounding to integer 
					if(rank < minRank) 
						minRank = rank;
				}
				rankings.put(a, minRank);
			}
			
			if(plan.size() == 0) {
				walkthroughWriter.write("PROMPT:\n" + prompt);
				walkthroughWriter.newLine();
			} else {
				walkthroughWriter.write("\n[PROMPT]... " + PromptText.STORY_BEGINS_WITH); // pre Raiders 3 this said Sequence no biggie 
				walkthroughWriter.newLine();
				for(CompiledAction a : plan) {
					walkthroughWriter.write(text.action(a));
					walkthroughWriter.newLine();
				}
				walkthroughWriter.write(PromptText.TWO_THINGS_NEXT);
				walkthroughWriter.newLine();
			}
			walkthroughWriter.write("\nSUGGESTIONS:\n");
			for(String ans : answers) {
				if(!ans.replaceAll("\\s", "").equals("")) {
					walkthroughWriter.write(ans);
					walkthroughWriter.newLine();
				}
			}
			walkthroughWriter.write("\nRANKS:\n");
			for(CompiledAction a : rankings.keySet()) {
				walkthroughWriter.write(a + " -> " + rankings.get(a));
				walkthroughWriter.newLine();
			}
			walkthroughWriter.flush();


		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		/*
		ArrayList<Integer> ranksOrdered = new ArrayList<>(rankings.values());
		Collections.sort(ranksOrdered);
		ArrayList<CompiledAction> actionsOrdered = new ArrayList<>();
		for(int i : ranksOrdered) {
			for(Entry<CompiledAction, Integer> e : rankings.entrySet()) {
				if(e.getValue() == i && !actionsOrdered.contains(e.getKey())) {
					actionsOrdered.add(e.getKey());
				}
			}
		}
		// only visit the n best children at each node
		int bf = 3;
		int num=0;
		for(CompiledAction action : actionsOrdered) {
			long child = space.getAfter(current, action);
			num++;
			if(num <= bf) {
				int rank = rankings.get(action);
				this.cost.setCost(child, cost + rank);
			} else
				this.cost.setCost(child, -1);
		}
		*/
		
		// Set the cost of every child node.
		System.out.println("Setting cost of " + possibleActions.length + " possible actions");
		for(CompiledAction a : possibleActions) {
			long child = (long)node.getChild(a).node;
			int rank = rankings.get(a); 
			this.cost.setCost(child, this.cost.getCost((long)node.node) + rank);
		}
		
		// Expand the node as usual.
		for(CompiledAction a : possibleActions)
			expand(node, a);
	}

	
	private void getExplainedNodes() throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(new File(explainedNodes_in))); 
		String line = reader.readLine();
		while(line != null) {
			String[] vals = line.split(";");
			if(vals[1].startsWith(" "))
				vals[1] = vals[1].substring(1);
			explainedNodes.add(vals[1]); 
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void setRun() {
		int i=0;
		File run_dir;
		do {
			run_dir = new File(OUT_DIR + "run_" + ++i);						
		} while (run_dir.exists()); 
		run_dir.mkdirs();
		this.search_out = OUT_DIR + "run_" + i + "/" + "search.txt";
		this.walkthrough_out = OUT_DIR + "run_" + i + "/" + "walkthrough.txt";
		try {
			this.searchWriter = new BufferedWriter(new FileWriter(new File(search_out)));
			this.walkthroughWriter = new BufferedWriter(new FileWriter(new File(walkthrough_out)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** 
	 * Get all previously stored string embeddings from file  
	 **/
	private void getStringEmbeddings() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(stringEmbeddings_io)));
		String line = reader.readLine();
		while(line != null) {
			String[] vals = line.split(",");
			if(vals.length == 1537) {
				if(!stringEmbeddings.containsKey(vals[0])) {
					double[] embedding = new double[1536];
					for(int i=0; i<1536; i++)
						embedding[i] = Double.parseDouble(vals[i + 1]);
					this.stringEmbeddings.put(vals[0], embedding);
				}
			}
			line = reader.readLine();
		}
		reader.close();
	}

	
	/** Get new embedding and store it **/
	private double[] embed(String str) {
		if(stringEmbeddings.containsKey(str))
			return stringEmbeddings.get(str);
		try {
			TimeUnit.SECONDS.sleep(SLEEP_SECONDS); // <-------------- wait for rate limit

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(openAi.getTextEmbedding(str)
					.doOnError(error -> System.err.println("Error from LLMSearch embed(): " + error.getMessage()))
					.block());

			JsonNode embeddingVector = jsonNode.path("data").path(0).path("embedding");
			double[] newEmbedding = new double[embeddingVector.size()];
			
			for(int j=0; j<newEmbedding.length; j++)
				newEmbedding[j] = embeddingVector.get(j).asDouble();
			stringEmbeddings.put(str, newEmbedding); 

			// Append new string embedding to file
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File("" + domain + "-string-embeddings.csv"), true));
			String line = str + ",";
			for(double d : newEmbedding)
				line += d + ",";
			writer.write(line.substring(0, line.length() - 1));
			writer.newLine();
			writer.close();	
			
			return newEmbedding;

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String listPossibleActions(CompiledAction[] actions) {
		String str = "";
		for(CompiledAction action : actions) {
			str += text.action(action) + "\\n";
		}
		return str;
	}

	private String makePrompt(long current) {
		String prompt = "";
		if(HANDWRITTEN_INITIAL_STATE) 
			prompt = text.handwrittenInitial + text.actionTypes(); 			
		else 
			prompt = text.initialState() + text.characterGoals() + text.actionTypes();			
		HeadPlan<CompiledAction> plan = ((ProgressionTreeSpace)space).getTree().getPlan(current);
		if(plan.size() > 0) {
			prompt += PromptText.STORY_BEGINS_WITH;
			for(CompiledAction action : plan) 
				prompt += text.action(action) + "\\n";
			prompt += text.goal();
			prompt += PromptText.TWO_THINGS_NEXT;
		} else {
			prompt += text.goal();
			prompt += PromptText.TWO_THINGS_FIRST;
		}
		return prompt;
	}
	
	private String handwritInitPrompt(long current) {
		String prompt = text.handwrittenInitial + text.actionTypes();
		HeadPlan<CompiledAction> plan = ((ProgressionTreeSpace)space).getTree().getPlan(current);
		if(plan.size() > 0) {
			prompt += PromptText.SEQUENCE_BEGINS_WITH;
			for(CompiledAction action : plan) 
				prompt += text.action(action) + "\\n";
			prompt += PromptText.EACH_CHARACTER_NEXT;
		} else {
			prompt += PromptText.EACH_CHARACTER_FIRST;
		}
		return prompt;
	}
	
	private String exactInitPrompt(long current) {
		String prompt = "The following facts describe the initial state of a scenario: ";
		prompt += text.initialState();
		HeadPlan<CompiledAction> plan = ((ProgressionTreeSpace)space).getTree().getPlan(current);
		if(plan.size() > 0) {
			// TODO
		}
		return prompt;
	}
	
	@Override
	protected <N> boolean push(SearchNode<N> node) {
		if(node.getNode() == node.getRoot()) 
			cost.setCost((long)node.getNode(), 0.0); 
		return super.push(node);
	}
	
	/** Print strings and embeddings for the paper example **/
	private void printExampleEmbeddings() {
		String[] signatures = {
				"attack(Tom, Tom, Cottage)",
				"loot(Guard, MerhcantSword, Merchant, Market)",
				"loot(Guard, Medicine, Merchant, Market)",
				"walk(Guard, Market, Crossroads)",
				"take(Bandit, BanditCoin, Chest, Camp)",
				"walk(Tom, Cottage, Crossroads)",
				"attack(Guard, Guard, Market)",
				"walk(Bandit, Camp, Crossroads)",
				"attack(Bandit, Bandit, Camp)"
		};
		String[] suggestions = {
				"The bandit attacks the hero.",
				"The hero takes the medicine to the cottage."
		};
		DecimalFormat f = new DecimalFormat("0.0000");

		for(String s : suggestions) {
			double[] embedding = stringEmbeddings.get(s);
			String abbr_embedding = "[ ";
			for(int i=0; i<8; i++)
				abbr_embedding += f.format(embedding[i]) + " ";
			abbr_embedding += " ... ]";
			System.out.println(s + ": " + abbr_embedding); // + "\n---");
		}
		for(String s : signatures) {
			for(CompiledAction a : problem.actions) {
				if(a.signature.toString().contentEquals(s)) {
					System.out.println(text.action(a));
					double[] embedding = stringEmbeddings.get(text.action(a));
					String abbr_embedding = "[ ";
					for(int i=0; i<8; i++)
						abbr_embedding += f.format(embedding[i]) + " ";
					abbr_embedding += " ... ]";
					System.out.println(abbr_embedding); // + "\n---");
				}
			}
		}
	}

	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
	    double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    for (int i = 0; i < vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    }   
	    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
	

}
