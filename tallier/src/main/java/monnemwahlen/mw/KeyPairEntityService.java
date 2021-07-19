package monnemwahlen.mw;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public interface KeyPairEntityService {





    default void createNewKeyPair(String privateKeyPath, String publicKeyPath) {
        try {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");

            keygen.initialize(2048);

            KeyPair pair = keygen.generateKeyPair();
            PrivateKey privateKey = pair.getPrivate();
            PublicKey publicKey = pair.getPublic();

            FileOutputStream fileOut = new FileOutputStream(privateKeyPath);
            ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
            objOut.writeObject(privateKey);
            objOut.close();
            System.out.println("new Private key, "+privateKeyPath);
            System.out.println("size: "+privateKey.getEncoded().length);

            fileOut = new FileOutputStream(publicKeyPath);
            objOut = new ObjectOutputStream(fileOut);
            objOut.writeObject(publicKey);
            objOut.close();

            String encoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            System.out.println("new Public key, "+publicKeyPath);
            System.out.println("size: "+publicKey.getEncoded().length);



        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    default String getKeyString(String path) {
        try {
            FileInputStream fileIn = new FileInputStream(path);
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            Key key;
                key = (Key) objIn.readObject();
            objIn.close();

            return Base64.getEncoder().encodeToString(key.getEncoded());

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }


    default Key getKey(String path) {
        try {
            FileInputStream fileIn = new FileInputStream(path);
            ObjectInputStream objIn = new ObjectInputStream(fileIn);
            Key key;
//            if(keyType.equals("private")){
//             key = (PrivateKey) objIn.readObject();
//            }
//            else
//            {
//                key = (PublicKey) objIn.readObject();
//            }
            key = (Key) objIn.readObject();
            objIn.close();

            return key;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    default PublicKey stringToPublicKey(String publicK) {
        try {
            byte[] publicBytes = org.apache.commons.codec.binary.Base64.decodeBase64(publicK);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return  keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

//
//    default PrivateKey stringToPrivateKey(String publicK) {
//        try {
//            byte[] publicBytes = org.apache.commons.codec.binary.Base64.decodeBase64(publicK);
//            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
//            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//            return (PrivateKey) keyFactory.generatePrivate(keySpec);
//        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }


}
