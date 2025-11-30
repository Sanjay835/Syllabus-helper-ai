package com.example;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class RAGEngine {

    private final HttpClient httpClient;
    private final String apiKey;
    private final List<Chunk> chunks = new ArrayList<>();

    public RAGEngine() {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = System.getenv("GROQ_API_KEY");
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            throw new RuntimeException("GROQ_API_KEY environment variable is not set.");
        }
    }

    // Load PDFs from /data folder and create chunks
    public void loadPdfsFromFolder(String folderPath) throws IOException {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Invalid data folder: " + folderPath);
        }

        int idCounter = 0;
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (!file.getName().toLowerCase().endsWith(".pdf")) continue;

            String subject = file.getName().substring(0, file.getName().lastIndexOf('.'));
            System.out.println("[*] Loading " + file.getName() + " (subject = " + subject + ")");

            String text = TextUtils.readPdfText(file.getAbsolutePath());
            List<String> chunkTexts = TextUtils.chunkText(text, 350, 70);

            for (String c : chunkTexts) {
                Chunk chunk = new Chunk("chunk_" + (idCounter++), subject, c);
                chunks.add(chunk);
            }
        }
        System.out.println("[✓] Total chunks: " + chunks.size());
    }

    // Get top-k chunks by simple keyword overlap
    private List<Chunk> getTopChunks(String question, int k) {
        return chunks.stream()
                .sorted((c1, c2) -> {
                    int s1 = TextUtils.keywordScore(question, c1.getText());
                    int s2 = TextUtils.keywordScore(question, c2.getText());
                    return Integer.compare(s2, s1);
                })
                .limit(k)
                .collect(Collectors.toList());
    }

    public String answerQuestion(String question, int topK) {
        if (chunks.isEmpty()) {
            return "Knowledge base is empty. Please load PDFs first.";
        }

        List<Chunk> topChunks = getTopChunks(question, topK);

        StringBuilder context = new StringBuilder();
        for (Chunk c : topChunks) {
            context.append("[").append(c.getSubject().toUpperCase()).append("]\n");
            context.append(c.getText()).append("\n\n");
        }

        String prompt = """
                You are a CSE department teaching assistant.
                Use ONLY the context from the syllabus/notes below to answer.

                Context:
                %s

                Question: %s

                Rules:
                - Answer in 4–8 lines.
                - Use simple exam-style English.
                - If the answer is not present in the context, say:
                  "This topic is not clearly covered in the given syllabus data."

                Now give the answer:
                """.formatted(context.toString(), question);

        try {
            return callGroq(prompt);
        } catch (Exception e) {
            e.printStackTrace();
            return "Error contacting Groq API: " + e.getMessage();
        }
    }

    private String callGroq(String prompt) throws IOException, InterruptedException {

        String safePrompt = escapeJson(prompt);

        String requestBody = """
        {
          "model": "llama-3.3-70b-versatile",
          "messages": [
            {
              "role": "user",
              "content": "%s"
            }
          ]
        }
        """.formatted(safePrompt);



        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return extractFirstContent(response.body());
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String extractFirstContent(String json) {
        String key = "\"content\":";
        int idx = json.indexOf(key);
        if (idx == -1) {
            return "Could not parse response.\nRaw:\n" + json;
        }

        int start = json.indexOf("\"", idx + key.length());
        int end = json.indexOf("\"", start + 1);
        if (start == -1 || end == -1) {
            return "Parse error.\nRaw:\n" + json;
        }

        String content = json.substring(start + 1, end);
        return content.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .trim();
    }
}
