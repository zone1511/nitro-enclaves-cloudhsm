package com.github.qyzhangaws.cloudhsm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.github.mrgatto.autoconfigure.EnableNitroEnclavesEnclaveSide;
import com.github.mrgatto.enclave.server.NitroEnclaveServer;

import com.amazonaws.cloudhsm.jce.provider.CloudHsmProvider;
import com.amazonaws.cloudhsm.jce.provider.KeyStoreWithAttributes;
import com.amazonaws.cloudhsm.jce.jni.exception.AddAttributeException;

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

import com.github.qyzhangaws.cloudhsm.handler.CloudHSMHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.security.Security;

@SpringBootApplication
@ComponentScan({ "com.github.qyzhangaws.cloudhsm" })
@EnableNitroEnclavesEnclaveSide
public class NitroEnclaveApplication {

	public static void main(String[] args) throws Exception {
		ApplicationContext ctx = SpringApplication.run(NitroEnclaveApplication.class, args);
		NitroEnclaveServer server = ctx.getBean(NitroEnclaveServer.class);

		// run server
		server.run();
	}
}
