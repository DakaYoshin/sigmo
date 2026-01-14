package com.gameserver.model.autofarm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.Config;
import com.gameserver.ai.CtrlEvent;
import com.gameserver.ai.CtrlIntention;
import com.gameserver.ai.NextAction;
import com.gameserver.geo.GeoData;
import com.gameserver.handler.IItemHandler;
import com.gameserver.handler.ItemHandler;
// VoicedAutoFarm moved to scripts - using direct messaging instead
import com.gameserver.model.L2Object;
import com.gameserver.model.L2ShortCut;
import com.gameserver.model.ShortCuts;
import com.gameserver.model.L2Skill;
import com.gameserver.model.L2WorldRegion;
import com.gameserver.model.actor.L2Character;
import com.gameserver.model.actor.L2Summon;
import com.gameserver.model.actor.instance.L2ChestInstance;
import com.gameserver.model.actor.instance.L2ItemInstance;
import com.gameserver.model.actor.instance.L2MonsterInstance;
import com.gameserver.model.actor.instance.L2PcInstance;
import com.gameserver.model.actor.instance.L2PetInstance;
import com.gameserver.network.SystemMessageId;
import com.gameserver.network.serverpackets.ActionFailed;
import com.gameserver.network.serverpackets.ExShowScreenMessage;
import com.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import com.gameserver.network.serverpackets.SystemMessage;
import com.gameserver.security.detector.BotDetector;
import com.gameserver.templates.skills.L2SkillType;
import com.gameserver.thread.ThreadPoolManager;

import com.gameserver.util.Util;
import com.util.random.Rnd;

/**
 * Adapted Autofarm Player Routine for Sigmo Project.
 * Integrates patch logic with security system.
 */
public class AutofarmPlayerRoutine {
    private final L2PcInstance player;
    private ScheduledFuture<?> _task;
    private L2Character committedTarget = null;

    // Security integration: Add timing variation to avoid bot detection
    private static final int BASE_DELAY = 600;
    private static final int DELAY_VARIATION = 300; // ±300ms variation for more human-like behavior

    public AutofarmPlayerRoutine(L2PcInstance player) {
        this.player = player;
    }

    public void start() {
        if (_task == null) {
            _task = ThreadPoolManager.getInstance().scheduleGeneral(() -> executeRoutine(), getNextDelay());

            player.sendPacket(new ExShowScreenMessage("Auto Farming Activated...", 5000, SMPOS.TOP_CENTER, false));
            player.sendPacket(new SystemMessage(SystemMessageId.AUTO_FARM_ACTIVATED));

            // Mark player as using legitimate autofarm for security system
            player.setAutoFarmActive(true);
        }
    }

    public void stop() {
        if (_task != null) {
            _task.cancel(false);
            _task = null;

            player.sendPacket(new ExShowScreenMessage("Auto Farming Deactivated...", 5000, SMPOS.TOP_CENTER, false));
            player.sendPacket(new SystemMessage(SystemMessageId.AUTO_FARM_DESACTIVATED));

            // Unmark player
            player.setAutoFarmActive(false);
        }
    }

    private int getNextDelay() {
        return BASE_DELAY + Rnd.get(-DELAY_VARIATION, DELAY_VARIATION);
    }

    public void executeRoutine() {
        try {
            if (!player.isAutoFarmActive() || !player.isAutoFarm()) {
                stop();
                return;
            }

            if (player.isNoBuffProtected() && player.getAllEffects().length <= 8) {
                player.sendMessage("You don't have buffs to use autofarm.");
                player.sendPacket(
                        new ExShowScreenMessage("Autofarm stopped - No buffs", 3000, SMPOS.TOP_CENTER, false));
                player.broadcastUserInfo();
                player.setAutoFarm(false);
                stop();
                return;
            }

            // Notify security system about legitimate actions
            BotDetector.getInstance().trackAction(player);

            calculatePotions();
            checkSpoil();
            targetEligibleCreature();

            if (player.isMageClass()) {
                useAppropriateSpell();
            } else if (shotcutsContainAttack()) {
                attack();
            } else {
                useAppropriateSpell();
            }
            checkSpoil();
            useAppropriateSpell();

        } catch (Exception e) {
            // Silently handle error to keep routine running
        } finally {
            // Schedule next execution with fresh variation
            if (_task != null && player.isAutoFarmActive()) {
                _task = ThreadPoolManager.getInstance().scheduleGeneral(() -> executeRoutine(), getNextDelay());
            }
        }
    }

    private void attack() {
        if (shotcutsContainAttack())
            physicalAttack();
    }

    private void useAppropriateSpell() {
        L2Skill chanceSkill = nextAvailableSkill(getChanceSpells(), AutofarmSpellType.Chance);

        if (chanceSkill != null) {
            useMagicSkill(chanceSkill, false);
            return;
        }

        L2Skill lowLifeSkill = nextAvailableSkill(getLowLifeSpells(), AutofarmSpellType.LowLife);

        if (lowLifeSkill != null) {
            useMagicSkill(lowLifeSkill, true);
            return;
        }

        L2Skill attackSkill = nextAvailableSkill(getAttackSpells(), AutofarmSpellType.Attack);

        if (attackSkill != null) {
            useMagicSkill(attackSkill, false);
            return;
        }
    }

    public L2Skill nextAvailableSkill(List<Integer> skillIds, AutofarmSpellType spellType) {
        for (Integer skillId : skillIds) {
            L2Skill skill = player.getKnownSkill(skillId);

            if (skill == null)
                continue;

            if (skill.getSkillType() == L2SkillType.SIGNET || skill.getSkillType() == L2SkillType.SIGNET_CASTTIME)
                continue;

            // Basic cast condition checks
            if (player.isSkillDisabled(skill.getId()))
                continue;

            if (player.getCurrentMp() < (player.getStat().getMpConsume(skill)
                    + player.getStat().getMpInitialConsume(skill)))
                continue;

            if (isSpoil(skillId)) {
                if (monsterIsAlreadySpoiled()) {
                    continue;
                }
                return skill;
            }

            if (spellType == AutofarmSpellType.Chance && getMonsterTarget() != null) {
                if (getMonsterTarget().getFirstEffect(skillId) == null)
                    return skill;
                continue;
            }

            if (spellType == AutofarmSpellType.LowLife && getHpPercentage() > player.getHealPercent())
                break;

            return skill;
        }

        return null;
    }

    private void checkSpoil() {
        if (canBeSweepedByMe() && getMonsterTarget().isDead()) {
            L2Skill sweeper = player.getKnownSkill(42);
            if (sweeper == null)
                return;

            useMagicSkill(sweeper, false);
        }
    }

    private Double getHpPercentage() {
        return player.getCurrentHp() * 100.0 / player.getMaxHp();
    }

    private Double percentageMpIsLessThan() {
        return player.getCurrentMp() * 100.0 / player.getMaxMp();
    }

    private Double percentageHpIsLessThan() {
        return player.getCurrentHp() * 100.0 / player.getMaxHp();
    }

    private List<Integer> getAttackSpells() {
        return getSpellsInSlots(AutofarmConstants.attackSlots);
    }

    private List<Integer> getSpellsInSlots(List<Integer> attackSlots) {
        try {
            // Access _shortCuts field via reflection since there's no public getter
            java.lang.reflect.Field field = player.getClass().getDeclaredField("_shortCuts");
            field.setAccessible(true);
            ShortCuts shortcuts = (ShortCuts) field.get(player);

            return Arrays.stream(shortcuts.getAllShortCuts())
                    .filter(shortcut -> shortcut.getPage() == player.getPage()
                            && shortcut.getType() == L2ShortCut.TYPE_SKILL && attackSlots.contains(shortcut.getSlot()))
                    .map(L2ShortCut::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Integer> getChanceSpells() {
        return getSpellsInSlots(AutofarmConstants.chanceSlots);
    }

    private List<Integer> getLowLifeSpells() {
        return getSpellsInSlots(AutofarmConstants.lowLifeSlots);
    }

    private boolean shotcutsContainAttack() {
        try {
            // Access _shortCuts field via reflection
            java.lang.reflect.Field field = player.getClass().getDeclaredField("_shortCuts");
            field.setAccessible(true);
            ShortCuts shortcuts = (ShortCuts) field.get(player);

            return Arrays.stream(shortcuts.getAllShortCuts())
                    .anyMatch(shortcut -> shortcut.getPage() == player.getPage()
                            && shortcut.getType() == L2ShortCut.TYPE_ACTION
                            && (shortcut.getId() == 2 || player.isSummonAttack() && shortcut.getId() == 22));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean monsterIsAlreadySpoiled() {
        return getMonsterTarget() != null && getMonsterTarget().getIsSpoiledBy() != 0;
    }

    private static boolean isSpoil(Integer skillId) {
        return skillId == 254 || skillId == 302;
    }

    private boolean canBeSweepedByMe() {
        return getMonsterTarget() != null && getMonsterTarget().isDead()
                && getMonsterTarget().getIsSpoiledBy() == player.getObjectId();
    }

    private void castSpellWithAppropriateTarget(L2Skill skill, Boolean forceOnSelf) {
        if (forceOnSelf) {
            L2Object oldTarget = player.getTarget();
            player.setTarget(player);
            player.useMagic(skill, false, false);
            player.setTarget(oldTarget);
            return;
        }

        player.useMagic(skill, false, false);
    }

    private void physicalAttack() {
        if (!(player.getTarget() instanceof L2MonsterInstance))
            return;

        L2MonsterInstance target = (L2MonsterInstance) player.getTarget();

        if (!player.isMageClass()) {
            if (target.isAutoAttackable(player) && GeoData.getInstance().canSeeTarget(player, target)) {
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);
                // player.onActionRequest(); // Mapped to whatever action request handles in
                // your server

                if (player.isSummonAttack() && player.getPet() != null) {
                    // Siege Golem's
                    if (player.getPet().getNpcId() >= 14702 && player.getPet().getNpcId() <= 14798
                            || player.getPet().getNpcId() >= 14839 && player.getPet().getNpcId() <= 14869)
                        return;

                    L2Summon activeSummon = player.getPet();
                    activeSummon.setTarget(target);
                    activeSummon.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);

                    int[] summonAttackSkills = { 4261, 4068, 4137, 4260, 4708, 4709, 4710, 4712, 5135, 5138, 5141, 5442,
                            5444, 6095, 6096, 6041, 6044 };
                    if (Rnd.get(100) < player.getSummonSkillPercent()) {
                        for (int skillId : summonAttackSkills) {
                            useMagicSkillBySummon(skillId, target);
                        }
                    }
                }
            } else {
                if (target.isAutoAttackable(player) && GeoData.getInstance().canSeeTarget(player, target))
                    player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, target);
            }
        } else {
            if (player.isSummonAttack() && player.getPet() != null) {
                // Siege Golem's
                if (player.getPet().getNpcId() >= 14702 && player.getPet().getNpcId() <= 14798
                        || player.getPet().getNpcId() >= 14839 && player.getPet().getNpcId() <= 14869)
                    return;

                L2Summon activeSummon = player.getPet();
                activeSummon.setTarget(target);
                activeSummon.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, target);

                int[] summonAttackSkills = { 4261, 4068, 4137, 4260, 4708, 4709, 4710, 4712, 5135, 5138, 5141, 5442,
                        5444, 6095, 6096, 6041, 6044 };
                if (Rnd.get(100) < player.getSummonSkillPercent()) {
                    for (int skillId : summonAttackSkills) {
                        useMagicSkillBySummon(skillId, target);
                    }
                }
            }
        }
    }

    public void targetEligibleCreature() {
        if (player.getTarget() == null) {
            selectNewTarget();
            return;
        }

        if (committedTarget != null) {
            if (!committedTarget.isDead() && GeoData.getInstance().canSeeTarget(player, committedTarget)) {
                attack();
                return;
            } else if (!GeoData.getInstance().canSeeTarget(player, committedTarget)) {
                committedTarget = null;
                selectNewTarget();
                return;
            }
            player.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, committedTarget);
            committedTarget = null;
            player.setTarget(null);
        }

        if (committedTarget instanceof L2Summon)
            return;

        List<L2MonsterInstance> targets = getKnownMonstersInRadius(player, player.getRadius(),
                creature -> GeoData.getInstance().canSeeTarget(player, creature)
                        && !player.ignoredMonsterContain(creature.getNpcId()) && !creature.isRaid()
                        && !creature.isDead() && !(creature instanceof L2ChestInstance)
                        && !(player.isAntiKsProtected() && creature.getTarget() != null
                                && creature.getTarget() != player && creature.getTarget() != player.getPet()));

        if (targets.isEmpty())
            return;

        L2MonsterInstance closestTarget = targets.stream()
                .min((o1, o2) -> Double.compare(player.getDistanceSq(o1), player.getDistanceSq(o2))).get();

        committedTarget = closestTarget;
        player.setTarget(closestTarget);

        // Notify security system about movement
        BotDetector.getInstance().trackMovement(player, closestTarget.getX(), closestTarget.getY(),
                closestTarget.getZ());
    }

    private void selectNewTarget() {
        List<L2MonsterInstance> targets = getKnownMonstersInRadius(player, player.getRadius(),
                creature -> GeoData.getInstance().canSeeTarget(player, creature)
                        && !player.ignoredMonsterContain(creature.getNpcId()) && !creature.isRaid()
                        && !creature.isDead() && !(creature instanceof L2ChestInstance)
                        && !(player.isAntiKsProtected() && creature.getTarget() != null
                                && creature.getTarget() != player && creature.getTarget() != player.getPet()));

        if (targets.isEmpty())
            return;

        L2MonsterInstance closestTarget = targets.stream()
                .min((o1, o2) -> Double.compare(player.getDistanceSq(o1), player.getDistanceSq(o2))).get();

        committedTarget = closestTarget;
        player.setTarget(closestTarget);

        // Notify security system about movement
        BotDetector.getInstance().trackMovement(player, closestTarget.getX(), closestTarget.getY(),
                closestTarget.getZ());
    }

    public final static List<L2MonsterInstance> getKnownMonstersInRadius(L2PcInstance player, int radius,
            Function<L2MonsterInstance, Boolean> condition) {
        final L2WorldRegion region = player.getWorldRegion();
        if (region == null)
            return Collections.emptyList();

        final List<L2MonsterInstance> result = new ArrayList<>();

        for (L2WorldRegion reg : region.getSurroundingRegions()) {
            for (L2Object obj : reg.getVisibleObjects()) {
                if (!(obj instanceof L2MonsterInstance) || !Util.checkIfInRange(radius, player, obj, true)
                        || !condition.apply((L2MonsterInstance) obj))
                    continue;

                result.add((L2MonsterInstance) obj);
            }
        }

        return result;
    }

    public L2MonsterInstance getMonsterTarget() {
        if (!(player.getTarget() instanceof L2MonsterInstance)) {
            return null;
        }

        return (L2MonsterInstance) player.getTarget();
    }

    private void useMagicSkill(L2Skill skill, Boolean forceOnSelf) {
        if (skill.getSkillType() == L2SkillType.RECALL && !Config.KARMA_PLAYER_CAN_TELEPORT && player.getKarma() > 0) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (skill.isToggle() && player.isMounted()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (player.isOutOfControl()) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (player.isAttackingNow())
            player.getAI().setNextAction(new NextAction(CtrlEvent.EVT_READY_TO_ACT, CtrlIntention.AI_INTENTION_CAST,
                    () -> castSpellWithAppropriateTarget(skill, forceOnSelf)));
        else
            castSpellWithAppropriateTarget(skill, forceOnSelf);
    }

    private boolean useMagicSkillBySummon(int skillId, L2Object target) {
        if (player == null || player.isInStoreMode())
            return false;

        final L2Summon activeSummon = player.getPet();
        if (activeSummon == null)
            return false;

        if (activeSummon instanceof L2PetInstance && activeSummon.getLevel() - player.getLevel() > 20) {
            player.sendPacket(SystemMessageId.PET_TOO_HIGH_TO_CONTROL);
            return false;
        }

        if (activeSummon.isOutOfControl()) {
            player.sendPacket(SystemMessageId.PET_REFUSING_ORDER);
            return false;
        }

        final L2Skill skill = activeSummon.getKnownSkill(skillId);
        if (skill == null)
            return false;

        if (skill.isOffensive() && player == target)
            return false;

        activeSummon.setTarget(target);
        activeSummon.useMagic(skill, false, false);
        return true;
    }

    private void calculatePotions() {
        if (percentageHpIsLessThan() < player.getHpPotionPercentage())
            forceUseItem(1539);

        if (percentageMpIsLessThan() < player.getMpPotionPercentage())
            forceUseItem(728);
    }

    private void forceUseItem(int itemId) {
        final L2ItemInstance potion = player.getInventory().getItemByItemId(itemId);
        if (potion == null)
            return;

        final IItemHandler handler = ItemHandler.getInstance().getItemHandler(potion.getItemId());
        if (handler != null)
            handler.useItem(player, potion);
    }
}
