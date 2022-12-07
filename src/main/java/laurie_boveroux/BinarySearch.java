package laurie_boveroux;
import java.io.*;
import java.util.*;

class BinarySearch{
    final RandomAccessFile file;
    long lineLen;
    int count;
    int maxCount;

    public BinarySearch(File f) throws FileNotFoundException {
        this.file = new RandomAccessFile(f, "r");
        this.lineLen = 72;
    }

    public List<byte[]> search(String element) throws IOException {
        long l = file.length();
        return search(element, 0, l-1, l);
    }

    private List<byte[]> search(String element, long low, long high, long l) throws IOException {
        if (low + lineLen >= high) {
            return null;
        }
        
        long middle = low + ((high - low) / 2) + 1;

        List<byte[]> result = new ArrayList<byte[]>();
        long quotient = middle / lineLen;
        long index = quotient * lineLen;
        byte[] line = nextLine(index, l);
        if (line == null) { // end of file
            return result;
        }

        int r = compare(line, element);
        if(r > 0) { // search in the left          
            result = search(element, low, index, l);
        } else if(r < 0) { // search in the right
            result = search(element, index, high, l);
        } else { // found
            result.add(line);
            byte[] nextline = nextLine(index + lineLen, l);
            if (nextline != null) {
                // add the next line to the result
                result.add(nextline);
            }
        }
        return result;    
    }

    

    private byte[] nextLine(long p, long l) throws IOException {
        if (p >= l) {
            return null;
        }
        file.seek(p);
        byte[] line = new byte[(int) lineLen];
        file.read(line);

        return line;
    }
    private int compare(byte[] s1, String s2) {
        byte[] s1Word = subArray(s1, 0, 64); // the first 64 bytes are the word
        String word = new String(s1Word);        
        word = word.trim(); // keep only the char different from 0
        return word.compareTo(s2);
    }

    private byte[] subArray(byte[] array, int start, int end) {
        byte[] result = new byte[end - start];
        for (int i = start; i < end; i++) {
            result[i - start] = array[i];
        }
        return result;
    }
}