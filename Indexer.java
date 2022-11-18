package laurie_boveroux;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import java.nio.ByteBuffer;


public class Indexer{

    public static int docid;
    public static int blockNumber;
    public static BufferedOutputStream invIndexDocidBuffer;
    public static BufferedOutputStream invIndexFreqBuffer;
    public static int totalNbBytes;
    // hash map for doc id : <docNo, length>
    public static TreeMap<Integer, List<Integer>> docIndexMap;
    public static File docIndexFile;


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
        this.docid = 0;
        this.blockNumber = 0;
        this.invIndexDocidBuffer = new BufferedOutputStream(new FileOutputStream("InvertedIndexDocid.txt"),4096 * 10000);
        this.invIndexFreqBuffer = new BufferedOutputStream(new FileOutputStream("InvertedIndexFreq.txt"),4096 * 10000);
        this.totalNbBytes = 0;
        this.docIndexMap = new TreeMap<Integer, List<Integer>>();
        // Create the file "DocumentIndex.txt"
        this.docIndexFile = new File("DocumentIndex.txt");
    }
 
    public static String preprocessingText(String text) throws UnsupportedEncodingException{
        byte[] bytes = text.getBytes("Windows-1252");
        // Decode the Windows-1252 or Latin-1 bytes back into UTF-8 to get the correct string
        text = new String(bytes, "UTF-8");
        // remove all the punctuation
        String preprocessedText = text.replaceAll("[^a-zA-Z0-9\\s]", " ");
        // remove all the numbers
        preprocessedText = preprocessedText.replaceAll("[0-9]", " ");
        // remove all the multiple spaces
        preprocessedText = preprocessedText.replaceAll("\\s+", " ");
        // remove all the spaces at the beginning and at the end of the text
        preprocessedText = preprocessedText.trim();
        // convert all the text to lower case
        preprocessedText = preprocessedText.toLowerCase();
        return preprocessedText;
    }

    public static void writeDocIndexMap() throws IOException{
        // write to the file "DocumentIndex.txt" without erasing the previous content
        FileWriter fw = new FileWriter(docIndexFile, true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);
        for (Map.Entry<Integer, List<Integer>> entry : docIndexMap.entrySet()) {
            out.println(entry.getKey() + " " + entry.getValue().get(0) + " " + entry.getValue().get(1));
        }
        out.close();        
    }

    public static int parseTsvFile(String collectionPath, int numberReadDoc) throws IOException{
        try{
            if (numberReadDoc == -1){ // if we want to read all the documents
                numberReadDoc = Integer.MAX_VALUE;
            }           
            // Read the collection file
            File collectionFile = new File(collectionPath);
            LineIterator it = FileUtils.lineIterator(collectionFile, "UTF-8");

            int numDocCurr = 0;   
            while (it.hasNext() && numDocCurr < numberReadDoc) { // For the whole collection
                //Setting the initial memory
                int totalMemory = (int) java.lang.Runtime.getRuntime().totalMemory();
                int usedMemory = totalMemory - (int) java.lang.Runtime.getRuntime().freeMemory();

                Map<String, List<Integer>> dictionary = new LinkedHashMap<String, List<Integer>>();

                System.out.println("New block in construction " + blockNumber);

                // While there is free memory
                while(usedMemory<0.95*totalMemory && it.hasNext() && numDocCurr < numberReadDoc){
                    
                    totalMemory = (int) Runtime.getRuntime().totalMemory();
                    usedMemory = totalMemory - (int) Runtime.getRuntime().freeMemory();
                    
                    // To keep track of the number of documents read
                    if (numDocCurr % 100000 == 0){
                        System.out.println("Number of documents read: " + numDocCurr);           
                    }

                    String document = it.nextLine(); // one line = one document
                    String[] documentArray = document.split("\t"); // docNo and text are separated by a tabulation
                    
                    // Information about the document
                    Integer docNo = Integer.parseInt(documentArray[0]);
                    String text = documentArray[1];
                    String preprocessedText = preprocessingText(text); // preprocess the text
                    String[] terms = preprocessedText.split(" "); // split the text into terms
                    
                    int nbStopWords = 0; // to keep track of the number of stop words to have the correct document length
                    for (String term : terms){
                        // if term is in stopword list, skip it
                        if (Arrays.asList(stopwordsList).contains(term) || term.length() == 0){
                            nbStopWords++; 
                            continue;
                        }                      
                        // The posting list is a list of integer
                        List<Integer> postingsList;

                        if (dictionary.get(term) == null){
                            //add the term to the dictionary and create a new posting list
                            postingsList = new ArrayList<Integer>();
                            dictionary.put(term, postingsList);
                        }
                        else{
                            postingsList = dictionary.get(term);
                        }
                        postingsList.add(docid);
                    }
                    // write to hashmap                    
                    docIndexMap.put(docid, Arrays.asList(docNo, terms.length - nbStopWords));
                    // Write the document in the document index
                    //writerDocument.println(docid + "\t" + docNo + "\t" + (terms.length - nbStopWords));
                    docid++;
                    numDocCurr++;
                }
                writeDocIndexMap(); // write docIndexMap to file
                docIndexMap.clear(); // clear the map
                sortAndWriteBlockToFile(dictionary); //Sort and write the block to disk.
                dictionary.clear(); // Clear the dictionary of the current block
                System.gc(); // Garbage collector
            }
            it.close(); // Close the iterator
        }

        catch(Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        return docid; // Return the number of documents read
    }   

    public static void sortAndWriteBlockToFile(Map<String, List<Integer>> dictionary) throws IOException{
        // Create blocks directory if does not exist
        File blocksDirectory = new File("blocks");
        if (!blocksDirectory.exists()){
            blocksDirectory.mkdir();
        }
        // Create the block file
        File blockFile = new File("blocks/block" + blockNumber + ".txt");
        // Sort the terms in the dictionary
        List<String> keys = new ArrayList<String>(dictionary.keySet());
		Collections.sort(keys);

        List<String> lines = new ArrayList<String>();
		for(String key : keys){
			Collections.sort(dictionary.get(key)); //sorting the postings list by docid
			String index = key + " : " + dictionary.get(key).toString(); // term : [docid1, docid2, ...]
			lines.add(index); // add the line to the list of lines
		}
        //write the block to disk
        FileUtils.writeLines(blockFile, lines);
        
        blockNumber++;
        keys.clear();
        lines.clear();
    }
    
    public static void mergeBlocks() throws IOException{

        PrintWriter writerLexicon = new PrintWriter("Lexicon.txt", "UTF-8");

        //Pointer to the files and info needed to merge the blocks
        List<File> collectionFile = new ArrayList<File>();
        List<LineIterator> iterators = new ArrayList<LineIterator>();
        List<String> termArray = new ArrayList<String>();
        List<String> postingArray = new ArrayList<String>();
        List<Boolean> finishBlock = new ArrayList<Boolean>();

        // HARDCODE !!!!!!
        //blockNumber = 8;
        // initialize the pointers to the files and the info needed to merge the blocks
        for (int i=0; i<blockNumber; i++){
            collectionFile.add(new File("blocks/block"+i+".txt"));
            iterators.add(FileUtils.lineIterator(collectionFile.get(i), "UTF-8"));
            String[] line = iterators.get(i).nextLine().split(" : ");
            termArray.add(line[0]);
            postingArray.add(line[1]);
            finishBlock.add(false);
        }

        int startPostList = 0; // to keep track of the start of the posting list in the inverted index
        // While all the blocks are not finished
        while (finishBlock.contains(false)){
            int byteWritten = 0; // to keep track of the number of bytes written in the inverted index

            // Find all the indexes (i.e. number of the block) that have the smaller term 
            List<Integer> minIndexes = new ArrayList<Integer>();
            String minTerm = Collections.min(termArray);
            for (int i=0; i<blockNumber; i++){
                if (termArray.get(i).equals(minTerm)){
                    minIndexes.add(i);
                }
            }
            // Write the posting list of each block at a time after reduce it (because of the duplicates)
            for (int i=0; i<minIndexes.size(); i++){
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
                byteWritten = byteWritten + reduceAndWritePostingList(postings);

                //update all the arrays
                int index = minIndexes.get(i);
                if (iterators.get(index).hasNext()){ // if the block is not finished
                    String[] line = iterators.get(index).nextLine().split(" : ");
                    termArray.set(index, line[0]); // update the term
                    postingArray.set(index, line[1]); // update the posting list
                }
                else{ // if the block is finished
                    iterators.get(index).close();
                    // Todo : find a better way to be at the end of the sort array
                    termArray.set(index, "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"); // term that will never be the smallest 
                    postingArray.set(index, "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
                    finishBlock.set(index, true);
                }
            }
            int endPostList = byteWritten; // to keep track of the end of the posting list in the inverted index
            writerLexicon.println(minTerm + " "+ startPostList);

            startPostList = startPostList + endPostList;
        }

        writeToFile(); // write the last buffer to the inverted index
        writerLexicon.println("\n");
        writerLexicon.close();
        invIndexDocidBuffer.close();
        invIndexFreqBuffer.close();
        
    }

    public static int reduceAndWritePostingList(String[] postingsString) throws IOException{
        /* Reduce the posting list: remove duplicates and add the frequencies 
         * Return the number of bytes written in the inverted index
         */
        int writtenBytes = 0;
        // if invIndexDocidBuffer is full, write it to the file
        // (avoid to write too often and slow down the process)   
        if (totalNbBytes >= 4096 * 10000){
            writeToFile();
        }

        int i = 0;
        while (i < postingsString.length) {
            // get the first element
            String currDocId = postingsString[i];
            // count the number of occurences
            int count = 0;
            while (i < postingsString.length && postingsString[i].equals(currDocId)) {
                count++;
                i++;
            }

            // // convert currDocId into 4 bytes
            // byte[] bytesDocID = ByteBuffer.allocate(4).putInt(Integer.parseInt(currDocId)).array();
            // invIndexDocidBuffer.write(bytesDocID);            
            // // convert currFreq into 4 bytes
            // byte[] bytesFreq = ByteBuffer.allocate(4).putInt(count).array();
            // invIndexFreqBuffer.write(bytesFreq);
            // // Keep track of the number of bytes written
            // totalNbBytes += 4; // to know if the buffer is full
            // writtenBytes += 4; // to know when a posting list ends


            // Compressed Inverted Index
            List<Integer> encodedDocId = new ArrayList<Integer>();
            List<Integer> encodedFreq = new ArrayList<Integer>();
            int currDocIdInt = Integer.parseInt(currDocId);
            int len = 0;

            if (currDocIdInt > count){ // if the docid is greater than the frequency, adapt the frequency
                encodedDocId = VBEncodeNumber(currDocIdInt);
                VBEncode(encodedDocId, true);
                len = encodedDocId.size();
                encodedFreq = VBEncodeNumber(count);
                while (encodedFreq.size() < len){
                    encodedFreq.add(0);
                }
                VBEncode(encodedFreq, false);                
            }
            else{ // if the frequency is greater than the docid, adapt the docid
                encodedFreq = VBEncodeNumber(count);
                VBEncode(encodedFreq, false);
                len = encodedFreq.size();
                encodedDocId = VBEncodeNumber(currDocIdInt);
                while (encodedDocId.size() < len){
                    encodedDocId.add(0);
                }
                VBEncode(encodedDocId, true);
            }
            totalNbBytes += len; // to know if the buffer is full
            writtenBytes += len; // to know when a posting list ends
        }
        return writtenBytes;
    }

    public static void writeToFile() throws IOException{
        // Write the buffer to the file
        invIndexDocidBuffer.flush();
        invIndexFreqBuffer.flush();
        totalNbBytes = 0;
    }

    public static List<Integer> VBEncodeNumber(Integer n){
        List<Integer> result = new ArrayList<Integer>();
        while(true){
            result.add(n % 128);
            if (n < 128){
                break;
            }
            else{
                n = n / 128;
            }
        }
        return result;        
    }

    public static Integer VBEncode(List<Integer> list, Boolean flagDocId) throws IOException{
        int len = 0;
        for (int i = 0; i < list.size(); i++){
            int n = list.get(i);
            String binary = Integer.toBinaryString(n);
            while (binary.length() < 7){ // add 0 to the left if the binary is not 8 bits
                binary = "0" + binary;
            }
            if (i == list.size() - 1){ // if it is the last number, add 0 to the left
                binary = "0" + binary;
            }else{ // else add 1 to the left
                binary = "1" + binary;
            }
            if (flagDocId){
                invIndexDocidBuffer.write(Integer.parseInt(binary, 2));
            }
            else{
                invIndexFreqBuffer.write(Integer.parseInt(binary, 2));
            }
            len += binary.length();
        }
        return len;
    }
}