package net.kdt.pojavlaunch.modloaders.modpacks.models

import java.util.Arrays

class ModDetail(
    item: ModItem,
    var versionNames: Array<String?>,
    var mcVersionNames: Array<String?>,
    var versionUrls: Array<String?>,
    var versionHashes: Array<String?>
) : ModItem(item.apiSource, item.isModpack, item.id, item.title, item.description, item.imageUrl) {
    init {
        for (i in versionNames.indices) {
            if (!versionNames[i]!!.contains(mcVersionNames[i]!!)) {
                versionNames[i] = "${versionNames[i]} - ${mcVersionNames[i]}"
            }
        }
    }

    override fun toString(): String {
        return "ModDetail{" +
                "versionNames=" + Arrays.toString(versionNames) +
                ", mcVersionNames=" + Arrays.toString(mcVersionNames) +
                ", versionIds=" + Arrays.toString(versionUrls) +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", apiSource=" + apiSource +
                ", isModpack=" + isModpack +
                '}'
    }
}
