$lines = Get-Content "E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\DarkGrey.java"
$outLines = @()
foreach ($line in $lines) {
    if ($line -match 'ComponentRegistry.register\("粉碎之骨"') {
        $outLines += '        ComponentRegistry.register("碎骨瓶", (Supplier<IRPGComponent>) com.greyhat.dark_grey.component.ComponentBoneFlask::new);'
        $outLines += $line
    } elseif ($line -match 'EntityStarBullet\.class') {
        $outLines += $line
    } elseif ($line -match '"star_bullet"') {
        $outLines += $line
    } elseif ($line -match '10,') {
        $outLines += $line
    } elseif ($line -match '\(Object\) DarkGrey\.instance,') {
        $outLines += $line
    } elseif ($line -match '64,') {
        $outLines += $line
    } elseif ($line -match 'true\);') {
        $outLines += $line
        # Check if we just matched the end of EntityStarBullet
        if ($outLines[-7] -match 'EntityStarBullet') {
            $outLines += '        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity((Class) com.greyhat.dark_grey.entity.EntityBoneFlask.class, "bone_flask", 11, (Object) DarkGrey.instance, 64, 10, true);'
            $outLines += '        cpw.mods.fml.common.registry.EntityRegistry.registerModEntity((Class) com.greyhat.dark_grey.entity.EntityBoneSpikesField.class, "bone_spikes_field", 12, (Object) DarkGrey.instance, 64, 10, false);'
        }
    } else {
        $outLines += $line
    }
}
$outLines | Set-Content "E:\Java\MinecraftMod\DarkGrey\src\main\java\com\greyhat\dark_grey\DarkGrey.java"
