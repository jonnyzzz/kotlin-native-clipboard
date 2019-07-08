import de.undercouch.gradle.tasks.download.Download
import java.util.*

plugins {
  id("de.undercouch.download")
}

val libzVersion = "1.2.11"
val libzURL = "https://www.zlib.net/zlib-$libzVersion.tar.gz"

val libzBase = File(buildDir, "libz")
val libzSource = File(libzBase, libzURL.split("/").last())
val libzUnpacked = File(libzBase, libzURL.split("/").last().removeSuffix(".tar.gz"))
val libzPrefix = File(libzBase, libzURL.split("/").last().removeSuffix(".tar.gz") + "-bin")

//TODO: download only one platform
val download = tasks.create<Download>("libz_download") {
  inputs.property("url", libzURL)
  outputs.file(libzSource)

  src(libzURL)
  dest(libzSource)
  overwrite(false)
}

val unpack = tasks.create("libz_unpack") {
  dependsOn(download)

  inputs.file(libzSource)
  outputs.file(File(libzUnpacked, "FAQ"))

  // Copy task incremental check triggers task when unneeded
  // because of created files during the build
  doFirst {
    delete(libzUnpacked)

    copy {
      from({ tarTree(resources.gzip(libzSource)) })
      into(libzUnpacked)

      includeEmptyDirs = false
      eachFile {
        path = path.split("/", limit = 2)[1]
      }
    }
  }
}

fun Exec.setupZLibEnvironment() {
  val outputFile = File(buildDir, "task-$name.output")
  doLast { outputFile.writeText("done ${Date()}")}
  outputs.file(outputFile)

  infix fun String.env(value: Any) {
    doFirst {
      environment(this@env, value.toString())
    }
    inputs.property(this, value)
  }

  "CPPFLAGS" env "-mmacosx-version-min=10.11"
}

val configure = tasks.create<Exec>("libz_configure") {
  dependsOn(unpack)

  setupZLibEnvironment()
  commandLine("/bin/sh", "-c", "./configure --static --prefix=$libzPrefix --archs=\"-arch x86_64\"")
  workingDir(libzUnpacked)
}

val make = tasks.create<Exec>("libz_make") {
  dependsOn(configure)

  setupZLibEnvironment()
  commandLine("/bin/sh", "-c", "make install prefix=$libzPrefix")
  workingDir(libzUnpacked)
}


val zlib by configurations.creating

artifacts.add(zlib.name, libzPrefix) {
  builtBy(make)
}

