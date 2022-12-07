package laurie_boveroux;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;

import org.apache.commons.io.LineIterator;

public class QueryTest{
    
    public void testQueryTime() throws IOException{
        QuerySearch querySearch = new QuerySearch(8841823, false);
        String queryFilePath = "data/queries.eval.tsv";
        File queryFile = new File(queryFilePath);
        LineIterator it = FileUtils.lineIterator(queryFile, "UTF-8");
        long time = 0;
        int nbQueries = 0;
        int nbQueriesWithoutResults = 0;
        for(int i=0; i<3600; i++){
        //while(it.hasNext()){
            String line = it.nextLine();
            String[] lineSplit = line.split("\t");
            String queryId = lineSplit[0];
            String queryText = lineSplit[1];
            //System.out.println("Query: " + queryText);           
            long startTime = System.currentTimeMillis();
            int tmp = querySearch.executeQuery("conjunctive", queryText, false, "okapibm25");
            long endTime = System.currentTimeMillis();
            if (tmp == 0){
                System.out.println(queryId + " " + queryText);
                nbQueriesWithoutResults++;
            }
            else{
                time += (endTime - startTime);
            }
            nbQueries++;
        }
        System.out.println("Total time: " + time + " in ms");
        // get the average time with 4 decimals
        double avgTime = (double) time/ (double) nbQueries;
        System.out.println("Average time: " + avgTime + " in ms");
        double avgTimeBis = (double) time/ (double) (nbQueries-nbQueriesWithoutResults);
        System.out.println("Average time without queries without results: " + avgTimeBis + " in ms");
        System.out.println("Number of queries: " + nbQueries);
        System.out.println("Number of queries without results: " + nbQueriesWithoutResults);
        
    }

    public static void main(String[] args) throws IOException{
        QueryTest queryTest = new QueryTest();
        queryTest.testQueryTime();
    }

}
