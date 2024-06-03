package com.github.qyzhangaws.cloudhsm.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import com.amazonaws.cloudhsm.jce.provider.CloudHsmProvider;
import com.amazonaws.cloudhsm.jce.provider.KeyStoreWithAttributes;
import com.amazonaws.cloudhsm.jce.provider.attributes.KeyAttribute;
import com.amazonaws.cloudhsm.jce.provider.attributes.KeyAttributesMap;
import com.amazonaws.cloudhsm.jce.provider.attributes.KeyType;

import com.github.mrgatto.enclave.handler.AbstractActionHandler;
import com.github.mrgatto.enclave.nsm.NsmClient;
import com.github.qyzhangaws.cloudhsm.Actions;
import com.github.qyzhangaws.cloudhsm.model.MyPojoData;
import com.github.qyzhangaws.cloudhsm.model.MyPojoDataResult;

import org.json.JSONObject;
import org.json.JSONException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

@Component
public class CloudHSMHandler extends AbstractActionHandler<MyPojoData, MyPojoDataResult> {

    // CloudHSM Secret Name in Secret Manager
    static String CLOUDHSM_SECRET_NAME = "cloudhsm/cluster-fx2oxs4zpnj";
    static Region REGION = Region.of("ap-northeast-1");

    @Autowired
    private NsmClient nsmClient;

    @Override
    public boolean canHandle(String action) {
        return Actions.CLOUDHSM.name().equalsIgnoreCase(action);
    }

    @Override
    public MyPojoDataResult handle(MyPojoData data) {
        String nsmModuleId = this.nsmClient.describeNsm().getModuleId();

        MyPojoDataResult result = new MyPojoDataResult();
        JsonNode jsonNode = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readValue(data.getValue(), JsonNode.class);
        } catch (Exception e) {
            e.printStackTrace();
            result.setValue(e.toString());
            return result;
        }

        AwsCredentialsProvider awsCredentialsProvider1;
        awsCredentialsProvider1 = StaticCredentialsProvider.create(AwsSessionCredentials.create(
                jsonNode.get("AK").asText(),
                jsonNode.get("SK").asText(),
                jsonNode.get("ST").asText()
        ));

        // set CloudHSM environment variables
        if (!isCloudHSMEnvVariablesSet()) {
            setCloudHSMEnvVariables(awsCredentialsProvider1);
        }

        // add provider
        try {
            if (Security.getProvider(CloudHsmProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new CloudHsmProvider());
            }
        } catch (Exception ex) {
            System.out.println(ex);
            result.setValue(ex.toString());
            return result;
        }

        String bucketName = "kl-enclave-upload";
        String tempFileContent = "This is a temporary file.";

        try {
            S3Client s3Client = S3Client.builder()
                    .region(REGION)
                    .credentialsProvider(awsCredentialsProvider1)
                    .endpointOverride(URI.create("https://s3.ap-northeast-1.amazonaws.com:8443"))
                    .build();

            String keyName = "temp-file.txt";
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(tempFileContent.getBytes(StandardCharsets.UTF_8)));
            System.out.println("File uploaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String keyMode, keyType, keyLabel;
        keyMode = keyType = keyLabel = null;

        if (jsonNode.has("KEY_MODE")) {
            keyMode = jsonNode.get("KEY_MODE").asText().toUpperCase();
        }

        if (jsonNode.has("KEY_TYPE")) {
            keyType = jsonNode.get("KEY_TYPE").asText().toUpperCase();
        }

        if (jsonNode.has("KEY_LABEL")) {
            keyLabel = jsonNode.get("KEY_LABEL").asText();
        }

        switch (keyMode) {
            case "CREATE_KEY": {
                result.setValue(CreateKey(keyType, keyLabel));
                break;
            }
            case "GET_KEY": {
                result.setValue(GetKey(keyType, keyLabel));
                break;
            }
            case "DELETE_KEY": {
                result.setValue(DeleteKey(keyType, keyLabel));
                break;
            }
            default:
                result.setValue("Error: Invalid KeyMode specified, only support CREATE_KEY, GET_KEY, DELETE_KEY");
                break;
        }

        return result;
    }

    // check if cloudhsm env variables set
    private boolean isCloudHSMEnvVariablesSet() {
        String cloudHSMUser = System.getProperty("HSM_USER");
        String cloudHSMPassword = System.getProperty("HSM_PASSWORD");

        if (cloudHSMUser == null || cloudHSMPassword == null) {
            return false;
        }
        return true;
    }

    // set environment variables for cloudhsm crendentials from Secret Manager
    private void setCloudHSMEnvVariables(AwsCredentialsProvider awsCredentialsProvider) {
        // Create a Secrets Manager client
        SecretsManagerClient client = SecretsManagerClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(REGION)
                .build();

        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(CLOUDHSM_SECRET_NAME)
                .build();

        GetSecretValueResponse getSecretValueResponse;
        try {
            getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
        } catch (Exception e) {
            // For a list of exceptions thrown, see
            // https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
            throw e;
        }

        String secret = getSecretValueResponse.secretString();
        try {
            JSONObject jsonObject = new JSONObject(secret);
            System.setProperty("HSM_USER", jsonObject.get("HSM_USER").toString());
            System.setProperty("HSM_PASSWORD", jsonObject.get("HSM_PASSWORD").toString());
        } catch (JSONException err) {
            System.out.println(String.format("{\"Error\":\"%s\"}", err.toString()));
            throw err;
        }
    }

    // Create Key
    private String CreateKey(String keyType, String keyLabel) {
        // Generate a 256-bit AES symmetric key.
        // This key is not yet in the HSM. It will have to be imported using a KeyAttributesMap.
        if (!keyType.equals("AES")) {
            return "Info: only KeyType:AES implemented!";
        }

        final KeyAttributesMap aesSpec = new KeyAttributesMap();
        try {
            aesSpec.put(KeyAttribute.LABEL, keyLabel);
            aesSpec.put(KeyAttribute.SIZE, 256);
            aesSpec.put(KeyAttribute.TOKEN, true);
        } catch (com.amazonaws.cloudhsm.jce.jni.exception.AddAttributeException err) {
            System.out.println(String.format("{\"Error\":\"%s\"}", err.toString()));
            return "Error: Failed to create key!";
        }

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(keyType, CloudHsmProvider.PROVIDER_NAME);
            keyGen.init(aesSpec);
            SecretKey aesKey = keyGen.generateKey();
        } catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidAlgorithmParameterException err) {
            System.out.println(String.format("{\"Error\":\"%s\"}", err.toString()));
            return "Error: Failed to create key!";
        }

        System.out.println("Info: Key created!");
        return "Info: Key created!";
    }

    // Get Key: Not implemented
    private String GetKey(String keyType, String keyLabel) {
        return "Info: not implemented!";
    }

    // Delete Key: Not implemented
    private String DeleteKey(String keyType, String keyLabel) {
        return "Info: not implemented!";
    }
}
