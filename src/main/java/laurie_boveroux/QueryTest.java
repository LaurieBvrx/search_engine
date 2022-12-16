package laurie_boveroux;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;

import org.apache.commons.io.LineIterator;

public class QueryTest{
    
    public void testQueryTime() throws IOException{
        String typeQuery = "conjunctive";//"disjunctive"; //"conjunctive";
        Boolean stemFlag = true;
        String rankingFunction = "okapibm25";
        Boolean stopWordsFlag = true;


        QuerySearch querySearch = new QuerySearch();
        String queryFilePath = "data/msmarco-test2020-queries.tsv";
        File queryFile = new File(queryFilePath);
        LineIterator it = FileUtils.lineIterator(queryFile, "UTF-8");
        long time = 0;
        int nbQueries = 0;
        int nbQueriesWithoutResults = 0;
        int i = 0;
        while(it.hasNext()){
            System.out.println(i);
            String line = it.nextLine();
            String[] lineSplit = line.split("\t");
            String queryId = lineSplit[0];
            String queryText = lineSplit[1];
            long startTime = System.currentTimeMillis();
            List<List<Double>> tmp = querySearch.executeQuery(typeQuery, queryText, stemFlag, rankingFunction, stopWordsFlag);
            long endTime = System.currentTimeMillis();
            if (tmp == null){
                System.out.println(queryId + " " + queryText);
                nbQueriesWithoutResults++;
            }
            else{
                time += (endTime - startTime);
            }
            if((endTime - startTime) > 1000){
                System.out.println(queryId + " " + queryText);
            }
            nbQueries++;
            i++;
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
