package laurie_boveroux;

import java.io.*;
import java.util.*;

public class QuerySearch{

    public static Map<String,Integer[]> lexicon;
    public static Map<Integer,Integer[]> docIndex;
    public static Integer averageDocumentSize = 0;

    public QuerySearch() throws FileNotFoundException{

    }

    public static void loadLexiconIntoMemory() throws IOException {
        String currentPath = new java.io.File(".").getCanonicalPath();
        String lexiconPath = currentPath + "/data/Lexicon.txt";        
        FileReader in = new FileReader(lexiconPath);
        BufferedReader br = new BufferedReader(in,4096 * 100000);

        String line;
        int count =0;
        while ((line = br.readLine()) != null) {

            String[] words = line.split(" ");

            Integer[] list = new Integer[3];            
            list[0]=Integer.parseInt(words[2]);
            list[1]=Integer.parseInt(words[4]);
            list[2]=Integer.parseInt(words[6]);

            lexicon.put(words[0],list);
            if(count%10000 == 0) {
                System.out.println("Loading lexicon " + count + " " + words[0]);
            }
            count++;

        }
        br.close();
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
        if(lexicon.containsKey(term)){
            ListPointer lp = new ListPointer(term,lexicon.get(term)[0],lexicon.get(term)[1]);
            //lp.readLastAndSizeArrays(lexicon.get(term)[0]);
            return lp;
        }else {
            return null;
        }

    }

    // public void executeQuery(String query) throws IOException {
    //     String[] q = query.toLowerCase().split(" ");
    //     int num = q.length;
    //     ListPointer[] lp = new ListPointer[num];
    //     for (int i = 0; i < num; i++){
    //         lp[i] = openList(q[i]);
    //     }
    //     Integer[] docFreq = new Integer[num];

    //     //SortListPointersResult s = sortListPointersAndRemoveNull(lp,q,docFreq);
    //     //lp = s.lp;
    //     //q = s.q;
    //     //docFreq = s.docFreq;

    //     num = lp.length;

    //     if(num == 0){
    //         return;
    //     }

    //     int maxDocID = findMaxDocId(lp[0]);

    //     //Algo for conjunctive Query Processing
    //     int did = 0;
    //     while (did <= maxDocID)
    //     {
    //         /* get next post from shortest list */
    //         did = nextGEQ(lp[0], did);
    //         int d=0;
    //         /* see if you find entries with same docID in other lists */
    //         for (int i=1; (i<num) && ((d=nextGEQ(lp[i], did)) == did); i++);

    //         if (d > did) did = d; /* not in intersection */

    //         else
    //         {
    //             int[] f = new int[num];
    //             /* docID is in intersection; now get all frequencies */
    //             for (int i=0; i<num; i++) {
    //                 f[i] = getFreq(lp[i]);
    //             }

    //             /* compute BM25 score from frequencies and other data */
    //             computeBM25Score(did,q,f,docFreq);
    //             did++; /* and increase did to search for next post */
    //         }
    //     }

    //     //Algo for disjunctive Query Processing
    //     if(top10Results.size()==0){
    //         System.out.println("No results found using conjunctive processing. Applying disjunctive processing...");
    //         for (int i = 0; i < num; i++){
    //             lp[i] = openList(q[i]);
    //         }

    //         for (int i=0;i<num;i++) {
    //             did = 0;
    //             maxDocID = findMaxDocId(lp[i]);
    //             while (did <= maxDocID) {
    //                 did = nextGEQ(lp[i], did);
    //                 computeBM25Score(did,q[i],getFreq(lp[i]),docFreq[i]);
    //                 did++;
    //             }
    //         }
    //     }

    // }


        
}