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
package handlers.voicedcommandhandlers;

import com.gameserver.handler.IVoicedCommandHandler;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.model.entity.ChaosEvent;

/**
 * @author Anarchy
 */
 public class ChaosCmd implements IVoicedCommandHandler
 {
 private static final String[] VOICED_COMMANDS =
 {
 "joinchaos", "leavechaos"
 };

 @Override
 public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target)
 {
 ChaosEvent chaos = new ChaosEvent();

 if (command.startsWith(VOICED_COMMANDS[0])) // joinchaos
 {
 if (ChaosEvent._isChaosActive)
 {
 chaos.registerToChaos(activeChar);
 return true;
 }
 activeChar.sendMessage("Chaos Event is not currently active.");
 return false;
 }
 if (command.startsWith(VOICED_COMMANDS[1])) // leavechaos
 {
 if (ChaosEvent._isChaosActive)
 {
 chaos.removeFromChaos(activeChar);
 return true;
 }
 activeChar.sendMessage("Chaos Event is not currently active.");
 return false;
 }
 return true;
 }

 @Override
 public String[] getVoicedCommandList()
 {
 return VOICED_COMMANDS;
 }
}