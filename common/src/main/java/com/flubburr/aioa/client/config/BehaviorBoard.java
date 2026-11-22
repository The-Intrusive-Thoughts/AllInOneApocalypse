package com.flubburr.aioa.client.config;

import com.flubburr.aioa.behavior.AioaBehaviorGraph.Node;
import com.flubburr.aioa.behavior.AioaBehaviorGraph.NodeType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.flubburr.aioa.compat.AioaEntityHelper;
import net.minecraft.resources.ResourceLocation;

/** Drawing and picking for the editor's redstone wiring board. Coordinates are GUI pixels. */
final class BehaviorBoard {
    static final int PATINA = 0xFF91DB9C;
    static final int ACCENT = 0xFFA2E7AA;
    static final int BONE = 0xFFF1F5EF;

    enum Part {
        ROOT, CLOCK, SENSOR, TARGET, MOTION, COMBAT, GATE, EFFECT, SIGNAL, EGG, BOOK;

        private static final ResourceLocation ATLAS = AioaEntityHelper.parseResourceLocation("aioa:textures/gui/behavior_parts.png");

        void draw(GuiGraphics g, int x, int y) {
            g.blit(ATLAS, x, y, ordinal() * 16, 0, 16, 16, 176, 16);
        }

        static Part of(Node node) {
            return of(node.type);
        }

        static Part of(NodeType type) {
            return switch (type.category) {
                case "Base" -> ROOT;
                case "Events", "Flow" -> CLOCK;
                case "Sensing" -> SENSOR;
                case "Targeting" -> TARGET;
                case "Movement" -> MOTION;
                case "Combat" -> COMBAT;
                case "Conditions", "Player Conditions" -> GATE;
                case "Effects", "Presentation" -> EFFECT;
                case "Attributes", "Values", "State" -> GATE;
                case "Equipment", "Model" -> ROOT;
                case "World" -> EGG;
                case "Scripting", "Organization" -> BOOK;
                default -> SIGNAL;
            };
        }
    }

    static void card(GuiGraphics g, Font font, Node node, String title,
                     int x, int y, int w, int h, boolean selected) {
        int right = x + w, bottom = y + h;
        Part part = Part.of(node);
        int border = selected ? ACCENT : 0xFF506356;
        g.fill(x + 2, y + 3, right + 2, bottom + 3, 0x88050907);
        g.fill(x, y, right, bottom, border);
        g.fill(x + 1, y + 1, right - 1, bottom - 1, 0xFF1D2822);
        if (w < 54 || h < 25) {
            if (w >= 18 && h >= 18) part.draw(g, x + (w - 16) / 2, y + (h - 16) / 2);
            return;
        }
        int band = Math.min(21, h - 13);
        int tint = switch (part) {
            case ROOT, CLOCK -> 0xFF375942;
            case SENSOR, TARGET, GATE -> 0xFF2B504B;
            case MOTION, COMBAT -> 0xFF3B4938;
            default -> 0xFF344A3E;
        };
        g.fill(x + 1, y + 1, right - 1, y + band, tint);
        boolean showIcon = w >= 90 && band >= 16;
        if (showIcon) part.draw(g, x + 5, y + (band - 16) / 2);
        int textX = x + (showIcon ? 25 : 7);
        if (!node.parameters.containsKey("_label") && font.width(title) > right - textX - 4) {
            title = switch (node.type) {
                case FIND_NEAREST_PLAYER -> "Find player";
                case MOVE_TO_TARGET -> "Pursue";
                case CAN_SEE_TARGET -> "Line of sight";
                case TARGET_IN_RANGE -> "In range";
                case EVERY_TICKS -> "Timer";
                case ATTACK_TARGET -> "Attack";
                default -> title;
            };
        }
        g.drawString(font, font.plainSubstrByWidth(title, Math.max(1, right - textX - 4)), textX, y + (band - 8) / 2, BONE, false);
        if (h < 37 || w < 76) return;
        String detail = switch (node.type) {
            case MOB_BASE -> node.parameters.getOrDefault("health", "20") + " HP";
            case FIND_NEAREST_PLAYER, TARGET_IN_RANGE, ATTACK_TARGET -> node.parameters.getOrDefault("range", "?") + " blocks";
            case EVERY_TICKS, COOLDOWN -> node.parameters.getOrDefault("ticks", "?") + " ticks";
            case MOVE_TO_TARGET, FLEE_TARGET -> "×" + node.parameters.getOrDefault("speed", "1");
            case WANDER -> node.parameters.getOrDefault("radius", "8") + " blocks";
            default -> node.type.category;
        };
        g.drawString(font, font.plainSubstrByWidth(detail, (w - 14) / 2), x + 8,
                y + band + (h - band - 8) / 2, 0xFFB8C7B9, false);
    }

    static void jack(GuiGraphics g, int x, int y, int color) {
        g.fill(x - 3, y - 2, x + 3, y + 3, 0xFF111A15);
        g.fill(x - 2, y - 3, x + 2, y + 4, color);
        g.fill(x - 3, y - 2, x + 3, y + 3, color);
        g.fill(x - 1, y - 1, x + 1, y + 2, 0xFF243128);
    }

    static int wireColor(String port) {
        return switch (port == null ? "next" : port) {
            case "missing", "false", "fail", "waiting" -> 0xFF83AAA3;
            case "true", "found", "ready", "success" -> 0xFFA8DF99;
            default -> 0xFFC3D2C5;
        };
    }

    record Wire(int x1, int y1, int x2, int y2) {
        private interface Segment { boolean visit(int ax, int ay, int bx, int by); }

        private int handle() { return Math.max(22, Math.min(112, Math.abs(x2 - x1) / 2)); }
        private int lift() { return x2 <= x1 && Math.abs(y1 - y2) < 24 ? 36 : 0; }

        boolean overlaps(int left, int top, int right, int bottom) {
            return Math.max(x1 + handle(), x2) >= left && Math.min(x2 - handle(), x1) <= right
                    && Math.max(y1, y2) >= top && Math.min(y1, y2) - lift() <= bottom;
        }

        private boolean visit(Segment segment) {
            if (y1 == y2 && x2 >= x1) return segment.visit(x1, y1, x2, y2);
            int reach = handle();
            if (x2 < x1) {
                int lane = (y1 + y2) / 2 - lift();
                int bend = Math.min(reach, 22);
                return bend(segment, x1, y1, lane, bend) || segment.visit(x1, lane, x2, lane)
                        || bend(segment, x2, lane, y2, -bend);
            }
            int steps = Math.max(8, Math.min(32, (Math.abs(y2 - y1) + Math.abs(x2 - x1)) / 12));
            int ax = x1, ay = y1;
            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps, u = 1.0 - t;
                double blend = t * t * (3 - 2 * t);
                int bx = (int) Math.round(x1 + (x2 - (double) x1) * blend + 3 * u * t * (u - t) * reach);
                int by = (int) Math.round(y1 + (y2 - (double) y1) * blend);
                if ((ax != bx || ay != by) && segment.visit(ax, ay, bx, by)) return true;
                ax = bx;
                ay = by;
            }
            return false;
        }

        private static boolean bend(Segment segment, int x, int fromY, int toY, int reach) {
            int ax = x, ay = fromY;
            for (int i = 1; i <= 12; i++) {
                double t = i / 12.0;
                int bx = x + (int) Math.round(3 * t * (1 - t) * reach);
                int by = fromY + (int) Math.round((toY - fromY) * t * t * (3 - 2 * t));
                if (segment.visit(ax, ay, bx, by)) return true;
                ax = bx;
                ay = by;
            }
            return false;
        }

        void draw(GuiGraphics g, int color) {
            visit((ax, ay, bx, by) -> {
                if (ay == by) {
                    g.fill(Math.min(ax, bx), ay, Math.max(ax, bx) + 1, ay + 2, color);
                } else {
                    var pose = g.pose();
                    pose.pushPose();
                    pose.translate(ax, ay, 0);
                    pose.mulPose(com.mojang.math.Axis.ZP.rotation((float) Math.atan2(by - ay, bx - ax)));
                    g.fill(0, 0, (int) Math.ceil(Math.hypot(bx - ax, by - ay)) + 1, 2, color);
                    pose.popPose();
                }
                return false;
            });
        }

        boolean contains(double x, double y) {
            if (!overlaps((int) x - 5, (int) y - 5, (int) x + 5, (int) y + 5)) return false;
            return visit((ax, ay, bx, by) -> {
                double dx = bx - ax, dy = by - ay;
                double t = Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / Math.max(1, dx * dx + dy * dy)));
                double ex = x - ax - t * dx, ey = y - ay - t * dy;
                return ex * ex + ey * ey <= 25;
            });
        }
    }

    private BehaviorBoard() {}
}
