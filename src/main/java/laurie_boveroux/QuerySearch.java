package laurie_boveroux;

import java.io.*;
import java.util.*;

import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class QuerySearch{

    public static Map<Integer,Integer[]> docIndex;
    public static Integer averageDocumentSize = 0;
    ExternalBinarySearch lexicon;
    public static Integer numberDoc;
    public List<Integer> scoreList; // List of the computed scores
    public List<Integer> docIdList; // List of the docID of the score


    public QuerySearch(int nbDoc) throws FileNotFoundException{
        this.lexicon = new ExternalBinarySearch(new File("lexicon.txt"));
        this.numberDoc = nbDoc; // Number of documents in the collection (to compute TF-IDF)
        this.scoreList = new ArrayList<Integer>();
        this.docIdList = new ArrayList<Integer>();
    }

    public void loadDocumentIndex() throws IOException {
        String currentPath = new java.io.File(".").getCanonicalPath();
        String docIndexPath = currentPath + "/data/DocumentIndex.txt";    
        FileReader in = new FileReader(docIndexPath);
        BufferedReader br = new BufferedReader(in,4096 * 100000);

        String line;
        int numberOfDocuments = 0;
        int count = 0;
        while ((line = br.readLine()) != null) {
            String[] words = line.split("\t");
            Integer[] list = new Integer[2];
            list[0]=Integer.parseInt(words[1]);
            list[1]=Integer.parseInt(words[2]);

            docIndex.put(Integer.parseInt(words[0]),list);
            if(count%10000 == 0) {
                System.out.println("Loading DocumentIndex " + docIndex + " " + words[0]);
            }
            count++;
            numberOfDocuments++;
            averageDocumentSize += Integer.parseInt(words[2]);
        }
        averageDocumentSize = averageDocumentSize/numberOfDocuments;
        br.close();
    }

    public ListPointer openList(String term) throws IOException {
        /* Get the posting list of the term and all info about it*/
        String line = lexicon.search(term);
        // Print line
        System.out.println("line: " + line);
        // if the term is not in the lexicon
        if(line == null) {
            return null;
        }
        // line: [term + " " + startIndex + "\n" + nextTerm + " " + endIndex]
        String[] split = line.split("\n");
        // print size of split
        System.out.println("size of split: " + split.length);
        
        // If this is the last term in the lexicon [term + " " + start]
        if (split.length == 1) {
            String start = split[0].split(" ")[1];
            start = start.substring(0, start.length() - 1);
            return new ListPointer(term, Integer.parseInt(start), -1);
        }else{
            String start = split[0].split(" ")[1];
            start = start.substring(0, start.length() - 1); // remove space that is added to the end of the line
            String end = split[1].split(" ")[1];
            end = end.substring(0, end.length() - 1);            
            return new ListPointer(term, Integer.parseInt(start), Integer.parseInt(end));
        }

    }

    public int nextGEQ(ListPointer lp, int d) throws IOException {
        // advances the iterator forward to the next posting with a document identifier greater than or equal to d
        // if there is no such posting, the iterator is advanced to the end of the list the method returns -1
        // returns the document identifier of the current posting
        int i = 0;
        lp.index = 0;
        while (i < lp.getLength() && lp.docIdsArray[i] < d) {
            i++;
            lp.index++;
        }
        if (i == lp.getLength()) {
            //lp.index--; // Pas sure
            return -1;
        }
        return lp.docIdsArray[lp.index];        
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

    public int computeScoreTFIDF(int docId, int[] tf, int[] df) {
        // computes the score of the current doc
        int score = 0;        
        for (int i = 0; i < tf.length; i++) {
            score += tf[i] * Math.log(numberDoc/df[i]);
        }
        return score;
    }

    public void addScore(int score, int did){
        /* add score to the list if size <10 or replace the lowest score if new score > lowest score */
        if (this.scoreList.size() < 10){
            this.scoreList.add(score);
            this.docIdList.add(did);
        }
        else{
            int min = Collections.min(this.scoreList);
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
        System.out.println("The " + size + " most relevant documents are: ");
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

    public void executeQuery(String typeQuery, String query) throws IOException {

        // Processing of the query
        query = Indexer.preprocessingText(query);
        String[] q = query.split(" ");
        int num = q.length;
        ListPointer[] lp = new ListPointer[num];
        for (int i = 0; i < num; i++){
            lp[i] = openList(q[i]);
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
                did = nextGEQ(list.get(0), did);
                if (did == -1 || did > minMaxDocId) {
                    break;
                }
                int d=0;
                // check if the document is in the other lists
                for (int i=1; (i<num) && ((d=nextGEQ(list.get(i), did)) == did); i++);

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
                    
                    int score = computeScoreTFIDF(did, tf, df);
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
                    int d = nextGEQ(list.get(i), did);
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
                    int score = computeScoreTFIDF(did, tf2, df2);
                    // add score to the list if size <10 or replace the lowest score
                    addScore(score, did);
                }
                did++;
            }           
        }
        else{
            System.out.println("Error: type of query not recognized");
        }
        System.out.println("ids : " + this.docIdList);

        // get the docNo
        List<Integer> docNoList = getDocNo();
        System.out.println("docNo : " + docNoList);
        System.out.println("scores : " + this.scoreList);


    }
    
    public List<Integer> getDocNo() throws IOException{
        List<Integer> docNoList = new ArrayList<Integer>();
        int size = this.docIdList.size();
        for (int i = 0; i < size; i++){
            String line = "";
            int docID = this.docIdList.get(i);
            Stream<String> lines = Files.lines(Paths.get("DocumentIndex.txt"));
            line = lines.skip(docID).findFirst().get();
            String[] documentArray = line.split(" ");
            int docNo = Integer.parseInt(documentArray[1]);
            docNoList.add(docNo);            
        }
        return docNoList;
    }
}
