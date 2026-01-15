package quests.q109_InSearchOfTheNest;

import com.gameserver.model.actor.L2Npc;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.model.quest.Quest;
import com.gameserver.model.quest.QuestState;
import com.gameserver.model.quest.State;

public class Q109_InSearchOfTheNest extends Quest {
    private static final String qn = "q109_InSearchOfTheNest";

    // NPCs
    private static final int Pierce = 31553;
    private static final int Corpse = 32015;
    private static final int Kahman = 31554;

    // Items
    private static final int Memo = 8083;
    private static final int Golden_Badge_Recruit = 7246;
    private static final int Golden_Badge_Soldier = 7247;

    // States as defined in the Python script
    private static final State CREATED = new State("Start", null);
    private static final State STARTED = new State("Started", null);
    private static final State COMPLETED = new State("Completed", null);

    public Q109_InSearchOfTheNest(int questId, String name, String descr) {
        super(questId, name, descr);

        // Fix state parent references (Quest constructor does this for initial, but
        // good to be explicit for others if needed)
        // Actually Quest constructor doesn't add states automatically unless we call
        // addState or similar
        // The python script did: CREATED = State('Start', QUEST)
        // In Java, we usually do this inside the constructor or init

        addState(CREATED);
        addState(STARTED);
        addState(COMPLETED);

        setInitialState(CREATED);

        addStartNpc(Pierce);
        addTalkId(Pierce);
        addTalkId(Corpse);
        addTalkId(Kahman);

        questItemIds = new int[] { Memo };
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
        QuestState st = player.getQuestState(qn);
        if (st == null)
            return null;

        String htmltext = event;
        int cond = st.getInt("cond");

        if (event.equals("Memo") && cond == 1) {
            st.giveItems(Memo, 1);
            st.set("cond", "2");
            st.playSound("ItemSound.quest_itemget");
            return null;
        } else if (event.equals("31553-02.htm") && cond == 2) {
            st.takeItems(Memo, -1);
            st.set("cond", "3");
            st.playSound("ItemSound.quest_middle");
        }

        return htmltext;
    }

    @Override
    public String onTalk(L2Npc npc, L2PcInstance player) {
        String htmltext = "<html><head><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
        QuestState st = player.getQuestState(qn);

        if (st != null) {
            int npcId = npc.getNpcId();
            int cond = st.getInt("cond");
            State state = st.getState();

            if (state == COMPLETED) {
                htmltext = "<html><body>This quest has already been completed.</body></html>";
            } else if (state == CREATED) {
                if (player.getLevel() >= 66 && npcId == Pierce && (st.getQuestItemsCount(Golden_Badge_Recruit) > 0
                        || st.getQuestItemsCount(Golden_Badge_Soldier) > 0)) {
                    st.setState(STARTED);
                    st.playSound("ItemSound.quest_accept");
                    st.set("cond", "1");
                    htmltext = "<html><body>Mercenary Captain Pierce:<br>I sent out a scout a while ago, and he hasn't reported back yet. Please follow his trail and discover his fate.</body></html>";
                } else {
                    htmltext = "31553-00.htm";
                    st.exitQuest(true);
                    st.playSound("ItemSound.quest_giveup");
                }
            } else if (state == STARTED) {
                if (npcId == Corpse) {
                    if (cond == 1)
                        htmltext = "32015-01.htm";
                    else if (cond == 2)
                        htmltext = "<html><body>This is nothing else here. Maybe you should take that memo to Pierce?</body></html>";
                } else if (npcId == Pierce) {
                    if (cond == 1)
                        htmltext = "<html><body>Mercenary Captain Pierce:<br>Please find my scout!</body></html>";
                    else if (cond == 2)
                        htmltext = "31553-01.htm";
                    else if (cond == 3)
                        htmltext = "<html><body>Mercenary Captain Pierce:<br>Thanks for your help. See Kahman for your reward!</body></html>";
                } else if (npcId == Kahman && cond == 3) {
                    htmltext = "31554-01.htm";
                    st.giveItems(57, 25461);
                    st.addExpAndSp(146113, 13723);
                    st.unset("cond");
                    st.setState(COMPLETED);
                    st.playSound("ItemSound.quest_finish");
                }
            }
        }

        return htmltext;
    }

    public static void main(String[] args) {
        new Q109_InSearchOfTheNest(109, qn, "In Search of the Nest");
    }
}
