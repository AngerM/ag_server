# Armeria+Guice Server ![Tests](https://github.com/AngerM/ag_server/actions/workflows/cd.yml/badge.svg) <a href="https://search.maven.org/search?q=g:dev.angerm.ag_server"><img src="https://img.shields.io/maven-central/v/dev.angerm.ag_server/base.svg?label=version" /></a>


This is an opinionated set of Kotlin/Java components that combine [Armeria](https://armeria.dev/), [Guice](https://github.com/google/guice) and a few other libraries and utilites in a way I think make sense.

You can selectively use the parts you want or add in your own modules and config as desired.

The [wiki page](https://github.com/AngerM/ag_server/wiki) has walkthroughs showing how to use the components AG Server Provides.

See the example folder for a simple example. The Dockerfile within also builds the example app

Available on [maven central](https://search.maven.org/search?q=g:dev.angerm.ag_server). Example:
```
dependencies {
    ['base',
    'grpc',
    'redis'
    ].each {
        implementation "dev.angerm.ag_server:${it}:1.2.0
    }
}
```
