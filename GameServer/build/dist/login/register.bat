@echo off
color 0C
title Register: Console
@java -Djava.util.logging.config.file=config/other/console.cfg -cp ./lib/*;L2jSigmo.jar com.gameserver.register.GameServerRegister
@pause
