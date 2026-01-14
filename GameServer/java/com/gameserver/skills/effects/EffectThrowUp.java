/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.gameserver.skills.effects;

import com.Config;
import com.gameserver.geo.GeoData;
import com.gameserver.model.L2Effect;
import com.gameserver.model.Location;
import com.gameserver.network.serverpackets.FlyToLocation;
import com.gameserver.network.serverpackets.FlyToLocation.FlyType;
import com.gameserver.network.serverpackets.ValidateLocation;
import com.gameserver.skills.Env;
import com.gameserver.templates.skills.L2EffectType;

public class EffectThrowUp extends L2Effect {
	private int _x, _y, _z;

	public EffectThrowUp(Env env, EffectTemplate template) {
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType() {
		return L2EffectType.THROW_UP;
	}

	@Override
	public void onStart() {
		// Get current position of the L2Character
		final int curX = getEffected().getX();
		final int curY = getEffected().getY();
		final int curZ = getEffected().getZ();

		// Get the difference between effector and effected positions
		double dx = getEffector().getX() - curX;
		double dy = getEffector().getY() - curY;
		double dz = getEffector().getZ() - curZ;

		// Handle Z axis movement by calculating the 3D distance and ensuring the
		// destination Z is updated from Geodata.
		final double distance = Math.sqrt(dx * dx + dy * dy);
		final double totalDistance = Math.sqrt(distance * distance + (dz * dz));
		if (totalDistance < 1) {
			return;
		}

		final int offset = Math.min((int) distance + getSkill().getFlyRadius(), 1400);
		final double sin = dy / distance;
		final double cos = dx / distance;

		// Calculate the new destination with offset included
		_x = getEffector().getX() - (int) ((offset > 5 ? offset : 5) * cos);
		_y = getEffector().getY() - (int) ((offset > 5 ? offset : 5) * sin);
		_z = getEffected().getZ();

		if (Config.GEODATA > 0) {
			Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(),
					getEffected().getZ(), _x, _y, _z);
			_x = destiny.getX();
			_y = destiny.getY();
			_z = destiny.getZ();
		}

		getEffected().startStunning();
		getEffected().broadcastPacket(new FlyToLocation(getEffected(), _x, _y, _z, FlyType.THROW_UP));
	}

	@Override
	public boolean onActionTime() {
		return false;
	}

	@Override
	public void onExit() {
		getEffected().stopStunning(null);
		getEffected().setXYZ(_x, _y, _z);
		getEffected().broadcastPacket(new ValidateLocation(getEffected()));
	}

}