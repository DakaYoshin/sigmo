/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.handlers.chathandlers;

import com.Config;
import com.gameserver.handler.IChatHandler;
import com.gameserver.model.BlockList;
import com.gameserver.model.L2World;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.network.serverpackets.CreatureSay;
import com.gameserver.util.FloodProtector;

public class ChatHeroVoice implements IChatHandler
{
	private static final int[] COMMAND_IDS = 
	{ 
		17
	};

	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		if (activeChar.isHero() || activeChar.isGM())
		{
			if (!FloodProtector.getInstance().tryPerformAction(activeChar.getObjectId(), FloodProtector.PROTECTED_HEROVOICE))
			{
				activeChar.sendMessage("Heroes can speak in the global channel once every " + Config.PROTECTED_HEROVOICE_C / 10 + " seconds.");
				return;
			}

			CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);

			for (L2PcInstance player : L2World.getInstance().getAllPlayers())
			{
				if (!BlockList.isBlocked(player, activeChar))
				{
					player.sendPacket(cs);
				}
			}
		}
	}

	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}
