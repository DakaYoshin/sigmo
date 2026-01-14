package com.handlers.admincommandhandlers;

import com.Config;
import com.gameserver.handler.IAdminCommandHandler;
import com.gameserver.model.L2Object;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.network.SystemMessageId;
import com.gameserver.network.serverpackets.EtcStatusUpdate;
import com.gameserver.network.serverpackets.SystemMessage;

public class AdminClanFull implements IAdminCommandHandler {
	private static final String ADMIN_COMMANDS[] = { "admin_clanfull" };

	public AdminClanFull() {
	}

	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar) {
		if (command.startsWith("admin_clanfull")) {
			try {
				adminAddClanSkill(activeChar);
				activeChar.sendMessage("Todas as skills de clan foram adicionadas.");
			} catch (Exception e) {
				activeChar.sendMessage("Usage: //clanfull");
			}
		}
		return true;
	}

	private static void adminAddClanSkill(L2PcInstance activeChar) {
		L2Object target = activeChar.getTarget();
		if (target == null) {
			target = activeChar;
		}
		L2PcInstance player = null;
		if (target instanceof L2PcInstance) {
			player = (L2PcInstance) target;
		} else {
			activeChar.sendPacket(new SystemMessage(SystemMessageId.INCORRECT_TARGET));
			return;
		}
		if (!player.isClanLeader()) {
			player.sendPacket((new SystemMessage(SystemMessageId.S1_IS_NOT_A_CLAN_LEADER)).addString(player.getName()));
			return;
		}
		player.getClan().changeLevel(Config.CLAN_LEVEL);
		player.ClanSkills();
		player.sendPacket(new EtcStatusUpdate(activeChar));
	}

	@Override
	public String[] getAdminCommandList() {
		return ADMIN_COMMANDS;
	}
}
