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
package com.gameserver.model.actor.instance;

import com.gameserver.ai.CtrlIntention;
import com.gameserver.model.actor.L2Npc;
import com.gameserver.model.actor.position.L2CharPosition;
import com.gameserver.network.serverpackets.ActionFailed;
import com.gameserver.network.serverpackets.MyTargetSelected;
import com.gameserver.network.serverpackets.ValidateLocation;
import com.gameserver.templates.chars.L2NpcTemplate;
import com.gameserver.thread.ThreadPoolManager;
import com.util.random.Rnd;

public class L2TownPetInstance extends L2Npc
{
	int randomX, randomY, spawnX, spawnY;

	public L2TownPetInstance(int objectId, L2NpcTemplate template)
	{
		super(objectId, template);

		ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new RandomWalkTask(), 2000, 2000);
	}

	@Override
	public void onAction(L2PcInstance player)
	{
		if(!canTarget(player))
		{
			return;
		}

		if(this != player.getTarget())
		{
			player.setTarget(this);
			MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
			player.sendPacket(my);
			player.sendPacket(new ValidateLocation(this));
		}
		else
		{
			if(!canInteract(player))
			{
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			}
		}

		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		spawnX = getX();
		spawnY = getY();
	}

	public class RandomWalkTask implements Runnable
	{
		@Override
		public void run()
		{
			if(!isInActiveRegion())
			{
				return;
			}
			randomX = spawnX + Rnd.get(2*50)-50;
			randomY = spawnY + Rnd.get(2*50)-50;
			setRunning();
			if((randomX != getX()) && (randomY != getY()))
			{
				getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(randomX,randomY,getZ(),0));
			}
		}
	}

}