package com.sending.sdk;

import java.io.*;

public class FileHelper {
    public static String sync_next_batch = "./nextBatch";

    public static void writeFile(String file, String content) throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(content);
        printWriter.flush();
        printWriter.close();
    }

    public static String readFile(String file) throws IOException {
        if(!new File(file).exists()){
            return "";
        }
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
            line = br.readLine();
        }
        return sb.toString();
    }
}
