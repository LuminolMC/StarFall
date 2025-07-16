package moe.luminolmc.data

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class BranchInfo (
    @SerializedName("branch_name")
    val name: String,
    @SerializedName("mc_versions")
    val mcVersion: String
){
}