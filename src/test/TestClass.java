package test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import static test.SimpleBot.dictionaryFill;

/**
 * Created by slavon on 01.05.17.
 */
public class TestClass {
    public static void main(String[] args) {
        Path dictPath = Paths.get("a.txt");
        ArrayList<HashMap<String,String>> maps = new ArrayList<>();
        try (Scanner in = new Scanner(dictPath)) {
            dictionaryFill(in, maps);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
