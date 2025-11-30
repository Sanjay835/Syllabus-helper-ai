package com.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TextUtils {

    // Read full text from a PDF
    public static String readPdfText(String filePath) throws IOException {
        try (PDDocument doc = PDDocument.load(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    // Split long text into word chunks
    public static List<String> chunkText(String text, int chunkSizeWords, int overlapWords) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + chunkSizeWords, words.length);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                sb.append(words[i]).append(" ");
            }
            chunks.add(sb.toString().trim());
            if (end == words.length) break;
            start += (chunkSizeWords - overlapWords);
        }

        return chunks;
    }

    // Simple keyword-based score
    public static int keywordScore(String question, String chunkText) {
        Set<String> qWords = normalizeToSet(question);
        Set<String> cWords = normalizeToSet(chunkText);

        int score = 0;
        for (String w : qWords) {
            if (cWords.contains(w)) {
                score++;
            }
        }
        return score;
    }

    // Normalize text: lowercase, remove symbols, convert to set
    private static Set<String> normalizeToSet(String text) {
        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9 ]", " ");
        String[] words = cleaned.split("\\s+");

        Set<String> set = new HashSet<>();
        for (String w : words) {
            if (w.length() <= 2) continue;
            set.add(w);
        }
        return set;
    }
}
