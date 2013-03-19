echo "Preparing build"
mvn clean

echo "mvn bootstrap install"
mvn -Dmaven.test.skip=true -Dclassifier=bootstrap install
echo "mvn dependencies"
mvn dependency:copy-dependencies
echo "mvn package"
mvn -Dmaven.test.skip=true -Dclassifier=bootstrap package

cd target
echo "Extractring red5"
tar zxvf red5-server-1.0.2-SNAPSHOT-server.tar.gz
#mv red5-server-1.0.2-SNAPSHOT-bootstrap.jar red5-server-1.0-bootstrap.jar
#mv red5-server-1.0.2-SNAPSHOT.jar red5-server-1.0.jar
rm red5-server-1.0.2-SNAPSHOT/lib/red5-client-*.jar

echo "Deploying red5"
cd ..
sudo rm -rf /usr/share/red5
sudo rm -rf /usr/share/red5-r4599
sudo cp -R target/red5-server-1.0.2-SNAPSHOT /usr/share/red5-r4599
sudo ln -s /usr/share/red5-r4599 /usr/share/red5
sudo cp extras/commons-fileupload-1.2.2.jar /usr/share/red5/lib/
sudo cp extras/red5.sh /usr/share/red5
sudo chmod 755 /usr/share/red5/red5.sh
sudo chown -R red5.adm /usr/share/red5
sudo chown -R red5.adm /usr/share/red5-r4599
sudo chmod -R 777 /usr/share/red5-r4599/webapps/

echo "Red5 ready"

