package laurie_boveroux;

import java.io.*;
import java.util.*;

public class ListPointer{

    private int startIndex;
    private int endIndex;
    private int lengthBytes;
    private List<Integer> docIdsArray;
    private List<Integer> freqsArray;
    private int index;
    private int nbRelevantDocs;


    public ListPointer(int startIndex,int endIndex, RandomAccessFile fileDocIds, RandomAccessFile fileFreqs) throws IOException {

        this.startIndex = startIndex;
        if (endIndex == -1){
            this.endIndex = (int) fileDocIds.length();
        }
        else{
            this.endIndex = endIndex;
        }
        this.lengthBytes = this.endIndex - this.startIndex;
        this.index = 0;
        this.docIdsArray = getDocIdArray(this.lengthBytes, this.startIndex, fileDocIds);
        this.freqsArray = getFreqArray(this.lengthBytes, this.startIndex, fileFreqs);  
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

        // decode the docIds
        List<Integer> decodedDocIdsList = VBDecode(bytesId);

        return decodedDocIdsList;
    }

    private static List<Integer> getFreqArray(int lengthBytes, int startIndex, RandomAccessFile fileFreqs) throws IOException{
        // read the frequencies: lengthBytes bytes from startIndex in fileFreqs
        byte[] bytesFreq = new byte[lengthBytes];
        fileFreqs.seek(startIndex);
        fileFreqs.read(bytesFreq);

        // decode freqs
        List<Integer> decodedFreqsList = VBDecode(bytesFreq);
        
        return decodedFreqsList;
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