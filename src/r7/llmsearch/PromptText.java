package r7.llmsearch;

public class PromptText {
		
	public static final String STORY_BEGINS_WITH = 
			"The story begins with the following steps:\\n";
		
	public static final String TWO_THINGS_NEXT = 
	        "List two different actions that could happen next. State each as a short sentence (e.g. 5-10 words) on its own line. ";
	// Paper version:
	//public static final String TWO_THINGS_NEXT = "List two different actions that could happen next. State each as a short sentence on its own line. ";

	public static final String TWO_THINGS_FIRST = TWO_THINGS_NEXT.replace("next", "first");	
	public static final String THREE_THINGS_NEXT = TWO_THINGS_NEXT.replace("two", "three");
	public static final String THREE_THINGS_FIRST = TWO_THINGS_FIRST.replace("two", "three");
	public static final String FOUR_THINGS_FIRST = TWO_THINGS_FIRST.replace("two", "four");
	public static final String FOUR_THINGS_NEXT = TWO_THINGS_NEXT.replace("two", "four");
	public static final String FIVE_THINGS_FIRST = TWO_THINGS_FIRST.replace("two", "five");
	public static final String FIVE_THINGS_NEXT = TWO_THINGS_NEXT.replace("two", "five");
	
}
