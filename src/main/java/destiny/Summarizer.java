package destiny;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.CoreMap;
import org.apache.lucene.analysis.core.StopAnalyzer;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class to summarize text
 */
public class Summarizer {

    /**
     * To hopefully cut down on the number of times we have to summarize the same text,
     * we will store the summarized text in a map with the original text as the key.
     */
    static Map<String, String> summarizedTexts = new HashMap<>();

    StanfordCoreNLP pipeline;

    Summarizer(PrintStream file) throws FileNotFoundException {
        // Create a StanfordCoreNLP object with properties for text summarization
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, parse, lemma");
        props.setProperty("tokenize.options", "untokenizable=allDelete");
        PrintStream errorOutputFile;
        try {
            errorOutputFile = new PrintStream(WishlistGenerator.errorOutputFileName);
        } catch (FileNotFoundException e) {
            System.out.println("Error creating error file: " + e);
            throw new FileNotFoundException();
        }
        System.setOut(errorOutputFile);
        System.setErr(errorOutputFile);
        pipeline = new StanfordCoreNLP(props);
        System.setOut(file);
        System.setErr(file);
    }

    /**
     * Analyze the words from the given text
     *
     * @param text
     * @return List<List < HashMap < String, Object>>> with keys word, lemma, pos, ne
     * @url https://cs.nyu.edu/grishman/jet/guide/PennPOS.html for POS tags
     */
    public List<List<HashMap<String, Object>>> sentenceAnalyzer(String text) {
        Annotation document = new Annotation(text);
        // Run all the selected Annotators on this text
        pipeline.annotate(document);

        // Get a list of sentences, with a list of words and values for each word
        List<List<HashMap<String, Object>>> words = new ArrayList<>();
        for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
            List<HashMap<String, Object>> sentenceWords = new ArrayList<>();
            for (CoreLabel word : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                HashMap<String, Object> wordMap = new HashMap<>();
                wordMap.put("word", word.get(CoreAnnotations.TextAnnotation.class));
                wordMap.put("lemma", word.get(CoreAnnotations.LemmaAnnotation.class));
                wordMap.put("pos", word.get(CoreAnnotations.PartOfSpeechAnnotation.class));
                wordMap.put("ne", word.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                sentenceWords.add(wordMap);
            }
            words.add(sentenceWords);
        }
        return words;
    }

    /**
     * Analyze the words from the given text using the frequency of the words
     *
     * @param text
     * @return String
     */
    public String sentenceAnalyzerUsingFrequency(String text) {
        if (summarizedTexts.containsKey(text)) {
            return summarizedTexts.get(text);
        }
        Annotation document = new Annotation(text);
        pipeline.annotate(document);
        Map<String, Integer> wordFrequency = new HashMap<>();
        // Get the frequency of each word
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class);
                String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                if (!Objects.equals(pos, word) && !StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(word.toLowerCase())) {
                    if (wordFrequency.containsKey(word.toLowerCase())) {
                        wordFrequency.put(word.toLowerCase(), wordFrequency.get(word.toLowerCase()) + 1);
                    } else {
                        wordFrequency.put(word.toLowerCase(), 1);
                    }
                }
            }
        }
        // Normalize the frequency of each word
        Double maxFrequency;
        try {
            maxFrequency = Collections.max(wordFrequency.values()).doubleValue();
        } catch (Exception ignored) {
            return text;
        }
        HashMap<String, Double> wordFrequencyNormalized = new HashMap<>();
        for (String word : wordFrequency.keySet()) {
            wordFrequencyNormalized.put(word, (wordFrequency.get(word) / maxFrequency));
        }
        // Score each sentence based on the frequency of the words
        Map<CoreMap, Double> sentenceScore = new HashMap<>();
        for (CoreMap sentence : sentences) {
            Double score = 0.0;
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String word = token.get(CoreAnnotations.TextAnnotation.class).toLowerCase();
                if (wordFrequencyNormalized.containsKey(word)) {
                    score += wordFrequencyNormalized.get(word);
                }
            }
            sentenceScore.put(sentence, score);
        }
        // Get the number of sentences to keep (roughly 3 - 1/3 of the total number of sentences)
        Integer numSentencesToKeep = (int) Double.max(sentences.size() * (1.0 / 3), 3.0);
        List<Map.Entry<CoreMap, Double>> entryList = new ArrayList<>(sentenceScore.entrySet());
        Collections.sort(entryList, (entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        // Keep the top numSentencesToKeep sentences
        entryList = entryList.subList(0, Math.min(numSentencesToKeep, entryList.size()));
        entryList.sort(Comparator.comparingInt(entry -> sentences.indexOf(entry.getKey())));
        String summarizedText = entryList.stream()
                .map(entry -> entry.getKey().toString())
                .collect(Collectors.joining(" "));
        summarizedTexts.put(text, summarizedText);
        return summarizedText;
    }
}