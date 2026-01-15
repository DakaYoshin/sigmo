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

import com.gameserver.handler.ISkillHandler;
import com.gameserver.model.L2Object;
import com.gameserver.model.L2Skill;
import com.gameserver.model.actor.L2Character;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.templates.skills.L2SkillType;

public class DrainSoul implements ISkillHandler {
	private static final Logger _log = LoggerFactory.getLogger(DrainSoul.class);

	private static final L2SkillType[] SKILL_IDS = { L2SkillType.DRAIN_SOUL };

	@Override
	public void useSkill(L2Character activeChar, L2Skill skill, L2Object[] targets) {
		if (!(activeChar instanceof L2PcInstance)) {
			return;
		}

		L2Object[] targetList = skill.getTargetList(activeChar);

		if (targetList == null) {
			return;
		}

		_log.debug("Soul Crystal casting succeded.");
	}

	@Override
	public L2SkillType[] getSkillIds() {
		return SKILL_IDS;
	}
}