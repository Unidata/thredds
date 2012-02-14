PWD="changeit"
SDN="CN=localhost, OU=Unidata, O=UCAR, L=Boulder, ST=Colorado, C=US"
CDN="CN=Client, OU=Unidata, O=UCAR, L=Boulder, ST=Colorado, C=US"
JAVACERTS="c:/tools/jdk1.6/jre/lib/security/cacerts"

rm -f *.jks *.cer *.p12 *.pem tmp*

# Create a server keystore containing key and cert
keytool -genkey -keyalg RSA -alias server -keystore server.jks -storepass $PWD -validity 360 -dname "$SDN" -keypass $PWD

# Create a client keystore containing key and cert
keytool -genkey -keyalg RSA -alias client -keystore client.jks -storepass $PWD -validity 360 -dname "$CDN" -keypass $PWD
 
# Export the client cert
keytool -exportcert -alias client -keystore client.jks -storepass $PWD -file clientcert.cer

# Create a server truststore containing client cert
cp emptykeystore.jks servertrust.jks
keytool -importcert -trustcacerts -alias server \
        -keystore servertrust.jks -storepass $PWD \
        -file clientcert.cer

# Convert client .jks to pk12 format
keytool -importkeystore -srckeystore client.jks -destkeystore client.p12 -srcstoretype JKS -deststoretype PKCS12 -srcstorepass $PWD -deststorepass $PWD -srcalias client -destalias client -srckeypass $PWD -destkeypass $PWD -noprompt

## Convert the client cert to pem format
#openssl x509 -inform der -in clientcert.cer -out clientcert.pem

# Export the client key in pem format
openssl pkcs12 -in client.p12 -out clientkey.pem -nodes -nocerts -password pass:$PWD

# Install the tomcat key and trust stores
rm -f "c:/tools/tomcat6/conf/keystore.jks"
rm -f "c:/tools/tomcat6/conf/truststore.jks"
cp ./server.jks "c:/tools/tomcat6/conf/keystore.jks"
cp ./servertrust.jks "c:/tools/tomcat6/conf/truststore.jks"



exit



