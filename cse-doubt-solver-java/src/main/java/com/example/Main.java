package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static void main(String[] args) throws IOException {

        System.out.println("CSE LLM Technical Doubt Solver (Java + Groq + LLaMA)");
        System.out.println("Type 'exit' to quit.\n");

        // Load RAG engine
        RAGEngine rag = new RAGEngine();

        // Load all PDFs from /data folder
        try {
            rag.loadPdfsFromFolder("data");
        } catch (Exception e) {
            System.out.println("Error loading PDFs: " + e.getMessage());
            return;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("\nEnter your question: ");
            String question = br.readLine();

            if (question == null) continue;
            question = question.trim();

            if (question.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            System.out.println("\n--- Answer ---");
            String answer = rag.answerQuestion(question, 5); // top 5 chunks
            System.out.println(answer);
            System.out.println("--------------");
        }
    }
}
