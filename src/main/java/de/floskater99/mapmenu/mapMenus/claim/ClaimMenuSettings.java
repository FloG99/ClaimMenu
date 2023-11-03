package de.floskater99.mapmenu.mapMenus.claim;

import com.google.common.collect.Sets;
import de.floskater99.mapmenu.Main;
import de.floskater99.mapmenu.MapMenu;
import de.floskater99.mapmenu.MapMenuAPI;
import de.floskater99.mapmenu.fontfix.MinecraftFontCopy;
import de.floskater99.mapmenu.gui.*;
import de.floskater99.mapmenu.gui.Frame;
import de.floskater99.mapmenu.packetwrapper.ExperienceWrapper;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MinecraftFont;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import static de.floskater99.mapmenu.gui.ElementProperty.*;

public class ClaimMenuSettings extends MapMenu {
    private final Collection<Team> allTeams = TeamController.teams.values();
    private Team team; // Current team for the team frame
    private static BufferedImage ownerIcon;
    private static BufferedImage memberIcon;
    private static BufferedImage editIcon;
    private static BufferedImage makeOwnerIcon;
    private static BufferedImage kickMemberIcon;
    private static BufferedImage deleteIcon;
    private static BufferedImage leaveTeamIcon;
    private static BufferedImage hamburgerMenuIcon;
    private static BufferedImage heartIcon;
    private static BufferedImage backIcon;
    private static BufferedImage arrowRightIcon;
    private Frame teamFrame;
    private Frame teamMemberFrame;
    private Frame teamCreationFrame;
    private Frame teamEditFrame;
    private Frame menuFrame;

    private Frame startPage;
    private Frame invitesFrame;

    static {
        try {
            ownerIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/Crown.png")));
            memberIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/Diamond.png")));
            editIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/Edit.png")));
            makeOwnerIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/MakeOwner.png")));
            kickMemberIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/KickMember.png")));
            deleteIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/Delete.png")));
            leaveTeamIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/Leave.png")));
            hamburgerMenuIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/Hamburger.png")));
            heartIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/Heart.png")));
            backIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/Back.png")));
            arrowRightIcon = ImageIO.read(Objects.requireNonNull(ClaimMenuSettings.class.getResourceAsStream("/images/ArrowRight.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ClaimMenuSettings(Player player, ItemStack map) {
        super(player, map);
    }
    
    private List<Team> getPlayerTeams() {
        return allTeams.stream().filter(team -> team.members.contains(player.getUniqueId())).toList();
    }

    @Override
    protected void onLoad() {
        canvas.backgroundColor = new Color(43, 43, 43);

        startPage = getStartPageFrame();
        window.showFrame(startPage);
    }

    private Frame getStartPageFrame() {
        Frame frame = new Frame(0, 0, 127, 127);

        Element closeButton = new ElementBuilder(117, 2, 7, 7)
            .setText("x", new Color(255, 255, 255), TextAlignment.TOP_RIGHT)
            .onClick(() -> {
                MapMenuAPI.openMenu(player, ClaimMenu.class);
            })
            .build();
        frame.addElement(closeButton);
        
        Element invites = new ElementBuilder(3, 14, 121, 16)
            .setText("Invites", new Color(255, 255, 255), TextAlignment.CENTER_CENTER)
            .setBorder(1, new Color(255, 255, 255), true)
            .setBackgroundColor(new Color(110, 110, 255))
            .onClick(() -> {
                window.hideFrame(frame);
                invitesFrame = getInvitesFrame();
                window.showFrame(invitesFrame);
            })
            .build();
        invites.setHoverProperty(BACKGROUND_COLOR, new Color(50, 50, 255));
        frame.addElement(invites);

        int inviteCount = TeamController.getInvites(player.getUniqueId()).size();
        if (inviteCount > 0) {
            Element inviteCountItem = new ElementBuilder(124 - MinecraftFont.Font.getWidth("" + inviteCount), 10, MinecraftFont.Font.getWidth("" + inviteCount) + 2, 9)
                .setText("" + inviteCount, new Color(255, 255, 255), TextAlignment.CENTER_CENTER)
                .setBackgroundColor(new Color(220, 0, 0))
                .build();

            frame.addElement(inviteCountItem);
        }

        int currY = 37;
        int textElementHeight = 15;
        int textElementMargin = 6;

        List<Team> teams = getPlayerTeams();
        
        for (Team team : teams) {
            Element teamElementHighlight = new ElementBuilder(3, currY, 2, textElementHeight)
                    .setBackgroundColor(team.teamColor)
                    .build();
            Element teamElementArrow = new ElementBuilder(119, currY + textElementHeight/2 - 2, 3, 5)
                    .setBackgroundImage(arrowRightIcon, false)
                    .setHidden(true)
                    .build();
            Element teamElement = new ElementBuilder(3, currY, 121, textElementHeight)
                    .setText(team.teamName, new Color(255, 255, 255), TextAlignment.CENTER_LEFT)
                    .setBackgroundColor(new Color(20, 20, 20))
                    .setPadding(0, 0, 0, 6)
                    .onClick(() -> {
                        this.team = team;

                        window.hideFrame(frame);
                        teamFrame = getTeamFrame();
                        window.showFrame(teamFrame);
                    })
                    .onHoverStart(() -> teamElementArrow.setStyleProperty(HIDDEN, false))
                    .onHoverStop(() -> teamElementArrow.setStyleProperty(HIDDEN, true))
                    .build();
            teamElement.setHoverProperty(TEXT_COLOR, team.teamMapColor);

            frame.addElement(teamElement);
            frame.addElement(teamElementHighlight);
            frame.addElement(teamElementArrow);

            currY += textElementHeight;
            currY += textElementMargin;
        }

        if (teams.size() < 2) {
            Element createNewTeamButton = new ElementBuilder(29, 106, 70, 12)
                .setText("Create Team", new Color(255, 255, 255), TextAlignment.CENTER_CENTER)
                .setBorder(1, new Color(255, 255, 255), false)
                .setBackgroundColor(new Color(20, 20, 20))
                .setBorderRadius(1)
                .build();
            //createNewTeamButton.setHoverProperty(BACKGROUND_COLOR, new Color(255, 255, 255));
            //createNewTeamButton.setHoverProperty(TEXT_COLOR, MapPalette.matchColor(new Color(0, 0, 0)));
            createNewTeamButton.setHoverProperty(WIDTH, createNewTeamButton.get(WIDTH) + 2);
            createNewTeamButton.setHoverProperty(X, createNewTeamButton.get(X) - 1);
            createNewTeamButton.setHoverProperty(HEIGHT, createNewTeamButton.get(HEIGHT) + 2);
            createNewTeamButton.setHoverProperty(Y, createNewTeamButton.get(Y) - 1);
            createNewTeamButton.onClick(() -> {
                window.hideFrame(frame);
                teamCreationFrame = getTeamCreationFrame(false);
                window.showFrame(teamCreationFrame);
            });

            frame.addElement(createNewTeamButton);
        }

        return frame;
    }

    private Frame getInvitesFrame() {
        Frame frame = new Frame(0, 0, 127, 127);

        Element backButton = new ElementBuilder(2, 2, 9, 9)
            .setBackgroundImage(backIcon, false)
            .onClick(() -> {
                window.hideFrame(frame);
                startPage = getStartPageFrame();
                window.showFrame(startPage);
            })
            .build();
        frame.addElement(backButton);

        int textElementHeight = 15;
        int textElementMargin = 6;

        Set<Team> teamInvites = TeamController.getInvites(player.getUniqueId());
        if (!teamInvites.isEmpty()) {
            int currY = 13;

            for (Team inviteTeam : teamInvites) {
                Element deleteInvite = new ElementBuilder(94, currY + 3, 7, 7)
                    .setText("x", new Color(255, 0, 0), TextAlignment.CENTER_CENTER)
                    .setHidden(true)
                    .onClick(() -> {
                        TeamController.getInvites(player.getUniqueId()).remove(inviteTeam);

                        window.hideFrame(invitesFrame);
                        invitesFrame = getInvitesFrame();
                        window.showFrame(invitesFrame);
                    })
                    .build();

                Element teamInviteElement = new ElementBuilder(3, currY, 120, textElementHeight)
                    .setText(inviteTeam.teamName, new Color(255, 255, 255), TextAlignment.CENTER_LEFT)
                    .setBackgroundColor(inviteTeam.teamColor)
                    .setPadding(0, 0, 0, 2)
                    .setBorder(1, inviteTeam.teamColor, false)
                    .setBorderRadius(1)
                    .onClick(() -> TeamController.addMemberToTeamWrapper(inviteTeam, player.getUniqueId()))
                    .onHoverStart(() -> deleteInvite.setStyleProperty(HIDDEN, false))
                    .onHoverStop(() -> deleteInvite.setStyleProperty(HIDDEN, true))
                    .build();
                teamInviteElement.setHoverProperty(BORDER_COLOR, new Color(255, 255, 255));

                frame.addElement(teamInviteElement);
                frame.addElement(deleteInvite);

                currY += textElementHeight;
                currY += textElementMargin;
            }
        } else {
            Element noMembersElement = new ElementBuilder(0, 0, 127, 80)
                .setText("No team invites.", new Color(255, 255, 255), TextAlignment.CENTER_CENTER)
                .build();
            frame.addElement(noMembersElement);
        }

        return frame;
    }

    private void askForConfirmation(String question, Runnable onConfirm) {
        Frame confirmFrame = new Frame(0, 0, 128, 128);

        Element darkenBackground = new ElementBuilder(0, 0, 128, 128)
            .setBackgroundColor(new Color(0, 0, 0, 100))
            .build();
        darkenBackground.onClick(() -> {
        }); // Disable clicking on things in the background
        darkenBackground.onHoverStart(() -> {
        }); // Disable hover events on things in the background
        confirmFrame.addElement(darkenBackground);

        Element questionEl = new ElementBuilder(14, 39, 100, 50)
            .setBackgroundColor(new Color(0, 0, 0))
            .setText(question, MapPalette.matchColor(new Color(255, 255, 255)), TextAlignment.TOP_CENTER)
            .setPadding(5, 0, 0, 0)
            .setBorder(1, new Color(255, 255, 255), false)
            .build();
        confirmFrame.addElement(questionEl);

        Element yes = new ElementBuilder(14, 89 - 16, 50, 16)
            .setText("Yes", MapPalette.matchColor(new Color(0, 255, 0)), TextAlignment.CENTER_CENTER)
            .build();
        yes.setHoverProperty(TEXT_COLOR, MapPalette.matchColor(new Color(255, 255, 255)));
        yes.setHoverProperty(BACKGROUND_COLOR, new Color(0, 255, 0));
        yes.onClick(() -> {
            window.hideFrame(confirmFrame);
            onConfirm.run();
        });
        confirmFrame.addElement(yes);

        Element no = new ElementBuilder(64, 89 - 16, 50, 16)
            .setText("No", MapPalette.matchColor(new Color(255, 0, 0)), TextAlignment.CENTER_CENTER)
            .build();
        no.setHoverProperty(TEXT_COLOR, MapPalette.matchColor(new Color(255, 255, 255)));
        no.setHoverProperty(BACKGROUND_COLOR, new Color(255, 0, 0));
        no.onClick(() -> {
            window.hideFrame(confirmFrame);
        });
        confirmFrame.addElement(no);

        window.showFrame(confirmFrame);
    }

    private Frame getMenuFrame() {
        Frame frame = new Frame(0, 0, 127, 127);
        
        Element closeButton = new ElementBuilder(117, 2, 7, 7)
            .setText("x", new Color(255, 255, 255), TextAlignment.TOP_RIGHT)
            .onClick(() -> {
                window.hideFrame(frame);
                teamFrame = getTeamFrame();
                window.showFrame(teamFrame);
            })
            .build();
        frame.addElement(closeButton);

        Element alliesButton = new ElementBuilder(24, 3, 80, 15)
            .setText("Allies", new Color(0, 255, 0), TextAlignment.CENTER_LEFT)
            .setPadding(0, 0, 0, 12)
            .setBackgroundColor(new Color(0, 0, 0))
            .setBorder(1, new Color(0, 255, 0), true)
            .setBorderRadius(2)
            .onClick(() -> {
                // Only the owner can edit the allies
                if (!team.owner.equals(player.getUniqueId())) {
                    return;
                }

                player.sendMessage("To be implemented.");
            })
            .build();
        Element alliesIcon = new ElementBuilder(26, 6, 9, 8)
            .setBackgroundImage(heartIcon, false)
            .build();

        Element editButton = new ElementBuilder(24, 24, 80, 15)
            .setText("Edit Team", new Color(255, 255, 0), TextAlignment.CENTER_LEFT)
            .setPadding(0, 0, 0, 12)
            .setBackgroundColor(new Color(0, 0, 0))
            .setBorder(1, new Color(255, 255, 0), true)
            .setBorderRadius(2)
            .onClick(() -> {
                // Only the owner can edit the team
                if (!team.owner.equals(player.getUniqueId())) {
                    return;
                }

                window.hideFrame(frame);
                teamEditFrame = getTeamCreationFrame(true);
                window.showFrame(teamEditFrame);
            })
            .build();
        editButton.setHoverProperty(BACKGROUND_COLOR, new Color(50, 50, 50));
        Element editIconElement = new ElementBuilder(27, 28, 7, 7)
            .setBackgroundImage(editIcon, false)
            .build();
        Element editIconTip = new ElementBuilder(27, 34, 1, 1)
            .setBackgroundColor(team.teamColor)
            .build();

        Element deleteButton = new ElementBuilder(24, 45, 80, 15)
            .setText("Delete Team", new Color(255, 0, 0), TextAlignment.CENTER_LEFT)
            .setPadding(0, 0, 0, 12)
            .setBackgroundColor(new Color(0, 0, 0))
            .setBorder(1, new Color(255, 0, 0), true)
            .setBorderRadius(2)
            .onClick(() -> {
                // Only the owner can delete the team
                if (!team.owner.equals(player.getUniqueId())) {
                    return;
                }

                askForConfirmation("Delete this team?", () -> {
                    Team deletedTeam = team;
                    team = null;
                    TeamController.deleteTeamWrapper(deletedTeam);

                    window.hideFrame(frame);
                    startPage = getStartPageFrame();
                    window.showFrame(startPage);
                });
            })
            .build();
        deleteButton.setHoverProperty(BACKGROUND_COLOR, new Color(50, 50, 50));
        Element deleteIconElement = new ElementBuilder(27, 49, 7, 7)
            .setBackgroundImage(deleteIcon, false)
            .build();

        frame.addElement(alliesButton);
        frame.addElement(alliesIcon);
        frame.addElement(editButton);
        frame.addElement(editIconElement);
        frame.addElement(editIconTip);
        frame.addElement(deleteButton);
        frame.addElement(deleteIconElement);

        return frame;
    }

    private Frame getTeamFrame() {
        Frame frame = new Frame(0, 0, 128, 128);

        Element header = new ElementBuilder(0, 0, 128, 14)
            .setBackgroundColor(new Color(0, 0, 0))
            .setText(team.teamName, team.teamMapColor, TextAlignment.CENTER_CENTER, false)
            .build();
        frame.addElement(header);

        Element backButton = new ElementBuilder(2, 2, 9, 9)
            .setBackgroundImage(backIcon, false)
            .onClick(() -> {
                window.hideFrame(frame);
                startPage = getStartPageFrame();
                window.showFrame(startPage);
            })
            .build();
        frame.addElement(backButton);

        Element leaveButton = new ElementBuilder(117, 4, 7, 7)
            .setBackgroundImage(leaveTeamIcon, false)
            .build();
        leaveButton.onClick(() -> {
            askForConfirmation("Leave this team?", () -> {
                TeamController.removeMemberFromTeamWrapper(team, player.getUniqueId());
            });
        });

        Element menuButton = new ElementBuilder(118, 5, 5, 5)
            .setBackgroundImage(hamburgerMenuIcon, false)
            .build();
        menuButton.onClick(() -> {
            window.hideFrame(teamFrame);
            menuFrame = getMenuFrame(); // Admin true / false
            window.showFrame(menuFrame);
        });


        int textElementHeight = 12;
        int textElementMargin = 4;

        String ownerName = Bukkit.getOfflinePlayer(team.owner).getName();
        if (ownerName == null) {
            ownerName = "?";
        }

        Element ownerIconElement = new ElementBuilder(2, 19, 19, textElementHeight)
            .setBackgroundImage(ownerIcon, false)
            .build();

        Element ownerElement = new ElementBuilder(24, 19, 100, textElementHeight)
            .setText(ownerName, MapPalette.matchColor(new Color(255, 255, 255)), TextAlignment.BOTTOM_LEFT)
            .build();

        List<UUID> teamMembers = team.members.stream().filter(member -> !member.equals(team.owner)).toList();
        int teamMembersFrameHeight = Math.min(4, teamMembers.size()) * (textElementMargin + textElementHeight);
        teamMemberFrame = new Frame(0, 34, 127, teamMembersFrameHeight);

        if (!teamMembers.isEmpty()) {
            int currY = 0;

            for (UUID teamMember : teamMembers) {
                Element teamMemberIcon = new ElementBuilder(3, currY + 1, 15, 11)
                    .setBackgroundImage(memberIcon, false)
                    .build();

                Element teamMemberElement = new ElementBuilder(24, currY, 120, textElementMargin + textElementHeight)
                    .setText(Bukkit.getOfflinePlayer(teamMember).getName(), MapPalette.matchColor(new Color(210, 220, 230)), TextAlignment.CENTER_LEFT)
                    .build();

                // Team owner actions (Kick or Promote member)
                if (team.owner.equals(player.getUniqueId())) {
                    Element makeOwner = new ElementBuilder(128 - 25, currY + 3, 8, 8)
                        .setBackgroundImage(makeOwnerIcon, false)
                        .setHidden(true)
                        .build();
                    makeOwner.onClick(() -> {
                        askForConfirmation("Make " + Bukkit.getOfflinePlayer(teamMember).getName() + " team leader?", () -> {
                            if (!team.owner.equals(player.getUniqueId())) {
                                return;
                            }

                            team.setOwner(teamMember);
                            team.members.forEach(member -> MapMenuAPI.liveUpdateMenu(Bukkit.getPlayer(member), ClaimMenuSettings.class, "teamMembers"));
                        });
                    });
                    teamMemberFrame.addElement(makeOwner);

                    Element kickMember = new ElementBuilder(128 - 15, currY + 5, 7, 7)
                        .setBackgroundImage(kickMemberIcon, false)
                        .setHidden(true)
                        .build();
                    kickMember.onClick(() -> {
                        askForConfirmation("Kick " + Bukkit.getOfflinePlayer(teamMember).getName() + "?", () -> {
                            if (!team.owner.equals(player.getUniqueId())) {
                                return;
                            }

                            TeamController.removeMemberFromTeamWrapper(team, teamMember);
                        });
                    });
                    teamMemberFrame.addElement(kickMember);

                    teamMemberElement.onHoverStart(() -> {
                        makeOwner.setStyleProperty(HIDDEN, false);
                        kickMember.setStyleProperty(HIDDEN, false);
                    });

                    teamMemberElement.onHoverStop(() -> {
                        makeOwner.setStyleProperty(HIDDEN, true);
                        kickMember.setStyleProperty(HIDDEN, true);
                    });
                }

                currY += textElementHeight;
                currY += textElementMargin;

                teamMemberFrame.addElement(teamMemberIcon);
                teamMemberFrame.addElement(teamMemberElement);
            }
        }
        frame.addSubFrame(teamMemberFrame);

        Element inviteElement = new ElementBuilder(3, 34 + teamMembersFrameHeight + 5, MinecraftFontCopy.Font.getWidth("+ Invite Player") + 4, 8)
            .setText("+ Invite Player", MapPalette.matchColor(255, 255, 255), TextAlignment.CENTER_CENTER)
            .setBorder(1, new Color(49, 187, 26), false)
            .setBorderRadius(2)
            .setBackgroundColor(new Color(49, 187, 26))
            .build();
        inviteElement.setHoverProperty(BACKGROUND_COLOR, new Color(33, 124, 18));
        inviteElement.setHoverProperty(BORDER_COLOR, new Color(33, 124, 18));
        inviteElement.onClick(() -> openAnvilTextEdit(player, "Invite Player", " ", (String name) -> {
            String playerToInvite = name.strip().replace(" ", "");
            Player invitedPlayer = Bukkit.getPlayer(playerToInvite);
            if (invitedPlayer == null) {
                player.spigot().sendMessage(ChatMessageType.SYSTEM, new ComponentBuilder("Player not found.").color(ChatColor.RED).create());
                return List.of();
            }

            player.spigot().sendMessage(ChatMessageType.SYSTEM, new ComponentBuilder("Invited ").color(ChatColor.GREEN).append(invitedPlayer.getName()).color(ChatColor.AQUA).append(" to your team.").color(ChatColor.GREEN).create());
            invitePlayerToTeam(invitedPlayer);
            invitedPlayer.sendMessage("§8[§bTeams§8] §7You got invited to §b" + team.teamName);

            return List.of(AnvilGUI.ResponseAction.close());
        }));

        frame.addElement(inviteElement);
        // Only the owner can edit or delete the team
        if (team.owner.equals(player.getUniqueId())) {
            frame.addElement(menuButton);
        } else {
            frame.addElement(leaveButton);
        }
        frame.addElement(ownerIconElement);
        frame.addElement(ownerElement);
        return frame;
    }

    private Frame getTeamCreationFrame(boolean isEdit) {
        Runnable onSubmit = () -> {
            window.hideFrame(isEdit ? teamEditFrame : teamCreationFrame);
            teamFrame = getTeamFrame();
            window.showFrame(teamFrame);
        };

        Team team = isEdit ? this.team : new Team(UUID.randomUUID(), player.getUniqueId(), Sets.newHashSet(player.getUniqueId()), "", null, 0);
        Frame frame = new Frame(0, 0, 128, 128);

        int beforeEditHash = team.hashCode();

        Element teamName = new ElementBuilder(64 - 52, 10, 104, 16)
            .setText(isEdit ? team.teamName : "<Enter team name>", MapPalette.matchColor(0, 0, 0), isEdit ? TextAlignment.CENTER_LEFT : TextAlignment.CENTER_CENTER)
            .setBackgroundColor(new Color(255, 255, 255))
            .setPadding(0, 0, 0, 3)
            .setBorder(1, new Color(0, 0, 0), true)
            .build();
        teamName.setHoverProperty(TEXT_COLOR, MapPalette.DARK_GRAY);
        teamName.onClick(() -> {
            String teamNameText = teamName.get(TEXT);
            openAnvilTextEdit(player, "Team Name", teamNameText.equals("<Enter team name>") ? " " : teamNameText, (String name) -> {
                String desiredTeamName = name.strip();
                if (desiredTeamName.length() > 20 || MinecraftFont.Font.getWidth(desiredTeamName) > 80) {
                    player.spigot().sendMessage(ChatMessageType.SYSTEM, new ComponentBuilder("Team name is too long.").color(ChatColor.RED).create());
                    return List.of();
                } else if (desiredTeamName.isEmpty()) {
                    player.spigot().sendMessage(ChatMessageType.SYSTEM, new ComponentBuilder("Team name cannot be empty.").color(ChatColor.RED).create());
                    return List.of();
                } else if (allTeams.stream().anyMatch(team_ -> team_.teamName.equals(desiredTeamName))) {
                    player.spigot().sendMessage(ChatMessageType.SYSTEM, new ComponentBuilder("This name is already taken.").color(ChatColor.RED).create());
                    return List.of();
                }

                team.teamName = desiredTeamName;
                this.team = team;
                teamName.setDefaultProperty(TEXT, desiredTeamName);
                teamName.setDefaultProperty(TEXT_ALIGNMENT, TextAlignment.CENTER_LEFT);
                MapMenuAPI.updateMap(player, this);

                return List.of(AnvilGUI.ResponseAction.close());
            });
        });

        Color[] teamColors = new Color[]{
            new Color(26, 255, 227),
            new Color(0, 57, 255),
            new Color(178, 76, 216),
            new Color(242, 127, 165),
            new Color(255, 0, 0),
            new Color(255, 114, 0),
            new Color(255, 242, 0),
            new Color(0, 217, 58),
            new Color(0, 124, 0),
            new Color(20, 180, 133)
        };
        Element[] colorElements = new Element[10];

        for (int i = 0; i < 10; i++) {
            Color color = teamColors[i];
            Element element = new ElementBuilder(64 - 52 + (i % 5) * 21, 40 + (i / 5) * 21, 20, 20)
                .setBackgroundColor(color)
                .setBorder(1, new Color(255, 255, 255), true)
                .build();
            element.setHoverProperty(BORDER_COLOR, new Color(70, 70, 70));
            if (color.equals(team.teamColor)) {
                element.setStyleProperty(BORDER_COLOR, new Color(0, 0, 0));

            }

            int current = i;
            element.onClick(() -> {
                Arrays.stream(colorElements).forEach(colorElement -> colorElement.setStyleProperty(BORDER_COLOR, null));
                element.setStyleProperty(BORDER_COLOR, new Color(0, 0, 0));
                team.setTeamColor(teamColors[current]);
            });

            colorElements[i] = element;

        }

        Element createTeam = new ElementBuilder(64 - 35, 128 - 18, 70, 12)
            .setText(isEdit ? "Confirm" : "Create Team", MapPalette.matchColor(255, 255, 255), TextAlignment.CENTER_CENTER)
            .setBorder(1, new Color(49, 187, 26), false)
            .setBorderRadius(2)
            .setBackgroundColor(new Color(49, 187, 26))
            .build();
        createTeam.setHoverProperty(BACKGROUND_COLOR, new Color(33, 124, 18));
        createTeam.setHoverProperty(BORDER_COLOR, new Color(33, 124, 18));
        createTeam.onClick(() -> {
            if (team.teamName == null || team.teamName.isBlank()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("Please choose a name for your team.").color(ChatColor.RED).create());
                return;
            }

            if (team.teamColor == null) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("Please choose a color for your team.").color(ChatColor.RED).create());
                return;
            }

            if (allTeams.stream().anyMatch(team_ -> team_ != team && team_.teamName.equalsIgnoreCase(team.teamName))) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("Team name is already taken.").color(ChatColor.RED).create());
                return;
            }

            onSubmit.run();
            if (!isEdit) {
                TeamController.addTeam(team);
            } else if (team.hashCode() != beforeEditHash) {
                TeamController.updateTeam(team);
            }
        });

        frame.addElement(teamName);
        frame.addElement(createTeam);
        for (Element element : colorElements) {
            frame.addElement(element);
        }
        return frame;
    }
    
    private void invitePlayerToTeam(OfflinePlayer invitedPlayer) {
        Set<Team> playerInvites = TeamController.getInvites(invitedPlayer.getUniqueId());
        playerInvites.add(team);
        MapMenuAPI.liveUpdateMenu(invitedPlayer, ClaimMenuSettings.class, "invites");
    }

    @Override
    protected void onScroll(int direction) {
        if (window.frames.contains(teamFrame)) {
            teamMemberFrame.scroll(direction * 16);
        }

        if (window.frames.contains(invitesFrame)) {
            invitesFrame.scroll(direction * 16);
        }
    }

    private void sendFakeXPUpdate(Player player, int fakeXP) {
        ExperienceWrapper expFake = new ExperienceWrapper();
        expFake.setLevel((short) (player.getLevel() + fakeXP));
        expFake.sendPacket(player);
    }

    private void openAnvilTextEdit(Player player, String title, String defaultValue, Function<String, List<AnvilGUI.ResponseAction>> onEdit) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta itemMeta = item.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(defaultValue);
        item.setItemMeta(itemMeta);

        boolean addFakeLevel = player.getLevel() == 0;
        if (addFakeLevel) {
            this.sendFakeXPUpdate(player, 1);
        }

        new AnvilGUI.Builder()
            .title(title)
            .itemLeft(item)
            .onClick((slot, stateSnapshot) -> {
                if (slot != AnvilGUI.Slot.OUTPUT) {
                    return Collections.emptyList();
                }

                return onEdit.apply(stateSnapshot.getText());
            })
            .onClose((closeListener) -> {
                if (addFakeLevel) {
                    this.sendFakeXPUpdate(player, 0);
                }
            })
            .plugin(Main.instance)
            .open(player);
    }

    @Override
    public void liveUpdate(String key) {
        switch (key) {
            case "invites" -> updateInvites();
            case "teamMembers" -> updateTeamMembers();
            case "kicked" -> onKicked();
            case "added" -> onAdded();
        }
    }

    private void onKicked() {
        if (!window.frames.contains(teamFrame)) {
            return;
        }

        team = null;
        window.hideFrame(teamFrame);
        startPage = getStartPageFrame();
        window.showFrame(startPage);
    }

    private void onAdded() {
        if (window.frames.contains(invitesFrame)) {
            window.hideFrame(invitesFrame);
            invitesFrame = getInvitesFrame();
            window.showFrame(invitesFrame);
        }
        
        if (window.frames.contains(startPage)) {
            window.hideFrame(startPage);
            startPage = getStartPageFrame();
            window.showFrame(startPage);
        }
    }

    private void updateInvites() {
        this.onAdded();
    }

    private void updateTeamMembers() {
        if (!window.frames.contains(teamFrame)) {
            return;
        }

        window.hideFrame(teamFrame);
        teamFrame = getTeamFrame();
        window.showFrame(teamFrame);
    }

    @Override
    protected void onMouseMove(int x, int y) {

    }

    @Override
    protected void onRender() {

    }

    @Override
    public boolean doRenderBase() {
        return false;
    }

    @Override
    public String getMapDisplayName() {
        return "§f- §6Claim Menu Settings§f -";
    }
}
