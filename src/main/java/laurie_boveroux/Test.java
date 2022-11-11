package laurie_boveroux;
import java.io.*;
import java.util.*;

public class Test{

    public void createFile() throws IOException {
        // Create a new file
        RandomAccessFile file = new RandomAccessFile("test.txt", "rw");
        // write 1000 strings of random length in the file
        List<String> listWords = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            // random length between 1 and 20
            int length = (int) (Math.random() * 20) + 1;
            String word = "";
            for (int j = 0; j < length; j++) {
                // random char between a and z
                int character = (int) (Math.random() * 26 + 'a');
                // pritn the word
                word += (char) character;
            }
            // add word to the list
            listWords.add(word);
            // print the word every 100 words
            if (i % 100 == 0) {
                System.out.println(word);
            }
        }

        // sort the list
        Collections.sort(listWords);
        // write the list in the file
        for (String word : listWords) {
            file.writeBytes(word);
            // random number
            int number = (int) (Math.random() * 100);
            // write the number
            file.writeBytes(" " + number + "\n");
        }
        file.close();

    }

    // String comparator
    public static class StringComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            // s1 is the line
            // split the line
            String[] split = s1.split(" ");
            // get the first element
            String word = split[0];
            // s2 is the element
            return word.compareTo(s2);
        }
    }

    public static void main(String[] args) throws IOException{
        System.out.println("Hello World!");
        if (false){
            Test test = new Test();
            try {
                test.createFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (true){
            int x = 4;
            // list of integers
            List<Integer> list = new ArrayList<Integer>();
            for (int i = 0; i < 10; i++){
                list.add(4);
            }
            int i = 0;
            while(i < list.size() && list.get(i) == x){
                System.out.println(i);
                i++;
            }

        }

    }
}

// 