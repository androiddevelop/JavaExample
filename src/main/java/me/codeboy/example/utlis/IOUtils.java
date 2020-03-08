package me.codeboy.example.utlis;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by yuedong.li on 2020/2/8
 */
public class IOUtils {
    public static void closeQuietly(Closeable closeable)  {
        if(closeable!=null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
