package monnemwahlen.mw;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public interface MoService {

    String IP = "http://localhost";
    Map<String, String> ports = new HashMap<>() {{
        put("auth", "8081");
        put("ew", "8082");
        put("val", "8083");
        put("tal", "8084");
    }};


    default String getUrl(String entity) {
        return IP + ":" + ports.get(entity);
    }

    default String encrypt(String input, int decryptMode, Key key) {
        try {
            //for demo purposes we use no padding instead of RSA/ECB/PKCS1Padding, for any other implementation padding must be used!
            Cipher cipher = Cipher.getInstance("RSA/ECB/NOPADDING");
            cipher.init(decryptMode, key);

            // return Base64.encodeBase64String(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));

            return crpt(input, cipher, decryptMode);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;

    }

    //encrypts larger encryption blocks. WIth this we can encrypt string which are already encrypted with other keys
    default String crpt(String input, Cipher cipher, int cryptType) throws BadPaddingException, IllegalBlockSizeException {

        // transform obj to byte [] for encryption
        byte[] inputArray;
        if (cryptType == Cipher.DECRYPT_MODE) {
            // since we encode BASE 64 strings after the key encryption, we need to decode it back
            inputArray = Base64.decodeBase64(input);}
        else {
            inputArray = input.getBytes();}
        int inputLength = inputArray.length;
        //max block size for 2048 bit keys
        int MAX_ENCRYPT_BLOCK = 256;
        // counter offset,
        int offSet = 0;
        //final product
        byte[] resultBytes = {};
        //cache for the  blocks
        byte[] cache = {};
        while (inputLength - offSet > 0) {
            //if the remaining block is still bigger than the max possible encryption block
            if (inputLength - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(inputArray, offSet, MAX_ENCRYPT_BLOCK);
                offSet += MAX_ENCRYPT_BLOCK;}
            // final block, in which the remaining block is either the same size or smaller than the max possible block
            else {
                cache = cipher.doFinal(inputArray, offSet, inputLength - offSet);
                offSet = inputLength;}
            //create a new array with the resultbytes and the cache
            resultBytes = Arrays.copyOf(resultBytes, resultBytes.length + cache.length);
            System.arraycopy(cache, 0, resultBytes, resultBytes.length - cache.length, cache.length);
        }
        if (cryptType == Cipher.ENCRYPT_MODE) {
            return Base64.encodeBase64String(resultBytes);}
        return new String(resultBytes, StandardCharsets.UTF_8);}



    default String digest(String ballot) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(ballot.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(hash);
    }

    default LinkedHashMap<String, Object> sendPostRequest(String url, Map<String, Object> jsonData) {
        try {

            HttpPost httppost = new HttpPost(url);

            ObjectMapper objectMapper = new ObjectMapper();

// Request parameters and other properties.
            String data = objectMapper.writeValueAsString(jsonData);
            List<NameValuePair> params = new ArrayList<NameValuePair>(2);
            params.add(new BasicNameValuePair("json", data));
            httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

            try (CloseableHttpClient httpclient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpclient.execute(httppost)) {

                String handledResponse = new BasicResponseHandler().handleResponse(response);

                JSONParser parser = new JSONParser(handledResponse);

                return parser.parseObject();

                //here we could work with the response which contains the voter ids mapped to the worker ids.

            } catch (ParseException | EOFException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }

    default LinkedHashMap<String, Object> sendGetRequest(String url) {

        HttpGet httpGet = new HttpGet(url);

        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = httpclient.execute(httpGet)) {

            String handledResponse = EntityUtils.toString(response.getEntity());

            JSONParser parser = new JSONParser(handledResponse);

            return parser.parseObject();

            //here we could work with the response which contains the voter ids mapped to the worker ids.

        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    default void saveFile(String path, Object obj) {
        try {
            File f = new File(path);
            FileOutputStream fileOut = new FileOutputStream(f);
            ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
            objOut.writeObject(obj);
            objOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    default Object loadFile(String path) {
        try {
            File f = new File(path);
            FileInputStream fileIn = new FileInputStream(f);
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            Object obj = objIn.readObject();
            objIn.close();
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

}
