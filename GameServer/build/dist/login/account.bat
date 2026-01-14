@echo off
color 0C
title L2jSigmo account manager console
@java -Djava.util.logging.config.file=config/other/console.cfg -cp ./lib/*;L2jSigmo.jar com.accmanager.SQLAccountManager
@pause