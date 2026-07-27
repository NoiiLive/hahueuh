package net.noiilive.hahueuh.client.gui;

public enum BookTab {
    INFO("hahueuh.gui.tab.info"),
    DIVINE_PROTECTIONS("hahueuh.gui.skills.divine_protections"),
    MAGIC_SKILLS("hahueuh.gui.skills.magic_skills"),
    AUTHORITIES("hahueuh.gui.skills.authorities"),
    MISC_SKILLS("hahueuh.gui.skills.misc_skills");

    public final String translationKey;

    BookTab(String translationKey) {
        this.translationKey = translationKey;
    }
}
