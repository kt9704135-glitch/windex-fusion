package dev.windex.fusion;

public enum Module {
    SPAWNER("Spawner finder", "Smoke + flame correlation, plus known spawner blocks"),
    PLAYER("Player radar", "Visible players + unverified light-change activity"),
    SUS("Sus surface", "Pins directly above underground evidence"),
    DEEPSLATE("Subsurface overlay", "Shows loaded-world hints, even below deepslate"),
    CAVE("Cave analyzer", "Scores air pockets and tunnel-like formations"),
    TERRAIN("Terrain analyzer", "Marks artificial-looking surface changes"),
    STORAGE("Storage radar", "Lists storage block entities in loaded chunks"),
    AUTO_TOTEM("Auto Totem", "Refills your offhand using an available totem"),
    AUTO_SPRINT("Auto Sprint", "Sprints while moving forward when possible");

    public final String title;
    public final String description;

    Module(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
