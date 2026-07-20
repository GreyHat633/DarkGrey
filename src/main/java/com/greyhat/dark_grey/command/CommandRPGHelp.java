//
// Decompiled by Procyon v0.6.0
//

package com.greyhat.dark_grey.command;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class CommandRPGHelp extends CommandBase {

    private static final Map<String, String[]> HELP_DOCS;

    public String getCommandName() {
        return "rpghelp";
    }

    public String getCommandUsage(final ICommandSender sender) {
        return "/rpghelp <\u7ec4\u4ef6\u540d\u79f0>";
    }

    public int getRequiredPermissionLevel() {
        return 0;
    }

    public void processCommand(final ICommandSender sender, final String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(
                (IChatComponent) new ChatComponentText("§c\u7528\u6cd5: " + this.getCommandUsage(sender)));
            sender.addChatMessage(
                (IChatComponent) new ChatComponentText(
                    "§e\u5f53\u524d\u652f\u6301\u8bf4\u660e\u7684\u7ec4\u4ef6: §f\u91cd\u51fb, \u5438\u8840, \u8d85\u65b0\u661f, \u8679\u4e4b\u613f, \u5706\u73af\u4e4b\u7406"));
            return;
        }
        final String compName = args[0];
        if (CommandRPGHelp.HELP_DOCS.containsKey(compName)) {
            for (final String line : CommandRPGHelp.HELP_DOCS.get(compName)) {
                sender.addChatMessage((IChatComponent) new ChatComponentText(line));
            }
        } else {
            sender.addChatMessage(
                (IChatComponent) new ChatComponentText(
                    "§c\u672a\u627e\u5230\u7ec4\u4ef6 '" + compName + "' \u7684\u8bf4\u660e\u4e66\u3002"));
        }
    }

    static {
        (HELP_DOCS = new HashMap<String, String[]>()).put(
            "\u91cd\u51fb",
            new String[] { "§6=== \u7ec4\u4ef6\u8bf4\u660e: \u91cd\u51fb ===",
                "§b\u529f\u80fd: §f\u6bcf\u9694\u6307\u5b9a\u79d2\u6570\uff0c\u4f7f\u4e0b\u4e00\u6b21\u8fd1\u6218\u547d\u4e2d\u8ffd\u52a0\u5f53\u524d\u624b\u6301\u6b66\u5668\u653b\u51fb\u529b\u500d\u7387\u4f24\u5bb3\u3002",
                "§b\u53ef\u7528\u53c2\u6570:",
                "§f- §eintervalSeconds§f: \u89e6\u53d1\u95f4\u9694\u79d2\u6570\uff0c\u9ed8\u8ba45\u3002",
                "§f- §emultiplier§f: \u624b\u6301\u6b66\u5668\u653b\u51fb\u529b\u7684\u9644\u52a0\u4f24\u5bb3\u500d\u7387\uff0c\u9ed8\u8ba44\u3002",
                "§7\u51b7\u5374\u7ed3\u675f\u65f6\u4f1a\u63d0\u793a\u6301\u6709\u8005\uff0c\u4e0b\u4e00\u6b21\u8fd1\u6218\u547d\u4e2d\u5c06\u89e6\u53d1\u91cd\u51fb\u3002" });
        CommandRPGHelp.HELP_DOCS.put(
            "\u5438\u8840",
            new String[] { "§6=== \u7ec4\u4ef6\u8bf4\u660e: \u5438\u8840 ===",
                "§b\u529f\u80fd: §f\u653b\u51fb\u65f6\u5c06\u9020\u6210\u4f24\u5bb3\u7684\u767e\u5206\u6bd4\u8f6c\u5316\u4e3a\u81ea\u8eab\u751f\u547d\u503c\u3002",
                "§b\u53ef\u7528\u53c2\u6570:",
                "§f- §epercent§f: \u5438\u8840\u767e\u5206\u6bd4\u3002\u4f8b\u5982 percent=0.15 \u8868\u793a15%\u3002" });
        CommandRPGHelp.HELP_DOCS.put(
            "\u8d85\u65b0\u661f",
            new String[] { "§6=== \u5957\u88c5\u8bf4\u660e: \u8d85\u65b0\u661f ===",
                "§b\u529f\u80fd: §f2\u4ef6\u5957\u83b7\u5f97\u72ec\u7acb\u51b7\u5374\u7684\u6b66\u5668\u653b\u51fb\u529b\u500d\u7387\u91cd\u51fb\uff1b4\u4ef6\u5957\u51fb\u6740\u56de\u6ee1\u8840\u91cf\u5e76\u83b7\u5f97\u5c5e\u6027\u589e\u76ca\u3002",
                "§b\u53ef\u7528\u53c2\u6570:",
                "§f- §eintervalSeconds§f: 2\u4ef6\u5957\u91cd\u51fb\u95f4\u9694\u79d2\u6570 (\u9ed8\u8ba4: 5)",
                "§f- §emultiplier§f: 2\u4ef6\u5957\u91cd\u51fb\u7684\u624b\u6301\u6b66\u5668\u653b\u51fb\u529b\u500d\u7387 (\u9ed8\u8ba4: 4)",
                "§f- §ebuffDuration§f: 4\u4ef6\u5957\u51fb\u6740\u540e\u83b7\u5f97\u7684\u836f\u6c34\u6548\u679c\u6301\u7eed\u65f6\u95f4 (\u4ee5tick\u4e3a\u5355\u4f4d, 20tick=1\u79d2, \u9ed8\u8ba4: 200)",
                "§f- §ebuffId§f: 4\u4ef6\u5957\u51fb\u6740\u540e\u83b7\u5f97\u7684\u836f\u6c34\u6548\u679cID (\u9ed8\u8ba4: 5 \u529b\u91cf)",
                "§f- §ebuffAmplifier§f: \u836f\u6c34\u6548\u679c\u7b49\u7ea7 (\u9ed8\u8ba4: 1, \u4e5f\u5c31\u662fII\u7ea7)" });
        CommandRPGHelp.HELP_DOCS.put(
            "\u8679\u4e4b\u613f",
            new String[] { "§d=== \u8679\u4e4b\u613f ===", "§b\u63cf\u8ff0: \u591a\u6bb5\u84c4\u529b\u9b54\u6cd5\u9635",
                "§a\u673a\u5236:", "§7- 1\u6bb5\u84c4\u529b (1s)\uff1a\u8f7b\u5fae\u4f24\u5bb3",
                "§7- 2\u6bb5\u84c4\u529b (2s)\uff1a\u4e2d\u7b49\u4f24\u5bb3",
                "§7- 3\u6bb5\u84c4\u529b (3s)\uff1a\u5de8\u989d\u4f24\u5bb3\uff0c\u9644\u5e26\u534e\u4e3d\u7c92\u5b50\u7279\u6548" });
        CommandRPGHelp.HELP_DOCS.put(
            "\u5706\u73af\u4e4b\u7406",
            new String[] { "§d=== \u5706\u73af\u4e4b\u7406 ===",
                "§b\u63cf\u8ff0: \u6ede\u7a7a\u5c55\u5f00\u795e\u529b\uff0c\u91ca\u653e\u5149\u77e2\u4e4b\u96e8",
                "§a\u673a\u5236:",
                "§7- \u5fc5\u987b\u5728\u6ede\u7a7a/\u98de\u884c\u72b6\u6001\u4e0b\u624d\u80fd\u62c9\u5f13\uff0c\u843d\u5730\u77ac\u95f4\u6253\u65ad\u84c4\u529b",
                "§7- \u84c4\u529b 5 \u79d2\u65f6\u5c06\u5728\u73a9\u5bb6\u9762\u524d\u51dd\u805a\u5de8\u5927\u7684\u795e\u529b",
                "§7- \u6d88\u8017[\u5149\u4e4b\u77e2]\u53ef\u5c04\u51fa 30 \u652f\u5149\u77e2\u5f62\u6210\u8303\u56f4\u6253\u51fb" });
        CommandRPGHelp.HELP_DOCS.put(
            "\u8679\u4e4b\u613f",
            new String[] { "§d=== \u8679\u4e4b\u613f ===",
                "§b\u63cf\u8ff0: \u968f\u7740\u84c4\u529b\u65f6\u95f4\u589e\u52a0\uff0c\u7bad\u77e2\u83b7\u5f97\u6781\u5927\u5f3a\u5316",
                "§a\u673a\u5236:", "§7- \u62c9\u5f13 1 \u79d2\uff1a\u5c04\u51fb\u4f24\u5bb3 +30%",
                "§7- \u62c9\u5f13 2 \u79d2\uff1a\u5c04\u51fb\u4f24\u5bb3 +70%",
                "§7- \u62c9\u5f13 3 \u79d2\u4ee5\u4e0a\uff1a\u5c04\u51fb\u4f24\u5bb3 +120%\uff0c\u5e76\u5728\u547d\u4e2d\u5904\u751f\u6210\u8303\u56f4\u9b54\u6cd5\u4f24\u5bb3" });
    }
}
