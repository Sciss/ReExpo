[![Build Status](https://github.com/Sciss/ReExpo/workflows/Scala%20CI/badge.svg?branch=main)](https://github.com/Sciss/ReExpo/actions?query=workflow%3A%22Scala+CI%22)

# ReExpo

A work-in-progress for parsing data from Research Catalogue expositions.

(C)opyright 2023 by Hanns Holger Rutz. All rights reserved. This project is released under the
[GNU Affero General Public License](https://github.com/Sciss/ReExpo/blob/main/LICENSE) v3+ and
comes with absolutely no warranties.
To contact the author, send an e-mail to `contact at sciss.de`.

## requirements

Currently relies on `curl` being installed, as I cannot make `sttp` to properly handle the cookies, yet.

## building

Builds with [sbt](https://www.scala-sbt.org/) against Scala 3. See options: `sbt 'run --help'`. E.g.

    sbt 'run --expo-id 835089 --weave-id 835129'

## notes

- [rcedit](https://github.com/grrrr/rcedit) - Python project that has figured out a substantial part of the RC's REST interface
