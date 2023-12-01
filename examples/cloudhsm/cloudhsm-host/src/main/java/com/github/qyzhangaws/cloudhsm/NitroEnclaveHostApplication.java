package com.github.qyzhangaws.cloudhsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.github.mrgatto.autoconfigure.EnableNitroEnclavesHostSide;
import com.github.mrgatto.host.NitroEnclaveClient;
import com.github.mrgatto.model.EnclaveRequest;
import com.github.mrgatto.model.EnclaveResponse;

import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.github.qyzhangaws.cloudhsm.Actions;
import com.github.qyzhangaws.cloudhsm.model.MyPojoData;
import com.github.qyzhangaws.cloudhsm.model.MyPojoDataResult;

@SpringBootApplication
@ComponentScan({ "com.github.qyzhangaws.cloudhsm" })
@EnableNitroEnclavesHostSide
public class NitroEnclaveHostApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(NitroEnclaveHostApplication.class, args);

		NitroEnclaveClient client = ctx.getBean(NitroEnclaveClient.class);

		// MyPojoData pojo = new MyPojoData();
		// pojo.setValue("Hello World!");

		MyPojoData pojoData = reqPojoData();

		EnclaveRequest<MyPojoData> request = new EnclaveRequest<>();
		request.setAction(Actions.CLOUDHSM.name());
		request.setData(pojoData);

		EnclaveResponse<MyPojoDataResult> response = client.send(request);

		if (response.getIsError()) {
			System.out.println(String.format("Something went wrong: %s", response.getError()));
			System.out.println(response.getErrorStacktrace());
		} else {
			System.out.println(response.getData().getValue());
		}

		System.out.println(String.format("Enclave execution time %sms", response.getDuration()));
	}

	public static MyPojoData reqPojoData() {
		// retrieve imds
		Ec2MetadataClient client = Ec2MetadataClient.create();
		Ec2MetadataResponse response1 = client.get("/latest/meta-data/iam/security-credentials/");
		String Ec2Role = response1.asString();
	
		Ec2MetadataResponse response2 = client.get("/latest/meta-data/iam/security-credentials/" + Ec2Role);
		String ec2TempCredsString = response2.asString();
		client.close();

		try {
			JsonNode ec2TempCreds = null;
			ec2TempCreds = new ObjectMapper().readValue(ec2TempCredsString, JsonNode.class);

			MyPojoData reqData = new MyPojoData();
			ObjectNode objectNode = new ObjectMapper().createObjectNode();
			objectNode.put("AK", ec2TempCreds.get("AccessKeyId").asText());
			objectNode.put("SK", ec2TempCreds.get("SecretAccessKey").asText());
			objectNode.put("ST", ec2TempCreds.get("Token").asText());
			objectNode.put("KEY_MODE", "CREATE_KEY");
			objectNode.put("KEY_TYPE", "AES");
			objectNode.put("KEY_LABEL", "nitro-enclave-1");

			reqData.setValue(objectNode.toString());
			return reqData;
		}catch (Exception ex) {
			System.out.println(ex);
			return null;
		}
	}
}