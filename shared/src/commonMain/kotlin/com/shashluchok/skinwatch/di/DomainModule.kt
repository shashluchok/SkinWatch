package com.shashluchok.skinwatch.di

import org.koin.dsl.module

/**
 * Reserved for pure business-logic bindings (use cases/interactors) once the project has any --
 * repository contracts are bound to their implementations in [dataModule], since those
 * implementations live in `data.*`.
 */
internal val domainModule = module {}
