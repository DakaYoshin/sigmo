package com.handlers.chathandlers;

import com.gameserver.handler.IChatHandler;
import com.gameserver.model.PartyMatchRoom;
import com.gameserver.model.PartyMatchRoomList;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.network.serverpackets.CreatureSay;

public class ChatPartyMatchRoom implements IChatHandler {
	private static final int[] COMMAND_IDS = {
			14
	};

	public void handleChat(int type, L2PcInstance activeChar, String target, String text) {
		if (activeChar.isInPartyMatchRoom()) {
			PartyMatchRoom _room = PartyMatchRoomList.getInstance().getPlayerRoom(activeChar);
			if (_room != null) {
				CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
				for (L2PcInstance _member : _room.getPartyMembers()) {
					_member.sendPacket(cs);
				}
			}
		}
	}

	public int[] getChatTypeList() {
		return COMMAND_IDS;
	}

}
