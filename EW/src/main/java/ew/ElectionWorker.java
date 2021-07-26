package ew;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import java.io.Serializable;
import java.security.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ElectionWorker implements Serializable {

    private String ID1;
    private String ID2;

    private Map<String, Boolean> voterList;

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

    public ElectionWorker(String ID1, String ID2) {
        KeyPairGenerator keygen;
        KeyPairGenerator smallKeygen;
        KeyGenerator symKeygen;

        voterList = new HashMap<>();
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

            System.out.println("Key pairs for " + ID1 + " generated");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

    public void addVoter(String voter_id) {
        voterList.put(voter_id, Boolean.TRUE);
    }

    public boolean hasVoter(String voter_id) {
        return voterList.containsKey(voter_id);
    }

    public boolean hasVote(String voter_id) {
        return voterList.get(voter_id);
    }

    public void setVoterHasVoted(String voter_id, boolean b) {
        voterList.put(voter_id, Boolean.FALSE);
    }

    //normally encryptingBallots
    public String encryptBallot(String ballot) {
        try {
            /// ballot = new ObjectMapper().writeValueAsString(json);
            return encrypt(ballot, Cipher.ENCRYPT_MODE, encryptionKey);

        } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String sign(String ballot, int key) {
        try {
            return encrypt(ballot, Cipher.ENCRYPT_MODE, key == 1 ? firstPrivateKey : secondPrivateKey);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String encrypt(String input, int decryptMode, Key key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        // cant use RSA/ECB/PKCS1Padding since it destroys the signature of the ballot
        Cipher cipher = Cipher.getInstance("RSA/ECB/NOPADDING");
        cipher.init(decryptMode, key);

        // return Base64.encodeBase64String(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));

        return crpt(input, cipher, decryptMode);
    }

    private String crpt(String input, Cipher cipher, int cryptType) throws BadPaddingException, IllegalBlockSizeException {
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


}
