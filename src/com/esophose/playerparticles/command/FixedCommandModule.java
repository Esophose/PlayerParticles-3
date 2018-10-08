package com.esophose.playerparticles.command;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.esophose.playerparticles.manager.DataManager;
import com.esophose.playerparticles.manager.LangManager;
import com.esophose.playerparticles.manager.ParticleManager;
import com.esophose.playerparticles.manager.PermissionManager;
import com.esophose.playerparticles.manager.LangManager.Lang;
import com.esophose.playerparticles.particles.FixedParticleEffect;
import com.esophose.playerparticles.particles.PPlayer;
import com.esophose.playerparticles.particles.ParticleEffect;
import com.esophose.playerparticles.particles.ParticleEffect.NoteColor;
import com.esophose.playerparticles.particles.ParticleEffect.OrdinaryColor;
import com.esophose.playerparticles.particles.ParticleEffect.ParticleProperty;
import com.esophose.playerparticles.particles.ParticlePair;
import com.esophose.playerparticles.styles.api.ParticleStyle;
import com.esophose.playerparticles.styles.api.ParticleStyleManager;
import com.esophose.playerparticles.util.ParticleUtils;

public class FixedCommandModule implements CommandModule {

    public void onCommandExecute(PPlayer pplayer, String[] args) {
        Player p = pplayer.getPlayer();

        if (!PermissionManager.canUseFixedEffects(p)) {
            LangManager.sendMessage(p, Lang.FIXED_NO_PERMISSION);
            return;
        }

        if (args.length == 0) { // General information on command
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_CREATE);
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_REMOVE);
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_LIST);
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_INFO);
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_CLEAR);
            return;
        }

        String cmd = args[0];

        String[] cmdArgs = new String[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            cmdArgs[i - 1] = args[i];
        }
        
        switch (cmd.toLowerCase()) {
        case "create":
            handleCreate(pplayer, p, cmdArgs);
            return;
        case "remove":
            handleRemove(pplayer, p, cmdArgs);
            return;
        case "list":
            handleList(pplayer, p, cmdArgs);
            return;
        case "info":
            handleInfo(pplayer, p, cmdArgs);
            return;
        case "clear":
            handleClear(pplayer, p, cmdArgs);
            return;
        default:
            LangManager.sendMessage(pplayer, Lang.FIXED_INVALID_COMMAND);
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_CREATE);
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_REMOVE);
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_LIST);
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_INFO);
            LangManager.sendMessage(p, Lang.COMMAND_DESCRIPTION_FIXED_CLEAR);
        }
    }
    
    /**
     * Handles the command /pp fixed create
     * 
     * @param pplayer The PPlayer
     * @param p The Player
     * @param args The command arguments
     */
    private void handleCreate(PPlayer pplayer, Player p, String[] args) {
        boolean reachedMax = DataManager.hasPlayerReachedMaxFixedEffects(pplayer);
        if (reachedMax) {
            LangManager.sendMessage(p, Lang.FIXED_MAX_REACHED);
            return;
        }

        if (args.length < 5) {
            LangManager.sendMessage(p, Lang.FIXED_CREATE_MISSING_ARGS, 5 - args.length);
            return;
        }

        double xPos = -1, yPos = -1, zPos = -1;
        try {
            if (args[0].startsWith("~")) {
                if (args[0].equals("~")) xPos = p.getLocation().getX();
                else xPos = p.getLocation().getX() + Double.parseDouble(args[0].substring(1));
            } else {
                xPos = Double.parseDouble(args[0]);
            }

            if (args[1].startsWith("~")) {
                if (args[1].equals("~")) yPos = p.getLocation().getY() + 1;
                else yPos = p.getLocation().getY() + 1 + Double.parseDouble(args[1].substring(1));
            } else {
                yPos = Double.parseDouble(args[1]);
            }

            if (args[2].startsWith("~")) {
                if (args[2].equals("~")) zPos = p.getLocation().getZ();
                else zPos = p.getLocation().getZ() + Double.parseDouble(args[2].substring(1));
            } else {
                zPos = Double.parseDouble(args[2]);
            }
        } catch (Exception e) {
            LangManager.sendMessage(p, Lang.FIXED_CREATE_INVALID_COORDS);
            return;
        }

        double distanceFromEffect = p.getLocation().distance(new Location(p.getWorld(), xPos, yPos, zPos));
        int maxCreationDistance = DataManager.getMaxFixedEffectCreationDistance();
        if (maxCreationDistance != 0 && distanceFromEffect > maxCreationDistance) {
            LangManager.sendMessage(p, Lang.FIXED_CREATE_OUT_OF_RANGE, maxCreationDistance + "");
            return;
        }

        ParticleEffect effect = ParticleManager.effectFromString(args[3]);
        if (effect == null) {
            LangManager.sendMessage(p, Lang.FIXED_CREATE_EFFECT_INVALID, args[3]);
            return;
        } else if (!PermissionManager.hasEffectPermission(p, effect)) {
            LangManager.sendMessage(p, Lang.FIXED_CREATE_EFFECT_NO_PERMISSION, effect.getName());
            return;
        }

        ParticleStyle style = ParticleStyleManager.styleFromString(args[4]);
        if (style == null) {
            LangManager.sendMessage(p, Lang.FIXED_CREATE_STYLE_INVALID, args[4]);
            return;
        } else if (!PermissionManager.hasStylePermission(p, style)) {
            LangManager.sendMessage(p, Lang.FIXED_CREATE_STYLE_NO_PERMISSION, args[4]);
            return;
        }

        if (!style.canBeFixed()) {
            LangManager.sendMessage(p, Lang.FIXED_CREATE_STYLE_NON_FIXABLE, style.getName());
            return;
        }

        Material itemData = null;
        Material blockData = null;
        OrdinaryColor colorData = null;
        NoteColor noteColorData = null;

        if (args.length > 5) {
            if (effect.hasProperty(ParticleProperty.COLORABLE)) {
                if (effect == ParticleEffect.NOTE) {
                    if (args[5].equalsIgnoreCase("rainbow")) {
                        noteColorData = new NoteColor(99);
                    } else {
                        int note = -1;
                        try {
                            note = Integer.parseInt(args[5]);
                        } catch (Exception e) {
                            LangManager.sendMessage(p, Lang.FIXED_CREATE_DATA_ERROR);
                            return;
                        }

                        if (note < 0 || note > 23) {
                            LangManager.sendMessage(p, Lang.FIXED_CREATE_DATA_ERROR);
                            return;
                        }

                        noteColorData = new NoteColor(note);
                    }
                } else {
                    if (args[5].equalsIgnoreCase("rainbow")) {
                        colorData = new OrdinaryColor(999, 999, 999);
                    } else {
                        int r = -1;
                        int g = -1;
                        int b = -1;

                        try {
                            r = Integer.parseInt(args[5]);
                            g = Integer.parseInt(args[6]);
                            b = Integer.parseInt(args[7]);
                        } catch (Exception e) {
                            LangManager.sendMessage(p, Lang.FIXED_CREATE_DATA_ERROR);
                            return;
                        }

                        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                            LangManager.sendMessage(p, Lang.FIXED_CREATE_DATA_ERROR);
                            return;
                        }

                        colorData = new OrdinaryColor(r, g, b);
                    }
                }
            } else if (effect.hasProperty(ParticleProperty.REQUIRES_MATERIAL_DATA)) {
                if (effect == ParticleEffect.BLOCK || effect == ParticleEffect.FALLING_DUST) {
                    Material material = null;
                    try {
                        material = ParticleUtils.closestMatch(args[5]);
                        if (material == null) material = Material.matchMaterial(args[5]);
                        if (material == null) throw new Exception();
                    } catch (Exception e) {
                        LangManager.sendMessage(p, Lang.FIXED_CREATE_DATA_ERROR);
                        return;
                    }

                    blockData = material;
                } else if (effect == ParticleEffect.ITEM) {
                    Material material = null;
                    try {
                        material = ParticleUtils.closestMatch(args[5]);
                        if (material == null) material = Material.matchMaterial(args[5]);
                        if (material == null) throw new Exception();
                    } catch (Exception e) {
                        LangManager.sendMessage(p, Lang.FIXED_CREATE_DATA_ERROR);
                        return;
                    }

                    itemData = material;
                }
            }
        }

        int nextFixedEffectId = pplayer.getNextFixedEffectId();
        ParticlePair particle = new ParticlePair(pplayer.getUniqueId(), nextFixedEffectId, effect, style, itemData, blockData, colorData, noteColorData);
        FixedParticleEffect fixedEffect = new FixedParticleEffect(p.getUniqueId(), nextFixedEffectId, p.getLocation().getWorld().getName(), xPos, yPos, zPos, particle);

        LangManager.sendMessage(p, Lang.FIXED_CREATE_SUCCESS);
        DataManager.saveFixedEffect(fixedEffect);
    }
    
    /**
     * Handles the command /pp fixed remove
     * 
     * @param pplayer The PPlayer
     * @param p The Player
     * @param args The command arguments
     */
    private void handleRemove(PPlayer pplayer, Player p, String[] args) {
        if (args.length < 1) {
            LangManager.sendMessage(p, Lang.FIXED_REMOVE_NO_ARGS);
            return;
        }

        int id = -1;
        try {
            id = Integer.parseInt(args[0]);
        } catch (Exception e) {
            LangManager.sendMessage(p, Lang.FIXED_REMOVE_ARGS_INVALID);
            return;
        }

        if (pplayer.getFixedEffectById(id) != null) {
            DataManager.removeFixedEffect(pplayer.getUniqueId(), id);
            LangManager.sendMessage(p, Lang.FIXED_REMOVE_SUCCESS, id + "");
        } else {
            LangManager.sendMessage(p, Lang.FIXED_REMOVE_INVALID, id + "");
        }
    }
    
    /**
     * Handles the command /pp fixed list
     * 
     * @param pplayer The PPlayer
     * @param p The Player
     * @param args The command arguments
     */
    private void handleList(PPlayer pplayer, Player p, String[] args) {
        List<Integer> ids = pplayer.getFixedEffectIds();
        Collections.sort(ids);

        if (ids.isEmpty()) {
            LangManager.sendMessage(p, Lang.FIXED_LIST_NONE);
            return;
        }

        String msg = "";
        boolean first = true;
        for (int id : ids) {
            if (!first) msg += ", ";
            else first = false;
            msg += id;
        }

        LangManager.sendMessage(p, Lang.FIXED_LIST_SUCCESS, msg);
    }
    
    /**
     * Handles the command /pp fixed info
     * 
     * @param pplayer The PPlayer
     * @param p The Player
     * @param args The command arguments
     */
    private void handleInfo(PPlayer pplayer, Player p, String[] args) {
        if (args.length < 1) {
            LangManager.sendMessage(p, Lang.FIXED_INFO_NO_ARGS);
            return;
        }

        int id = -1;
        try {
            id = Integer.parseInt(args[0]);
        } catch (Exception e) {
            LangManager.sendMessage(p, Lang.FIXED_INFO_INVALID_ARGS);
            return;
        }

        FixedParticleEffect fixedEffect = pplayer.getFixedEffectById(id);
        if (fixedEffect == null) {
            LangManager.sendMessage(p, Lang.FIXED_INFO_INVALID, id + "");
            return;
        }

        ParticlePair particle = fixedEffect.getParticlePair();

        DecimalFormat df = new DecimalFormat("0.##"); // Decimal formatter so the coords aren't super long
        LangManager.sendMessage(p, // @formatter:off
                                Lang.FIXED_INFO_SUCCESS,
                                fixedEffect.getId() + "",
                                fixedEffect.getLocation().getWorld().getName(),
                                df.format(fixedEffect.getLocation().getX()) + "",
                                df.format(fixedEffect.getLocation().getY()) + "",
                                df.format(fixedEffect.getLocation().getZ()) + "",
                                particle.getEffect().getName(),
                                particle.getStyle().getName(),
                                particle.getDataString()
                               ); // @formatter:on
    }
    
    /**
     * Handles the command /pp fixed clear
     * 
     * @param pplayer The PPlayer
     * @param p The Player
     * @param args The command arguments
     */
    private void handleClear(PPlayer pplayer, Player p, String[] args) {
        if (!p.hasPermission("playerparticles.fixed.clear")) {
            LangManager.sendMessage(p, Lang.FIXED_CLEAR_NO_PERMISSION);
            return;
        }

        if (args.length < 1) {
            LangManager.sendMessage(p, Lang.FIXED_CLEAR_NO_ARGS);
            return;
        }

        int radius = -1;
        try {
            radius = Math.abs(Integer.parseInt(args[0]));
        } catch (Exception e) {
            LangManager.sendMessage(p, Lang.FIXED_CLEAR_INVALID_ARGS);
            return;
        }

        ArrayList<FixedParticleEffect> fixedEffectsToRemove = new ArrayList<FixedParticleEffect>();

        for (PPlayer ppl : ParticleManager.getPPlayers())
            for (FixedParticleEffect fixedEffect : ppl.getFixedParticles())
                if (fixedEffect.getLocation().getWorld().equals(p.getLocation().getWorld()) && fixedEffect.getLocation().distance(p.getLocation()) <= radius) 
                    fixedEffectsToRemove.add(fixedEffect);

        for (FixedParticleEffect fixedEffect : fixedEffectsToRemove)
            DataManager.removeFixedEffect(fixedEffect.getOwnerUniqueId(), fixedEffect.getId());

        LangManager.sendMessage(p, Lang.FIXED_CLEAR_SUCCESS, fixedEffectsToRemove.size() + "", radius + "");
    }

    public List<String> onTabComplete(PPlayer pplayer, String[] args) {
        Player p = pplayer.getPlayer();
        List<String> matches = new ArrayList<String>();
        String[] subCommands = new String[] { "create", "remove", "list", "info", "clear" };
        
        if (args.length <= 1) {
            List<String> possibleCmds = new ArrayList<String>();
            possibleCmds.addAll(new ArrayList<String>(Arrays.asList(subCommands)));
            if (args.length == 0) matches = possibleCmds;
            else StringUtil.copyPartialMatches(args[0], possibleCmds, matches);
        } else {
            switch (args[0].toLowerCase()) {
            case "create":
                if (args.length >= 2 && args.length <= 4) {
                    List<String> possibleValues = new ArrayList<String>();
                    if (args.length == 4) {
                        possibleValues.add("~");
                    }
                    if (args.length == 3) {
                        possibleValues.add("~ ~");
                    }
                    if (args.length == 2) {
                        possibleValues.add("~ ~ ~");
                    }
                    StringUtil.copyPartialMatches(args[args.length - 1], possibleValues, matches);
                } else if (args.length == 5) {
                    StringUtil.copyPartialMatches(args[4], PermissionManager.getEffectsUserHasPermissionFor(p), matches);
                } else if (args.length == 6) {
                    StringUtil.copyPartialMatches(args[5], PermissionManager.getStylesUserHasPermissionFor(p), matches);
                } else if (args.length >= 7) {
                    ParticleEffect effect = ParticleEffect.fromName(args[4]);
                    if (effect != null) {
                        if (effect.hasProperty(ParticleProperty.COLORABLE)) {
                            List<String> possibleValues = new ArrayList<String>();
                            if (effect == ParticleEffect.NOTE) { // Note data
                                if (args.length == 7) {
                                    possibleValues.add("<0-23>");
                                    possibleValues.add("rainbow");
                                }
                            } else { // Color data
                                if (args.length <= 9 && !args[2].equalsIgnoreCase("rainbow")) {
                                    possibleValues.add("<0-255>");
                                }
                                if (args.length <= 8 && !args[2].equalsIgnoreCase("rainbow")) {
                                    possibleValues.add("<0-255> <0-255>");
                                }
                                if (args.length <= 7) {
                                    possibleValues.add("<0-255> <0-255> <0-255>");
                                    possibleValues.add("rainbow");
                                }
                            }
                            StringUtil.copyPartialMatches(args[args.length - 1], possibleValues, matches);
                        } else if (args.length == 7 && effect.hasProperty(ParticleProperty.REQUIRES_MATERIAL_DATA)) {
                            if (effect == ParticleEffect.BLOCK || effect == ParticleEffect.FALLING_DUST) { // Block material
                                matches = StringUtil.copyPartialMatches(args[6], ParticleUtils.getAllBlockMaterials(), matches);
                            } else if (effect == ParticleEffect.ITEM) { // Item material
                                matches = StringUtil.copyPartialMatches(args[6], ParticleUtils.getAllItemMaterials(), matches);
                            }
                        }
                    }
                }
                break;
            case "remove":
                StringUtil.copyPartialMatches(args[1], pplayer.getFixedEffectIds().stream().map((x) -> String.valueOf(x)).collect(Collectors.toList()), matches);
                break;
            case "list":
                break;
            case "info":
                StringUtil.copyPartialMatches(args[1], pplayer.getFixedEffectIds().stream().map((x) -> String.valueOf(x)).collect(Collectors.toList()), matches);
                break;
            case "clear":
                matches.add("<radius>");
                break;
            }
        }
        
        return matches;
    }

    public String getName() {
        return "fixed";
    }

    public Lang getDescription() {
        return Lang.COMMAND_DESCRIPTION_FIXED;
    }

    public String getArguments() {
        return "<args>";
    }

    public boolean requiresEffects() {
        return true;
    }

}
