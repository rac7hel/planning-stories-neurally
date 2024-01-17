package r7.llmsearch;

public class PromptText {
	
	/** Using **/
	public static final String SEQUENCE_BEGINS_WITH = 
			"The sequence begins with the following steps:\\n";
	public static final String STORY_BEGINS_WITH = SEQUENCE_BEGINS_WITH.replace("sequence", "story");
	public static final String WHICH_HAPPENS_NEXT = 
			"Predict the next step in the sequence using one of the following options.\\n"; 
	public static final String WHICH_HAPPENS_FIRST = WHICH_HAPPENS_NEXT.replace("next","first");

	public static final String TWO_THINGS_NEXT = 
			//"Predict the next step in the sequence. Give two different answers, each on its own line. ";
			//"Suggest the next step in the sequence. List two different options, each as a short sentence on its own line. ";
	        "List two different actions that could happen next. State each as a short sentence on its own line. ";
	public static final String TWO_THINGS_FIRST = TWO_THINGS_NEXT.replace("next", "first");
	public static final String THREE_THINGS_NEXT = TWO_THINGS_NEXT.replace("two", "three");
	public static final String THREE_THINGS_FIRST = TWO_THINGS_FIRST.replace("two", "three");
	public static final String FOUR_THINGS_FIRST = TWO_THINGS_FIRST.replace("two", "four");
	public static final String FOUR_THINGS_NEXT = TWO_THINGS_NEXT.replace("two", "four");
	public static final String FIVE_THINGS_FIRST = TWO_THINGS_FIRST.replace("two", "five");
	public static final String FIVE_THINGS_NEXT = TWO_THINGS_NEXT.replace("two", "five");

	public static final String EACH_CHARACTER_NEXT  = 
			"What is the next step in each character's plan? State each on its own line in the present active tense, like '[character] [action].' ";
	public static final String EACH_CHARACTER_FIRST = EACH_CHARACTER_NEXT.replace("next", "first");
	
	/** GPT Ranked Search **/
	public static final String POSSIBLE_FIRST_STEPS = 
			"These are the possible actions that could happen first:\\n";
	public static final String POSSIBLE_NEXT_STEPS = 
			"These are the possible next actions:\\n";
	public static final String RANK_POSSIBLE_STEPS_1 = 
			"Rank all of the possible actions in order of how quickly they will lead to "; 
	public static final String RANK_POSSIBLE_STEPS_2 = 
			"Use only their label. ";
	public static final String RANK_POSSIBLE_STEPS_1_FOR_AGENT = 
			"Rank all of the possible actions in order of how helpful they would be for ";

	/** GPT Vector Search **/
	public static final String WHAT_HAPPENS_NEXT = 
			"What action happens next? Answer in one short sentence. ";
	
	/** GPT Heuristic TM **/
	public static final String COMPLETE_THE_PLAN = 
			"List the steps to complete the story plan. ";
	
}
