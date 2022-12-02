package laurie_boveroux;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Paths;
import opennlp.tools.stemmer.PorterStemmer;
import java.nio.ByteBuffer;

public class QuerySearch{

    public static Map<Integer,Integer[]> docIndex;
    public Integer averageDocumentSize;
    BinarySearch lexicon;
    public Integer numberDoc;
    public List<Double> scoreList; // List of the computed scores
    public List<Integer> docIdList; // List of the docID of the score


    public QuerySearch(int nbDoc) throws IOException{
        this.lexicon = new BinarySearch(new File("Lexicon.txt"));
        this.scoreList = new ArrayList<Integer>();
        this.docIdList = new ArrayList<Integer>();

        // Open file metaDataCollection.txt and read all the file
        String currentPath = new java.io.File(".").getCanonicalPath();
        String metaDataCollectionPath = currentPath + "/metaDataCollection.txt";
        String lines = new String(Files.readAllBytes(Paths.get(metaDataCollectionPath)));
        String[] linesArray = lines.split(" ");
        this.numberDoc = Integer.parseInt(linesArray[1]);
        this.averageDocumentSize = Integer.parseInt(linesArray[0]);
    }

    public ListPointer openList(String term) throws IOException {
        /* Get the posting list of the term and all info about it*/
        List<byte[]> line = lexicon.search(term);
        // if the term is not in the lexicon
        if(line == null) {
            return null;
        }
        if (line.size() == 1) { // Last term of the lexicon (no end for the posting list)
            byte[] termStartBytes = (byte[]) line.get(0);
            byte[] startBytes = Arrays.copyOfRange(termStartBytes, 64, 68);
            int start = ByteBuffer.wrap(startBytes).getInt();
            byte[] termBytes = Arrays.copyOfRange(termStartBytes, 0, 64);
            String termString = new String(termBytes);
            return new ListPointer(termString, start, -1);
        }else{
            byte[] termStartBytes = (byte[]) line.get(0);
            byte[] startBytes = Arrays.copyOfRange(termStartBytes, 64, 68);
            int start = ByteBuffer.wrap(startBytes).getInt();
            byte[] termStart = Arrays.copyOfRange(termStartBytes, 0, 64);
            String termStartString = new String(termStart);

            byte[] termEndBytes = (byte[]) line.get(1);
            byte[] endBytes = Arrays.copyOfRange(termEndBytes, 64, 68);
            int end = ByteBuffer.wrap(endBytes).getInt();

            return new ListPointer(termStartString, start, end);
        }
    }

    public int next(ListPointer lp){
        // advances the iterator to the next posting
        // returns the document identifier of the current posting
        // if the iterator is already past the end of the list, the method returns -1
        if (lp.index == lp.getLength() - 1) {
            return -1;
        }
        lp.index++;
        return lp.docIdsArray[lp.index];
    }

    public int getFreq(ListPointer lp) throws IOException {
        // returns the frequency of the current posting
        return lp.getFreq(lp.index);
    }

    public Double computeScore(int docId, int[] tf, int[] df, String typeScore) throws IOException{
        if (typeScore == "tfidf"){
            return computeScoreTFIDF(docId, tf, df);
        }
        else if (typeScore == "okapibm25"){
            return computeScoreOkapiBM25(docId, tf, df);
        }
        else{
            return (double) 0;
        }
    }

    public Double computeScoreTFIDF(int docId, int[] tf, int[] df) {
        // computes the score of the current doc
        Double score = (double) 0;
        for (int i = 0; i < tf.length; i++) {
            double idf = computeScoreIDF(df[i]);
            score += tf[i] * idf;
        }
        return score;
    }

    public Double computeScoreOkapiBM25(int docId, int[] tf, int[] df) throws IOException {
        // computes the score of the current doc
        Double score = (double) 0;
        double b = 0.75;
        double k1 = 1.5;
        int docLen = getDocLength(docId);

        for (int i = 0; i < tf.length; i++) {
            double idf = computeScoreIDF(df[i]);
            double den = k1*((1-b) + b*(docLen/this.averageDocumentSize));
            score += (tf[i] / (den + tf[i])) * idf;
        }
        return score;
    }

    public double computeScoreIDF(int df) {
        return Math.log(numberDoc/df);
    }



    public void addScore(Double score, int did){
        /* add score to the list if size <10 or replace the lowest score if new score > lowest score */
        if (this.scoreList.size() < 10){
            this.scoreList.add(score);
            this.docIdList.add(did);
        }
        else{
            Double min = Collections.min(this.scoreList);
            if (score > min){
                int index = this.scoreList.indexOf(min);
                this.scoreList.remove(index);
                this.scoreList.add(score);
                this.docIdList.remove(index);
                this.docIdList.add(did);
            }
        }
    }

    public void printRelevantDocs() throws IOException{
        /* print the 10 most relevant documents */
        int size = this.scoreList.size();
        System.out.println("The " + size + " most relevant documents are: \n");
        for (int i = 0; i < size; i++){
            String line = "";
            int docID = this.docIdList.get(i);
            try (Stream<String> lines = Files.lines(Paths.get("data/collection.tsv"))) {
                line = lines.skip(docID).findFirst().get();
            }
            String[] documentArray = line.split("\t"); // docNo and text are separated by a tabulation               
            Integer docNo = Integer.parseInt(documentArray[0]);
            String text = documentArray[1];
            System.out.println("Document " + docNo + " with score " + this.scoreList.get(i) + " : \n" + text);
        }
    }

    public void closeList(){
        //clean score List and docId List
        this.scoreList.clear();
        this.docIdList.clear();
    }

    public void executeQuery(String typeQuery, String query, boolean stemFlag, String typeScore) throws IOException {
        // Processing of the query
        String[] queryStem = {""};
        query = Indexer.preprocessingText(query);
        String[] q = query.split(" ");
        int num = q.length;
        ListPointer[] lp = new ListPointer[num];

        PorterStemmer pStemQ = new PorterStemmer();
        for (int i = 0; i < num; i++) {
            if (Arrays.asList(Indexer.stopwordsList).contains(q[i]) || q[i].length() == 0) {
                continue;
            }
            if (stemFlag){
                lp[i] = openList(pStemQ.stem(q[i]));
            }else {
                lp[i] = openList(q[i]);
            }
        }

        // sort the list pointers
        Arrays.sort(lp, new Comparator<ListPointer>() {
            @Override
            public int compare(ListPointer p1, ListPointer p2) {
                if(p1 == null && p2==null) return 0;
                else if(p1 == null && p2!=null) return 0 - p2.getLength();
                else if (p1 != null && p2==null) return p1.getLength() - 0;
                return p1.getLength() - p2.getLength();
            }
        });

        // remove the null value
        List<ListPointer> list = new ArrayList<ListPointer>();
        for (ListPointer p : lp) {
            if (p != null) {
                list.add(p);
            }
        }
        num = list.size();

        if (num == 0){
            System.out.println("No result found");
            return;
        }
        // print num of terms in the query
        System.out.println("Number of terms in the query: " + num);

        // find the min max doc id among all the list pointers
        int minMaxDocId = list.get(0).getMaxDocId();
        for (int i = 1; i < list.size(); i++){
            if (list.get(i).getMaxDocId() < minMaxDocId){
                minMaxDocId = list.get(i).getMaxDocId();
            }
        }

        // find the max doc id among all the list pointers
        int maxDocId = 0;
        for (ListPointer p : list) {
            int tmpMaxDocId = p.getMaxDocId();
            if (tmpMaxDocId > maxDocId) {
                maxDocId = tmpMaxDocId;
            }
        }        

        if (typeQuery.equals("conjunctive")){
            int did = 0;   // document id
            while (did <= minMaxDocId){
                // get next post from shortest list
                did = next(list.get(0));
                //did = next(list.get(0));
                if (did == -1 || did > minMaxDocId) {
                    break;
                }
                int d=0;
                // check if the document is in the other lists
                for (int i=1; (i<num) && ((d=next(list.get(i))) == did); i++);

                if (d > did){
                    did = d; // no match, advance to next document
                }
                else{
                    int[] tf = new int[num]; // term frequencies array
                    int[] df = new int[num]; // document frequencies array
                    // get the frequencies
                    for (int i=0; i<num; i++) {
                        tf[i] = getFreq(list.get(i)); // get the frequency of all the terms of the query that are in the document
                        df[i] = list.get(i).getDocFreq(); // get the document frequency of all the terms of the query
                    }
                    
                    Double score = computeScore(did, tf, df, typeScore);
                    // add score to the list if size <10 or replace the lowest score if new score > lowest score
                    addScore(score, did);                    
                }

                did++; /* and increase did to search for next post */
            }
        }
        else if (typeQuery.equals("disjunctive")){ // disjunctive query: compute the score for all docs that have a list one term
            int did =0;
            while (did <= maxDocId){
                int[] tf = new int[num]; // term frequencies array
                int[] df = new int[num]; // document frequencies array
                int nbTerm = 0;
                // print doc id and num
                for (int i=0; i<num; i++) {
                    int d = next(list.get(i));
                    if (d == did){
                        tf[i] = getFreq(list.get(i));
                        df[i] = list.get(i).getDocFreq();
                        nbTerm++;
                    }
                }               
                if (nbTerm > 0){
                    int[] df2 = new int[nbTerm];
                    int[] tf2 = new int[nbTerm];
                    int j = 0;
                    for (int i=0; i<num; i++) {
                        if (tf[i] != 0){
                            df2[j] = df[i];
                            tf2[j] = tf[i];
                            j++;
                        }
                    }
                    Double score = computeScoreTFIDF(did, tf2, df2);
                    // add score to the list if size <10 or replace the lowest score
                    addScore(score, did);
                }
                did++;
            }           
        }
        else{
            System.out.println("Error: type of query not recognized");
        }

        Double currScore = (double) 0;
        List<Integer> docIdOrderedList = new ArrayList<Integer>();
        List<Double> scoreOrderedList = new ArrayList<Double>();
        // order score and docid list with same order
        for (int i = 0; i < this.scoreList.size(); i++){
            currScore = Collections.max(this.scoreList);
            int index = this.scoreList.indexOf(currScore);
            docIdOrderedList.add(this.docIdList.get(index));
            scoreOrderedList.add(currScore);
            // replace the score by -1
            this.scoreList.set(index, (double) -1);
        }
        System.out.println("ids ordered : " + docIdOrderedList);
        // Get docNo from docId
        List<Integer> docNoList = getDocNo();
        System.out.println("docNo : " + docNoList);
        System.out.println("ids   : " + this.docIdList);
        System.out.println("scores : " + scoreOrderedList);
    }
    
    public List<Integer> getDocNo() throws IOException{
        List<Integer> docNoList = new ArrayList<Integer>();
        RandomAccessFile docIndex = new RandomAccessFile("documentIndex.txt", "r");
        int size = this.docIdList.size();
        for (int i = 0; i < size; i++){
            int docIdcurr = this.docIdList.get(i);
            docIndex.seek(docIdcurr*8); // 4 bytes for docNo + 4 bytes for docLength
            // read 4 bytes
            byte[] b = new byte[4];
            docIndex.read(b);
            int docNocurr = ByteBuffer.wrap(b).getInt();
            docNoList.add(docNocurr);
        }
        docIndex.close();
        return docNoList;
    }

    public int getDocLength(int docId) throws IOException{
        RandomAccessFile docIndex = new RandomAccessFile("documentIndex.txt", "r");
        docIndex.seek(docId*8 + 4); // 4 bytes for docNo + 4 bytes for docLength
        // read 4 bytes
        byte[] b = new byte[4];
        docIndex.read(b);
        int docLength = ByteBuffer.wrap(b).getInt();
        docIndex.close();
        return docLength;
    }
}
