package net.kdt.pojavlaunch.modloaders.modpacks.models

open class ModItem(
    apiSource: Int,
    isModpack: Boolean,
    var id: String,
    var title: String,
    var description: String,
    var imageUrl: String
) : ModSource() {
    init {
        this.apiSource = apiSource
        this.isModpack = isModpack
    }

    override fun toString(): String {
        return "ModItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", apiSource=" + apiSource +
                ", isModpack=" + isModpack +
                '}'
    }

    fun getIconCacheTag(): String = "${apiSource}_$id"
}
