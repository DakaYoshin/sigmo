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

import com.Config;
import com.gameserver.geo.GeoData;
import com.gameserver.handler.ISkillHandler;
import com.gameserver.managers.FishingZoneManager;
import com.gameserver.model.L2Object;
import com.gameserver.model.L2Skill;
import com.gameserver.model.actor.L2Character;
import com.gameserver.model.actor.instance.L2ItemInstance;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.model.itemcontainer.Inventory;
import com.gameserver.model.zone.type.L2FishingZone;
import com.gameserver.model.zone.type.L2WaterZone;
import com.gameserver.network.SystemMessageId;
import com.gameserver.network.serverpackets.InventoryUpdate;
import com.gameserver.network.serverpackets.SystemMessage;
import com.gameserver.templates.item.L2Weapon;
import com.gameserver.templates.item.L2WeaponType;
import com.gameserver.templates.skills.L2SkillType;
import com.gameserver.util.Util;
import com.util.random.Rnd;

public class Fishing implements ISkillHandler
{
	private static final L2SkillType[] SKILL_IDS = { L2SkillType.FISHING };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets)
	{
		if(activeChar == null || !(activeChar instanceof L2PcInstance))
		{
			return;
		}

		L2PcInstance player = (L2PcInstance) activeChar;

		if(!Config.ALLOW_FISHING && !player.isGM())
		{
			player.sendMessage("Fishing is disabled on this server.");
			return;
		}

		if(player.isFishing())
		{
			if(player.GetFishCombat() != null)
			{
				player.GetFishCombat().doDie(false);
			}
			else
			{
				player.EndFishing(false);
			}

			player.sendPacket(new SystemMessage(SystemMessageId.FISHING_ATTEMPT_CANCELLED));
			return;
		}

		L2Weapon weaponItem = player.getActiveWeaponItem();
		if((weaponItem == null || weaponItem.getItemType() != L2WeaponType.ROD))
		{
			return;
		}

		L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if(lure == null)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_FISH_HERE));
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addString(skill.getName());
			player.sendPacket(sm);
			return;
		}

		player.SetLure(lure);
		L2ItemInstance lure2 = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);

		if(lure2 == null || lure2.getCount() < 1)
		{
			player.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_BAIT));
			return;
		}

		if(player.isInBoat())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_FISH_ON_BOAT));
				return;
		}

		if(player.isInCraftMode() || player.isInStoreMode())
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_FISH_WHILE_USING_RECIPE_BOOK));
			if(!player.isGM())
			{
				return;
			}
		}

		int rnd = Rnd.get(200) + 200;
		double angle = Util.convertHeadingToDegree(player.getHeading());
		double radian = Math.toRadians(angle);
		double sin = Math.sin(radian);
		double cos = Math.cos(radian);
		int x1 = (int) (cos * rnd);
		int y1 = (int) (sin * rnd);
		int x = player.getX() + x1;
		int y = player.getY() + y1;
		int z = player.getZ() - 30;

		L2FishingZone aimingTo = FishingZoneManager.getInstance().isInsideFishingZone(x, y, z);
		L2WaterZone water = FishingZoneManager.getInstance().isInsideWaterZone(x, y, z);
		if(aimingTo != null && water != null && (GeoData.getInstance().canSeeTarget(player.getX(), player.getY(), player.getZ() + 50, x, y, water.getWaterZ() - 50)))
		{
			z = water.getWaterZ() + 10;
		}
		else if(aimingTo != null && GeoData.getInstance().canSeeTarget(player.getX(), player.getY(), player.getZ() + 50, x, y, water.getWaterZ() - 50))
		{
			z = aimingTo.getWaterZ() + 10;
		}
		else
		{
			player.sendPacket(new SystemMessage(SystemMessageId.BAIT_ON_HOOK_BEFORE_FISHING));
			SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED);
			sm.addString(skill.getName());
			player.sendPacket(sm);
				return;
		}

		if(player.getZ() <= -3800 || player.getZ() < (z - 32))
		{
			player.sendPacket(new SystemMessage(SystemMessageId.CANNOT_FISH_UNDER_WATER));
			if(!player.isGM())
			{
				return;
			}
		}

		lure2 = player.getInventory().destroyItem("Consume", player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, player, null);
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(lure2);
		player.sendPacket(iu);
		player.startFishing(x, y, z);
	}

	@Override
	public L2SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}