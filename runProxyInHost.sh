#!/bin/bash

entries=(
  "- {address: secretsmanager.ap-northeast-1.amazonaws.com, port: 443}"
  "- {address: 10.0.1.90, port: 2223}"
)

# Check if the entries exist in the file
for entry in "${entries[@]}"; do
  if ! grep -q "$entry" /etc/nitro_enclaves/vsock-proxy.yaml; then
    # Entry not found, append it to the file
    echo "Adding entry: $entry"
    sudo bash -c "echo '$entry' >> /etc/nitro_enclaves/vsock-proxy.yaml"
  else
    echo "Entry already exists: $entry"
  fi
done

sudo vsock-proxy 443 secretsmanager.ap-northeast-1.amazonaws.com 443 &
sudo vsock-proxy 2223 10.0.1.90 2223 &