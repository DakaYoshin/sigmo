/* This program is free software; you can redistribute it and/or modify
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
package com.gameserver.skills.effects;

import com.gameserver.datatables.SkillTable;
import com.gameserver.model.L2Effect;
import com.gameserver.model.L2Skill;
import com.gameserver.model.actor.L2Character;
import com.gameserver.model.actor.instance.L2EffectPointInstance;
import com.gameserver.network.SystemMessageId;
import com.gameserver.network.serverpackets.MagicSkillUser;
import com.gameserver.network.serverpackets.SystemMessage;
import com.gameserver.skills.Env;
import com.gameserver.skills.l2skills.L2SkillSignet;
import com.gameserver.skills.l2skills.L2SkillSignetCasttime;
import com.gameserver.templates.skills.L2EffectType;

public final class EffectSignet extends L2Effect
{
	private L2Skill _skill;
	private L2EffectPointInstance _actor;

	public EffectSignet(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.SIGNET_EFFECT;
	}

	@Override
	public void onStart()
	{
		if(getSkill() instanceof L2SkillSignet)
		{
			_skill = SkillTable.getInstance().getInfo(((L2SkillSignet) getSkill()).effectId, getLevel());
		}
		else if(getSkill() instanceof L2SkillSignetCasttime)
		{
			_skill = SkillTable.getInstance().getInfo(((L2SkillSignetCasttime) getSkill()).effectId, getLevel());
		}
		_actor = (L2EffectPointInstance) getEffected();
	}

	@Override
	public boolean onActionTime()
	{
		if(_skill == null)
		{
			return true;
		}

		int mpConsume = _skill.getMpConsume();

		if(mpConsume > getEffector().getStatus().getCurrentMp())
		{
			getEffector().sendPacket(new SystemMessage(SystemMessageId.SKILL_REMOVED_DUE_LACK_MP));
			return false;
		}

		getEffector().reduceCurrentMp(mpConsume);

		for(L2Character cha : _actor.getKnownList().getKnownCharactersInRadius(getSkill().getSkillRadius()))
		{
			if(cha == null)
			{
				continue;
			}

			_skill.getEffects(_actor, cha);

			_actor.broadcastPacket(new MagicSkillUser(_actor, cha, _skill.getId(), _skill.getLevel(), 0, 0));
		}
		return true;
	}

	@Override
	public void onExit()
	{
		if(_actor != null)
		{
			_actor.deleteMe();
		}
	}
}