#!/bin/bash
mvn package
# entries=(
#   "- {address: secretsmanager.ap-northeast-1.amazonaws.com, port: 443}"
#   "- {address: 10.0.1.90, port: 2223}"
#   "- {address: s3.ap-northeast-1.amazonaws.com, port: 443}"
# )

# Check if the entries exist in the file
# for entry in "${entries[@]}"; do
#   if ! grep -q "$entry" /etc/nitro_enclaves/vsock-proxy.yaml; then
#     # Entry not found, append it to the file
#     echo "Adding entry: $entry"
#     sudo bash -c "echo '$entry' >> /etc/nitro_enclaves/vsock-proxy.yaml"
#   else
#     echo "Entry already exists: $entry"
#   fi
# done
sudo pkill -f 'vsock-proxy 443 secretsmanager.ap-northeast-1.amazonaws.com 443'
sudo pkill -f 'vsock-proxy 8443 s3.ap-northeast-1.amazonaws.com 443'
sudo pkill -f 'vsock-proxy 2223 10.0.1.90 2223'
sudo vsock-proxy 443 secretsmanager.ap-northeast-1.amazonaws.com 443 &
sudo vsock-proxy 8443 s3.ap-northeast-1.amazonaws.com 443 &
sudo vsock-proxy 2223 10.0.1.90 2223 &

nitro-cli terminate-enclave --enclave-id $(nitro-cli describe-enclaves | jq -r '.[0].EnclaveID')

docker build --build-arg HSM_IP="10.0.10.103" -t cloudhsm-enclave ./cloudhsm-enclave
nitro-cli build-enclave --docker-uri cloudhsm-enclave:latest --output-file cloudhsm-enclave.eif
nitro-cli run-enclave --eif-path cloudhsm-enclave.eif --memory 6144 --cpu-count 2 --enclave-cid 5 --debug-mode
#view the console output of enclave
nitro-cli console --enclave-id $(nitro-cli describe-enclaves | jq -r '.[0].EnclaveID')
