package com.lkps.ctApp.utils.mapper.core

interface Mapper<I, O> {
    fun map(input: I): O
}