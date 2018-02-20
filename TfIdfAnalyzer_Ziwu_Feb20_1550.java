package search.analyzers;

import datastructures.concrete.ChainedHashSet;
import datastructures.concrete.KVPair;
import datastructures.concrete.dictionaries.ChainedHashDictionary;
import datastructures.interfaces.IDictionary;
import datastructures.interfaces.IList;
import datastructures.interfaces.ISet;
import search.models.Webpage;

import java.net.URI;

/**
 * This class is responsible for computing how "relevant" any given document is
 * to a given search query.
 *
 * See the spec for more details.
 */
public class TfIdfAnalyzer {
    // This field must contain the IDF score for every single word in all
    // the documents.
    private IDictionary<String, Double> idfScores;

    // This field must contain the TF-IDF vector for each webpage you were given
    // in the constructor.
    //
    // We will use each webpage's page URI as a unique key.
    private IDictionary<URI, IDictionary<String, Double>> documentTfIdfVectors;

    // Feel free to add extra fields and helper methods.
    private IDictionary<URI, Double> normOfDocumentVectors;
    
    public TfIdfAnalyzer(ISet<Webpage> webpages) {
        // Implementation note: We have commented these method calls out so your
        // search engine doesn't immediately crash when you try running it for the
        // first time.
        //
        // You should uncomment these lines when you're ready to begin working
        // on this class.

        this.idfScores = this.computeIdfScores(webpages);
        this.documentTfIdfVectors = this.computeAllDocumentTfIdfVectors(webpages);
        this.normOfDocumentVectors = this.normHelper();
    }

    // Note: this method, strictly speaking, doesn't need to exist. However,
    // we've included it so we can add some unit tests to help verify that your
    // constructor correctly initializes your fields.
    public IDictionary<URI, IDictionary<String, Double>> getDocumentTfIdfVectors() {
        return this.documentTfIdfVectors;
    }

    // Note: these private methods are suggestions or hints on how to structure your
    // code. However, since they're private, you're not obligated to implement exactly
    // these methods: feel free to change or modify these methods however you want. The
    // important thing is that your 'computeRelevance' method ultimately returns the
    // correct answer in an efficient manner.

    /**
     * Return a dictionary mapping every single unique word found
     * in every single document to their IDF score.
     */
    private IDictionary<String, Double> computeIdfScores(ISet<Webpage> pages) {
        IDictionary<String, Double> dict = new ChainedHashDictionary<>();
        int size = pages.size();
        for (Webpage page : pages) {
            IList<String> words = page.getWords();
            ISet<String> record = new ChainedHashSet<>();
            for (String next : words) {
                if (!record.contains(next)) {
                    record.add(next);
                    if (dict.containsKey(next)) {
                        dict.put(next, dict.get(next)+1);
                    } else {
                        dict.put(next, 1.0);
                    }
                }
            }
        }
        for (KVPair<String, Double> pair : dict) {
            dict.put(pair.getKey(), Math.log(size/pair.getValue()));
        }
        return dict;
    }

    /**
     * Returns a dictionary mapping every unique word found in the given list
     * to their term frequency (TF) score.
     *
     * The input list represents the words contained within a single document.
     */
    private IDictionary<String, Double> computeTfScores(IList<String> words) {
        IDictionary<String, Double> dict = new ChainedHashDictionary<>();
        int size = words.size();
        for (String next : words) {
            if (dict.containsKey(next)) {
                dict.put(next, dict.get(next)+1);
            } else {
                dict.put(next, 1.0);
            }
        }
        for (KVPair<String, Double> pair : dict) {
            dict.put(pair.getKey(), pair.getValue() / size);
        }
        return dict;
    }

    /**
     * See spec for more details on what this method should do.
     */
    private IDictionary<URI, IDictionary<String, Double>> computeAllDocumentTfIdfVectors(ISet<Webpage> pages) {
        // Hint: this method should use the idfScores field and
        // call the computeTfScores(...) method.
        IDictionary<URI, IDictionary<String, Double>> dict = new ChainedHashDictionary<>();
        for (Webpage page : pages) {
            URI name = page.getUri();
            IList<String> words = page.getWords();
            IDictionary<String, Double> tfScores = computeTfScores(words);
            IDictionary<String, Double> docWordScores = new ChainedHashDictionary<>();
            for (String next : words) {
                if (!idfScores.containsKey(next)) {
                    docWordScores.put(next, 0.0);
                } else {
                    docWordScores.put(next, tfScores.get(next)*idfScores.get(next));
                }
            }
            dict.put(name, docWordScores);
        }
        return dict;
    }

    /**
     * Returns the cosine similarity between the TF-IDF vector for the given query and the
     * URI's document.
     *
     * Precondition: the given uri must have been one of the uris within the list of
     *               webpages given to the constructor.
     */
    public Double computeRelevance(IList<String> query, URI pageUri) {
        // Note: The pseudocode we gave you is not very efficient. When implementing,
        // this method, you should:
        //
        // 1. Figure out what information can be precomputed in your constructor.
        //    Add a third field containing that information.
        //
        // 2. See if you can combine or merge one or more loops.
        IDictionary<String, Double> documentVector = documentTfIdfVectors.get(pageUri);
        IDictionary<String, Double> queryVector = new ChainedHashDictionary<>();
        double numerator = 0.0;
        for (String word : query) {
            queryVector.put(word, computeTfScores(query).get(word)*idfScores.get(word));
            Double docWordScore = 0.0;
            if (documentVector.containsKey(word)) {
                docWordScore = documentVector.get(word);
            }
            Double queryWordScore = queryVector.get(word);
            numerator += docWordScore * queryWordScore;
        }
        Double denominator = normOfDocumentVectors.get(pageUri) * norm(queryVector);
        if (denominator != 0) {
            return numerator / denominator;
        }
        return 0.0;
    }
    
    private Double norm(IDictionary<String, Double> vector) {
        Double output = 0.0;
        for (KVPair<String, Double> pair : vector) {
            Double score = pair.getValue();
            output += score * score;
        }
        return Math.sqrt(output);
    }
    
    private IDictionary<URI, Double> normHelper() {
        IDictionary<URI, Double> norm = new ChainedHashDictionary<>();
        for (KVPair<URI, IDictionary<String, Double>> pair : documentTfIdfVectors) {
            norm.put(pair.getKey(), norm(pair.getValue()));
        }
        return norm;
    }
}
