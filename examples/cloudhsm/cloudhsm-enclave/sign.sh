source /etc/profile
cd ~/.aws/

secret_value=$(aws secretsmanager get-secret-value --secret-id cloudhsm/cluster-fx2oxs4zpnj --query SecretString --output text)

# Check if the command was successful
if [ $? -eq 0 ]; then
    # Parse the secret value JSON
    HSM_USER=$(echo "$secret_value" | jq -r '.HSM_USER')
    HSM_PASSWORD=$(echo "$secret_value" | jq -r '.HSM_PASSWORD')

    # Set the environment variables
    export HSM_USER
    export HSM_PASSWORD

    echo "Environment variables set successfully:"
else
    echo "Failed to retrieve secret value from AWS Secrets Manager"
    exit 1
fi


cd /app

apksigner2 sign --in unsigned.apk --lineage rotate.jks  --ks hsm-keystore.jks --ks-key-alias oldkey \
--ks-type CLOUDHSM --ks-pass pass:123456 --next-signer --ks hsm-keystore.jks --ks-key-alias newkey \
--ks-type CLOUDHSM --ks-pass pass:123456 --out signed.apk

return_code=$?

if [ $return_code -eq 0 ]; then
    echo "Command executed successfully."
else
    echo "Command failed with return code $return_code."
fi


apksigner verify --print-certs signed.apk > report.txt

aws s3 cp report.txt s3://kl-enclave-upload > /dev/null

aws s3 cp bitget-resign.apk s3://kl-enclave-upload > /dev/null

echo "apk uploaded"
