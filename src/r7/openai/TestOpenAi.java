package r7.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestOpenAi {

	private static String prompt;

	public static void main(String[] args) throws Exception {
		OpenAi openAi = new OpenAi();
		prompt = "Roadeez is a self-driving car startup company. ";
		prompt += "How can investigators confirm or reject this hypothesis? ";

		String response = openAi.completeChat(new String[] {prompt})
				.doOnError(error -> System.err.println("Error from completeChat method: " + error.getMessage()))
				.block();
		
		System.out.println("Succeeded");

		// Handle success case

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode jsonNode = objectMapper.readTree(response);
			String content = jsonNode
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();
	        System.out.println("\n\nResponse: \n" + content);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Finished.");
		System.exit(0);
	}
}
