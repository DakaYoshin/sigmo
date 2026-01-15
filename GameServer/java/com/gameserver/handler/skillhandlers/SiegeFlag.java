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
package com.gameserver.handler.skillhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gameserver.datatables.xml.NpcTable;
import com.gameserver.handler.ISkillHandler;
import com.gameserver.idfactory.IdFactory;
import com.gameserver.managers.CastleManager;
import com.gameserver.managers.SiegeManager;
import com.gameserver.model.L2Object;
import com.gameserver.model.L2Skill;
import com.gameserver.model.actor.L2Character;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.gameserver.model.entity.siege.Castle;
import com.gameserver.network.SystemMessageId;
import com.gameserver.network.serverpackets.SystemMessage;
import com.gameserver.templates.skills.L2SkillType;

public class SiegeFlag implements ISkillHandler
{
	private static final Logger _log = LoggerFactory.getLogger(SiegeFlag.class);

	private static final L2SkillType[] SKILL_IDS = { L2SkillType.SIEGEFLAG };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance player = (L2PcInstance) activeChar;
		if(player.getClan() == null || player.getClan().getLeaderId() != player.getObjectId())
		{
			return;
		}

		Castle castle = CastleManager.getInstance().getCastle(player);
		if((castle == null))
		{
			return;
		}

		if(castle != null)
		{
			if(!checkIfOkToPlaceFlag(player, castle, true))
			{
				return;
			}
		}

		try
		{
			L2SiegeFlagInstance flag = new L2SiegeFlagInstance(player, IdFactory.getInstance().getNextId(), NpcTable.getInstance().getTemplate(35062));
			flag.setTitle(player.getClan().getName());
			flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
			flag.setHeading(player.getHeading());
			flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);

			if(castle != null)
			{
				castle.getSiege().getFlag(player.getClan()).add(flag);
			}
		}
		catch(Exception e)
		{
			player.sendMessage("Error placing flag:" + e);
			_log.error("", e);
		}
	}

	public static boolean checkIfOkToPlaceFlag(L2Character activeChar, boolean isCheckOnly)
	{
		return checkIfOkToPlaceFlag(activeChar, CastleManager.getInstance().getCastle(activeChar), isCheckOnly);
	}

	public static boolean checkIfOkToPlaceFlag(L2Character activeChar, Castle castle, boolean isCheckOnly)
	{
		if(activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return false;
		}

		SystemMessage sm = new SystemMessage(SystemMessageId.S1_S2);
		L2PcInstance player = (L2PcInstance) activeChar;

		if(castle == null || castle.getCastleId() <= 0)
		{
			sm.addString("You must be on castle ground to place a flag.");
		}
		else if(!castle.getSiege().getIsInProgress())
		{
			sm.addString("You can only place a flag during a siege.");
		}
		else if(castle.getSiege().getAttackerClan(player.getClan()) == null)
		{
			sm.addString("You must be an attacker to place a flag.");
		}
		else if(player.getClan() == null || !player.isClanLeader())
		{
			sm.addString("You must be a clan leader to place a flag.");
		}
		else if(castle.getSiege().getAttackerClan(player.getClan()).getNumFlags() >= SiegeManager.getInstance().getFlagMaxCount())
		{
			sm.addString("You have already placed the maximum number of flags possible.");
		}
		else if(player.isInsideZone(L2Character.ZONE_NOHQ))
		{
			sm.addString("You cannot place flag here.");
		}
		else
		{
			return true;
		}

		if(!isCheckOnly)
		{
			player.sendPacket(sm);
		}

		return false;
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}
