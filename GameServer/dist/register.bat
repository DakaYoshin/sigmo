@echo off
color 0C
title Register: Console
@java -Djava.util.logging.config.file=config/console.cfg -cp ./lib/*; com.gameserver.register.GameServerRegister
@pause
