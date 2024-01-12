package r7.openai;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class OpenAi {

    private static final String CHAT_COMPLETION_URL = "https://api.openai.com/v1/chat/completions";
    private static final String EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    private String completions_model = "text-davinci-003";
    private String chat_model = "gpt-3.5-turbo";
    private String embedding_model = "text-embedding-ada-002";

    private final String apiKey = System.getenv("OPENAI_KEY");
    
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();

    public OpenAi() {
    	if(apiKey != null)
    		System.out.println("Found API key");
    	else
    		System.out.println("Could not find API key.");
    }

    public Mono<String> completeChat(String[] messages) {
        String requestJson = "{\"model\":\"" + chat_model + "\",\"messages\":[{\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},";
        for (String m : messages) {
            requestJson += "{\"role\": \"user\", \"content\": \"" + m + "\"},";
        }
        requestJson = requestJson.substring(0, requestJson.length() - 1);
        requestJson += "]}";

        return webClient.post()
                .uri(CHAT_COMPLETION_URL)
                .body(BodyInserters.fromValue(requestJson))
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getVector(String prompt) {
        String requestJson = "{\"model\":\"" + embedding_model + "\",\"input\":\"" + prompt + "\"}";
        return webClient.post()
                .uri(EMBEDDINGS_URL)
                .body(BodyInserters.fromValue(requestJson))
                .retrieve()
                .bodyToMono(String.class);
    }
}
