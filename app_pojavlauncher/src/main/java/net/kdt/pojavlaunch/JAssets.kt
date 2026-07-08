package net.kdt.pojavlaunch

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.Map

@Keep
class JAssets {
    @SerializedName("map_to_resources") var mapToResources: Boolean = false
    var objects: Map<String, JAssetInfo>? = null
    var virtual: Boolean = false
}
