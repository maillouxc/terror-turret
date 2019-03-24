package edu.fgcu.scaryturret.network.webrtc.dtos

import com.google.gson.annotations.SerializedName

data class CallRequestOptionsDto(
        @SerializedName("force_hw_vcodec")
        val forceHwVcodec: Boolean? = null,
        @SerializedName("trickle_ice")
        val trickleIce: Boolean? = null,
        val vformat: Int? = null
)
