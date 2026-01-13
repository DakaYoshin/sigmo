#!/bin/sh
java -Djava.util.logging.config.file=config/other/console.cfg -cp ./lib/*:L2JL2jSigmo.jar:mysql-connector-java-5.1.18-bin.jar com.accmanager.SQLAccountManager
