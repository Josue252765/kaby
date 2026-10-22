package com.mofoxx.kaby.domain

import java.math.BigDecimal
import java.math.RoundingMode

object MovementPolicy {
    fun validate(
        direction: String,
        nature: String,
        currency: String,
        amount: BigDecimal,
        category: String,
        thirdParty: String?,
    ) {
        require(direction in setOf("GASTO", "INGRESO"))
        require(nature in setOf("NORMAL", "AJUSTE_CUADRE", "TURNO_BOLSO"))
        require(currency in setOf("Bs", "USDT"))
        require(amount > BigDecimal.ZERO)
        require(category.isNotBlank())
        if (direction == "INGRESO" && nature == "NORMAL") {
            require(currency == "Bs") { "Otra plata que entra se registra en Bs." }
            require(!thirdParty.isNullOrBlank()) { "Indica de quién y por qué entró el dinero." }
        }
    }
}

object P2pPremium {
    fun calculate(netBs: String, usdt: String, bcv: String): String =
        BigDecimal(netBs).subtract(BigDecimal(usdt).multiply(BigDecimal(bcv)))
            .setScale(2, RoundingMode.HALF_UP).toPlainString()
}

object BcvValuation {
    fun usdEquivalent(bolivares: BigDecimal, confirmedRate: BigDecimal?): BigDecimal? {
        if (confirmedRate == null || confirmedRate <= BigDecimal.ZERO) return null
        return bolivares.divide(confirmedRate, 2, RoundingMode.HALF_UP)
    }
}
