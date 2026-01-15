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

import gnu.trove.map.hash.TIntObjectHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.util.StringUtil;

public class VoicedCommandHandler {
	private static final Logger _log = LoggerFactory.getLogger(VoicedCommandHandler.class);

	private static final TIntObjectHashMap<IVoicedCommandHandler> _datatable = new TIntObjectHashMap<IVoicedCommandHandler>();

	public static VoicedCommandHandler getInstance() {
		return SingletonHolder.INSTANCE;
	}

	private VoicedCommandHandler() {

	}

	public void registerVoicedCommandHandler(IVoicedCommandHandler handler) {
		String[] ids = handler.getVoicedCommandList();
		for (int i = 0; i < ids.length; i++) {
			if (_log.isDebugEnabled()) {
				_log.debug(StringUtil.concat("Adding handler for command ", ids[i]));
			}
			_datatable.put(ids[i].hashCode(), handler);
		}
	}

	public IVoicedCommandHandler getVoicedCommandHandler(String voicedCommand) {
		String command = voicedCommand;
		if (voicedCommand.indexOf(" ") != -1) {
			command = voicedCommand.substring(0, voicedCommand.indexOf(" "));
		}
		if (_log.isDebugEnabled()) {
			_log.debug(StringUtil.concat("getting handler for command: ", command, " -> ",
					String.valueOf(_datatable.get(command.hashCode()) != null)));
		}
		return _datatable.get(command.hashCode());
	}

	public int size() {
		return _datatable.size();
	}

	private static class SingletonHolder {
		protected static final VoicedCommandHandler INSTANCE = new VoicedCommandHandler();
	}
}