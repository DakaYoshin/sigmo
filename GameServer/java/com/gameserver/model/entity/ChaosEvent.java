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
package com.gameserver.model.entity;

import java.util.Vector;

import com.Config;
import com.gameserver.datatables.SkillTable;
import com.gameserver.model.L2Effect;
import com.gameserver.model.L2Skill;
import com.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author Anarchy
 */
// Configs added by Pauler

public class ChaosEvent
{
 public static Vector<L2PcInstance> _players = new Vector<L2PcInstance>();
 public static L2PcInstance _topplayer, _topplayer2, _topplayer3, _topplayer4, _topplayer5;
 public static int _topkills = 0, _topkills2 = 0, _topkills3 = 0, _topkills4 = 0, _topkills5 = 0;
 public static boolean _isChaosActive;

 public void startChaos()
 {
 _isChaosActive = true;
 _players.clear();
 Announcements.getInstance().announceToAll("Chaos Event Iniciado !");
 Announcements.getInstance().announceToAll("Comandos: joinchaos para registrar .leavechaos para sair!");
 }

 public void stopChaos()
 {
 _isChaosActive = false;
 Announcements.getInstance().announceToAll("Chaos Event has ended!");
 getTopKiller();
 calculateRewards();
 for (L2PcInstance player : _players)
 {
 removeSuperHaste(player);
 }
 cleanColors();
 cleanPlayers();
 _players.clear();
 }

 public void cleanColors()
 {
 for (L2PcInstance player : _players)
 {
 player.getAppearance().setNameColor(0xFFFFFF);
 player.broadcastUserInfo();
 }
 }

 public void cleanPlayers()
 {
 for (L2PcInstance player : _players)
 {
 player._inChaosEvent = false;
 player._chaosKills = 0;
 _topkills = 0;
 _topplayer = null;
 }
 }

 public void registerToChaos(L2PcInstance player)
 {
 if (!registerToChaosOk(player))
 {
 return;
 }
 _players.add(player);
 player._inChaosEvent = true;
 player._chaosKills = 0;
 player.getAppearance().setNameColor(Config.CHAOS_COLOR);
 player.broadcastUserInfo();
 player.sendMessage("You have joined Chaos Event.");
 addSuperHaste(player);
 }

public void addSuperHaste(L2PcInstance player)
 {
 L2Skill skill = SkillTable.getInstance().getInfo(Config.CHAOS_SKILL_ID, Config.CHAOS_SKILL_LVL);
 if (skill != null)
 {
 skill.getEffects(player, player);
 }
 }

 public boolean registerToChaosOk(L2PcInstance chaosplayer)
 {
 if (chaosplayer._inChaosEvent)
 {
 chaosplayer.sendMessage("You already are in Chaos Event.");
 return false;
 }
 return true;
 }

 public void removeFromChaos(L2PcInstance player)
 {
 if (!removeFromChaosOk(player))
 {
 return;
 }
 _players.remove(player);
 player._chaosKills = 0;
 player._inChaosEvent = false;
 player.sendMessage("You have left Chaos Event.");
 player.getAppearance().setNameColor(0xFFFFFF);
 player.broadcastUserInfo();
 removeSuperHaste(player);
 }

 public boolean removeFromChaosOk(L2PcInstance chaosplayer)
 {
 if (!chaosplayer._inChaosEvent)
 {
 chaosplayer.sendMessage("You are not in Chaos Event.");
 return false;
 }
 return true;
 }

 public static void getTopKiller()
 {
 for (L2PcInstance player : _players)
 {
 if (player._chaosKills > _topkills)
 {
 _topplayer = player;
 _topkills = player._chaosKills;
 }
 if ((player._chaosKills > _topkills2) && (player._chaosKills < _topkills))
 {
 _topplayer2 = player;
 _topkills2 = player._chaosKills;
 }
 if ((player._chaosKills > _topkills3) && (player._chaosKills < _topkills2))
 {
 _topplayer3 = player;
 _topkills3 = player._chaosKills;
 }
 if ((player._chaosKills > _topkills4) && (player._chaosKills < _topkills3))
 {
 _topplayer4 = player;
 _topkills4 = player._chaosKills;
 }
 if ((player._chaosKills > _topkills5) && (player._chaosKills < _topkills4))
 {
 _topplayer5 = player;
 _topkills5 = player._chaosKills;
 }
 }
 }

 public void calculateRewards()
 {
 Announcements.getInstance().announceToAll("Winner of Chaos Event:");
 if (_topplayer != null)
 {
 _topplayer.addItem("Chaos Event Reward", Config.CHAOS_FIRST_WINNER_REWARD_ID, Config.CHAOS_FIRST_WINNER_REWARD_QUANTITY, _topplayer, true);
 Announcements.getInstance().announceToAll("1) " + _topplayer.getName());
 }
 if (_topplayer2 != null)
 {
 _topplayer2.addItem("Chaos Event Reward 2", Config.CHAOS_SECOND_WINNER_REWARD_ID, Config.CHAOS_SECOND_WINNER_REWARD_QUANTITY, _topplayer2, true);
 Announcements.getInstance().announceToAll("2) " + _topplayer2.getName());
 }
 if (_topplayer3 != null)
 {
 _topplayer3.addItem("Chaos Event Reward 3", Config.CHAOS_THIRD_WINNER_REWARD_ID, Config.CHAOS_THIRD_WINNER_REWARD_QUANTITY, _topplayer3, true);
 Announcements.getInstance().announceToAll("3) " + _topplayer3.getName());
 }
 if (_topplayer4 != null)
 {
 _topplayer4.addItem("Chaos Event Reward 4", Config.CHAOS_FOURTH_WINNER_REWARD_ID, Config.CHAOS_FOURTH_WINNER_REWARD_QUANTITY, _topplayer4, true);
 Announcements.getInstance().announceToAll("4) " + _topplayer4.getName());
 }
 if (_topplayer5 != null)
 {
 _topplayer5.addItem("Chaos Event Reward 5", Config.CHAOS_FIFTH_WINNER_REWARD_ID, Config.CHAOS_FIFTH_WINNER_REWARD_QUANTITY, _topplayer5, true);
 Announcements.getInstance().announceToAll("5) " + _topplayer5.getName());
 }
 }

 public void removeSuperHaste(L2PcInstance activeChar)
 {
 if (activeChar != null)
 {
 L2Effect[] effects = activeChar.getAllEffects();

 for (L2Effect e : effects)
 {
 if ((e != null) && (e.getSkill().getId() == Config.CHAOS_SKILL_ID))
 {
 e.exit();
 }
 }
 }
 }
}