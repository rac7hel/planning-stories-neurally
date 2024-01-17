package r7.llmsearch;

import java.io.*;
import java.util.HashMap;

import r7.openai.OpenAi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WriteStringEmbeddings {

	public static final String DOMAIN = "gramma"; 
	public static final String URL = "../problems/" + DOMAIN + ".txt"; // for KCC, use path "problems/" NOTE: It has MerhchantSword spelling but that's ok
	/** TODO: Run this. 
	 * Write the text for all the Gramma actions to gramma-actions.csv **/
	public static void main(String[] args) throws Exception {
		/** Get existing embeddings **/
		BufferedReader embeddingReader = new BufferedReader(new FileReader(new File(DOMAIN + "-string-embeddings.csv")));
		HashMap<String, double[]> embeddings = new HashMap<>();
		String line = embeddingReader.readLine();
		while(line != null) {
			String[] vals = line.split(",");
			if(vals.length == 1537 && !embeddings.containsKey(vals[0])) {
				double[] embedding = new double[1536];
				for(int i=0; i<embedding.length; i++)
					embedding[i] = Double.parseDouble(vals[i+1]);
				embeddings.put(vals[0], embedding);
			}
			line = embeddingReader.readLine();
		}
		embeddingReader.close();
		/** Find and add new embeddings **/
		OpenAi openAi = new OpenAi();
		BufferedReader suggestionReader = new BufferedReader(new FileReader(new File(DOMAIN + "-node-suggestions.csv")));
		line = suggestionReader.readLine(); 
		ObjectMapper objectMapper = new ObjectMapper();
		while(line != null) {
			String[] vals = line.split(",");
			if(!embeddings.containsKey(vals[1])) {
				double[] embedding = new double[1536];
				/**
				 * 			JsonNode jsonNode = objectMapper.readTree(response);
			String content = jsonNode
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

				 */
				JsonNode jsonNode = objectMapper.readTree(vals[1]);
				// TODO: Print jsonNode and follow path to double array
				//JSONObject jsonObj = new JSONObject(openAi.getVector(vals[1]));
				//JSONArray jsonArr = jsonObj.getJSONArray("data").getJSONObject(0).getJSONArray("embedding");
				//for(int i=0; i<embedding.length; i++)
				//	embedding[i] = jsonArr.getDouble(i);
				embeddings.put(vals[1], embedding);
			}
			line = suggestionReader.readLine();
		}
		suggestionReader.close();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(DOMAIN + "-string-embeddings-new.csv")));
		for(String str : embeddings.keySet()) {
			String embeddingStr = "";
			for(double d : embeddings.get(str)) {
				embeddingStr += d + ",";
			}
			writer.write(str + "," + embeddingStr.substring(0, embeddingStr.length() - 1));
			writer.newLine();
		}
		writer.close();
	}
}
