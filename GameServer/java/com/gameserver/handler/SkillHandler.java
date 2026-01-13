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
package com.gameserver.handler;

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gameserver.handler.skillhandlers.BalanceLife;
import com.gameserver.handler.skillhandlers.Blow;
import com.gameserver.handler.skillhandlers.Charge;
import com.gameserver.handler.skillhandlers.ClanGate;
import com.gameserver.handler.skillhandlers.CombatPointHeal;
import com.gameserver.handler.skillhandlers.Continuous;
import com.gameserver.handler.skillhandlers.CpDam;
import com.gameserver.handler.skillhandlers.Craft;
import com.gameserver.handler.skillhandlers.DeluxeKey;
import com.gameserver.handler.skillhandlers.Disablers;
import com.gameserver.handler.skillhandlers.DrainSoul;
import com.gameserver.handler.skillhandlers.Dummy;
import com.gameserver.handler.skillhandlers.Fishing;
import com.gameserver.handler.skillhandlers.FishingSkill;
import com.gameserver.handler.skillhandlers.GetPlayer;
import com.gameserver.handler.skillhandlers.Harvest;
import com.gameserver.handler.skillhandlers.Heal;
import com.gameserver.handler.skillhandlers.InstantJump;
import com.gameserver.handler.skillhandlers.ManaHeal;
import com.gameserver.handler.skillhandlers.Manadam;
import com.gameserver.handler.skillhandlers.Mdam;
import com.gameserver.handler.skillhandlers.Pdam;
import com.gameserver.handler.skillhandlers.Recall;
import com.gameserver.handler.skillhandlers.Resurrect;
import com.gameserver.handler.skillhandlers.SiegeFlag;
import com.gameserver.handler.skillhandlers.Sow;
import com.gameserver.handler.skillhandlers.Spoil;
import com.gameserver.handler.skillhandlers.StrSiegeAssault;
import com.gameserver.handler.skillhandlers.SummonFriend;
import com.gameserver.handler.skillhandlers.SummonTreasureKey;
import com.gameserver.handler.skillhandlers.Sweep;
import com.gameserver.handler.skillhandlers.TakeCastle;
import com.gameserver.handler.skillhandlers.Unlock;
import com.gameserver.handler.skillhandlers.ZakenPlayer;
import com.gameserver.handler.skillhandlers.ZakenSelf;
import com.gameserver.templates.skills.L2SkillType;

public class SkillHandler
{
	private static final Log _log = LogFactory.getLog(SkillHandler.class.getName());

	private static SkillHandler _instance;

	private Map<L2SkillType, ISkillHandler> _datatable;

	public static SkillHandler getInstance()
	{
		if(_instance == null)
		{
			_instance = new SkillHandler();
		}

		return _instance;
	}

	private SkillHandler()
	{
		_datatable = new TreeMap<L2SkillType, ISkillHandler>();
		registerSkillHandler(new Blow());
		registerSkillHandler(new Pdam());
		registerSkillHandler(new Mdam());
		registerSkillHandler(new CpDam());
		registerSkillHandler(new Manadam());
		registerSkillHandler(new Heal());
		registerSkillHandler(new CombatPointHeal());
		registerSkillHandler(new ManaHeal());
		registerSkillHandler(new BalanceLife());
		registerSkillHandler(new Charge());
		registerSkillHandler(new ClanGate());
		registerSkillHandler(new Continuous());
		registerSkillHandler(new Resurrect());
		registerSkillHandler(new Spoil());
		registerSkillHandler(new Sweep());
		registerSkillHandler(new StrSiegeAssault());
		registerSkillHandler(new SummonFriend());
		registerSkillHandler(new SummonTreasureKey());
		registerSkillHandler(new Disablers());
		registerSkillHandler(new Recall());
		registerSkillHandler(new SiegeFlag());
		registerSkillHandler(new TakeCastle());
		registerSkillHandler(new Unlock());
		registerSkillHandler(new DrainSoul());
		registerSkillHandler(new Craft());
		registerSkillHandler(new Fishing());
		registerSkillHandler(new FishingSkill());
		registerSkillHandler(new DeluxeKey());
		registerSkillHandler(new Sow());
		registerSkillHandler(new Harvest());
		registerSkillHandler(new Dummy());
		registerSkillHandler(new InstantJump());
		registerSkillHandler(new GetPlayer());
		registerSkillHandler(new ZakenPlayer());
		registerSkillHandler(new ZakenSelf());
		_log.info("SkillHandler: Loaded " + _datatable.size() + " handlers.");

	}

	public void registerSkillHandler(ISkillHandler handler)
	{
		L2SkillType[] types = handler.getSkillIds();

		for(L2SkillType t : types)
		{
			_datatable.put(t, handler);
		}
		types = null;
	}

	public ISkillHandler getSkillHandler(L2SkillType skillType)
	{
		return _datatable.get(skillType);
	}

	public int size()
	{
		return _datatable.size();
	}

}