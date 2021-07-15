package ew;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class ElectionWorker implements Serializable {

    private String ID1;
    private String ID2;

    private String ballot;
    private String cipherBallot;

    private List<String> voterList;

    private KeyPair firstPair;
    private PrivateKey firstPrivateKey;
    private PublicKey firstPublicKey;

    private KeyPair secondPair;
    private PrivateKey secondPrivateKey;
    private PublicKey secondPublicKey;

    private KeyPair ek_dkPair;
    private PrivateKey encryptionKey;
    private PublicKey decryptionKey;

    private KeyPair blindKeyPair;
    private PrivateKey blindingKey;
    private PublicKey deblindingKey;

    private SecretKey symmKey;
    private String[] VoterIDs;
    private String hashed_cipher_Ballot;

    public ElectionWorker(String ID1, String ID2) {
        KeyPairGenerator keygen;
        KeyPairGenerator smallKeygen;
        KeyGenerator symKeygen;

        voterList = new ArrayList<>();
        this.ID1 = ID1;
        this.ID2 = ID2;
        try {
            keygen = KeyPairGenerator.getInstance("RSA");

            System.out.println("Generating Key pairs for " + ID1);
            keygen.initialize(2048);

            firstPair = keygen.generateKeyPair();
            firstPrivateKey = firstPair.getPrivate();
            firstPublicKey = firstPair.getPublic();

            secondPair = keygen.generateKeyPair();
            secondPrivateKey = secondPair.getPrivate();
            secondPublicKey = secondPair.getPublic();

            ek_dkPair = keygen.generateKeyPair();
            encryptionKey = ek_dkPair.getPrivate();
            decryptionKey = ek_dkPair.getPublic();


            smallKeygen = KeyPairGenerator.getInstance("RSA");

            smallKeygen.initialize(2048);

            blindKeyPair = smallKeygen.generateKeyPair();
            blindingKey = blindKeyPair.getPrivate();
            deblindingKey = blindKeyPair.getPublic();

            symKeygen = KeyGenerator.getInstance("AES");
            symKeygen.init(256);
            symmKey = symKeygen.generateKey();

            System.out.println("Key pairs for " + ID1 + " generated");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

    public void addVoter(String voter_id) {
        voterList.add(voter_id);
    }

    public boolean hasVoter(String voter_id) {
        return voterList.contains(voter_id);
    }

    public void test() {

        ballot = "json.toString()23123123312as2dsfs2f23f233123123123123123";
        try {
            //first encrypt with ek
            String encrypted_Ballot = encrypt(ballot, Cipher.ENCRYPT_MODE, encryptionKey);

            //hash it
            String hashed_encrypted_Ballot = digest(encrypted_Ballot);
            //blind it
            String blinded_hashed_encrypted_Ballot = encrypt(hashed_encrypted_Ballot, Cipher.ENCRYPT_MODE, blindingKey);

            String re_encryptedBallot = encrypt(blinded_hashed_encrypted_Ballot, Cipher.DECRYPT_MODE, deblindingKey);



            //sign it test
            String signed_h_e_B = encrypt(re_encryptedBallot, Cipher.ENCRYPT_MODE, firstPrivateKey);

            //remove blind


            //remove sign
            String re_hashed_encrypted_Ballot = encrypt(signed_h_e_B, Cipher.DECRYPT_MODE, firstPublicKey);

            System.out.println("" + re_encryptedBallot + re_hashed_encrypted_Ballot);
//            cipherBallot = blind(digest(encrypt(ballot)));

        } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }


    public void encryptAndSetBallot(Map<String, Object> json) {

        ballot = json.toString();
        try {
            cipherBallot = encrypt(ballot, Cipher.ENCRYPT_MODE, encryptionKey);

            //hash it
            hashed_cipher_Ballot = digest(cipherBallot);

        } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException  e) {
            e.printStackTrace();
        }

    }

    //
    public String digest(String ballot) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(ballot.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(hash);
    }
//

    public String encrypt(String input, int decryptMode, Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        // cant use RSA/ECB/PKCS1Padding since it destroys the signature of the ballot
        Cipher cipher = Cipher.getInstance("RSA/ECB/NOPADDING");
        cipher.init(decryptMode, key);

       // return Base64.encodeBase64String(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));

        return crpt(input,cipher,decryptMode);
    }

    public String crpt(String input, Cipher cipher, int cryptType) throws BadPaddingException, IllegalBlockSizeException {
        byte[] inputArray;
        if (cryptType == Cipher.DECRYPT_MODE) {
            inputArray = Base64.decodeBase64(input);
        } else {
            inputArray = input.getBytes();
        }
        int inputLength = inputArray.length;
        int MAX_ENCRYPT_BLOCK = 256;
        // logo
        int offSet = 0;
        byte[] resultBytes = {};
        byte[] cache = {};
        while (inputLength - offSet > 0) {
            if (inputLength - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(inputArray, offSet, MAX_ENCRYPT_BLOCK);
                offSet += MAX_ENCRYPT_BLOCK;
            } else {
                cache = cipher.doFinal(inputArray, offSet, inputLength - offSet);
                offSet = inputLength;
            }
            resultBytes = Arrays.copyOf(resultBytes, resultBytes.length + cache.length);
            System.arraycopy(cache, 0, resultBytes, resultBytes.length - cache.length, cache.length);
        }
        if (cryptType == Cipher.ENCRYPT_MODE) {
            return Base64.encodeBase64String(resultBytes);
        }
        return new String(resultBytes);

    }


    public String getID1() {
        return ID1;
    }

    public String getID2() {
        return ID2;
    }

    public PublicKey getFirstPublicKey() {
        return firstPublicKey;
    }

    public PublicKey getSecondPublicKey() {
        return secondPublicKey;
    }

    public PublicKey getDecryptionKey() {
        return decryptionKey;
    }


//    private String sign(String blinded_hashed_encrypted_ballot) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
//
//        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//
//        cipher.init(Cipher.ENCRYPT_MODE, firstPrivateKey);
//
//        return; Base64.encodeBase64String(cipher.doFinal(blinded_hashed_encrypted_ballot.getBytes(StandardCharsets.UTF_8)))
//    }
//
//    public String clearSign(String blindedBallot) throws
//            UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
//        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//
//        cipher.init(Cipher.DECRYPT_MODE, firstPublicKey);
//
//        return new String(cipher.doFinal(Base64.decodeBase64(blindedBallot)));
//    }


//    public String encrypt(String ballot) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException,
//            UnsupportedEncodingException {
//        Cipher cipher = null;
//        try {
//            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
//            e.printStackTrace();
//        }
//
//        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
//
//        return Base64.encodeBase64String(cipher.doFinal(ballot.getBytes("UTF-8")));
//    }
//
//    public String decrypt(String encryptedBallot) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
//        Cipher cipher = null;
//
//        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
//
//        cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
//
//        return new String(cipher.doFinal(Base64.decodeBase64(encryptedBallot)));
//    }

    //    public String blind(String hashed_encrypted_Ballot) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
//
////        byte[] byted_Ballot = ByteBuffer.allocate(4).putInt(hashed_encrypted_Ballot).array();
////
//        Cipher cipher = Cipher.getInstance("AES");
//
//        cipher.init(Cipher.ENCRYPT_MODE, symmKey);
//
//        return crpt();
//
//    }
//
//    public String removeBlind(String blindedBallot) throws
//            UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
//        Cipher cipher = Cipher.getInstance("AES");
//
//        cipher.init(Cipher.DECRYPT_MODE, symmKey);
//
//        return new String(cipher.doFinal(Base64.decodeBase64(blindedBallot)));
//    }

}
