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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastMap;

import com.util.StringUtil;

public class ChatHandler {
	private static final Logger _log = LoggerFactory.getLogger(ChatHandler.class);

	private static final FastMap<Integer, IChatHandler> _datatable = new FastMap<Integer, IChatHandler>();

	public static ChatHandler getInstance() {
		return SingletonHolder.INSTANCE;
	}

	private ChatHandler() {
	}

	public void registerChatHandler(IChatHandler handler) {
		int[] ids = handler.getChatTypeList();
		for (int i = 0; i < ids.length; i++) {
			if (_log.isDebugEnabled()) {
				_log.debug(StringUtil.concat("Adding handler for chat type ", String.valueOf(ids[i])));
			}
			_datatable.put(ids[i], handler);
		}
	}

	public IChatHandler getChatHandler(int chatType) {
		return _datatable.get(chatType);
	}

	public int size() {
		return _datatable.size();
	}

	private static class SingletonHolder {
		protected static final ChatHandler INSTANCE = new ChatHandler();
	}
}