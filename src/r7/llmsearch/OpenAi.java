package r7.llmsearch;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OpenAi {

	/* https://platform.openai.com/docs/models/model-endpoint-compatibility 
	 *  01/18/2024 */
	private static final String[] ENDPOINTS = new String[] { 
			"v1/assistants", // All models except gpt-3.5-turbo-0301 supported. retrieval tool requires gpt-4-1106-preview or gpt-3.5-turbo-1106.
			"v1/audio/transcriptions", // whisper-1
			"v1/audio/translations", // whisper-1
			"v1/audio/speech", // tts-1, tts-1-hd
			"v1/chat/completions", // gpt-4 and dated model releases, gpt-4-1106-preview, gpt-4-vision-preview, gpt-4-32k and dated model releases, gpt-3.5-turbo and dated model releases, gpt-3.5-turbo-16k and dated model releases, fine-tuned versions of gpt-3.5-turbo
			"v1/completions", // (Legacy) gpt-3.5-turbo-instruct, babbage-002, davinci-002
			"v1/embeddings", // text-embedding-ada-002
			"v1/fine_tuning/jobs", // gpt-3.5-turbo, babbage-002, davinci-002
			"v1/moderations", // text-moderation-stable, text-moderation-latest
			"v1/images/generations" // dall-e-2, dall-e-3
	};
	
	private static final String URL = "https://api.openai.com/";	
    private static final String EMBEDDING_MODEL = "text-embedding-ada-002";
    private static final String[] CHAT_MODELS = new String[]{ 
    		"gpt-3.5-turbo-1106", // New Updated GPT 3.5 Turbo.
    		"gpt-3.5-turbo", // Currently points to gpt-3.5-turbo-0613.	
    		"gpt-3.5-turbo-instruct", 
    		"gpt-4-1106-preview", 
    		"gpt-4-vision-preview",
    		"gpt-4", 
    		"gpt-4-32k", 
    		"gpt-4-0613", 
    		"gpt-4-32k-0613" 
    }; 
    
    private static final String[] SYSTEM_ROLES = new String[] {
    		"You are a helpful assistant.",
    		"You are outlining a story.",
    		"You are helping the user outline a story."
    };

    private final String apiKey = System.getenv("OPENAI_KEY");
    
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();

    public OpenAi() {
    	System.out.println((apiKey != null ? "Found API key" : "Could not find API key."));    	
    }
    
    public Mono<String> completeChat(String[] messages) {
        String requestJson = "{\"model\":\"" + CHAT_MODELS[1] + 
        		"\",\"messages\":[{\"role\": \"system\", \"content\": \"" + SYSTEM_ROLES[1] + 
        		"\"},";
        for (String m : messages)
            requestJson += "{\"role\": \"user\", \"content\": \"" + m + "\"},";
        requestJson = requestJson.substring(0, requestJson.length() - 1) + "]}";
        return webClient.post()
                .uri(URL + ENDPOINTS[4])
                .body(BodyInserters.fromValue(requestJson))
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getTextEmbedding(String prompt) {
        String requestJson = "{\"model\":\"" + EMBEDDING_MODEL + "\",\"input\":\"" + prompt + "\"}";
        return webClient.post()
                .uri(URL  + ENDPOINTS[6])
                .body(BodyInserters.fromValue(requestJson))
                .retrieve()
                .bodyToMono(String.class);
    }
}