package com.github.sheauoian.croissantsys.store.product

import com.github.sheauoian.croissantsys.user.online.UserDataOnline
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface CProduct {
    fun canPurchase(user: UserDataOnline): Boolean { return true }
    fun purchase(user: UserDataOnline)
}