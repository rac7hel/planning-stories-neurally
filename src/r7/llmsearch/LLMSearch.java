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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LLMSearch extends GoalFirstSearch {

	private static final int SEARCH_NODE_LIMIT = 5; 
	private static final int SLEEP_SECONDS = 5; 		
	private static final int RESPONSE_TOKEN_LIMIT = 25; 
	private static final int EMBEDDING_LENGTH = 1537;
	private static final float TEMPERATURE = 0.3f;
	private static final double SIMILARITY_PRECISION = 100.0; 
	private static final boolean USE_HANDWRITTEN_INITIAL_STATE = false;
	private static final String OUT_DIR = "out/";
	private static final String EXPLAINED_DIR = "explained/";
	private static final String EMBEDDINGS_DIR = "embeddings/";
	private static final String EXPLAINED_FILE_SUFFIX = "-explained.txt";
	private static final String EMBEDDINGS_FILE_SUFFIX = "-string-embeddings.csv";
	private static final OpenAIComponent OPENAI_COMPONENT = new OpenAIComponent();
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final CostTable cost;
	private final String stringEmbeddingsFile;
	private final HashMap<String, double[]> stringEmbeddings;
	private final HashSet<String> explainedNodes;
	private String search_out;
	private String transcript_out;
	private DomainText domainText; 
	private int nodeCount = 0;
	
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
		this.explainedNodes = readExplainedNodes(EXPLAINED_DIR + domain + EXPLAINED_FILE_SUFFIX);
		this.stringEmbeddingsFile = EMBEDDINGS_DIR + domain + EMBEDDINGS_FILE_SUFFIX;
		this.stringEmbeddings = readStringEmbeddings(stringEmbeddingsFile); 
		setRun();
	}
	
	public void configureDomainText(int goal) {
		this.domainText = DomainText.get(problem, goal);
	}
	
	@Override
	protected <N> boolean push(SearchNode<N> node) {
		if(node.getNode() == node.getRoot()) 
			cost.setCost((long)node.getNode(), 0.0); 
		return super.push(node);
	}
	
	@Override
	public void expand(SearchNode<?> node, EventTree<CompiledAction> actions) {
	    if(nodeCount == SEARCH_NODE_LIMIT)
	        throw new IllegalStateException("Maximum nodes visited: " + nodeCount);
	    if(isCharacterNode(node))
	        return;
	    nodeCount++;
	    HeadPlan<CompiledAction> plan = ((ProgressionTreeSpace) space).getTree().getPlan((long) node.node);	    
	    System.out.println("Expanding node " + node.node + " [" + plan + "]");
	    writeNodeVisit(node, plan);

	    if(nodeMeetsGoal(node)) {
	        checkAndProcessSolution(plan);
	        return;
	    }

	    processChildren(node, plan);
	}

	private boolean isCharacterNode(SearchNode<?> node) {
		return node.getCharacter() != null;		
	}
	
	private void writeNodeVisit(SearchNode<?> node, HeadPlan<CompiledAction> plan) {
		try (BufferedWriter searchWriter = new BufferedWriter(new FileWriter(new File(search_out), true))){
			searchWriter.write(this.cost.getCost((long)node.node) + ": " + plan.toString());
			searchWriter.newLine();
			searchWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean nodeMeetsGoal(SearchNode<?> node) {
		return node.getUtility(null).compareTo(this.getGoal()) >= 0;
	}
	
	/** Check if all steps are explained using precomputed list **/
	private void checkAndProcessSolution(HeadPlan<CompiledAction> plan) {
		boolean solution = true;
		StringBuilder planStr = new StringBuilder();
		for(CompiledAction step : plan) {
			if(!planStr.isEmpty())
				planStr.append(" ");
			planStr.append(step);
			if(!explainedNodes.contains(planStr.toString())) {
				solution = false;
				break;
			}
		}

		if(solution)
			throw new IllegalStateException("Solution found: " + planStr);
	}
	
	/** Set costs for children and expand **/
	private void processChildren(SearchNode<?> node, HeadPlan<CompiledAction> plan) {

		String prompt = makePrompt((long) node.node);
	    State state = fluent -> node.getValue(fluent);
	    CompiledAction[] possibleActions = Utilities.toArray(problem.actions.getEvery(state), CompiledAction.class);

	    try {
			String[] suggestions = getSuggestions(prompt);
			printSuggestions(suggestions);
		    HashMap<CompiledAction, Integer> actionCosts = getActionCosts(possibleActions, suggestions);
	        writeTranscript(prompt, node, plan, suggestions, actionCosts);
		    setCostForChildren(node, possibleActions, actionCosts);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    for (CompiledAction action : possibleActions) {
	        expand(node, action);
	    }
	}

	private void printSuggestions(String[] suggestions) {
		StringBuilder message = new StringBuilder("Suggestions: ");
		for(int i=0; i<suggestions.length; i++)
			message.append("\n  ").append(suggestions[i]);		
		System.out.println(message);
	}
	
	private String makePrompt(long current) {
		StringBuilder prompt = new StringBuilder();
		if(USE_HANDWRITTEN_INITIAL_STATE) 
			prompt.append(domainText.handwrittenInitial)
			      .append(domainText.actionTypes()); 
		else 
			prompt.append(domainText.initialState())
			      .append(domainText.characterGoals())
			      .append(domainText.actionTypes());
		HeadPlan<CompiledAction> plan = ((ProgressionTreeSpace)space).getTree().getPlan(current);
		if(plan.size() > 0) {
			prompt.append(PromptText.STORY_BEGINS_WITH);
			for(CompiledAction action : plan) 
				prompt.append(domainText.action(action)).append("\\n");
			prompt.append(domainText.goal());
			prompt.append(PromptText.TWO_THINGS_NEXT);
		} else {
			prompt.append(domainText.goal());
			prompt.append(PromptText.TWO_THINGS_FIRST);
		}
		return prompt.toString();
	}

	/** Send prompt to GPT and wait for response **/
	private String[] getSuggestions(String prompt) {
		sleepForRateLimit();
		try {
		    JsonNode jsonNode = OBJECT_MAPPER.readTree(OPENAI_COMPONENT.completeChat(new String[] {prompt}, RESPONSE_TOKEN_LIMIT, TEMPERATURE)
					.block());			
			return (jsonNode.path("choices").path(0).path("message").path("content").asText()).split("\n");
		} catch(JsonProcessingException e) {
			e.printStackTrace();
		}		
		return null;
	}
	
	/** Calculate action costs based on distance to the nearest suggestion **/
	private HashMap<CompiledAction, Integer> getActionCosts(CompiledAction[] possibleActions, String[] suggestions) {
		ArrayList<double[]> suggestionEmbeddings = new ArrayList<>();					
		for(int i=0; i<suggestions.length; i++) {
			if(!suggestions[i].replaceAll("\\s", "").equals("")) 
				suggestionEmbeddings.add(embed(suggestions[i]));
		}		
		HashMap<CompiledAction, Integer> actionCosts = new LinkedHashMap<>();
		for(CompiledAction action : possibleActions) {
			double[] actionEmbedding = embed(domainText.action(action));
			actionCosts.put(action, getMinDist(actionEmbedding, suggestionEmbeddings));
		}
		return actionCosts;
	}
	
	private void writeTranscript(String prompt, SearchNode<?> node, HeadPlan<CompiledAction> plan, String[] suggestions, HashMap<CompiledAction, Integer> actionCosts) throws IOException {
		try(BufferedWriter transcriptWriter = new BufferedWriter(new FileWriter(new File(transcript_out), true))){
			if(plan.size() == 0) {
				transcriptWriter.write("PROMPT:\n" + prompt);
				transcriptWriter.newLine();
			} else {
				transcriptWriter.write("\n[PROMPT]... " + PromptText.STORY_BEGINS_WITH);  
				transcriptWriter.newLine();
				for(CompiledAction action : plan) {
					transcriptWriter.write(domainText.action(action));
					transcriptWriter.newLine();
				}
				transcriptWriter.write(PromptText.TWO_THINGS_NEXT);
				transcriptWriter.newLine();
			}
			transcriptWriter.write("\nSUGGESTIONS:\n");
			for(String suggestion : suggestions) {
				if(!suggestion.replaceAll("\\s", "").equals("")) {
					transcriptWriter.write(suggestion);
					transcriptWriter.newLine();
				}
			}
			transcriptWriter.write("\nACTION COSTS:\n");
			for(CompiledAction action : actionCosts.keySet()) {
				transcriptWriter.write(action + " -> " + actionCosts.get(action));
				transcriptWriter.newLine();
			}
			transcriptWriter.flush();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void setCostForChildren(SearchNode<?> node, CompiledAction[] possibleActions, HashMap<CompiledAction, Integer> actionCosts) {
		for(CompiledAction action : possibleActions) {
			long child = (long)node.getChild(action).node;
			this.cost.setCost(child, this.cost.getCost((long)node.node) + actionCosts.get(action));
		}
	}

	/** Get a stored embedding or create and store a new one **/
	private double[] embed(String stringToEmbed) {
		if(!stringEmbeddings.containsKey(stringToEmbed)) {
			double[] embedding = getNewStringEmbedding(stringToEmbed);
			writeStringEmbedding(stringToEmbed, embedding);			
			stringEmbeddings.put(stringToEmbed, embedding); 
		}
		return stringEmbeddings.get(stringToEmbed);
	}
	
	/** Ask GPT for a string embedding **/
	private double[] getNewStringEmbedding(String stringToEmbed) {
		try{
			TimeUnit.SECONDS.sleep(SLEEP_SECONDS); 
			JsonNode jsonNode = OBJECT_MAPPER.readTree(OPENAI_COMPONENT.getTextEmbedding(stringToEmbed)
					.doOnError(error -> System.err.println(error.getMessage()))
					.block());
			JsonNode embeddingNode = jsonNode.path("data").path(0).path("embedding");
			double[] embedding = new double[embeddingNode.size()];			
			for(int i=0; i<embedding.length; i++)
				embedding[i] = embeddingNode.get(i).asDouble();
			return embedding;
		} catch(InterruptedException | JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/** Append new embedding to the string embeddings file **/
	private void writeStringEmbedding(String stringToEmbed, double[] embedding) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(stringEmbeddingsFile), true))){
			StringBuilder csvLine = new StringBuilder(stringToEmbed).append(",");
			for(double d : embedding)
				csvLine.append(d).append(",");
			csvLine.deleteCharAt(csvLine.length() - 1);
			writer.write(csvLine.toString());
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		} 		
	}
	
	/** Find the minimum cosine distance between the action and any of the suggestions **/
	private int getMinDist(double[] actionEmbedding, ArrayList<double[]> suggestionEmbeddings) {
		int minDist = Integer.MAX_VALUE;
		for(int i=0; i<suggestionEmbeddings.size(); i++) {
			double similarity = cosineSimilarity(suggestionEmbeddings.get(i), actionEmbedding); 
			similarity = (similarity < -1.0) ? -1.0 : (similarity > 1.0) ? 1.0 : similarity; // Fix rounding errors
			double scaled = (similarity + 1.0) / 2.0; // scale between 0 and 1
			double asDistance = (1.0 - scaled);
			int roundedScaledDistance = (int) Math.round(SIMILARITY_PRECISION * asDistance); 
			if(roundedScaledDistance < minDist) 
				minDist = roundedScaledDistance;
		}
		return minDist;
	}
	
	/** Calculate the cosine similarity between two vectors **/
	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
	    double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    for(int i=0; i<vectorA.length; i++) {
	        dotProduct += vectorA[i] * vectorB[i];
	        normA += Math.pow(vectorA[i], 2);
	        normB += Math.pow(vectorB[i], 2);
	    }   
	    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
	
	/** Determine run count and set output paths **/
	private void setRun() {
		int i=0;
		File run_dir;
		do {
			run_dir = new File(OUT_DIR + "run_" + ++i);	
		} while (run_dir.exists()); 
		run_dir.mkdirs();
		this.search_out = OUT_DIR + "run_" + i + "/" + "search.txt";
		this.transcript_out = OUT_DIR + "run_" + i + "/" + "transcript.txt";
	}
	
	/** Read in all explained nodes for this problem **/
	private HashSet<String> readExplainedNodes(String explainedNodes_in) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(new File(explainedNodes_in))); 
		HashSet<String> explainedNodes = new LinkedHashSet<>();
		String line = reader.readLine();
		while(line != null) {
			String[] splitLine = line.split(";");
			if(splitLine[1].startsWith(" "))
				splitLine[1] = splitLine[1].substring(1);
			explainedNodes.add(splitLine[1]); 
			line = reader.readLine();
		}
		reader.close();
		return explainedNodes;
	}
	
	/** Read in all previously stored string embeddings for this domain **/
	private HashMap<String, double[]> readStringEmbeddings(String stringEmbeddings_io) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File(stringEmbeddings_io)));
		HashMap<String, double[]> stringEmbeddings = new LinkedHashMap<>();
		String line = reader.readLine();
		while(line != null) {
			String[] splitLine = line.split(",");
			if(splitLine.length == EMBEDDING_LENGTH) {
				if(!stringEmbeddings.containsKey(splitLine[0])) {
					double[] embedding = new double[EMBEDDING_LENGTH];
					for(int i=0; i<EMBEDDING_LENGTH-1; i++)
						embedding[i] = Double.parseDouble(splitLine[i + 1]);
					stringEmbeddings.put(splitLine[0], embedding);
				}
			}
			line = reader.readLine();
		}
		reader.close();
		return stringEmbeddings;
	}
	
	private void sleepForRateLimit() {
		try {
			TimeUnit.SECONDS.sleep(SLEEP_SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} 
	}


	/** Print example strings and embeddings for the paper **/
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

		for(String suggestion : suggestions) {
			double[] embedding = stringEmbeddings.get(suggestion);
			StringBuilder abbr_embedding = new StringBuilder("[ ");
			for(int i=0; i<8; i++)
				abbr_embedding.append(f.format(embedding[i])).append(" ");
			abbr_embedding.append(" ... ]");
			System.out.println(suggestion + ": " + abbr_embedding); 
		}
		for(String signature : signatures) {
			for(CompiledAction action : problem.actions) {
				if(action.signature.toString().contentEquals(signature)) {
					System.out.println(domainText.action(action));
					double[] embedding = stringEmbeddings.get(domainText.action(action));
					StringBuilder abbr_embedding = new StringBuilder("[ ");
					for(int i=0; i<8; i++)
						abbr_embedding.append(f.format(embedding[i])).append(" ");
					abbr_embedding.append(" ... ]");
					System.out.println(abbr_embedding); 
				}
			}
		}
	}

}
