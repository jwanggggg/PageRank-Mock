import java.io.*;
import java.util.*;

public class PageRank {
	// Calculates TFIDF scores for abstracts/queries and then cosine similarities.
	public static void main(String[] args) throws IOException {
		// queries = cran.qry, abstracts = cran.all.1400
		File queries = new File(args[0]);
		File abstracts = new File(args[1]);
		File submission = new File("output.txt");
		
		Scanner queriesInput = new Scanner(queries);
		Scanner abstractsInput = new Scanner(abstracts);
		
		Hashtable<Integer, Hashtable<String, Integer>> queryTFs = new Hashtable<Integer, Hashtable<String, Integer>>();
		Hashtable<String, int[]> queryIDF = new Hashtable<String, int[]>();
		
		Hashtable<Integer, Hashtable<String, Double>> queryTFIDF = new Hashtable<Integer, Hashtable<String, Double>>();
		
		// Populate TF, IDF, and returns total # of documents
		int queryTotalDocuments = populateQueryInstancesTFIDF(queries, queriesInput, queryTFs, queryIDF, queryTFIDF);
		
		// Now go through query vector and change the value to TFIDF (original is TF)
		
		calculateTFIDF(queryTFs, queryIDF, queryTFIDF, queryTotalDocuments);
		
		// ---------------------------------------------------------------------------------
		
		// Identical process for abstracts
		
		Hashtable<Integer, Hashtable<String, Integer>> abstractTFs = new Hashtable<Integer, Hashtable<String, Integer>>();
		Hashtable<String, int[]> abstractIDF = new Hashtable<String, int[]>();
		Hashtable<Integer, Hashtable<String, Double>> abstractTFIDF = new Hashtable<Integer, Hashtable<String, Double>>();
		
		int abstractTotalDocuments = populateQueryInstancesTFIDF(abstracts, abstractsInput, abstractTFs, abstractIDF, abstractTFIDF);
		
		calculateTFIDF(abstractTFs, abstractIDF, abstractTFIDF, abstractTotalDocuments);
		
		queriesInput.close();
		abstractsInput.close();
		
		// ---------------------------------------------------------------------------------
		
		// All vectors
		
		Hashtable<Integer, ArrayList<Hashtable<Integer, Double>>> allVectors = cosineSimilarities(queryTFIDF, abstractTFIDF);
		
		// Reverse sort the list due to inherent hashtable ordering
		
		Integer[] queryNums = Arrays.asList(allVectors.keySet().toArray()).toArray(new Integer[allVectors.keySet().toArray().length]);
		Arrays.sort(queryNums);
		
		if (!submission.exists()) {
			PrintWriter output = new PrintWriter(submission);
			for (int i : queryNums) {
				ArrayList<Hashtable<Integer, Double>> currentList = allVectors.get(i);
				for (Hashtable<Integer, Double> currentHashtable : currentList) {
					for (int abs : currentHashtable.keySet()) {
						output.printf(i + " " +  abs + " " + "%.9f\n", currentHashtable.get(abs));
					}	
				}
			}
			output.close();
		}
	}
	
	// Go through the queries and compare them to each abstract
	
	public static Hashtable<Integer, ArrayList<Hashtable<Integer, Double>>> cosineSimilarities(Hashtable<Integer, Hashtable<String, Double>> queryTFIDF, Hashtable<Integer, Hashtable<String, Double>> abstractTFIDF) {
		// The integer is the query number
		// ArrayList is list of hashtables
		// Integer is abstract number, double is cosine similarity
		Hashtable<Integer, ArrayList<Hashtable<Integer, Double>>> allVectors = new Hashtable<Integer, ArrayList<Hashtable<Integer, Double>>>();
		
		for (int currentQueryNum : queryTFIDF.keySet()) {
			Hashtable<String, Double> currentQueryTFIDF = queryTFIDF.get(currentQueryNum);
			// Insert query numbers into allVectors
			allVectors.put(currentQueryNum, new ArrayList<Hashtable<Integer, Double>>());
			
			for (int currentAbstractNum : abstractTFIDF.keySet()) {
				Hashtable<String, Double> currentAbstractTFIDF = abstractTFIDF.get(currentAbstractNum);
				
				double numerator = 0;
				double queryDenominator = 0;
				double abstractDenominator = 0;
				
				// Sum numerator, denominator for current query's values
				
				for (String queryWord : currentQueryTFIDF.keySet()) {
					
					if (currentAbstractTFIDF.keySet().contains(queryWord)) {
						numerator += (currentQueryTFIDF.get(queryWord) * currentAbstractTFIDF.get(queryWord));
					}
					queryDenominator += Math.pow(currentQueryTFIDF.get(queryWord), 2);
					
				} // end queryWord for loop
				
				// Sum up the current abstract's values
				
				for (double TFIDF : currentAbstractTFIDF.values()) {
					abstractDenominator += Math.pow(TFIDF, 2);					
				}
				
				// Integer is the current abstract, double is current abstract's cosine similarity
				
				double cosineSimilarity = numerator/Math.sqrt((queryDenominator * abstractDenominator));
				
				// Some abstracts will be empty, set them to 0
				
				if (Double.isNaN(cosineSimilarity)) {
					cosineSimilarity = 0.0;
				}
					
				
				Hashtable<Integer, Double> vectorCosineSimilarity = new Hashtable<Integer, Double>();
				vectorCosineSimilarity.put(currentAbstractNum, cosineSimilarity); 
				
				// Now place vectorCosineSimilarity into the list sorted.
				
				ArrayList<Hashtable<Integer, Double>> currentQueryNumAbstract = allVectors.get(currentQueryNum);
				// Place cosine similarity, sorted
				placeVectorSorted(currentQueryNumAbstract, vectorCosineSimilarity, currentAbstractNum); 
			} // end currentAbstractNum for loop
			
		} // end currentQueryNum for loop
		
		return allVectors;
	}
	
	
	// This function places the vector, sorted, into the ArrayList
	
	public static void placeVectorSorted(ArrayList<Hashtable<Integer, Double>> currentQueryNumAbstracts,
										Hashtable<Integer, Double> vectorCosineSimilarity, int currentAbstractNum) {
		
		for (int i = 0; i < currentQueryNumAbstracts.size(); i++) {
			Hashtable<Integer, Double> currentAbstract = currentQueryNumAbstracts.get(i);
			
			// There's only one cosineSimilarity, so we can just use a for each loop
			
			for (double cosineSimilarity : currentAbstract.values()) {
				if (cosineSimilarity > vectorCosineSimilarity.get(currentAbstractNum))
					continue;
				currentQueryNumAbstracts.add(i, vectorCosineSimilarity);
				return;
			}
		}
		currentQueryNumAbstracts.add(vectorCosineSimilarity);
	}
	
	// Populates TFs and IDF
	
	public static int populateQueryInstancesTFIDF(File file, Scanner input, Hashtable<Integer, Hashtable<String, Integer>> TFs, 
												Hashtable<String, int[]> IDF, Hashtable<Integer, Hashtable<String, Double>> TFIDF) {
		String[] stopWords = {"a","the","an","and","or","but","about","above","after","along","amid","among",
                "as","at","by","for","from","in","into","like","minus","near","of","off","on",
                "onto","out","over","past","per","plus","since","till","to","under","until","up",
                "via","vs","with","that","can","cannot","could","may","might","must",
                "need","ought","shall","should","will","would","have","had","has","having","be",
                "is","am","are","was","were","being","been","get","gets","got","gotten",
                "getting","seem","seeming","seems","seemed",
                "enough", "both", "all", "your", "those", "this", "these", 
                "their", "the", "that", "some", "our", "no", "neither", "my",
                "its", "his", "her", "every", "either", "each", "any", "another",
                "an", "a", "just", "mere", "such", "merely", "right", "no", "not",
                "only", "sheer", "even", "especially", "namely", "as", "more",
                "most", "less", "least", "so", "enough", "too", "pretty", "quite",
                "rather", "somewhat", "sufficiently", "same", "different", "such",
                "when", "why", "where", "how", "what", "who", "whom", "which",
                "whether", "why", "whose", "if", "anybody", "anyone", "anyplace", 
                "anything", "anytime", "anywhere", "everybody", "everyday",
                "everyone", "everyplace", "everything", "everywhere", "whatever",
                "whenever", "whereever", "whichever", "whoever", "whomever", "he",
                "him", "his", "her", "she", "it", "they", "them", "its", "their","theirs",
                "you","your","yours","me","my","mine","I","we","us","much","and/or"
		};

		int totalDocuments = 0; // Will be used to calculate IDF
		int currentDocumentCount = 1;
		
		
		// If you encounter .W, turn a flag to true.
		// While the flag is true, read in files.
		// If you encounter .I, turn the flag off.
		
		boolean readIn = false;
		
		Hashtable<String, Integer> currentQueryTFs;
		Hashtable<String, Double> currentQueryTFIDF;
		int currentQueryNum = 0;
		
		// 1) Go through all the text and add words into a hashtable.
		while (input.hasNext()) {
			String line = input.nextLine();
			if (line.charAt(0) == '.' && line.charAt(1) == 'W') {
				readIn = true;
				totalDocuments++;
			}
				
			// Clear hashtable, instances' key becomes .001, value is hashtable
			else if (line.charAt(0) == '.' && line.charAt(1) == 'I') {
				readIn = false;
				currentQueryTFs = new Hashtable<String, Integer>();
				currentQueryNum = currentDocumentCount;
				currentQueryTFIDF = new Hashtable<String, Double>();
				TFs.put(currentQueryNum, currentQueryTFs);
				TFIDF.put(currentQueryNum, currentQueryTFIDF);
				currentDocumentCount++;
			}
			
			if (line.charAt(0) != '.' && readIn) { // Don't account for .I .T etc.
				// Remove punc/numbers, set to array
				String[] lineArray = line.replaceAll("[^a-zA-Z ]", "").toLowerCase().split(" ");
				
				for (String word : lineArray) {
					if (!isStopWord(word, stopWords)) { // Validate that this is not a stop word
						// Add one occurrence of word to current TFs
						if (TFs.get(currentQueryNum).containsKey(word))
							TFs.get(currentQueryNum).put(word, TFs.get(currentQueryNum).get(word) + 1);
						else
							TFs.get(currentQueryNum).put(word, 1);
						
						// Add one occurrence of word to total IDF
						if (IDF.containsKey(word)) {
							// Check that the number of times word has appeared in doc is 0.
							int[] appearancesInDoc = IDF.get(word);
							if (appearancesInDoc[1] == 0) {
							appearancesInDoc[0]++;
							appearancesInDoc[1]++;
						}
						
						}
						else {
							// 0 = num of doc appearances, 1 = number of times this word has appeared, 2 = IDF
							int[] appearancesInDoc = {1, 1}; 
							IDF.put(word, appearancesInDoc);
						}
					
					} // end if 
				} // end for
				
				resetIDFCounts(IDF);
			} // end if line.charAt(0) != '.'
			
		} // end while input.hasNext()
		return totalDocuments;	
	}
	
	// Checks for stop word
	
	public static boolean isStopWord(String word, String[] stopWords) {
		return Arrays.asList(stopWords).contains(word);
	}
	
	// Resets the IDF count
	
	public static void resetIDFCounts(Hashtable<String, int[]> IDF) {
		for (String key : IDF.keySet()) {
			IDF.get(key)[1] = 0;
		}
	}
	
	// Auxiliary print function
	
	public static void printIDF(Hashtable<String, int[]> IDF) {
		System.out.print("{");
		for (String key : IDF.keySet()) {
			System.out.print(key + "=" + Arrays.toString(IDF.get(key)));
		}
		System.out.print("}\n");
	}
	
	// Calculates the TFIDF for current vector
	
	public static void calculateTFIDF(Hashtable<Integer, Hashtable<String, Integer>> TFs, 
									Hashtable<String, int[]> IDF, Hashtable<Integer, Hashtable<String, Double>> TFIDF, 
									int totalDocuments) {
		
		for (int docNum : TFs.keySet()) {
			Hashtable<String, Integer> currentDocument = TFs.get(docNum);
			for (String word : currentDocument.keySet()) {
				int currentTF = currentDocument.get(word);
				double currentIDF = Math.log(((double)totalDocuments) / IDF.get(word)[0]);
				double currentTFIDF = currentTF * currentIDF;
				TFIDF.get(docNum).put(word, currentTFIDF);
			}
		}
	}
	
}
