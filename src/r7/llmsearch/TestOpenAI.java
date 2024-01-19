package r7.llmsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestOpenAI {

	private static String prompt;
	private static int maxTokens = 5;

	public static void main(String[] args) throws Exception {
		OpenAI openAI = new OpenAI();
		openAI.setSystemRole(OpenAI.SYSTEM_ROLES[0]);

		prompt = "Just like a boiled potato, I ";

		String response = openAI.completeChat(new String[] {prompt}, maxTokens)
				.doOnError(error -> System.err.println("Error from completeChat method: " + error.getMessage()))
				.block();
		
		System.out.println("Succeeded");

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode jsonNode = objectMapper.readTree(response);
			String content = jsonNode
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
	        System.out.println("\n" + prompt + content);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Finished.");
	}
}
