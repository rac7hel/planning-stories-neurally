package r7.llmsearch;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.text.DecimalFormat;

import edu.uky.cs.nil.sabre.HeadPlan;
import edu.uky.cs.nil.sabre.State;
import edu.uky.cs.nil.sabre.Utilities;
import edu.uky.cs.nil.sabre.comp.CompiledAction;
import edu.uky.cs.nil.sabre.comp.CompiledProblem;
import edu.uky.cs.nil.sabre.etree.EventTree;
import edu.uky.cs.nil.sabre.prog.*;
import edu.uky.cs.nil.sabre.ptree.ProgressionTreeSpace;

import r7.nl.DomainText;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LLMSearch extends GoalFirstSearch {

	private static final int MAX_NODES = 1; 			// or use -1 for unlimited
	private static final int MAX_TOKENS = 25; 			// tokens per response
	private static final int SLEEP_SECONDS = 10; 		// increase when hit rate limit
	private static final float TEMPERATURE = 0.3f; 		// (0 is deterministic)
	private static final double PRECISION_FACTOR = 100.0; 	// decimal places to preserve from similarity values	
	private static final boolean HANDWRIT_INIT = false;
	private static final String OUT_DIR = "out/";
	private final OpenAI openAi = new OpenAI();
	private final HashMap<String, double[]> stringEmbeddings;
	private final HashSet<String> explainedNodes;
	protected final CostTable costTable;
	private int nodeCount = 0;
	private String search_out;
	private String walkthrough_out;
	private String explainedNodes_in;
	private String stringEmbeddings_io; 
	private DomainText domainText; 
	private BufferedWriter searchWriter; 
	private BufferedWriter walkthroughWriter;
	
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
		this.costTable = (CostTable) super.cost;
		this.explainedNodes_in = "explained/" + domain + "-explained.txt";
		this.explainedNodes = new HashSet<>();
		this.stringEmbeddings_io = "embeddings/" + domain + "-string-embeddings.csv";
		this.stringEmbeddings = new LinkedHashMap<>();
		readExplainedNodes();
		readStringEmbeddings();
		setRun();
		this.searchWriter = new BufferedWriter(new FileWriter(new File(search_out)));
		this.walkthroughWriter = new BufferedWriter(new FileWriter(new File(walkthrough_out)));
	}
	
	public void setText(DomainText domainText) {
		this.domainText = domainText;
	}
	
	@Override
	protected <N> boolean push(SearchNode<N> node) {
		if(node.getNode() == node.getRoot()) 
			costTable.setCost((long)node.getNode(), 0.0); 
		return super.push(node);
	}
	
	@Override
	public void expand(SearchNode<?> node, EventTree<CompiledAction> actions) {
		
	    if (nodeCount == MAX_NODES) {
	        throw new IllegalStateException("Maximum nodes visited: " + nodeCount);
	    }

	    if (isCharacterNode(node)) {
	        return;
	    }

	    nodeCount++;
	    HeadPlan<CompiledAction> plan = ((ProgressionTreeSpace) space).getTree().getPlan((long) node.node);
	    System.out.println("Expanding node " + node.node + ": [" + plan + "]");

	    try {
	        writeNodeVisit(node, plan);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	    if (nodeMeetsGoal(node)) {
	        checkAndProcessSolution(plan);
	        return;
	    }

	    processChildren(node, plan);
	}

	private boolean isCharacterNode(SearchNode<?> node) {
		return node.getCharacter() != null;		
	}
	
	private void writeNodeVisit(SearchNode<?> node, HeadPlan<CompiledAction> plan) throws IOException {
		this.searchWriter.write(this.costTable.getCost((long)node.node) + ": " + plan.toString());
		this.searchWriter.newLine();
		this.searchWriter.flush();
	}
	
	private boolean nodeMeetsGoal(SearchNode<?> node) {
		return node.getUtility(null).compareTo(this.getGoal()) >= 0;
	}

	
	/** Check for solution using precomputed list of explained nodes **/
	private void checkAndProcessSolution(HeadPlan<CompiledAction> plan) {
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
			throw new IllegalStateException("Solution found: " + planStr);
		}
	}
	
	private void processChildren(SearchNode<?> node, HeadPlan<CompiledAction> plan) {

		String prompt = makePrompt((long) node.node);
	    State state = fluent -> node.getValue(fluent);
	    CompiledAction[] possibleActions = Utilities.toArray(problem.actions.getEvery(state), CompiledAction.class);

	    try {
			String[] suggestions = getSuggestions(prompt);
		    HashMap<CompiledAction, Integer> rankings = getActionRankings(suggestions, possibleActions);
	        writeTranscript(prompt, node, plan, suggestions, rankings);
		    setCostForChildren(node, possibleActions, rankings);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    for (CompiledAction a : possibleActions) {
	        expand(node, a);
	    }
	}

	private String makePrompt(long current) {
		String prompt = "";
		if(HANDWRIT_INIT) 
			prompt = domainText.handwrittenInitial + domainText.actionTypes(); 			
		else 
			prompt = domainText.initialState() + domainText.characterGoals() + domainText.actionTypes();			
		HeadPlan<CompiledAction> plan = ((ProgressionTreeSpace)space).getTree().getPlan(current);
		if(plan.size() > 0) {
			prompt += PromptText.STORY_BEGINS_WITH;
			for(CompiledAction action : plan) 
				prompt += domainText.action(action) + "\\n";
			prompt += domainText.goal();
			prompt += PromptText.TWO_THINGS_NEXT;
		} else {
			prompt += domainText.goal();
			prompt += PromptText.TWO_THINGS_FIRST;
		}
		return prompt;
	}
	
	private String[] getSuggestions(String prompt) throws InterruptedException, JsonMappingException, JsonProcessingException {
		/** Avoid hitting OpenAI rate limit **/
		TimeUnit.SECONDS.sleep(SLEEP_SECONDS); 

		/** Send prompt to GPT and wait for response **/
		ObjectMapper objectMapper = new ObjectMapper();
	    JsonNode jsonNode = objectMapper.readTree(openAi.completeChat(new String[] {prompt}, MAX_TOKENS, TEMPERATURE)
				.block());

		/** Get response text **/
		String completionText = jsonNode.path("choices").path(0).path("message").path("content").asText();

		/** Embed suggestions **/
		return completionText.split("\n");		
	}
	
	private HashMap<CompiledAction, Integer> getActionRankings(String[] suggestions, CompiledAction[] possibleActions) {

		ArrayList<double[]> suggestionEmbeddings = new ArrayList<>();					
		for(int i=0; i<suggestions.length; i++) {
			if(!suggestions[i].replaceAll("\\s", "").equals("")) {
				System.out.println("-> " + suggestions[i]);
				suggestionEmbeddings.add(embed(suggestions[i]));
			}
		}
		
		/** Calculate action costs based on distance to the nearest suggestion **/
		HashMap<CompiledAction, Integer> rankings = new LinkedHashMap<>();
		for(CompiledAction a : possibleActions) {
			double[] actionEmbedding = embed(domainText.action(a));
			rankings.put(a, getMinDist(actionEmbedding, suggestionEmbeddings));
		}
		return rankings;
	}
	
	private void writeTranscript(String prompt, SearchNode<?> node, HeadPlan<CompiledAction> plan, String[] suggestions, HashMap<CompiledAction, Integer> rankings) throws IOException {
		if(plan.size() == 0) {
			walkthroughWriter.write("PROMPT:\n" + prompt);
			walkthroughWriter.newLine();
		} else {
			walkthroughWriter.write("\n[PROMPT]... " + PromptText.STORY_BEGINS_WITH);  
			walkthroughWriter.newLine();
			for(CompiledAction a : plan) {
				walkthroughWriter.write(domainText.action(a));
				walkthroughWriter.newLine();
			}
			walkthroughWriter.write(PromptText.TWO_THINGS_NEXT);
			walkthroughWriter.newLine();
		}
		walkthroughWriter.write("\nSUGGESTIONS:\n");
		for(String ans : suggestions) {
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
	}
	
	private void setCostForChildren(SearchNode<?> node, CompiledAction[] possibleActions, HashMap<CompiledAction, Integer> rankings) {
		for(CompiledAction a : possibleActions) {
			long child = (long)node.getChild(a).node;
			this.costTable.setCost(child, this.costTable.getCost((long)node.node) + rankings.get(a));
		}
	}

	/** Get a stored embedding or create and store a new one **/
	private double[] embed(String str) {
		if(stringEmbeddings.containsKey(str))
			return stringEmbeddings.get(str);
		try {
			TimeUnit.SECONDS.sleep(SLEEP_SECONDS); 
			ObjectMapper objectMapper = new ObjectMapper();
			
			/** Get embedding from GPT **/
			JsonNode jsonNode = objectMapper.readTree(openAi.getTextEmbedding(str)
					.doOnError(error -> System.err.println("Error from LLMSearch embed(): " + error.getMessage()))
					.block());
			JsonNode embeddingVector = jsonNode.path("data").path(0).path("embedding");
			double[] newEmbedding = new double[embeddingVector.size()];			
			for(int j=0; j<newEmbedding.length; j++)
				newEmbedding[j] = embeddingVector.get(j).asDouble();
			stringEmbeddings.put(str, newEmbedding); 

			/** Append new embedding to the file **/
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(stringEmbeddings_io), true));
			String line = str + ",";
			for(double d : newEmbedding)
				line += d + ",";
			writer.write(line.substring(0, line.length() - 1));
			writer.newLine();
			writer.close();		
			
			return newEmbedding;
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}

	private int getMinDist(double[] actionEmbedding, ArrayList<double[]> suggestionEmbeddings) {
		int minDist = Integer.MAX_VALUE;
		for(int i=0; i<suggestionEmbeddings.size(); i++) {
			double similarity = cosineSimilarity(suggestionEmbeddings.get(i), actionEmbedding); 
			/** Fix rounding errors (cosSim should range from -1 to 1) **/
			similarity = (similarity < -1.0) ? -1.0 : (similarity > 1.0) ? 1.0 : similarity;
			// TODO: Verify this works ^
			//if(similarity < -1.0)
			//	similarity = -1.0;
			//else if(similarity > 1.0)
			//	similarity = 1.0;
			/** Scale between 0 and 1 **/
			double scaled = (similarity + 1.0) / 2.0; 
			/** Convert to distance, multiply to preserve decimal places, and round to integer **/
			int dist = (int) Math.round(PRECISION_FACTOR * (1.0 - scaled)); 
			if(dist < minDist) 
				minDist = dist;
		}
		return minDist;
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
	
	/** Increase run count and set output paths **/
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
	
	/** Read in all explained nodes for this problem **/
	private void readExplainedNodes() throws IOException{
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
	
	/** Read in all previously stored string embeddings for this domain **/
	private void readStringEmbeddings() throws IOException {
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
			System.out.println(s + ": " + abbr_embedding); 
		}
		for(String s : signatures) {
			for(CompiledAction a : problem.actions) {
				if(a.signature.toString().contentEquals(s)) {
					System.out.println(domainText.action(a));
					double[] embedding = stringEmbeddings.get(domainText.action(a));
					String abbr_embedding = "[ ";
					for(int i=0; i<8; i++)
						abbr_embedding += f.format(embedding[i]) + " ";
					abbr_embedding += " ... ]";
					System.out.println(abbr_embedding); 
				}
			}
		}
	}

}
