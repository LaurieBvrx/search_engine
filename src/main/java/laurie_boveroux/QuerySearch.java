package laurie_boveroux;

import java.io.*;
import java.util.*;

public class QuerySearch{

    public static Map<Integer,Integer[]> docIndex;
    public static Integer averageDocumentSize = 0;
    ExternalBinarySearch lexicon;
    public static String typeQuery;
    public static Integer numberDoc;


    public QuerySearch(String type, int nbDoc) throws FileNotFoundException{
        this.lexicon = new ExternalBinarySearch(new File("lexicon.txt"));
        if (type.equals("conjunctive") || type.equals("disjunctive")){
            this.typeQuery = type;
        }
        else{
            System.out.println("Please enter a valid type of query");
        }
        //this.numberDoc = docIndex.size();
        this.numberDoc = nbDoc;
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
                System.out.println("Loading lexicon " + docIndex + " " + words[0]);
            }
            count++;
            numberOfDocuments++;
            averageDocumentSize += Integer.parseInt(words[2]);
        }
        averageDocumentSize = averageDocumentSize/numberOfDocuments;
        br.close();
    }

    public ListPointer openList(String term) throws IOException {
        
        String line = lexicon.search(term);
        //debug
        System.out.println("line: " + line);
        // if the term is not in the lexicon
        if(line == null) {
            return null;
        }
        // line: [term + " " + startIndex + "\n" + nextTerm + " " + endIndex]
        String[] split = line.split("\n");
        // If this is the last term in the lexicon [term + " " + start]
        if (split.length == 1) {
            String start = split[0].split(" ")[1];
            //debug
            System.out.println("start: " + start);
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
        // if there is no such posting, the iterator is advanced to the end of the list
        // returns the document identifier of the current posting
        // if the iterator is already past the end of the list, the method returns -1
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

    public int getTermFreq(ListPointer lp) throws IOException {
        // returns the frequency of the current posting
        return lp.getTermFreq(lp.index);
    }

    public int computeScoreTFIDF(int docId, int[] tf, int[] df) {
        // computes the score of the current doc
        // print tf
        System.out.println("tf: " + Arrays.toString(tf));
        // print df
        System.out.println("df: " + Arrays.toString(df));
        int score = 0;        
        for (int i = 0; i < tf.length; i++) {
            score += tf[i] * Math.log(numberDoc/df[i]);
        }
        return score;
    }

    public List<List<Integer>> addScore(List<Integer> scoreList, int score, List<Integer> docIdList, int did) {
        if (scoreList.size() < 10){
            scoreList.add(score);
            docIdList.add(did);
        }
        else{
            int min = Collections.min(scoreList);
            if (score > min){
                int index = scoreList.indexOf(min);
                scoreList.remove(index);
                scoreList.add(score);
                docIdList.remove(index);
                docIdList.add(did);
            }
        }
        List<List<Integer>> result = new ArrayList<List<Integer>>();
        result.add(scoreList);
        result.add(docIdList);
        return result;
    }
    public List<List<Integer>> executeQuery(String query) throws IOException {
        String[] q = query.toLowerCase().split(" ");
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

        // find the min Max doc id
        int minMaxDocId = list.get(0).getMaxDocId();
        for (int i = 1; i < list.size(); i++){
            if (list.get(i).getMaxDocId() < minMaxDocId){
                minMaxDocId = list.get(i).getMaxDocId();
            }
        }

        int maxDocId = 0;
        for (ListPointer p : list) {
            int tmpMaxDocId = p.getMaxDocId();
            if (tmpMaxDocId > maxDocId) {
                maxDocId = tmpMaxDocId;
            }
        }

        // result
        List<Integer> scoreList = new ArrayList<Integer>();
        // docID list of the result
        List<Integer> docIdList = new ArrayList<Integer>();


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
                        tf[i] = getTermFreq(list.get(i));
                        df[i] = list.get(i).getDocFreq();
                    }
                    int score = computeScoreTFIDF(did, tf, df);
                    // add score to the list if size <10 or replace the lowest score
                    List<List<Integer>> tmp =  addScore(scoreList, score, docIdList, did);
                    scoreList = tmp.get(0);
                    docIdList = tmp.get(1);
                    
                }

                did++; /* and increase did to search for next post */
            }
        }
        else{ // disjunctive query: compute the score for all docs that have a list one term
            int did =0;
            while (did <= maxDocId){
                int[] tf = new int[num]; // term frequencies array
                int[] df = new int[num]; // document frequencies array
                int nbTerm = 0;
                // print doc id and num
                for (int i=0; i<num; i++) {
                    int d = nextGEQ(list.get(i), did);
                    if (d == did){
                        tf[i] = getTermFreq(list.get(i));
                        df[i] = list.get(i).getDocFreq();
                        // print df[i]
                        System.out.println("df : " + df[i]);
                        System.out.println("tf : " + df[i]);
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
                    List<List<Integer>> tmp =  addScore(scoreList, score, docIdList, did);
                    scoreList = tmp.get(0);
                    docIdList = tmp.get(1);
                }
                did++;
            }           
        }
        return Arrays.asList(docIdList, scoreList);
    }        
}