package laurie_boveroux;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.*;

import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.ByteBuffer;

public class Test{

    public static BufferedOutputStream invIndexDocidBuffer;
    public static BufferedOutputStream invIndexFreqBuffer;
    public static int totalInvIndexBytesId;
    public static int totalInvIndexBytesFreq;

    public Test() throws IOException{
        this.invIndexDocidBuffer = new BufferedOutputStream(new FileOutputStream("InvertedIndexDocid.txt"),4096 * 10000);
        this.invIndexFreqBuffer  = new BufferedOutputStream(new FileOutputStream("InvertedIndexFreq.txt"),4096 * 10000);
        this.totalInvIndexBytesId = 0;
        this.totalInvIndexBytesFreq = 0;
    }

    public static void writeToFileInvertedIndex() throws IOException{
        // Write the buffer to the file
        invIndexDocidBuffer.flush();
        invIndexFreqBuffer.flush();
        totalInvIndexBytesId = 0;
        totalInvIndexBytesFreq = 0;
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

    public static void VBEncode(List<Integer> list, Boolean flag) throws IOException{
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
            if (flag){
                invIndexDocidBuffer.write(Integer.parseInt(binary,2));
            }
            else{
                invIndexFreqBuffer.write(Integer.parseInt(binary,2));
            }
        }
    }

    public static List<Integer> VBDecode(byte[] bytesTab){
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

    public static String[] gammaEncode(int n, String l, String postinglist, Boolean last) throws IOException {
        int ceiling = 0;
        String filler = "";

        String[] lengthAndPostings = new String[2];

        System.out.println("\nOg DocId: "+ n);
        // Convert the number to binary
        String binary = Integer.toBinaryString(n);
        // Compute the number of bits needed to represent the binary number
        int numBits = binary.length();

        int length = Integer.parseInt(l);
        length += (numBits*2) +1;
        System.out.println("Length: "+ length);

        //print length
        System.out.println("Num bits: " +numBits);

        // Compute the unary representation of the number of bits
        String unary = "1".repeat(numBits) +"0";

        if (last && !(length % 8 == 0)){
            ceiling = (int) (8*(Math.ceil(Math.abs(length/8)))) +8 ;
            System.out.println("Ceiling: " + ceiling + " \tFiller: " + (ceiling-length));
            filler = "0".repeat(ceiling-length);
        }

        // Write the concatenation of the unary representation and the binary number
        binary = unary+binary+filler;
        String postings = postinglist + binary;
        System.out.println("Length in Bytes: " + length/8);
        System.out.println("Gamma Encoding: " + binary);

        if (last) {
            for (int i = 0; i < postings.length(); i += 8) {
                String byteString = postings.substring(i, Math.min(postings.length(), i + 8));
                //System.out.println("Byte String to Encode: " + byteString);
                invIndexDocidBuffer.write(Integer.parseInt(byteString, 2));
            }
        }

        lengthAndPostings[0]= Integer.toString(length);
        lengthAndPostings[1]= postings;
        return lengthAndPostings;

    }

    public static List<Integer> gammaDecodeList(byte[] bytesTab) throws IOException {
//        System.out.println("Gamma Decoding: " + new String(bytesTab));
        // Initialize an empty list to store the decoded numbers
        List<Integer> numbers = new ArrayList<>();
        String docIdBinary = "";
        String postings = "";

        for(int i = 0; i < bytesTab.length; i++) {
            //System.out.println("Byte: " + bytesTab2[i]);
            String binary = String.format("%8s", Integer.toBinaryString(bytesTab[i] & 0xFF)).replace(' ', '0');
            postings = postings+binary;
        }

        // Keep track of the current position in the encoded string
        int pos = 0;
        // Loop until we reach the end of the bytesTab
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
                //convert ascii to binary
                int numericValue = Character.getNumericValue(postings.charAt(pos));
                //System.out.println("Pos: " + pos + "\tNumeric Value: " + numericValue);
                //convert to string
                docIdBinary += Integer.toString(numericValue);
                //print binary
                //System.out.println("Binary: " + docIdBinary);
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

    public static String[] unaryEncode(int n, String separator, String l, String postinglist, Boolean last) throws IOException {
        int ceiling = 0;
        String filler = "";
        String unary = "";
        //initialize array result
        String[] lengthAndPostings = new String[2];


        System.out.println("\nOg Freq: "+ n);

        // Compute the unary representation of the number
        if (last){
            unary = ("1").repeat(n);
        } else {
            unary = ("1").repeat(n)+ separator;
        }
//        String unary = ("1").repeat(n);
        int length = Integer.parseInt(l);

        length += unary.length();
        System.out.println("Length: "+ length);

        if (last && !(length % 8 == 0)){
            ceiling = (int) (8*(Math.ceil(Math.abs(length/8)))) +8 ;
            System.out.println("Ceiling: " + ceiling + " \tFiller: " + (ceiling-length));
            filler = "0".repeat(ceiling-length);
        }

        String postings = postinglist+unary+filler;
        System.out.println("Unary Encoding: " + postings);

        if (last) {
            for (int i = 0; i < postings.length(); i += 8) {
                String byteString = postings.substring(i, Math.min(postings.length(), i + 8));
                System.out.println("Byte String to Encode: " + byteString);
                invIndexFreqBuffer.write(Integer.parseInt(byteString, 2));
            }
        }

        lengthAndPostings[0]= Integer.toString(length);
        lengthAndPostings[1]= postings;
        return lengthAndPostings;
    }

    public static List<Integer> unaryDecodeList(byte[] bytesTab2) {
        // Initialize an empty list to store the decoded numbers
        List<Integer> numbers = new ArrayList<>();
        String postings = "";

        for(int i = 0; i < bytesTab2.length; i++) {
            //System.out.println("Byte: " + bytesTab2[i]);
            String binary = String.format("%8s", Integer.toBinaryString(bytesTab2[i] & 0xFF)).replace(' ', '0');
            postings = postings.concat(binary);
        }
        //System.out.println("Binary: " + binary);
        int count = 0;
        for (int j = 0; j < postings.length(); j++) {
            if (postings.charAt(j) == '1') {
                count++;
            } else {
                if (count > 0) {
                    numbers.add(count);
                    count = 0;
                }
            }
        }
        if (count > 0) {
            numbers.add(count);
        }

        // Return the list of decoded numbers
        return numbers;
    }

    public static void main(String[] args) throws IOException{
        // Variable-bytes code, an algorithm to compress integer to bytes
        // https://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html
        Test test = new Test();
        List<Integer> listDocId = new ArrayList<Integer>();
        String postingListDocId = "";
        //store the size of the posting list to know how many filler 0s to add in the end
        String[] postingListGamma = {"0", ""};
        String[] postingListUnary = {"0", ""};
        Boolean last = false;

        //add random numbers between 0 and 1000000
        for (int i = 0; i < 5; i++){
            listDocId.add((int)(Math.random() * 1000000));
        }

        List<Integer> listFreq = new ArrayList<Integer>();

        //add random numbers between 0 and 1000000
        for (int i = 0; i < 5; i++){
            listFreq.add((int)(Math.random() * 200));
        }

        //Go through every element in the posting list
        for (int i = 0; i < listDocId.size(); i++) {
            //check if last element
            if (i + 1 >= listDocId.size()) {
                last = true;
            }

            postingListGamma = gammaEncode(listDocId.get(i), postingListGamma[0], postingListGamma[1], last);

            postingListUnary = unaryEncode(listFreq.get(i),"0", postingListUnary[0], postingListUnary[1], last);

        }

        invIndexDocidBuffer.close();
        invIndexFreqBuffer.close();

        // read the file
        byte[] bytesTab = Files.readAllBytes(Paths.get("InvertedIndexDocid.txt"));
        // decode the file
        List<Integer> decodedList = gammaDecodeList(bytesTab);
        System.out.println("DocId List: " + decodedList);

        // read the file
        byte[] bytesTab2 = Files.readAllBytes(Paths.get("InvertedIndexFreq.txt"));
        // decode the file
        List<Integer> decodedList2 = unaryDecodeList(bytesTab2);
        System.out.println("Freq List: " + decodedList2);

    }

//    public static void main(String[] args) throws IOException{
//        // Variable-bytes code, an algorithm to compress integer to bytes
//        // https://nlp.stanford.edu/IR-book/html/htmledition/variable-byte-codes-1.html
//        Test test = new Test();
//        List<Integer> listDocId = new ArrayList<Integer>();
//        //add random numbers between 0 and 1000000
//        for (int i = 0; i < 1000000; i++){
//            listDocId.add((int)(Math.random() * 1000000));
//        }
//        // listDocId.add(6964361);
//
//        List<Integer> listFreq = new ArrayList<Integer>();
//        //add random numbers between 0 and 1000000
//        for (int i = 0; i < 1000000; i++){
//            listFreq.add((int)(Math.random() * 200));
//        }
//        //listFreq.add(7);
//
//        for (int i = 0; i < listDocId.size(); i++){
//            List<Integer> encodedDocId = new ArrayList<Integer>();
//            List<Integer> encodedFreq = new ArrayList<Integer>();
//
//            if (listDocId.get(i) > listFreq.get(i)){ // if the docid is greater than the frequency, adapt the frequency
//                encodedDocId = VBEncodeNumber(listDocId.get(i));
//                VBEncode(encodedDocId, true);
//                int lenDocID = encodedDocId.size();
//                encodedFreq = VBEncodeNumber(listFreq.get(i));
//                while (encodedFreq.size() < lenDocID){
//                    encodedFreq.add(0);
//                }
//                VBEncode(encodedFreq, false);
//            }
//            else{ // if the frequency is greater than the docid, adapt the docid
//                encodedFreq = VBEncodeNumber(listFreq.get(i));
//                VBEncode(encodedFreq, false);
//                int lenFreq = encodedFreq.size();
//                encodedDocId = VBEncodeNumber(listDocId.get(i));
//                while (encodedDocId.size() < lenFreq){
//                    encodedDocId.add(0);
//                }
//                VBEncode(encodedDocId, true);
//            }
//        }
//
//        invIndexDocidBuffer.close();
//        invIndexFreqBuffer.close();
//
//        // read the file
//        byte[] bytesTab = Files.readAllBytes(Paths.get("InvertedIndexDocid.txt"));
//        List<Integer> decodedList = VBDecode(bytesTab);
//        //System.out.println(decodedList);
//
//        // read the file
//        byte[] bytesTab2 = Files.readAllBytes(Paths.get("InvertedIndexFreq.txt"));
//        List<Integer> decodedList2 = VBDecode(bytesTab2);
//        //System.out.println(decodedList2);
//
//
//    }
}