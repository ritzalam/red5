#!/bin/bash

set -x


#RED5_SRC=/home/firstuser/dev/red5/red5
RED5_SRC=$(pwd)
RED5_DIR=/usr/share/red5
NEW_RED5=/usr/share/red5-r4643

echo "Preparing build"
mvn clean

echo "mvn bootstrap install"
mvn -Dmaven.test.skip=true -Dclassifier=bootstrap install
echo "mvn dependencies"
mvn dependency:copy-dependencies
echo "mvn package"
mvn -Dmaven.test.skip=true -Dclassifier=bootstrap package

cd $RED5_SRC/target
echo "Extractring red5"
tar zxvf red5-server-1.0.2-SNAPSHOT-server.tar.gz
#mv red5-server-1.0.2-SNAPSHOT-bootstrap.jar red5-server-1.0-bootstrap.jar
#mv red5-server-1.0.2-SNAPSHOT.jar red5-server-1.0.jar
rm red5-server-1.0.2-SNAPSHOT/lib/red5-client-*.jar

echo "Deploying red5"
cd $RED5_SRC

sudo rm -rf $RED5_DIR
sudo rm -rf $NEW_RED5
sudo cp -R target/red5-server-1.0.2-SNAPSHOT $NEW_RED5
sudo ln -s $NEW_RED5 $RED5_DIR
sudo cp extras/commons-fileupload-1.2.2.jar $RED5_DIR/lib/
sudo chown -R red5.adm $RED5_DIR
sudo chown -R red5.adm $NEW_RED5
sudo chmod -R 777 $NEW_RED5/webapps/
sudo cp extras/red5-yourkit.sh $RED5_DIR/red5.sh
sudo chmod 755 $RED5_DIR/red5.sh

echo "Red5 ready"

DESKSHARE=/home/firstuser/dev/source/bigbluebutton/deskshare
VOICE=/home/firstuser/dev/source/bigbluebutton/bbb-voice
VIDEO=/home/firstuser/dev/source/bigbluebutton/bbb-video
APPS=/home/firstuser/dev/source/bigbluebutton/bigbluebutton-apps
RED5_JAR=red5-1.0r4641.jar
NEW_RED5_JAR=target/red5-server-1.0.2-SNAPSHOT/red5-server.jar

echo "Copying red5 jar"
cp $NEW_RED5_JAR $DESKSHARE/lib/$RED5_JAR
cp $NEW_RED5_JAR $VOICE/lib/$RED5_JAR
cp $NEW_RED5_JAR $VIDEO/lib/$RED5_JAR
cp $NEW_RED5_JAR $APPS/lib/$RED5_JAR

echo "Building apps"
cd $APPS
gradle clean war deploy

echo "Building voice"
cd $VOICE
gradle clean war deploy

echo "Building video"
cd $VIDEO
gradle clean war deploy

echo "Building deskshare"
cd $DESKSHARE/app
gradle clean war deploy

cd $RED5_SRC

sudo chown -R red5.adm $RED5_DIR
sudo chown -R red5.adm $NEW_RED5
sudo chmod -R 777 $NEW_RED5/webapps/

