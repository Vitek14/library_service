package com.example.Serializers.rent

import com.example.models.rent.RentRequest
import kotlinx.serialization.Serializable

@Serializable
data class ListRentResponse(val rents: List<RentRequest>)
