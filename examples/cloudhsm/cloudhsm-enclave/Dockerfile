FROM amazonlinux:2

ARG HSM_IP
ARG S3_IP

# install tools and dependencies 
# RUN yum install -y \
#         java-1.8.0-openjdk \
#         maven \
#         wget \
#         ncurses-compat-libs \
#         iputilsa \
#         gcc \
#         make \
#         tar \
#         net-tools

RUN yum install -y \
        java-17-amazon-corretto-headless \
        maven \
        wget \
        ncurses-compat-libs \
        iputilsa \
        gcc \
        make \
        tar \
        net-tools \
        unzip

RUN yum install -y \
        jq

# install socat
RUN wget http://www.dest-unreach.org/socat/download/socat-1.7.4.4.tar.gz \
    && tar -xzvf socat-1.7.4.4.tar.gz \
    && cd socat-1.7.4.4 \
    && ./configure \
    && make \
    && make install \
    && ln -s /usr/local/bin/socat /usr/bin/socat

RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
RUN unzip awscliv2.zip
RUN ./aws/install


WORKDIR /app

RUN mkdir -p /app/tmp
env _JAVA_OPTIONS=-Djava.io.tmpdir=/app/tmp
env HSM_IP=$HSM_IP
env S3_IP=$S3_IP

# install cloudhsm jce 
RUN wget https://s3.amazonaws.com/cloudhsmv2-software/CloudHsmClient/EL7/cloudhsm-jce-latest.el7.x86_64.rpm
RUN rpm -ivh cloudhsm-jce-latest.el7.x86_64.rpm
COPY customerCA.crt /opt/cloudhsm/etc/
RUN /opt/cloudhsm/bin/configure-jce -a $HSM_IP --disable-key-availability-check

# copy jars and scripts
COPY target/nitro-enclaves-cloudhsm-enclave-1.0.0-SNAPSHOT.jar ./app.jar
COPY libs/libvsockj-native-1.0-SNAPSHOT.so /usr/lib/
COPY libs/nsm-cli ./nsm-cli
COPY run_sample.sh ./

WORKDIR /
RUN curl "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "commandlinetools-linux-11076708_latest.zip"
RUN unzip commandlinetools-linux-11076708_latest.zip

RUN JAVA_HOME=$(dirname $(dirname $(readlink -f /usr/bin/java))) && \
    echo "export JAVA_HOME=$JAVA_HOME" >> /etc/profile

RUN mkdir -p ~/.aws

WORKDIR /app/tmp
COPY config ./
RUN cp /app/tmp/* ~/.aws

COPY installBuildTool.sh ./
RUN chmod +x /app/tmp/installBuildTool.sh
RUN /app/tmp/installBuildTool.sh



WORKDIR /cmdline-tools/build-tools/34.0.0
COPY apksigner2 ./
RUN chmod +x /cmdline-tools/build-tools/34.0.0/apksigner2

RUN echo 'PATH="/cmdline-tools/build-tools/34.0.0/:$PATH"' >> /etc/profile

RUN JAVA_HOME=$(dirname $(dirname $(readlink -f /usr/bin/java))) && \
echo 'security.provider.13=com.amazonaws.cloudhsm.jce.provider.CloudHsmProvider' >> $JAVA_HOME/conf/security/java.security


WORKDIR /app
COPY hsm-keystore.jks ./
COPY rotate.jks ./
COPY sign.sh ./
COPY unsigned.apk ./

# Run the script
CMD ["bash","/app/run_sample.sh"]