package manga.process.subtitle;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphPath;
import org.jgrapht.WeightedGraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultListenableGraph;
import org.jgrapht.graph.GraphWalk;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.util.CollectionUtils;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import manga.detect.Speaker;

/**
 * @author PuiWa
 *
 */
public class SubtitleCompressor {
	// initialize properties and pipeline
	private static SubtitleCompressor instance = new SubtitleCompressor();
	
	private static int sentCounter = 0; // for testing
	
	private static StanfordCoreNLP pipeline;
	
	//******************import variables, can be adjusted*************
    
    // directed graph
	private static ListenableDirectedWeightedGraph<WordPosTuple, CustomEdge> wordGraph = 
			new ListenableDirectedWeightedGraph<>(CustomEdge.class);
	private static WordPosTuple startNode;
	private static WordPosTuple endNode;
	
	// stopwords will be loaded and concatenated with the separator
	private static String stopWordSeparator = ",";
	private static String linkedStopwords = "";
	
	private static String punct_tag = "PUNCT";
	
	// minimum number of words for compression
	private static int nb_words = 6;
	
	// at least one verb for each compression
	private static String[] verbs = {"VB", "VBD", "VBP", "VBZ", "VH", "VHD", "VHP", "VV", "VVD", "VVP", "VVZ"};
	
	// number of sentences to generate, can be adjusted
	private static int nb_senetences = 50;
	
	private SubtitleCompressor() {
		if (pipeline == null) {
			// creates a StanfordCoreNLP object, with POS tagging, lemmatization,
			// NER, parsing, and coreference resolution
			Properties props = new Properties();
			
			/**
			 * tokenize: divides text into a sequence of words
			 * ssplit: Splits a sequence of tokens into sentences(?)
			 * pos: Labels tokens with their POS tag
			 * lemma: distinguish words in different forms, like plural form and tenses
			 * ner: Named Entity Recognizer
			 * parse: syntax analysis
			 * dcoref: coreference resolution
			 */
//			props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
			// ssplit depends on tokenize
			props.put("annotators", "tokenize, ssplit, pos, lemma");
			
			pipeline = new StanfordCoreNLP(props);
		}
	}

	private static List<CoreMap> detectSentences(String paragraph) {
		List<CoreMap> sentenceList = new ArrayList<>();

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(paragraph);

		// run all Annotators on this text
		pipeline.annotate(document);

		// these are all the sentences in this document
		// a CoreMap is essentially a Map that uses class objects as keys and
		// has values with custom types
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);

		for (CoreMap sentence : sentences) {
			String sentenceStr = sentence.toString();
			
			// ellipsis("...") at the end of sentence will be parsed as ".." with "." as next sentence
			if (sentenceStr.length() == 1 && StringUtils.isPunct(sentenceStr)) {
				sentenceStr = null;
			}
			
			if (sentenceStr != null && !sentenceStr.isEmpty()) {
				sentenceList.add(sentence);
			}
			
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
//			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
//				// this is the text of the token
//				String word = token.get(TextAnnotation.class);
//				// this is the POS tag of the token
//				String pos = token.get(PartOfSpeechAnnotation.class);
//				// this is the NER label of the token
//				String ne = token.get(NamedEntityTagAnnotation.class);
//			}
		}
		return sentenceList;
	}

	/**
	 * Sentence based deletion
	 */
	private static List<CoreMap> removeRepeatedSentences(List<CoreMap> splitedSentences) {
		// sentence to be checked, updated after every checking
		List<CoreMap> sentencesToCheck = splitedSentences;
		// sentence to delete
		List<CoreMap> sentencesToDel = new ArrayList<>();
		// final sentence list to return
		List<CoreMap> sentencesToKeep = new ArrayList<>();

		// remove equal sentences
		List<List<CoreMap>> sentenceToCheckNgram = CollectionUtils.getNGrams(sentencesToCheck, 2, 2);
		for (List<CoreMap> sentenceList : sentenceToCheckNgram) {
			String currSentenceTxt = sentenceList.get(0).toString().toLowerCase();
			String nextSentenceTxt = sentenceList.get(1).toString().toLowerCase();
			if (currSentenceTxt.equals(nextSentenceTxt)) {
				sentencesToDel.add(sentenceList.get(1));
			}
		}
		sentencesToCheck.removeAll(sentencesToDel);
		
		// reset sentenceToDel list for next checking 
		sentencesToDel = new ArrayList<>();
		
		// remove partly repeated sentence
		sentenceToCheckNgram = CollectionUtils.getNGrams(sentencesToCheck, 2, 2);
		for (List<CoreMap> sentenceList : sentenceToCheckNgram) {
			// check every two sentences
			if (sentenceList.get(0) != null && sentenceList.get(1) != null) {
				List<CoreLabel> tokenListToInclude = new ArrayList<>();
				CoreMap baseSentence = null;
				CoreMap sentenceToCompare = null;
				// longer sentence may contain shorter sentence
				if (sentenceList.get(0).size() >= sentenceList.get(1).size()) {
					baseSentence = sentenceList.get(0);
					sentenceToCompare = sentenceList.get(1);
				} else {
					baseSentence = sentenceList.get(1);
					sentenceToCompare = sentenceList.get(0);
				}
				List<CoreLabel> sentenceToCompareTokenList = sentenceToCompare.get(TokensAnnotation.class);
				// get clean token list (remove punctuation)
				for (CoreLabel token : sentenceToCompareTokenList) {
					if (!StringUtils.isPunct(token.get(TextAnnotation.class))) {
						tokenListToInclude.add(token);
					}
				}
				// check if base sentence contains tokens in same order as shorter sentence
				List<List<CoreLabel>> baseTokenListNgram = 
						CollectionUtils.getNGrams(
								baseSentence.get(TokensAnnotation.class), 
								tokenListToInclude.size(), 
								tokenListToInclude.size());
				for (List<CoreLabel> tokenList : baseTokenListNgram) {
					String shorterSentenceTxt = PTBTokenizer.labelList2Text(tokenListToInclude).toLowerCase();
					String longerSentenceTxt = PTBTokenizer.labelList2Text(tokenList).toLowerCase();
					if (longerSentenceTxt.contains(shorterSentenceTxt)) {
						sentencesToDel.add(baseSentence);
					}
				}
			}
		}
		sentencesToCheck.removeAll(sentencesToDel);
		
		// reset sentenceToDel list for next checking 
		sentencesToDel = new ArrayList<>();
		
		// if no more checking
		sentencesToKeep = sentencesToCheck;
		
		return sentencesToKeep;
	}

	/**
	 * Word based deletion for each sentence
	 */
	private static List<CoreMap> removeRepeatedWords(List<CoreMap> processedSentences) {
		List<CoreMap> newSenetenceList = new ArrayList<>();
		for (CoreMap sentence : processedSentences) {
			List<CoreLabel> tokensToKeep = sentence.get(TokensAnnotation.class);
			List<CoreLabel> tokensToDel = new ArrayList<>();
			List<List<CoreLabel>> tokenToKeepNgram = CollectionUtils.getNGrams(tokensToKeep, 2, tokensToKeep.size()-1);
			for (List<CoreLabel> tokenList : tokenToKeepNgram) {
				
			}
		}
		return null;
	}
	
	private static String getTxtFromCoreMapList(List<CoreMap> processedSentences) {
		String linkedTxt = "";
		for (int i=0; i<processedSentences.size(); i++) {
			String currSentenceTxt = processedSentences.get(i).toString();
			// deal with the problem of treating last dot of ellipsis as sentence boundary
			if (currSentenceTxt.contains("..") && (currSentenceTxt.charAt(currSentenceTxt.lastIndexOf("..")-1) != '.')) {
				currSentenceTxt = currSentenceTxt+".";
			}
			// extra one space after sentence, except for last sentence
			if (i<processedSentences.size()-1) {
				linkedTxt = linkedTxt+currSentenceTxt+" ";
			} else {
				linkedTxt = linkedTxt+currSentenceTxt;
			}
		}
		return linkedTxt;
	}
	
	public static Subtitle summarizeSubtitles(ArrayList<Subtitle> subtitlesOfSameSpeaker) {
		String linkedSubtitleTxt = Subtitle.getLinkedSubtitlesText(subtitlesOfSameSpeaker);
		
		List<CoreMap> splitedSentences = detectSentences(linkedSubtitleTxt);
		
		List<CoreMap> processedSentences = removeRepeatedSentences(splitedSentences);
		
		String processedSubtitleTxt = compressSentences(processedSentences);
		
		if (processedSubtitleTxt == null) {
			// return original subtitle if no compression
			processedSubtitleTxt = getTxtFromCoreMapList(processedSentences);
		}
		
		// trim extra space and capitalize if subtitle starts with alphabet 
		processedSubtitleTxt = processedSubtitleTxt.trim();
		if (Character.isAlphabetic(processedSubtitleTxt.charAt(0))) {
			processedSubtitleTxt = Character.toUpperCase(processedSubtitleTxt.charAt(0)) + processedSubtitleTxt.substring(1);
		}
		
		// dummy code, to be changed
		double sTime = subtitlesOfSameSpeaker.get(0).getsTime();
		double eTime = subtitlesOfSameSpeaker.get(0).geteTime();
		if (subtitlesOfSameSpeaker.size() > 1) {
			eTime = subtitlesOfSameSpeaker.get(subtitlesOfSameSpeaker.size()-1).geteTime();
		}
		
		Subtitle summarizedSubtitle = new Subtitle(sTime, eTime, processedSubtitleTxt);
		
		Speaker speaker = null;
		for (Subtitle subtitle : subtitlesOfSameSpeaker) {
			if (subtitle.getSpeaker() != null) {
				speaker = subtitle.getSpeaker();
				break;
			}
		}
		summarizedSubtitle.setSpeaker(speaker);
		// dummy code, to be changed
		
		return summarizedSubtitle;
	}

	//*****************Sentence compression*************************
	private static String compressSentences(List<CoreMap> processedSentences) {
		initialize();
		
		// add start and end node to the graph
		startNode = new WordPosTuple("-start-", "-start-");
		wordGraph.addVertex(startNode);
		endNode = new WordPosTuple("-end-", "-end-");
		wordGraph.addVertex(endNode);
		
		List<ArrayList<WordPosTuple>> sentenceList = new ArrayList<>();
		
		for (CoreMap sentence : processedSentences) {
			ArrayList<WordPosTuple> wordPosTupleList = new ArrayList<>();
			wordPosTupleList.add(startNode);
			// traversing the words in the current sentence
			// a CoreLabel is a CoreMap with additional token-specific methods
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// this is the text of the token
				String word = token.get(TextAnnotation.class);
				// this is the POS tag of the token
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (StringUtils.isPunct(word)) {
					wordPosTupleList.add(new WordPosTuple(word, punct_tag));
				} else {
					wordPosTupleList.add(new WordPosTuple(word.toLowerCase(), pos));
				}
			}
			wordPosTupleList.add(endNode);
			sentenceList.add(wordPosTupleList);
		}
		
		compute_statistics(sentenceList);
		
		build_graph(sentenceList);
		
		GraphPath<WordPosTuple, CustomEdge> compression = get_compression(nb_senetences);
		
		if (compression != null) {
			return graphPathToText(compression);
		} else {
			return null;
		}
		
	}
	
    private static void initialize() {
		loadStopwordsList();
    	wordGraph = new ListenableDirectedWeightedGraph<>(CustomEdge.class);
	}

	private static void loadStopwordsList() {
    	if (linkedStopwords.length() == 0) {
        	BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(SubtitleCompressor.class.getResource("/stopwords.txt").getPath().substring(1)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		    try {
		        StringBuilder sb = new StringBuilder();
		        String line = br.readLine();

		        while (line != null) {
		            sb.append(line.trim().toLowerCase());
		            sb.append(stopWordSeparator);
		            line = br.readLine();
		        }
		        linkedStopwords = sb.delete(sb.length()-stopWordSeparator.length(), sb.length()).toString();
		    } catch (IOException e) {
				e.printStackTrace();
			} finally {
		        try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    }
    	}
	}
    
	private static String graphPathToText(GraphPath<WordPosTuple, CustomEdge> p) {
		if (p != null) {
			String sentenceText = "";
			for (WordPosTuple wpt : p.getVertexList()) {
				if (!wpt.equals(startNode) && !wpt.equals(endNode)) {
					sentenceText = sentenceText+" "+wpt.getText();
				}
			}
			return sentenceText;
		} else {
			return null;
		}
	}
	
	private static GraphPath<WordPosTuple, CustomEdge> get_compression(int nb_senetences) {
		List<GraphPath<WordPosTuple, CustomEdge>> kshortestpaths = getKShortestPaths(nb_senetences);
		
		if (kshortestpaths.size() > 0) {
			return getCompressionWithHighestScore(kshortestpaths);
		} else {
			// if no k shortest paths
			return null;
		}
	}
	
	/**
	 * For evaluation and testing
	 */
	private static GraphPath<WordPosTuple, CustomEdge> getCompressionWithHighestScore(
			List<GraphPath<WordPosTuple, CustomEdge>> kshortestpaths) {
		HashMap<GraphPath<WordPosTuple, CustomEdge>, Double> pathScoreMap = new HashMap<>();
		for (GraphPath<WordPosTuple, CustomEdge> path : kshortestpaths) {
			double normalizedScore = (double) path.getWeight() / (double) path.getLength();
			pathScoreMap.put(path, normalizedScore);
//			System.out.printf("%f %s\n", normalizedScore, graphPathToText(path));
		}
		
		// compute compression with highest score
		double maxScore = 0.0;
		GraphPath<WordPosTuple, CustomEdge> selectedPath = null;
		for (Map.Entry<GraphPath<WordPosTuple, CustomEdge>, Double> entry : pathScoreMap.entrySet()) {
		    GraphPath<WordPosTuple, CustomEdge> path = entry.getKey();
		    double score = entry.getValue().doubleValue();
		    if (score > maxScore) {
		    	maxScore = score;
		    	selectedPath = path;
		    }
		}
		return selectedPath;
	}
	
	private static List<GraphPath<WordPosTuple, CustomEdge>> getKShortestPaths(int nb_senetences2) {
		List<GraphPath<WordPosTuple, CustomEdge>> kshortestpaths = new ArrayList<>();
		
		// vertex with weight
		List<Triplet<Double, WordPosTuple, Integer>> orderedX = new ArrayList<>();
		Triplet<Double, WordPosTuple, Integer> triplet = new Triplet<>((Double) 0.0, startNode, (Integer) 0);
		orderedX.add(triplet);
		
		HashMap<Triplet<Double, WordPosTuple, Integer>, ArrayList<WordPosTuple>> paths = new HashMap<>();
		ArrayList<WordPosTuple> tuples = new ArrayList<>();
		tuples.add(startNode);
		paths.put(triplet, tuples);
		
		HashMap<WordPosTuple, Integer> visited = new HashMap<>();
		visited.put(startNode, 0);
		
		// Initialize the sentence container that will be used to remove 
        // duplicate sentences passing through different nodes
		List<String> sentence_container = new ArrayList<>();
		
		// While the number of shortest paths isn't reached or all paths explored
		while (kshortestpaths.size() < nb_senetences && orderedX.size() > 0) {
			Triplet<Double, WordPosTuple, Integer> shortest = orderedX.get(0);
			ArrayList<WordPosTuple> shortestPath = paths.get(shortest);
			
			// Removing the shortest node from X and paths
			orderedX.remove(shortest);
			paths.remove(shortest);
			
			// Iterating over the accessible nodes
			for (CustomEdge edge : wordGraph.outgoingEdgesOf(shortest.getNode())) {
				
				// To avoid loops
				if (shortestPath.contains(wordGraph.getEdgeTarget(edge)))
					continue;
				
				// Compute the weight to node
				double w = shortest.getWeight()+wordGraph.getEdgeWeight(edge);
				
				// If found the end, adds to k-shortest paths
				if (wordGraph.getEdgeTarget(edge).equals(endNode)) {
					/**
					 * Constraints on the shortest paths
					 * 
					 * 1. Check if path contains at least one werb 
					 * 2. Check the length of the shortest path, without 
					 *    considering punctuation marks and starting node (-1 
					 *    in the range loop, because nodes are reversed) 
					 * 3. Check the paired parentheses and quotation marks 
					 * 4. Check if sentence is not redundant
					 */
					int nb_verbs = 0;
					int length = 0;
					int paired_paratheses = 0;
					int quotation_mark_number = 0;
					String raw_sentence = "";
					
					for (int i = 0; i<shortestPath.size()-1; i++) {
						WordPosTuple currNode = shortestPath.get(i);
						String word = currNode.getText();
						String pos = currNode.getPos();
						
						// 1.
						if (Arrays.asList(verbs).contains(pos)) {
							nb_verbs+=1;
						}
						// 2.
						if (!punct_tag.equals(pos)) {
							length+=1;
						// 3.
						} else {
							if (word.equals("(")) {
								paired_paratheses-=1;
							} else if (word.equals(")")) {
								paired_paratheses+=1;
							} else if (word.equals("\"")) {
								quotation_mark_number+=1;
							}
						}
						// 4.
						raw_sentence = word + " " + raw_sentence;
					}
					
					// Remove extra space from sentence
					raw_sentence = raw_sentence.trim();
					
					if (nb_verbs > 0 
							&& length >= nb_words 
							&& paired_paratheses == 0 
							&& (quotation_mark_number % 2) == 0
							&& !sentence_container.contains(raw_sentence)) {
						List<WordPosTuple> path = new ArrayList<>();
						path.addAll(shortestPath);
						Collections.reverse(path);
						double weight = (double) w;
						kshortestpaths.add(new GraphWalk<>(wordGraph, startNode, endNode, getVisitedEdges(path), weight));
						sentence_container.add(raw_sentence);
					}
					
				} else {
					// test if node has already been visited
					WordPosTuple tupleToCheck = wordGraph.getEdgeTarget(edge);
					if (visited.containsKey(tupleToCheck)) {
						int newValue = visited.get(tupleToCheck) + 1;
						Integer integer = (Integer) newValue;
						visited.put(tupleToCheck, integer);
					} else {
						visited.put(tupleToCheck, (Integer) 0);
					}
					int hashcode = visited.get(tupleToCheck).hashCode();
					
					Triplet<Double, WordPosTuple, Integer> newTriplet = new Triplet<>(w, tupleToCheck, hashcode);
					
					// Add the node to orderedX
					orderedX.add(newTriplet);
					Collections.sort(orderedX, new Comparator<Triplet<Double, WordPosTuple, Integer>>(){

						@Override
						public int compare(Triplet<Double, WordPosTuple, Integer> o1,
								Triplet<Double, WordPosTuple, Integer> o2) {
							if (o1.getX() > o2.getX()) return 1;
							if (o1.getX() == o2.getX()) return 0;
							if (o1.getX() < o2.getX()) return -1;
							return 0;
						}
						
					});
					
					// Add the node to paths
					ArrayList<WordPosTuple> newTuples = new ArrayList<>();
					newTuples.add(tupleToCheck);
					newTuples.addAll(shortestPath);
					paths.put(newTriplet, newTuples);
				}
			}
		}
		
		return kshortestpaths;
	}

	private static List<CustomEdge> getVisitedEdges(List<WordPosTuple> visitedNodes) {
		List<CustomEdge> visitedEdges = new ArrayList<>();
		for (int i = 0; i<visitedNodes.size()-1; i++) {
			visitedEdges.add(wordGraph.getEdge(visitedNodes.get(i), visitedNodes.get(i+1)));
		}
		return visitedEdges;
	}
	
	private static void build_graph(List<ArrayList<WordPosTuple>> sentenceList) {
		for (int i = 0; i<sentenceList.size(); i++) {
			ArrayList<WordPosTuple> curr_sentence = sentenceList.get(i);
			
			int sentence_len = sentenceList.get(i).size();
			
			WordPosTuple[] mapping = new WordPosTuple[sentence_len];
			/**
			 * 1. non-stopwords for which no candidate exists in the graph or
			 * for which an unambiguous mapping is possible or which occur
			 * more than once in the sentence.
			 */
			for (int j = 0; j<sentence_len; j++) {
				WordPosTuple currTuple = curr_sentence.get(j);
				
				String word = currTuple.getText();
				String pos = currTuple.getPos();
				if (isStopword(word) || punct_tag.equals(pos)) {
					continue;
				}
				
				int k = ambiguous_nodes(currTuple);
				
				// If there is no node in the graph, create one with id = 0
				if (k == 0) {
					currTuple.setId(0);
					currTuple.addInfo(i, j);
					wordGraph.addVertex(currTuple);
					mapping[j] = currTuple;
				} else if (k == 1) {
					// Get the sentences id of this node
					WordPosTuple existingTuple = null;
					Set<WordPosTuple> allVertices = wordGraph.vertexSet();
					for (WordPosTuple wpt : allVertices) {
						if (wpt.equals(currTuple)) {
							existingTuple = wpt;
							break;
						}
					}
					Set<Integer> ids = existingTuple.getSentenceIdSet();
					// Update the node in the graph if not same sentence
					if (!ids.contains(i)) {
						existingTuple.setId(0);
						existingTuple.addInfo(i, j);
						wordGraph.addVertex(existingTuple);
						mapping[j] = existingTuple;
					// Else Create new node for redundant word
					} else {
						currTuple.setId(1);
						currTuple.addInfo(i, j);
						wordGraph.addVertex(currTuple);
						mapping[j] = currTuple;
					}
				}
			}
			
			/**
			 * 2. non-stopwords for which there are either several possible
			 * candidates in the graph.
			 */
			for (int j = 0; j<sentence_len; j++) {
				WordPosTuple currTuple = curr_sentence.get(j);
				
				String word = currTuple.getText();
				String pos = currTuple.getPos();
				if (isStopword(word) || punct_tag.equals(pos)) {
					continue;
				}
				
				// If word is not already mapped to a node
				if (mapping[j] == null) {
					WordPosTuple prev_tuple = getPrevious(curr_sentence, j);
					WordPosTuple next_tuple = getNext(curr_sentence, j);
					
					int k = ambiguous_nodes(currTuple);
					
					HashMap<WordPosTuple, Integer> ambinode_overlap = new HashMap<>();
					HashMap<WordPosTuple, Integer> ambinode_frequency = new HashMap<>();
					
					for (int c=0; c<k; c++) {
						WordPosTuple tupleToCheck = new WordPosTuple(currTuple, c);
						Set<WordPosTuple> allTuples = wordGraph.vertexSet();
						for (WordPosTuple t : allTuples) {
							if (t.equals(tupleToCheck)) {
								tupleToCheck = t;
								break;
							}
						}
						List<WordPosTuple> l_context = get_directed_context(sentenceList, tupleToCheck, "left", false);
						List<WordPosTuple> r_context = get_directed_context(sentenceList, tupleToCheck, "right", false);
						
						int l_context_count = 0;
						int r_context_count = 0;
						for (WordPosTuple t : l_context) {
							if (t.hasSameTxtAndPos(prev_tuple))
								l_context_count++;
						}
						for (WordPosTuple t : r_context) {
							if (t.hasSameTxtAndPos(next_tuple))
								r_context_count++;
						}
						
						int val = l_context_count+r_context_count;
						
						ambinode_overlap.put(tupleToCheck, val);
						
						ambinode_frequency.put(tupleToCheck, tupleToCheck.getInfoList().size());
					}
					
					boolean found = false;
					WordPosTuple selected = null;
					while (!found) {
						// Select the ambiguous node
						selected = max_index(ambinode_overlap);
						
						if (ambinode_overlap.get(selected) == null) {
							selected = max_index(ambinode_frequency);
						}
						
						// Get the sentences id of this node
						Set<Integer> ids = new HashSet<>();
						if (selected != null) {
							ids = selected.getSentenceIdSet();
						}
						
						// Test if there is no loop
	                    if (!ids.contains(i)) {
	                    	found = true;
	                    	break;
	                	// Remove the candidate from the lists
	                    } else {
	                    	ambinode_overlap.remove(selected);
	                    	ambinode_frequency.remove(selected);
	                    }
	                    
	                    // Avoid endless loops
	                    if (ambinode_overlap.isEmpty() && ambinode_frequency.isEmpty()) {
	                    	break;
	                    }
					}
			                
	                // Update the node in the graph if not same sentence
					if (found) {
						selected.addInfo(i, j);
						wordGraph.addVertex(selected);
						mapping[j] = selected;
					} else {
						// Else create new node for redundant word
						currTuple.setId(k);
						currTuple.addInfo(i, j);
						wordGraph.addVertex(currTuple);
						mapping[j] = currTuple;
					}
				}
			}
			
			/**
			 * 3. map the stopwords to the nodes
			 */
			for (int j = 0; j<sentence_len; j++) {
				WordPosTuple currTuple = curr_sentence.get(j);
				String word = currTuple.getText();
				
				// If *NOT* stopword, continues
				if (!isStopword(word)) {
					continue;
				}
				
				// Find the number of ambiguous nodes in the graph
				int k = ambiguous_nodes(currTuple);
				
				// If there is no node in the graph, create one with id = 0
				if (k == 0) {
					// Add the node in the graph
					currTuple.setId(0);
					currTuple.addInfo(i, j);
					wordGraph.addVertex(currTuple);
					mapping[j] = currTuple;
				// Else find the node with overlap in context or create one
				} else {
					WordPosTuple prev_tuple = getPrevious(curr_sentence, j);
					WordPosTuple next_tuple = getNext(curr_sentence, j);
					
					HashMap<WordPosTuple, Integer> ambinode_overlap = new HashMap<>();
					
					for (int c=0; c<k; c++) {
						WordPosTuple tupleToCheck = new WordPosTuple(currTuple, c);
						Set<WordPosTuple> allTuples = wordGraph.vertexSet();
						for (WordPosTuple t : allTuples) {
							if (t.equals(tupleToCheck)) {
								tupleToCheck = t;
								break;
							}
						}
						List<WordPosTuple> l_context = get_directed_context(sentenceList, tupleToCheck, "left", true);
						List<WordPosTuple> r_context = get_directed_context(sentenceList, tupleToCheck, "right", true);
						
						int l_context_count = 0;
						int r_context_count = 0;
						for (WordPosTuple t : l_context) {
							if (t.hasSameTxtAndPos(prev_tuple))
								l_context_count++;
						}
						for (WordPosTuple t : r_context) {
							if (t.hasSameTxtAndPos(next_tuple))
								r_context_count++;
						}
						
						int val = l_context_count+r_context_count;
						
						ambinode_overlap.put(tupleToCheck, val);
					}
					
					// Get best overlap candidate
					WordPosTuple selected = max_index(ambinode_overlap);
					
					// Get the sentences id of this node
					Set<Integer> ids = new HashSet<>();
					if (selected != null)
						ids = selected.getSentenceIdSet();
					
					// Update the node in the graph if not same sentence and
					// there is at least one overlap in context
					if (selected != null && !ids.contains(i) && ambinode_overlap.get(selected).intValue() > 0) {
						// Update the node in the graph
						selected.addInfo(i, j);
						mapping[j] = selected;
					// Else create a new node
					} else {
						// Add the node in the graph
						currTuple.setId(k);
						currTuple.addInfo(i, j);
						wordGraph.addVertex(currTuple);
						mapping[j] = currTuple;
					}
				}
			}
			
			/**
			 * 4. lastly map the punctuation marks to the nodes
			 */
			for (int j = 0; j<sentence_len; j++) {
				WordPosTuple currTuple = curr_sentence.get(j);
				
				if (!punct_tag.equals(currTuple.getPos())) {
					continue;
				}
				
				int k = ambiguous_nodes(currTuple);
				
				if (k == 0) {
					currTuple.setId(0);
					currTuple.addInfo(i, j);
					wordGraph.addVertex(currTuple);
					mapping[j] = currTuple;
				} else {
					WordPosTuple prevTuple = getPrevious(curr_sentence, j);
					WordPosTuple nextTuple = getNext(curr_sentence, j);
					
					HashMap<WordPosTuple, Integer> ambinode_overlap = new HashMap<>();
					
					for (int c=0; c<k; c++) {
						WordPosTuple tupleToCheck = new WordPosTuple(currTuple, c);
						Set<WordPosTuple> allTuples = wordGraph.vertexSet();
						for (WordPosTuple t : allTuples) {
							if (t.equals(tupleToCheck)) {
								tupleToCheck = t;
								break;
							}
						}
						List<WordPosTuple> l_context = get_directed_context(sentenceList, tupleToCheck, "left", true);
						List<WordPosTuple> r_context = get_directed_context(sentenceList, tupleToCheck, "right", true);
						
						int l_context_count = 0;
						int r_context_count = 0;
						for (WordPosTuple t : l_context) {
							if (t.hasSameTxtAndPos(prevTuple))
								l_context_count++;
						}
						for (WordPosTuple t : r_context) {
							if (t.hasSameTxtAndPos(nextTuple))
								r_context_count++;
						}
						
						int val = l_context_count+r_context_count;
						
						ambinode_overlap.put(tupleToCheck, val);
					}
					
					WordPosTuple selected = max_index(ambinode_overlap);
					
					Set<Integer> ids = new HashSet<>();
					if (selected != null) {
						ids = selected.getSentenceIdSet();
					}

					if (selected != null && !ids.contains(i) && ambinode_overlap.get(selected).intValue() > 1) {
						selected.addInfo(i, j);
						wordGraph.addVertex(selected);
						mapping[j] = selected;
					} else {
						currTuple.setId(k);
						currTuple.addInfo(i, j);
						wordGraph.addVertex(currTuple);
						mapping[j] = currTuple;
					}
				}
			}
			
			/**
			 * 4. Connects the mapped words with directed edges
			 */
			for (int j = 1; j<mapping.length; j++) {
				wordGraph.addEdge(mapping[j-1], mapping[j]);
			}
		}
		
		// Assigns a weight to each node in the graph
		Set<CustomEdge> allEdges = wordGraph.edgeSet();
		for (CustomEdge edge : allEdges) {
			// Get the list of (sentence_id, pos_in_sentence) for node1
			WordPosTuple source = wordGraph.getEdgeSource(edge);
			// Get the list of (sentence_id, pos_in_sentence) for node2
			WordPosTuple target = wordGraph.getEdgeTarget(edge);
			
			// set the edge weight except for edges with start/end nodes
			double edge_weight = get_edge_weight(sentenceList, source, target);
			wordGraph.setEdgeWeight(edge, edge_weight);
		}
	}
	
	private static double get_edge_weight(List<ArrayList<WordPosTuple>> sentenceList, WordPosTuple source, WordPosTuple target) {
		// Compute the weight of an edge between source node and target node.
		// It is computed as e_ij = (A / B) / C with:
		// 
		// - A = freq(i) + freq(j), 
		// - B = Sum (s in S) 1 / diff(s, i, j)
		// - C = freq(i) * freq(j)
		//
		// A node is a tuple of ('word/POS', unique_id).
		
		ArrayList<InfoTuple> info1 = source.getInfoList();
		ArrayList<InfoTuple> info2 = target.getInfoList();
		
		int freq1 = info1.size();
		int freq2 = info2.size();
		
		ArrayList<Double> diff = new ArrayList<>(); 
		
		for (int s=0; s<sentenceList.size(); s++) {
			ArrayList<Integer> pos_i_in_s = new ArrayList<>();
			ArrayList<Integer> pos_j_in_s = new ArrayList<>();
			
			for (InfoTuple itp : info1) {
				if (itp.getSentenceId() == s) {
					pos_i_in_s.add(itp.getPositionInSentence());
				}
			}
			
			for (InfoTuple itp : info2) {
				if (itp.getSentenceId() == s) {
					pos_j_in_s.add(itp.getPositionInSentence());
				}
			}
			
			ArrayList<Double> all_diff_pos_i_j = new ArrayList<>();
			
			// Loop over all the i, j couples
			for (int x=0; x<pos_i_in_s.size(); x++) {
				for (int y=0; y<pos_j_in_s.size(); y++) {
					double diff_i_j = pos_i_in_s.get(x) - pos_j_in_s.get(y);
					// Test if word i appears *BEFORE* word j in s
					if (diff_i_j<0) {
						all_diff_pos_i_j.add(-1.0*diff_i_j);
					}
				}
			}
			
			// Add the minimum distance to diff (i.e. in case of multiple
			// occurrences of i or/and j in sentence s), 0 otherwise.
			if (all_diff_pos_i_j.size() > 0) {
				diff.add(1.0/Collections.min(all_diff_pos_i_j));
			} else {
				diff.add(0.0);
			}
		}
		
		int weight1 = freq1;
		int weight2 = freq2;
		
		double diffSum = 0.0;
		for (Double d : diff) {
			diffSum+=d.doubleValue();
		}
		
		double final_weight = ( (double) (freq1 + freq2) / (double) diffSum ) / (double) (weight1 * weight2);
		if (final_weight == Double.POSITIVE_INFINITY) {
			return 1.0;
		} else {
			return final_weight;
		}
	}

	private static List<WordPosTuple> get_directed_context(List<ArrayList<WordPosTuple>> sentenceList, WordPosTuple wpt, String dir, boolean non_pos) {
		// Define the context containers
		List<WordPosTuple> l_context = new ArrayList<>();
		List<WordPosTuple> r_context = new ArrayList<>();
		
		for (InfoTuple itp : wpt.getInfoList()) {
			// For all the sentence/position tuples
			WordPosTuple prev = getPrevious(sentenceList.get(itp.getSentenceId()), itp.getPositionInSentence());
			WordPosTuple next = getNext(sentenceList.get(itp.getSentenceId()), itp.getPositionInSentence());
			
			if (non_pos) {
				if (!isStopword(prev.getText())) {
					l_context.add(prev);
				}				
				if (!isStopword(next.getText())) {
					r_context.add(next);
				}
			} else {
				l_context.add(prev);
				r_context.add(next);
			}
		}
		
		// Returns the left (previous) context
		if (dir.equals("left")) {
			return l_context;
		// Returns the right (next) context
		} else if (dir.equals("right")) {
			return r_context;
		// Returns the whole context
		} else {
			l_context.addAll(r_context);
			return l_context;
		}
	}

	private static WordPosTuple max_index(HashMap<WordPosTuple, Integer> map) {
		if (map.isEmpty()) return null;
		
		int max_val = 0;
		WordPosTuple max_tuple = null;
   		for (Entry<WordPosTuple, Integer> entry : map.entrySet()) {
   			if (entry.getValue().intValue() > max_val) {
   				max_val = entry.getValue().intValue();
   				max_tuple = entry.getKey();
   			}
   		}
		return max_tuple;
	}

	private static int ambiguous_nodes(WordPosTuple currTuple) {
		int nb_node = 0;
		Set<WordPosTuple> allTuples = wordGraph.vertexSet();
		WordPosTuple tupleToCheck = currTuple;
		while (allTuples.contains(tupleToCheck)) {
			nb_node += 1;
			tupleToCheck = new WordPosTuple(tupleToCheck, nb_node);
		}
		return nb_node;
	}

	private static HashMap<WordPosTuple, Integer> compute_statistics(List<ArrayList<WordPosTuple>> sentenceList) {
		// key: tuple of word and pos
		// value: sentences which contain the tuple
		HashMap<WordPosTuple, List<ArrayList<WordPosTuple>>> terms = new HashMap<>();
		
		// loop over sentences
		for (ArrayList<WordPosTuple> sentence : sentenceList) {
			// for each tuple
			for (WordPosTuple tuple : sentence) {
				if (!terms.containsKey(tuple)) {
					// create new entry for new tuple
					List<ArrayList<WordPosTuple>> value = new ArrayList<>();
					value.add(sentence);
					terms.put(tuple, value);
				} else {
					// add sentence entry for existing tuple
					List<ArrayList<WordPosTuple>> value = terms.get(tuple);
					value.add(sentence);
					terms.put(tuple, value);
				}
			}
		}
		
		HashMap<WordPosTuple, Integer> term_freq = new HashMap<>();
		// for each tuple
   		for (Map.Entry<WordPosTuple, List<ArrayList<WordPosTuple>>> entry : terms.entrySet()) {
   			// compute frequency
   			WordPosTuple tuple = entry.getKey();
   			List<ArrayList<WordPosTuple>> sentences = entry.getValue();
   			term_freq.put(tuple, sentences.size());
   		}
   		return term_freq;
	}
	
    /**
     * a listenable directed multigraph that allows loops and parallel edges.
     */
	private static class ListenableDirectedWeightedGraph<V, E> extends DefaultListenableGraph<V, E>
			implements DirectedGraph<V, E>, WeightedGraph<V, E> {
		private static final long serialVersionUID = 1L;

		ListenableDirectedWeightedGraph(Class<E> edgeClass) {
			super(new DefaultDirectedWeightedGraph<>(edgeClass));
		}
	}
	
	private static WordPosTuple getNext(ArrayList<WordPosTuple> wordList, int idx) {
	    if (idx < 0 || idx+1 >= wordList.size()) return null;
	    return wordList.get(idx + 1);
	}

	private static WordPosTuple getPrevious(ArrayList<WordPosTuple> wordList, int idx) {
	    if (idx <= 0) return null;
	    return wordList.get(idx - 1);
	}

	private static boolean isStopword(String word) {
		ArrayList<String> stopwordList = new ArrayList<>(Arrays.asList(linkedStopwords.split(stopWordSeparator)));
		if (stopwordList.contains(word.toLowerCase())) {
			return true;
		}
		return false;
	}
}
