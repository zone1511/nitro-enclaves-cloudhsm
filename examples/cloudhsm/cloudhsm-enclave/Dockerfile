FROM amazonlinux:2

ARG HSM_IP

# install tools and dependencies 
RUN yum install -y \
        java-1.8.0-openjdk \
        maven \
        wget \
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

RUN mkdir -p /app/tmp
env _JAVA_OPTIONS=-Djava.io.tmpdir=/app/tmp
env HSM_IP=$HSM_IP

# install cloudhsm jce 
RUN wget https://s3.amazonaws.com/cloudhsmv2-software/CloudHsmClient/EL7/cloudhsm-jce-latest.el7.x86_64.rpm
RUN rpm -ivh cloudhsm-jce-latest.el7.x86_64.rpm
COPY customerCA.crt /opt/cloudhsm/etc/
RUN /opt/cloudhsm/bin/configure-jce -a $HSM_IP

# copy jars and scripts
COPY target/nitro-enclaves-cloudhsm-enclave-1.0.0-SNAPSHOT.jar ./app.jar
COPY libs/libvsockj-native-1.0-SNAPSHOT.so /usr/lib/
COPY libs/nsm-cli ./nsm-cli
COPY run_sample.sh ./

# Run the script
CMD ["bash","/app/run_sample.sh"]