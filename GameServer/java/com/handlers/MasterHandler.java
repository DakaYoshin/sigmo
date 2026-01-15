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
package com.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.Config;
import com.gameserver.handler.AdminCommandHandler;
import com.gameserver.handler.ChatHandler;
import com.gameserver.handler.ItemHandler;
import com.gameserver.handler.UserCommandHandler;
import com.gameserver.handler.VoicedCommandHandler;
import com.handlers.admincommandhandlers.*;
import com.handlers.chathandlers.*;
import com.handlers.itemhandlers.*;
import com.handlers.usercommandhandlers.*;
import com.handlers.voicedcommandhandlers.*;

public class MasterHandler {
	private static final Logger _log = LoggerFactory.getLogger(MasterHandler.class);

	private static void loadAdminHandlers() {
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminAdmin());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminAio());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminBoat());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminInvul());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminDelete());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminDebug());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminKill());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminTarget());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminShop());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEvents());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminAnnouncements());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminCreateItem());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminHide());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminHeal());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminHelpPage());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminShutdown());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminSpawn());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminSkill());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminScript());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminExpSp());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminGmChat());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEditChar());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminGm());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminTeleport());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminRepairChar());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminChangeAccessLevel());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminChristmas());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminBan());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminPolymorph());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminReload());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminKick());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMonsterRace());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEditNpc());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminFightCalculator());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMenu());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminSiege());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminPetition());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminPForge());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminBBS());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEffects());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminDoorControl());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminTest());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminEnchant());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMassRecall());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMassControl());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMobGroup());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminRes());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminMammon());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminUnblockIp());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminPledge());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminRideWyvern());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminLogin());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminCache());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminLevel());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminQuest());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminVip());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminZone());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminCursedWeapons());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminGeodata());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminManor());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminHero());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminNoble());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminBuffs());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminClanFull());
		AdminCommandHandler.getInstance().registerAdminCommandHandler(new AdminChaos());
		_log.info("Loaded " + AdminCommandHandler.getInstance().size() + " AdminHandlers");
	}

	private static void loadChatHandlers() {
		ChatHandler.getInstance().registerChatHandler(new ChatAll());
		ChatHandler.getInstance().registerChatHandler(new ChatAlliance());
		ChatHandler.getInstance().registerChatHandler(new ChatClan());
		ChatHandler.getInstance().registerChatHandler(new ChatHeroVoice());
		ChatHandler.getInstance().registerChatHandler(new ChatParty());
		ChatHandler.getInstance().registerChatHandler(new ChatPartyMatchRoom());
		ChatHandler.getInstance().registerChatHandler(new ChatPartyRoomAll());
		ChatHandler.getInstance().registerChatHandler(new ChatPartyRoomCommander());
		ChatHandler.getInstance().registerChatHandler(new ChatPetition());
		ChatHandler.getInstance().registerChatHandler(new ChatShout());
		ChatHandler.getInstance().registerChatHandler(new ChatTell());
		ChatHandler.getInstance().registerChatHandler(new ChatTrade());
		_log.info("Loaded " + ChatHandler.getInstance().size() + " ChatHandlers");
	}

	private static void loadUserHandlers() {
		UserCommandHandler.getInstance().registerUserCommandHandler(new Time());
		UserCommandHandler.getInstance().registerUserCommandHandler(new OlympiadStat());
		UserCommandHandler.getInstance().registerUserCommandHandler(new ChannelLeave());
		UserCommandHandler.getInstance().registerUserCommandHandler(new ChannelDelete());
		UserCommandHandler.getInstance().registerUserCommandHandler(new ChannelListUpdate());
		UserCommandHandler.getInstance().registerUserCommandHandler(new ClanPenalty());
		UserCommandHandler.getInstance().registerUserCommandHandler(new ClanWarsList());
		UserCommandHandler.getInstance().registerUserCommandHandler(new DisMount());
		UserCommandHandler.getInstance().registerUserCommandHandler(new Escape());
		UserCommandHandler.getInstance().registerUserCommandHandler(new Loc());
		UserCommandHandler.getInstance().registerUserCommandHandler(new Mount());
		UserCommandHandler.getInstance().registerUserCommandHandler(new PartyInfo());
		_log.info("Loaded " + UserCommandHandler.getInstance().size() + " UserCommandHandlers");
	}

	private static void loadVoicedHandlers() {
		if (Config.BANKING_SYSTEM_ENABLED)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Banking());
		if (Config.ALLOW_WEDDING)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Wedding());
		if (Config.MULTILANG_ENABLE)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new Lang());
		if (Config.ENABLED_CHAOS_EVENT)
			VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new ChaosCmd());

		VoicedCommandHandler.getInstance().registerVoicedCommandHandler(new VoicedAutoFarm());

		_log.info("Loaded " + VoicedCommandHandler.getInstance().size() + " VoicedCommandHandlers");
	}

	private static void loadItemHandlers() {
		ItemHandler.getInstance().registerItemHandler(new ScrollOfEscape());
		ItemHandler.getInstance().registerItemHandler(new ScrollOfResurrection());
		ItemHandler.getInstance().registerItemHandler(new SoulShots());
		ItemHandler.getInstance().registerItemHandler(new SpiritShot());
		ItemHandler.getInstance().registerItemHandler(new BlessedSpiritShot());
		ItemHandler.getInstance().registerItemHandler(new BeastSoulShot());
		ItemHandler.getInstance().registerItemHandler(new BeastSpiritShot());
		ItemHandler.getInstance().registerItemHandler(new ChestKey());
		ItemHandler.getInstance().registerItemHandler(new PaganKeys());
		ItemHandler.getInstance().registerItemHandler(new Maps());
		ItemHandler.getInstance().registerItemHandler(new MapForestOfTheDead());
		ItemHandler.getInstance().registerItemHandler(new Potions());
		ItemHandler.getInstance().registerItemHandler(new Recipes());
		ItemHandler.getInstance().registerItemHandler(new RollingDice());
		ItemHandler.getInstance().registerItemHandler(new MysteryPotion());
		ItemHandler.getInstance().registerItemHandler(new EnchantScrolls());
		ItemHandler.getInstance().registerItemHandler(new EnergyStone());
		ItemHandler.getInstance().registerItemHandler(new Book());
		ItemHandler.getInstance().registerItemHandler(new Remedy());
		ItemHandler.getInstance().registerItemHandler(new Scrolls());
		ItemHandler.getInstance().registerItemHandler(new CrystalCarol());
		ItemHandler.getInstance().registerItemHandler(new SoulCrystals());
		ItemHandler.getInstance().registerItemHandler(new SevenSignsRecord());
		ItemHandler.getInstance().registerItemHandler(new CharChangePotions());
		ItemHandler.getInstance().registerItemHandler(new Firework());
		ItemHandler.getInstance().registerItemHandler(new Seed());
		ItemHandler.getInstance().registerItemHandler(new Harvester());
		ItemHandler.getInstance().registerItemHandler(new MercTicket());
		ItemHandler.getInstance().registerItemHandler(new Nectar());
		ItemHandler.getInstance().registerItemHandler(new FishShots());
		ItemHandler.getInstance().registerItemHandler(new ExtractableItems());
		ItemHandler.getInstance().registerItemHandler(new SpecialXMas());
		ItemHandler.getInstance().registerItemHandler(new SummonItems());
		ItemHandler.getInstance().registerItemHandler(new BeastSpice());
		ItemHandler.getInstance().registerItemHandler(new JackpotSeed());
		ItemHandler.getInstance().registerItemHandler(new MOSKey());
		ItemHandler.getInstance().registerItemHandler(new BreakingArrow());
		ItemHandler.getInstance().registerItemHandler(new ChristmasTree());
		ItemHandler.getInstance().registerItemHandler(new Crystals());
		ItemHandler.getInstance().registerItemHandler(new Primeval());

		_log.info("Loaded " + ItemHandler.getInstance().size() + " ItemHandlers");
	}

	public static void main(String[] args) {
		_log.info("Loading Handlers..");
		loadAdminHandlers();
		loadChatHandlers();
		loadUserHandlers();
		loadVoicedHandlers();
		loadItemHandlers();
		_log.info("Handlers loaded!");
	}
}