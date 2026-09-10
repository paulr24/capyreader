package com.jocmp.capy.accounts

enum class MaxArticles(val limit: Long?) {
    LIMIT_2500(2500L),
    LIMIT_5000(5000L),
    LIMIT_10000(10000L),
    LIMIT_25000(25000L),
    UNLIMITED(null);

    companion object {
        val default = UNLIMITED
    }
}
