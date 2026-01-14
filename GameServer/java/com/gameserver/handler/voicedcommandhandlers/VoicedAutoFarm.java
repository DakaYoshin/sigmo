package com.gameserver.handler.voicedcommandhandlers;

import com.gameserver.model.actor.L2Character;
import com.gameserver.handler.IVoicedCommandHandler;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.network.serverpackets.NpcHtmlMessage;

public class VoicedAutoFarm implements IVoicedCommandHandler {
    private static final String[] _voicedCommands = { "autofarm", "_clearIgnoredAutoFarm" };

    @Override
    public boolean useVoicedCommand(String command, L2PcInstance activeChar, String target) {
        if (command.equalsIgnoreCase("autofarm")) {
            showAutoFarm(activeChar);
        } else if (command.equalsIgnoreCase("_clearIgnoredAutoFarm")) {
            activeChar.getIgnoredMonsters().clear();
            showAutoFarm(activeChar);
        }
        return true;
    }

    public static void showAutoFarm(L2PcInstance player) {
        NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setFile("data/html/mods/menu/autofarm.htm");

        html.replace("%radius%", String.valueOf(player.getRadius()));
        html.replace("%page%", "F" + (player.getPage() + 1));
        html.replace("%heal%", String.valueOf(player.getHealPercent()));
        html.replace("%buffProtect%", player.isNoBuffProtected() ? "ON" : "OFF");
        html.replace("%buffProtectColor%", player.isNoBuffProtected() ? "00FF00" : "FF0000");
        html.replace("%antiKs%", player.isAntiKsProtected() ? "ON" : "OFF");
        html.replace("%antiKsColor%", player.isAntiKsProtected() ? "00FF00" : "FF0000");
        html.replace("%summonAttack%", player.isSummonAttack() ? "ON" : "OFF");
        html.replace("%summonAttackColor%", player.isSummonAttack() ? "00FF00" : "FF0000");
        html.replace("%summonSkill%", String.valueOf(player.getSummonSkillPercent()));
        html.replace("%hpPotion%", String.valueOf(player.getHpPotionPercentage()));
        html.replace("%mpPotion%", String.valueOf(player.getMpPotionPercentage()));
        html.replace("%autofarm%", player.isAutoFarm() ? "STOP" : "START");
        html.replace("%autofarmColor%", player.isAutoFarm() ? "FF0000" : "00FF00");

        String targetName = "Nenhum";
        String targetHp = "";
        if (player.getTarget() != null && player.getTarget() instanceof L2Character) {
            L2Character target = (L2Character) player.getTarget();
            targetName = target.getName();
            targetHp = " <font color=FF9999>(" + (int) target.getStatus().getCurrentHp() + ")</font>";
        }
        html.replace("%currentTarget%", targetName + targetHp);

        StringBuilder ignoredList = new StringBuilder();
        if (player.getIgnoredMonsters().isEmpty()) {
            ignoredList.append("<tr><td align=center><font color=777777>Nenhum alvo ignorado</font></td></tr>");
        } else {
            int count = 0;
            ignoredList.append("<tr><td>");
            for (int npcId : player.getIgnoredMonsters()) {
                if (count > 0)
                    ignoredList.append(", ");
                ignoredList.append(npcId);
                count++;
                if (count >= 10)
                    break;
            }
            ignoredList.append("</td></tr>");
            ignoredList.append(
                    "<tr><td align=center><button value=\"Limpar Lista\" action=\"bypass -h _clearIgnoredAutoFarm\" width=100 height=21 back=\"L2UI_ch3.bigbutton2_down\" fore=\"L2UI_ch3.bigbutton2\"></td></tr>");
        }
        html.replace("%ignoredMonsters%", ignoredList.toString());

        player.sendPacket(html);
    }

    @Override
    public String[] getVoicedCommandList() {
        return _voicedCommands;
    }
}
