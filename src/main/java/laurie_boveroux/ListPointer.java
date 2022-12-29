package laurie_boveroux;

import java.io.*;
import java.util.*;

public class ListPointer{

    private int startIndexId;
    private int startIndexFreq;
    private int endIndexId;
    private int endIndexFreq;
    private int lengthBytesId;
    private int lengthBytesFreq;
    private List<Integer> docIdsArray;
    private List<Integer> freqsArray;
    private int index;
    private int nbRelevantDocs;


    public ListPointer(int startIndexId, int startIndexFreq, int endIndexId, int endIndexFreq, RandomAccessFile fileDocIds, RandomAccessFile fileFreqs) throws IOException {

        this.startIndexId = startIndexId;
        this.startIndexFreq = startIndexFreq;
        if (endIndexId == -1){
            this.endIndexId = (int) fileDocIds.length();
            this.endIndexFreq = (int) fileFreqs.length();
        }
        else{
            this.endIndexId = endIndexId;
            this.endIndexFreq = endIndexFreq;
        }
        this.lengthBytesId = this.endIndexId - this.startIndexId;
        this.lengthBytesFreq = this.endIndexFreq - this.startIndexFreq;
        this.index = 0;
        this.docIdsArray = getDocIdArray(this.lengthBytesId, this.startIndexId, fileDocIds);
        this.freqsArray = getFreqArray(this.lengthBytesFreq, this.startIndexFreq, fileFreqs);
        this.nbRelevantDocs = this.docIdsArray.size();
    }

    public int getLength() {
        return this.nbRelevantDocs;
    }

    public int getDocFreq() {
        return this.nbRelevantDocs;
    }

    public int getMaxDocId() {
        return docIdsArray.get(nbRelevantDocs-1);
    }

    public int getFreq(int index){
        return freqsArray.get(index);
    }

    public int getIndex(){
        return this.index;
    }

    public void setIndex(int index){
        this.index = index;
    }

    public int getDocId(int index){
        return docIdsArray.get(index);
    }

    private static List<Integer> getDocIdArray(int lengthBytes, int startIndex, RandomAccessFile fileDocIds) throws IOException{
        // read the docIds: lengthBytes bytes from startIndex in fileDocIds
        byte[] bytesId = new byte[lengthBytes];
        fileDocIds.seek(startIndex);
        fileDocIds.read(bytesId);
        //System.out.println("bytesId: " + Arrays.toString(bytesId));

        // decode the docIds
        //List<Integer> decodedDocIdsList = VBDecode(bytesId);
        List<Integer> decodedDocIdsList = gammaDecodeList(bytesId);

        return decodedDocIdsList;
    }

    private static List<Integer> getFreqArray(int lengthBytes, int startIndex, RandomAccessFile fileFreqs) throws IOException{
        // read the frequencies: lengthBytes bytes from startIndex in fileFreqs
        byte[] bytesFreq = new byte[lengthBytes];
        fileFreqs.seek(startIndex);
        fileFreqs.read(bytesFreq);
        //System.out.println("bytesFreq: " + Arrays.toString(bytesFreq));

        // decode freqs
        //List<Integer> decodedFreqsList = VBDecode(bytesFreq);
        List<Integer> decodedFreqsList = unaryDecodeList(bytesFreq);

        return decodedFreqsList;
    }

    public static List<Integer> gammaDecodeList(byte[] bytesTab) {
        // Initialize an empty list to store the decoded numbers
        List<Integer> numbers = new ArrayList<>();
        String docIdBinary = "";
        String postings = "";

        // concatenate a string of all bytes
        for(int i = 0; i < bytesTab.length; i++) {
            //System.out.println("Byte: " + bytesTab2[i]);
            String binary = String.format("%8s", Integer.toBinaryString(bytesTab[i] & 0xFF)).replace(' ', '0');
            postings = postings+binary;
        }

        // Keep track of the current position in the string
        int pos = 0;
        // Loop until we reach the end of the posting list
        while (pos < postings.length()) {

            int unary = 0;
            // Count the number of 1s in the unary representation
            while (postings.charAt(pos) == '1') {
                unary++;
                pos++;
            }
            // Skip the 0 in the unary representation
            pos++;

            for (int i =0; i < unary; i++) {
                //Create binary string of the docId
                docIdBinary += postings.charAt(pos);
                pos++;

            }

            // Convert the binary representation to an integer
            if (docIdBinary.length() > 0) {
                numbers.add(Integer.parseInt(docIdBinary, 2));
                docIdBinary = "";
            }
        }

        // Return the list of decoded numbers
        return numbers;
    }

    public static List<Integer> unaryDecodeList(byte[] bytesTab2) {
        // Initialize an empty list to store the decoded numbers
        List<Integer> numbers = new ArrayList<>();
        String postings = "";

        // concatenate a string of all bytes
        for(int i = 0; i < bytesTab2.length; i++) {
            //Convert byte to binary string and replace gaps
            String binary = String.format("%8s", Integer.toBinaryString(bytesTab2[i] & 0xFF)).replace(' ', '0');
            postings = postings.concat(binary);
        }

        int count = 0;
        // Loop until we reach the end of the posting list
        for (int j = 0; j < postings.length(); j++) {
            // Count the number of 1s until finding the separator 0
            if (postings.charAt(j) == '1') {
                count++;
            } else {
                // Add the number of 1s to the list of decoded numbers
                if (count > 0) {
                    numbers.add(count);
                    count = 0;
                }
            }
        }
        //if the last number isn't followed by a zero, write the count into numbers
        if (count > 0) {
            numbers.add(count);
        }

        // Return the list of decoded numbers
        return numbers;
    }

    private static List<Integer> VBDecode(byte[] bytesTab){
        List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < bytesTab.length; i++){
            String intermediateNumber = "";
            while (bytesTab[i] < 0){ // 1xxxxxxx, read the next byte while the first bit is 1
                String binaryNum = Integer.toBinaryString(bytesTab[i] & 0x7F); // to binary by replacing the first bit by 0
                while (binaryNum.length() < 7){ // add 0 to the left if the binary is not 8 bits
                    binaryNum = "0" + binaryNum;
                }
                intermediateNumber = binaryNum + intermediateNumber;
                i++;
            }
            // last number
            String binaryNum = Integer.toBinaryString(bytesTab[i] & 0x7F);
            intermediateNumber = binaryNum + intermediateNumber;
            result.add(Integer.parseInt(intermediateNumber,2));
        }
        return result;        
    }
}