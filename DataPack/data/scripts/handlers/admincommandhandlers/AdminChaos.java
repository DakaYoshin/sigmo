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
package handlers.admincommandhandlers;

import com.gameserver.handler.IAdminCommandHandler;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.model.entity.ChaosEvent;
import com.gameserver.network.clientpackets.EnterWorld;


/**
+ * @author Anarchy
 */
public class AdminChaos implements IAdminCommandHandler
{
 private static final String[] ADMIN_COMMANDS =
 {
 "admin_startchaos",
 "admin_endchaos",
 "admin_warnchaos"
 };

 @Override
 public boolean useAdminCommand(String command, L2PcInstance activeChar)
 {
 ChaosEvent chaos = new ChaosEvent();

 if (command.equals("admin_warnchaos"))
 {
 if (ChaosEvent._isChaosActive)
 {
 activeChar.sendMessage("You can only warn the players if Chaos Event isn't active.");
 return false;
 }

 EnterWorld.warnAllPlayers();
 return true;
 }
 if (command.equals("admin_startchaos"))
 {
 if (!ChaosEvent._isChaosActive)
 {
 chaos.startChaos();
 activeChar.sendMessage("You have succesfully started Chaos Event. Press //endchaos to stop it.");
 return true;
 }

 activeChar.sendMessage("Chaos Event is already active.");
 return false;
 }
 if (command.equals("admin_endchaos"))
 {
 if (ChaosEvent._isChaosActive)
 {
 chaos.stopChaos();
 activeChar.sendMessage("You have succesfully stopped Chaos Event.");
 return true;
 }

 activeChar.sendMessage("Chaos Event is not active.");
 return false;
 }

 return true;
 }

 @Override
 public String[] getAdminCommandList()
 {
 return ADMIN_COMMANDS;
 }
}