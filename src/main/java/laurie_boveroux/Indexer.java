package laurie_boveroux;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.ByteBuffer;


public class Indexer{

    public static int memsize;
    public static int docid;
    public static int blockNumber;
    public static BufferedOutputStream invIndexDocidBuffer;
    public static BufferedOutputStream invIndexFreqBuffer;
    public static int totalNbBytes;

    public static String[] stopwordsList = { "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any",
				"are", "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both",
				"but", "by", "can't", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't", "doing",
				"don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
				"have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself",
				"him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is",
				"isn't", "it", "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no",
				"nor", "not", "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves",
				"out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so",
				"some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then",
				"there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those",
				"through", "to", "too", "under", "until", "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're",
				"we've", "were", "weren't", "what", "what's", "when", "when's", "where", "where's", "which", "while",
				"who", "who's", "whom", "why", "why's", "with", "won't", "would", "wouldn't", "you", "you'd", "you'll",
				"you're", "you've", "your", "yours", "yourself", "yourselves", "s", "t", "re", "ve", "m", "ll", "d" };


    public Indexer() throws FileNotFoundException{
        this.memsize = 10^8;
        this.docid = 0;
        this.blockNumber = 0;
        this.invIndexDocidBuffer = new BufferedOutputStream(new FileOutputStream("invIndexDocidBuffer"),4096 * 10000);
        this.invIndexFreqBuffer = new BufferedOutputStream(new FileOutputStream("IntermediateInvertedIndexFreq"),4096 * 10000);
        this.totalNbBytes = 0;
    }
 
    public static String preprocessingText(String text) throws UnsupportedEncodingException{
        byte[] bytes = text.getBytes("Windows-1252");
        // Decode the Windows-1252 or Latin-1 bytes back into UTF-8 to get the correct string
        text = new String(bytes, "UTF-8");
        // remove all the punctuation but keep '
        String preprocessedText = text.replaceAll("[^a-zA-Z0-9'\\s]", " ");
        // remove all the numbers
        preprocessedText = preprocessedText.replaceAll("[0-9]", " ");
        // remove all the multiple spaces
        preprocessedText = preprocessedText.replaceAll("\\s+", " ");
        // remove all the spaces at the beginning and at the end of the text
        preprocessedText = preprocessedText.trim();
        // convert all the text to lower case
        preprocessedText = preprocessedText.toLowerCase();
        // remove all the english stop words
        // for (String stopword : stopwords) {
        //     preprocessedText = preprocessedText.replaceAll("\\b" + stopword + "\\b", "");
        // }
        //preprocessedText = removeStopWords(preprocessedText);

        //Lemmatization
        preprocessedText = lemmatize(preprocessedText);

        return preprocessedText;
    }

    public static String lemmatize(String text) throws UnsupportedEncodingException{
        String[] words = text.split(" ");
        String lemmatizedText = "";
        for (String word : words) {
            if (word.length() > 2) {
                lemmatizedText += word + " ";
            }
        }
        return lemmatizedText;
    }

    public static void parseTsvFile(String collectionPath, int numberReadDoc) throws IOException{
        try{
            if (numberReadDoc == -1){
                numberReadDoc = Integer.MAX_VALUE;
            }            
            
            // Read the collection file
            File collectionFile = new File(collectionPath);
            LineIterator it = FileUtils.lineIterator(collectionFile, "UTF-8");

            int numDocCurr = 0;
    

            // For the whole collection
            while (it.hasNext() && numDocCurr < numberReadDoc) {

                PrintWriter writerDocument = new PrintWriter("DocumentIndex.txt", "UTF-8");
            
                //Setting the initial memory
                int totalMemory = (int) java.lang.Runtime.getRuntime().totalMemory();
                int usedMemory = (int) java.lang.Runtime.getRuntime().totalMemory() - (int) java.lang.Runtime.getRuntime().freeMemory();;

                Map<String, List<Integer>> dictionary = new LinkedHashMap<String, List<Integer>>();

                System.out.println("Memory full : new block in construction " + Integer.toString(usedMemory));

                // Where there is free memory
                while(usedMemory<0.95*totalMemory && it.hasNext() && numDocCurr < numberReadDoc){
                    usedMemory = (int) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                    totalMemory = (int) Runtime.getRuntime().totalMemory();

                    if (numDocCurr % 100000 == 0){
                        System.out.println("Number of documents read: " + numDocCurr);
                        System.out.println("Used Memory   :  " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) + " bytes");
                        System.out.println("Free Memory   : " + Runtime.getRuntime().freeMemory() + " bytes");
                        System.out.println("Total Memory  : " + Runtime.getRuntime().totalMemory() + " bytes");
                        System.out.println("Max Memory    : " + Runtime.getRuntime().maxMemory() + " bytes");            
                    }

                    String document = it.nextLine(); // one line = one document
                    String[] documentArray = document.split("\t"); // docNo and text are separated by a tabulation
                    
                    // Information about the document
                    Integer docNo = Integer.parseInt(documentArray[0]);
                    String text = documentArray[1];
                    String preprocessedText = preprocessingText(text); // preprocess the text
                    String[] terms = preprocessedText.split(" "); // split the text into terms
                    
                    int nbStopWords = 0;
                    //Looping through all the terms
                    for (String term : terms){
                        // if term is in stopword list, skip it
                        if (Arrays.asList(stopwordsList).contains(term) || term.length() == 0){
                            nbStopWords++;
                            continue;
                        }                      
                        // The posting list is a list of integer
                        List<Integer> postingsList;

                        if (dictionary.get(term) == null){
                            //add the term to the dictionary
                            postingsList = new ArrayList<Integer>();
                            dictionary.put(term, postingsList);
                        }
                        else{
                            postingsList = dictionary.get(term);
                        }
                        postingsList.add(docid);
                        //System.out.println(term);
                        //System.out.println(postingsList);
                    }
                    //System.out.println(docid);

                    // Write the document in the document index
                    writerDocument.println(docid + "\t" + docNo + "\t" + (terms.length - nbStopWords));

                    docid++;
                    numDocCurr++;
                }

                writerDocument.close();
                //Sort and write the block to disk.
                sortAndWriteBlockToFile(dictionary);
                dictionary.clear();
                System.gc();


            }
            it.close();
        }

        catch(Exception e){
            System.err.println("Error: " + e.getMessage());
        }
    }   

    public static void sortAndWriteBlockToFile(Map<String, List<Integer>> dictionary) throws IOException{
        //Sort the dictionary
        Path file = Paths.get("block"+blockNumber+".txt");

        List<String> keys = new ArrayList<String>(dictionary.keySet());
		Collections.sort(keys);

        List<String> lines = new ArrayList<String>();
		for(String key : keys){
			Collections.sort(dictionary.get(key)); //sorting the postings list
			String index = key + " : " + dictionary.get(key).toString();
            //System.out.println(index);
			lines.add(index);
		}

        Files.write(file, lines);
        
        blockNumber++;
        keys.clear();
        lines.clear();

    }

    public static void test() throws IOException{
        String collectionPath = "C:\\ULIEGE\\MASTER\\MASTER2\\MIRCV\\projectMaven2-11\\search_engine\\data\\collection.tsv";
        File collectionFile = new File(collectionPath);
        //LineIterator it = FileUtils.lineIterator(collectionFile, "UTF-8");
        LineIterator it = FileUtils.lineIterator(collectionFile);


        for (int i=0; i<2; i++){
            String document = it.nextLine(); // one line = one document
            List<Integer> listI = new ArrayList<Integer>();
            listI.add(0);
            listI.add(1);
            listI.add(7826342);
            listI.add(895414);
            listI.add(978731);
            listI.add(495894);

            if (listI.contains(i)){
                String[] documentArray = document.split("\t"); // docNo and text are separated by a tabulation
            
                // Information about the document
                Integer docNo = Integer.parseInt(documentArray[0]);
                String text = documentArray[1];
                byte[] bytes = text.getBytes("Windows-1252");
                // Decode the Windows-1252 or Latin-1 bytes back into UTF-8 to get the correct string
                text = new String(bytes, "UTF-8");
                // print before preprocessing
                System.out.println("before preprocessing: " + docNo + " " + text);
                String preprocessedText = preprocessingText(text); // preprocess the text
                System.out.println("----------------------");
                //System.out.println(preprocessedText);
                String[] terms = preprocessedText.split(" ");
                List<String> termsWithoutStopWords = new ArrayList<String>();
                // remove stopwords
                for (String term : terms){
                    if (Arrays.asList(stopwordsList).contains(term)){
                        continue;
                    }
                    termsWithoutStopWords.add(term);
                }
                // order terms
                Collections.sort(termsWithoutStopWords);
                // Merge termsWithoutStopWords into a string
                String termsWithoutStopWordsString = String.join(" ", termsWithoutStopWords);
                

                System.out.println("after preprocessing: " + docNo + " " + termsWithoutStopWordsString);
            }
            
        }

        it.close();
    }
        
    public static void mergeBlocks() throws IOException{
        int tmpblockNumber = 25;

        // Inverted index file
        RandomAccessFile invIndexDocId = new RandomAccessFile("invertedIndexDocId.txt", "rw");
        RandomAccessFile invIndexFreq = new RandomAccessFile("invertedIndexFreq.txt", "rw");

        //Lexicon file
        PrintWriter writerLexicon = new PrintWriter("Lexicon.txt", "UTF-8");

        //Pointer to the files
        List<File> collectionFile = new ArrayList<File>();
        List<LineIterator> iterators = new ArrayList<LineIterator>();
        List<String> termArray = new ArrayList<String>();
        List<String> postingArray = new ArrayList<String>();
        List<Boolean> finishBlock = new ArrayList<Boolean>();

        for (int i=0; i<tmpblockNumber; i++){
            collectionFile.add(new File("block"+i+".txt"));
            iterators.add(FileUtils.lineIterator(collectionFile.get(i), "UTF-8"));
            String[] line = iterators.get(i).nextLine().split(" : ");
            termArray.add(line[0]);
            postingArray.add(line[1]);
            finishBlock.add(false);
        }

        int counter = 0;
        int startPostList = 0;

        // While all the blocks are not finished
        while (finishBlock.contains(false) && counter < Integer.MAX_VALUE){

            int byteWritten = 0;

            // Find all the indexes that have the smaller term
            List<Integer> minIndexes = new ArrayList<Integer>();
            String minTerm = Collections.min(termArray);

            //long startPostList = invIndexDocId.getFilePointer();

            for (int i=0; i<tmpblockNumber; i++){
                if (termArray.get(i).equals(minTerm)){
                    minIndexes.add(i);
                }
            }

            //debug
            //System.out.println("minIndexes : " + minIndexes);

            int nbRemove = 0;

            // Write the posting list of each block at a time after reduce it (because of the duplicates)
            for (int i=0; i<minIndexes.size(); i++){

                //debug
                // System.out.println("minIndexes.size() : " + minIndexes.size());
                // System.out.println("minIndexes.get(i) : " + minIndexes.get(i));
                // System.out.println("postingArray.size() : " + postingArray.size());
                //System.out.println("postingArray.get(minIndexes.get(i)) : " + postingArray.get(minIndexes.get(i)));

                // get posting list in integer
                String[] postings = postingArray.get(minIndexes.get(i)).split(", ");
                // if last character of first element is [, remove it
                if (postings[0].charAt(0) == '['){
                    postings[0] = postings[0].substring(1);
                }
                // if last character of last element is ], remove it
                if (postings[postings.length-1].charAt(postings[postings.length-1].length()-1) == ']'){
                    postings[postings.length-1] = postings[postings.length-1].substring(0, postings[postings.length-1].length()-1);
                }
                byteWritten = byteWritten + reduceAndWritePostingList(postings, invIndexDocId, invIndexFreq);

                // Update all the arrays
                // ERROR WITH INDEX OUT OF BOUND, mayeb bc of remove
                // int index = minIndexes.get(i);
                // if (iterators.get(index).hasNext()){
                //     String[] line = iterators.get(index).nextLine().split(" : ");
                //     termArray.set(index, line[0]);
                //     postingArray.set(index, line[1]);
                // }
                // else{
                //     iterators.get(index-nbRemove).close();
                //     iterators.remove(index-nbRemove);
                //     termArray.remove(index-nbRemove);
                //     postingArray.remove(index-nbRemove);
                //     tmpblockNumber--;
                //     nbRemove++;
                // }

                //update all the arrays
                int index = minIndexes.get(i);
                if (iterators.get(index).hasNext()){
                    String[] line = iterators.get(index).nextLine().split(" : ");
                    termArray.set(index, line[0]);
                    postingArray.set(index, line[1]);
                }
                else{
                    iterators.get(index).close();
                    termArray.set(index, "zzzzzzzzzzzzzzzzzzz");
                    postingArray.set(index, "zzzzzzzzzzzzzzzzzzz");
                    finishBlock.set(index, true);
                    tmpblockNumber--;
                }
            }

            //long endPostList = invIndexDocId.getFilePointer();
            int endPostList = byteWritten;
            writerLexicon.println(minTerm + "\t"+ startPostList +"\t"+ endPostList);
            startPostList = startPostList + endPostList;
            //System.out.println("minTerm: " + minTerm);

            counter++;           
        }
        writerLexicon.close();   
    }

    public static int reduceAndWritePostingList(String[] postingsString, RandomAccessFile invIndexDocId, RandomAccessFile invIndexFreq) throws IOException{

        int writtenBytes = 0;
        List<byte[]> byteDocId = new ArrayList<byte[]>();
        List<byte[]> byteFreq = new ArrayList<byte[]>();

        String currDocId = postingsString[0];
        int currFreq = 1;

        // if invIndexDocidBuffer is full, write it to the file        
        if (totalNbBytes >= 4096 * 10000){
            System.out.println("Buffer is full, writing to file");
            writeToFile();
        }
        for (int i=0; i<postingsString.length; i++){
            if (postingsString[i] == currDocId){
                currFreq++;
            }
            else{
                // convert currDocId into 4 bytes
                byte[] bytesDocID = ByteBuffer.allocate(4).putInt(Integer.parseInt(currDocId)).array();
                invIndexDocId.write(bytesDocID);
                totalNbBytes += 4;
                writtenBytes += 4;

                // convert currFreq into 4 bytes
                byte[] bytesFreq = ByteBuffer.allocate(4).putInt(currFreq).array();
                invIndexFreq.write(bytesFreq);
            }
        }
        return writtenBytes;
    }

    public static void writeToFile() throws IOException{
        // Write the buffer to the file
        invIndexDocidBuffer.flush();
        invIndexFreqBuffer.flush();
        totalNbBytes = 0;
    }
}
    
