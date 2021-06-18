# Armeria+Guice Server ![Tests](https://github.com/AngerM/ag_server/actions/workflows/cd.yml/badge.svg) ![MavenBadge](https://maven-badges.herokuapp.com/maven-central/dev.angerm.ag_server/base/badge.svg?style=flat)

This is an opinionated set of Kotlin/Java components that combine [Armeria](https://armeria.dev/), [Guice](https://github.com/google/guice) and a few other libraries and utilites in a way I think make sense.

You can selectively use the parts you want or add in your own modules and config as desired.

See the example folder for a simple example. The Dockerfile also builds the example app

Available on [maven central](https://search.maven.org/search?q=g:dev.angerm.ag_server). Example:
```
dependencies {
    ['base',
    'grpc',
    'redis'
    ].each {
        implementation "dev.angerm.ag_server:${it}:<VERSION_HERE>"
    }
}
```
