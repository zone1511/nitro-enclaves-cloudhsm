# nitro-enalcave-cloudhsm example

## Adjust code according to your environment

Change secrets manager secret name in file(cloudhsm-enclave/src/main/java/com/github/qyzhangaws/cloudhsm/handler/CloudHSMHandler.java)

```
static String CLOUDHSM_SECRET_NAME = "cloudhsm/cluster-teeultuftgy";
static Region REGION = Region.of("ap-southeast-1");
```

Replace the placeholder file examples/cloudhsm/cloudhsm-enclave/customerCA.crt with your CloudHSM cluster customerCA.crt

## Build Jars

Ensure framework library jars is in your local Maven repo (from base project dir):

```bash
mvn install
```

Package this sample application(from examples/cloudhsm):

```bash
mvn validate
mvn package
```

## Build Enclave image 

```bash
# replace the HSM_IP to yours
docker build --build-arg HSM_IP="10.0.10.103" -t cloudhsm-enclave ./cloudhsm-enclave
nitro-cli build-enclave --docker-uri cloudhsm-enclave:latest --output-file cloudhsm-enclave.eif
```

## Run Enclave image

```bash
nitro-cli terminate-enclave --all
nitro-cli run-enclave --eif-path cloudhsm-enclave.eif --memory 6144 --cpu-count 4 --enclave-cid 5 --debug-mode
```

### Debug-mode Console

```bash
nitro-cli console --enclave-id $(nitro-cli describe-enclaves | jq -r '.[0].EnclaveID')
```

## Run Host App

Ensure that you have  the lib [libvsockj-native-1.0-SNAPSHOT.so] somewhere on your Java Classpath. Usually you can just copy it to */usr/lib64*.

```bash
# under folder example/cloudhsm
copy libs/libvsockj-native-1.0-SNAPSHOT.so /usr/lib/
CID=5 java -jar cloudhsm-host/target/nitro-enclaves-cloudhsm-host-1.0.0-SNAPSHOT.jar
```

You should see the following output which denote the key created successfully

```bash
Info: Key created!
```

Change the KEY_LABEL value to create more keys for testing
```java
objectNode.put("KEY_LABEL", "nitro-enclave-1");
```