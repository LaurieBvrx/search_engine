package laurie_boveroux;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import opennlp.tools.stemmer.PorterStemmer;
import java.nio.ByteBuffer;




public class Indexer{

    public static int docid;
    public static int blockNumber;
    public static BufferedOutputStream invIndexDocidBuffer;
    public static BufferedOutputStream invIndexFreqBuffer;
    public static BufferedOutputStream docIndexBuffer;
    public static BufferedOutputStream lexiconBuffer;
    public static int totalInvIndexBytes;
    public static int averageDocumentSize;
    public static int docIndexBytes;
    public static int lexiconBytes;


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


    public Indexer(String filenamelexicon) throws FileNotFoundException{
        this.docid = 0;
        this.blockNumber = 0;
        this.invIndexDocidBuffer = new BufferedOutputStream(new FileOutputStream("InvertedIndexDocid.txt"),4096 * 10000);
        this.invIndexFreqBuffer = new BufferedOutputStream(new FileOutputStream("InvertedIndexFreq.txt"),4096 * 10000);
        this.docIndexBuffer = new BufferedOutputStream(new FileOutputStream("DocumentIndex.txt"),4096 * 10000);
        this.lexiconBuffer = new BufferedOutputStream(new FileOutputStream(filenamelexicon),4096 * 10000);
        this.totalInvIndexBytes = 0;
        this.averageDocumentSize = 0;
        this.docIndexBytes = 0;
        this.lexiconBytes = 0;
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
    public static int parseTsvFile(String collectionPath, int numberReadDoc, boolean stemFlag) throws IOException{

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

            System.out.println("Block " + blockNumber + " in construction ");

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
                String stem;
                PorterStemmer pStem = new PorterStemmer();

                
                int nbStopWords = 0; // to keep track of the number of stop words to have the correct document length
                for (String term : terms) {
                    // keep only the fisrt 64 bits of the term
                    if (term.length() > 64){
                        term = term.substring(0, 64);
                    }
                    // if term is in stopword list, skip it
                    if (Arrays.asList(stopwordsList).contains(term) || term.length() == 0) {
                        nbStopWords++;
                        continue;
                    }
                
                    List<Integer> postingsList;
                
                    if (!stemFlag) {
                        // The posting list is a list of integer
                        if (dictionary.get(term) == null) {
                            //add the term to the dictionary and create a new posting list
                            postingsList = new ArrayList<Integer>();
                            dictionary.put(term, postingsList);
                        } else {
                            postingsList = dictionary.get(term);
                        }
                    } else {
                        // The posting list of a word stem
                        stem = pStem.stem(term);
                
                        if (dictionary.get(stem) == null) {
                            //add the stem to the stem dictionary and create a new posting list for it
                            postingsList = new ArrayList<Integer>();
                            dictionary.put(stem, postingsList);
                        } else {
                            postingsList = dictionary.get(stem);
                        }
                    }
                    postingsList.add(docid);
                }
                // Write the document index
                if (docIndexBytes >= 4096 * 10000 - 2*8){
                    docIndexBuffer.flush();
                    docIndexBytes = 0;
                }else{
                    docIndexBytes += 2*8;
                    // docNo to bytes
                    byte[] docNoBytes = ByteBuffer.allocate(4).putInt(docNo).array();
                    docIndexBuffer.write(docNoBytes);
                    // docLength to bytes
                    byte[] docLengthBytes = ByteBuffer.allocate(4).putInt(terms.length - nbStopWords).array();
                    docIndexBuffer.write(docLengthBytes);
                }
            
                // Write the document in the document index
                docid++;
                numDocCurr++;
                averageDocumentSize += terms.length - nbStopWords;
            }
            sortAndWriteBlockToFile(dictionary); //Sort and write the block to disk.
            dictionary.clear(); // Clear the dictionary of the current block
            System.gc(); // Garbage collector
        }
        it.close(); // Close the iterator
        docIndexBuffer.close();
        docIndexBuffer.flush();
        averageDocumentSize = averageDocumentSize / docid;

        // write data needed for scoring
        String averageDocumentSizeString = Integer.toString(averageDocumentSize);
        String docidString = Integer.toString(docid);
        String metaDataFile = "metaDataCollection.txt";
        FileWriter myWriter = new FileWriter(metaDataFile);
        myWriter.write(averageDocumentSizeString + " " + docidString);
        myWriter.close();

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
        System.out.println("Starting to merge blocks");
        long startTimeMerge = System.currentTimeMillis();

        //Pointer to the files and info needed to merge the blocks
        List<File> collectionFile = new ArrayList<File>();
        List<LineIterator> iterators = new ArrayList<LineIterator>();
        List<String> termArray = new ArrayList<String>();
        List<String> postingArray = new ArrayList<String>();
        List<Boolean> finishBlock = new ArrayList<Boolean>();

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

            // write the term in the tab
            if (lexiconBytes >= 4096 * 10000 *0.95){
                lexiconBuffer.flush();
                lexiconBytes = 0;
            }else{
                byte[] termBytes = stringTo64Bytes(minTerm);
                byte[] startPostListBytes = ByteBuffer.allocate(4).putInt(startPostList).array();
                lexiconBuffer.write(termBytes);
                lexiconBuffer.write(startPostListBytes);
            }
            startPostList = startPostList + endPostList;
        }

        writeToFile(); // write the last buffer to the inverted index
        lexiconBuffer.flush();
        lexiconBuffer.close();
        invIndexDocidBuffer.close();
        invIndexFreqBuffer.close();
        long endTimeMerge = System.currentTimeMillis();
        System.out.println("Time to merge blocks : " + (endTimeMerge - startTimeMerge) + " ms");
        
    }

    private static  byte[] stringTo64Bytes(String s){
        byte[] bytes = new byte[64];
        byte[] sBytes = s.getBytes();
        if (sBytes.length > 64){
            // keep the first 64 bytes by copy
            System.arraycopy(sBytes, 0, bytes, 0, 64);
        }else{
            // copy the bytes of the string
            System.arraycopy(sBytes, 0, bytes, 0, sBytes.length);
            // fill the rest with 0
            for (int i=sBytes.length; i<64; i++){
                bytes[i] = 0;
            }
        }
        return bytes;
    }

    public static int reduceAndWritePostingList(String[] postingsString) throws IOException{
        /* Reduce the posting list: remove duplicates and add the frequencies 
         * Return the number of bytes written in the inverted index
         */
        int writtenBytesPost = 0;
        // if invIndexDocidBuffer is full, write it to the file
        // (avoid to write too often and slow down the process)   
        if (totalInvIndexBytes >= 4096 * 10000){
            writeToFile();
            totalInvIndexBytes = 0;
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

            // Compressed Inverted Index with VB Encode
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
            totalInvIndexBytes += len; // to know if the buffer is full
            writtenBytesPost += len; // to know when a posting list ends
        }
        return writtenBytesPost;
    }

    public static void writeToFile() throws IOException{
        // Write the buffer to the file
        invIndexDocidBuffer.flush();
        invIndexFreqBuffer.flush();
        totalInvIndexBytes = 0;
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
            len += 1; //binary.length();
        }
        return len;
    }
}