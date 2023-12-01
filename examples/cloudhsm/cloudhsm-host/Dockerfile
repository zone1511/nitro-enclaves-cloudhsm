FROM amazoncorretto:8u302

WORKDIR /app

COPY target/nitro-enclaves-simple-echo-enclave-1.0.0-SNAPSHOT.jar ./app.jar

COPY libs/libvsockj-native-1.0-SNAPSHOT.so /usr/lib/
COPY libs/nsm-cli ./nsm-cli

CMD ["java", "-jar", "/app/app.jar"]

FROM amazonlinux:2

# Install python for running the server and net-tools for modifying network configWORKDIR /app
ARG HSM_IP
env HSM_IP=$HSM_IP

RUN yum install -y \
        java-1.8.0-openjdk \
        maven \
        wget \
        unzip \
        ncurses-compat-libs \
        iputilsa \
        gcc \
        make \
        tar \
        net-tools

# install socat
RUN wget http://www.dest-unreach.org/socat/download/socat-1.7.4.4.tar.gz \
    && tar -xzvf socat-1.7.4.4.tar.gz \
    && cd socat-1.7.4.4 \
    && ./configure \
    && make \
    && make install \
    && ln -s /usr/local/bin/socat /usr/bin/socat
  
WORKDIR /app

# install cloudhsm jce 
RUN wget https://s3.amazonaws.com/cloudhsmv2-software/CloudHsmClient/EL7/cloudhsm-jce-latest.el7.x86_64.rpm
RUN rpm -ivh cloudhsm-jce-latest.el7.x86_64.rpm

#env JDK_JAVA_OPTIONS=--add-opens=java.base/java.security=ALL-UNNAMED

# configre cloudhsm
COPY customerCA.crt /opt/cloudhsm/etc/
RUN /opt/cloudhsm/bin/configure-jce -a $HSM_IP
# RUN /opt/cloudhsm/bin/configure-jce --enable-clear-key-extraction-in-software

# RUN export CLOUDHSM_CLIENT_VERSION=`rpm -qi cloudhsm-jce | awk -F': ' '/Version/ {print $2}'` \
#         && mvn validate -DskipTests -DcloudhsmVersion=$CLOUDHSM_CLIENT_VERSION \
#         && mvn clean package -DskipTests -DcloudhsmVersion=$CLOUDHSM_CLIENT_VERSION

# copy run_sample.sh
COPY target/nitro-enclaves-cloudhsm-enclave-1.0.0-SNAPSHOT.jar ./app.jar

COPY libs/libvsockj-native-1.0-SNAPSHOT.so /usr/lib/
COPY libs/nsm-cli ./nsm-cli
COPY run_sample.sh ./

# Run the script
CMD ["bash","/app/run_sample.sh"]