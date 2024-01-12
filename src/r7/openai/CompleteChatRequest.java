package r7.openai;

public class CompleteChatRequest {
	private String prompt;
	 private float temperature;
	 private int maxTokens;

	 String getPrompt() {
		 return prompt;
	 }
	 
	 float getTemperature() {
		 return temperature;
	 }

	 int getMaxTokens() {
		 return maxTokens;
	 }
}
